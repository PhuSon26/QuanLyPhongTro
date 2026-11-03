package com.example.renthub.ui.issue;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renthub.R;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter hiển thị danh sách các sự cố báo cáo phòng trọ
 * Hỗ trợ hiển thị tên phòng, loại sự cố, mức độ ưu tiên, trạng thái và chỉ báo hình ảnh.
 */
public class IssueAdapter extends RecyclerView.Adapter<IssueAdapter.IssueViewHolder> {

    private List<Issue> issueList;
    private Map<String, String> roomNamesMap = new HashMap<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Issue issue);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public IssueAdapter(List<Issue> issueList) {
        this.issueList = issueList;
    }

    /**
     * Cập nhật danh sách ánh xạ Mã phòng -> Tên phòng
     */
    public void setRoomNamesMap(Map<String, String> map) {
        if (map != null) {
            this.roomNamesMap = map;
            notifyDataSetChanged();
        }
    }

    /**
     * Cập nhật danh sách sự cố mới
     */
    public void updateData(List<Issue> newList) {
        this.issueList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public IssueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_issue, parent, false);
        return new IssueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IssueViewHolder holder, int position) {
        Issue issue = issueList.get(position);

        // 1. Tiêu đề sự cố
        holder.tvTitle.setText(issue.getTitle() != null && !issue.getTitle().isEmpty() ? issue.getTitle() : "Sự cố không có tiêu đề");

        // 2. Tên phòng trọ
        String roomId = issue.getRoomId();
        if (roomId != null && roomNamesMap.containsKey(roomId)) {
            holder.tvRoom.setText(roomNamesMap.get(roomId));
        } else if (roomId != null && !roomId.trim().isEmpty()) {
            holder.tvRoom.setText("Phòng " + roomId);
        } else {
            holder.tvRoom.setText("Chưa gán phòng");
        }

        // 3. Loại sự cố
        String type = issue.getType();
        if (type != null && !type.trim().isEmpty()) {
            holder.tvType.setText("Loại: " + type.trim());
        } else {
            holder.tvType.setText("Sự cố chung");
        }

        // 4. Thời gian gửi
        if (issue.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.tvDate.setText(sdf.format(issue.getCreatedAt().toDate()));
        } else {
            holder.tvDate.setText("Chưa cập nhật ngày");
        }

        // 5. Trạng thái sự cố (Badge bo góc)
        String status = issue.getStatus();
        String s = status != null ? status.trim().toUpperCase() : "PENDING";
        if (s.contains("DONE") || s.contains("RESOLVED") || s.contains("ĐÃ")) {
            holder.tvStatus.setText("Đã xử lý");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_done);
        } else if (s.contains("PROCESSING") || s.contains("PROCESS") || s.contains("ĐANG")) {
            holder.tvStatus.setText("Đang xử lý");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_processing);
        } else {
            holder.tvStatus.setText("Chờ xử lý");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
        }

        // 6. Mức độ ưu tiên
        bindPriorityUI(holder.tvPriority, issue, s);

        // 7. Chỉ báo có hình ảnh đính kèm
        if (issue.getImageUrl() != null && !issue.getImageUrl().trim().isEmpty()) {
            holder.ivHasImageIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.ivHasImageIndicator.setVisibility(View.GONE);
        }

        // 8. Bắt sự kiện click chuyển sang màn chi tiết
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(issue);
            }
        });
    }

    /**
     * Hiển thị mức độ ưu tiên với màu sắc trực quan
     */
    private void bindPriorityUI(TextView tvPriority, Issue issue, String normalizedStatus) {
        String priority = issue.getPriority();
        if (priority != null && !priority.trim().isEmpty()) {
            String p = priority.trim().toUpperCase();
            if (p.contains("KHẨN") || p.contains("URGENT")) {
                setPriorityBadge(tvPriority, "Khẩn cấp", "#DC2626", "#FEF2F2", "#FECACA");
            } else if (p.contains("CAO") || p.contains("HIGH")) {
                setPriorityBadge(tvPriority, "Ưu tiên cao", "#D97706", "#FFFBEB", "#FDE68A");
            } else if (p.contains("THẤP") || p.contains("LOW")) {
                setPriorityBadge(tvPriority, "Ưu tiên thấp", "#64748B", "#F1F5F9", "#E2E8F0");
            } else {
                setPriorityBadge(tvPriority, "Trung bình", "#0E8B75", "#E0F2EF", "#B2DFDB");
            }
            return;
        }

        // Tự động suy ra mức độ ưu tiên theo trạng thái và tính khẩn cấp của loại sự cố
        if (normalizedStatus.contains("DONE") || normalizedStatus.contains("RESOLVED") || normalizedStatus.contains("ĐÃ")) {
            setPriorityBadge(tvPriority, "Đã xong", "#64748B", "#F1F5F9", "#E2E8F0");
        } else if (normalizedStatus.contains("PROCESSING") || normalizedStatus.contains("PROCESS") || normalizedStatus.contains("ĐANG")) {
            setPriorityBadge(tvPriority, "Đang xử lý", "#0284C7", "#F0F9FF", "#BAE6FD");
        } else {
            // PENDING: Chưa xử lý
            String type = issue.getType() != null ? issue.getType().toLowerCase() : "";
            if (type.contains("điện") || type.contains("nước")) {
                setPriorityBadge(tvPriority, "Khẩn cấp", "#DC2626", "#FEF2F2", "#FECACA");
            } else {
                setPriorityBadge(tvPriority, "Ưu tiên cao", "#D97706", "#FFFBEB", "#FDE68A");
            }
        }
    }

    /**
     * Cài đặt nền và màu cho Badge ưu tiên
     */
    private void setPriorityBadge(TextView tv, String text, String textColor, String bgColor, String strokeColor) {
        tv.setText(text);
        tv.setTextColor(Color.parseColor(textColor));

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(16f);
        shape.setColor(Color.parseColor(bgColor));
        shape.setStroke(2, Color.parseColor(strokeColor));
        tv.setBackground(shape);
    }

    @Override
    public int getItemCount() {
        return issueList != null ? issueList.size() : 0;
    }

    /**
     * ViewHolder tối ưu hóa hiệu năng danh sách
     */
    public static class IssueViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvStatus, tvRoom, tvType, tvPriority;
        ImageView ivHasImageIndicator;

        public IssueViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvIssueTitle);
            tvDate = itemView.findViewById(R.id.tvIssueDate);
            tvStatus = itemView.findViewById(R.id.tvIssueStatus);
            tvRoom = itemView.findViewById(R.id.tvIssueRoom);
            tvType = itemView.findViewById(R.id.tvIssueType);
            tvPriority = itemView.findViewById(R.id.tvIssuePriority);
            ivHasImageIndicator = itemView.findViewById(R.id.ivHasImageIndicator);
        }
    }
}
