package com.example.renthub.ui.community;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renthub.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter danh sách bài đăng thông báo trong mục Community (Bảng tin).
 * Sử dụng ViewHolder pattern tối ưu theo chuẩn AGENTS.md.
 */
public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.AnnouncementViewHolder> {

    public interface OnAnnouncementClickListener {
        void onItemClick(Announcement announcement);
        void onDeleteClick(Announcement announcement);
    }

    private final List<Announcement> announcementList;
    private final boolean isLandlord;
    private final OnAnnouncementClickListener listener;

    public AnnouncementAdapter(List<Announcement> announcementList, boolean isLandlord, OnAnnouncementClickListener listener) {
        this.announcementList = announcementList;
        this.isLandlord = isLandlord;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AnnouncementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_announcement, parent, false);
        return new AnnouncementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnnouncementViewHolder holder, int position) {
        Announcement announcement = announcementList.get(position);
        holder.bind(announcement, isLandlord, listener);
    }

    @Override
    public int getItemCount() {
        return announcementList != null ? announcementList.size() : 0;
    }

    /**
     * ViewHolder quản lý hiển thị từng thẻ thông báo
     */
    static class AnnouncementViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCategoryBadge;
        private final TextView tvTargetScopeBadge;
        private final TextView tvAnnouncementDate;
        private final ImageButton btnDeleteAnnouncement;
        private final TextView tvAnnouncementTitle;
        private final TextView tvAnnouncementContent;
        private final TextView tvLandlordName;

        public AnnouncementViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryBadge = itemView.findViewById(R.id.tvCategoryBadge);
            tvTargetScopeBadge = itemView.findViewById(R.id.tvTargetScopeBadge);
            tvAnnouncementDate = itemView.findViewById(R.id.tvAnnouncementDate);
            btnDeleteAnnouncement = itemView.findViewById(R.id.btnDeleteAnnouncement);
            tvAnnouncementTitle = itemView.findViewById(R.id.tvAnnouncementTitle);
            tvAnnouncementContent = itemView.findViewById(R.id.tvAnnouncementContent);
            tvLandlordName = itemView.findViewById(R.id.tvLandlordName);
        }

        public void bind(Announcement item, boolean isLandlord, OnAnnouncementClickListener listener) {
            if (item == null) return;

            tvAnnouncementTitle.setText(item.getTitle() != null ? item.getTitle() : "Thông báo");
            tvAnnouncementContent.setText(item.getContent() != null ? item.getContent() : "");

            // Định dạng ngày đăng
            if (item.getCreatedAt() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                tvAnnouncementDate.setText(sdf.format(new Date(item.getCreatedAt())));
            } else {
                tvAnnouncementDate.setText("");
            }

            // Tên người đăng
            String landlordName = item.getLandlordName() != null && !item.getLandlordName().isEmpty()
                    ? item.getLandlordName()
                    : "Chủ trọ";
            tvLandlordName.setText("Đăng bởi: " + landlordName);

            // Thể loại thông báo và màu sắc tương ứng
            String category = item.getCategory() != null ? item.getCategory() : "Thông báo chung";
            tvCategoryBadge.setText(category.toUpperCase(Locale.getDefault()));
            styleCategoryBadge(category);

            // Phạm vi thông báo
            if (Announcement.TARGET_ALL.equalsIgnoreCase(item.getTargetType())) {
                tvTargetScopeBadge.setText("Toàn bộ người thuê");
                tvTargetScopeBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EFF6FF")));
                tvTargetScopeBadge.setTextColor(Color.parseColor("#2563EB"));
            } else {
                List<String> roomNames = item.getTargetRoomNames();
                if (roomNames != null && !roomNames.isEmpty()) {
                    String roomsText = android.text.TextUtils.join(", ", roomNames);
                    tvTargetScopeBadge.setText("Phòng: " + roomsText);
                } else {
                    tvTargetScopeBadge.setText("Phòng chỉ định");
                }
                tvTargetScopeBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FEF3C7")));
                tvTargetScopeBadge.setTextColor(Color.parseColor("#D97706"));
            }

            // Nút xóa: Chỉ chủ trọ mới có quyền xóa
            if (isLandlord) {
                btnDeleteAnnouncement.setVisibility(View.VISIBLE);
                btnDeleteAnnouncement.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteClick(item);
                    }
                });
            } else {
                btnDeleteAnnouncement.setVisibility(View.GONE);
            }

            // Bấm vào thẻ để xem chi tiết
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }

        private void styleCategoryBadge(String category) {
            String catLower = category.toLowerCase(Locale.getDefault());
            if (catLower.contains("khẩn")) {
                tvCategoryBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
                tvCategoryBadge.setTextColor(Color.parseColor("#DC2626"));
            } else if (catLower.contains("bảo trì") || catLower.contains("sửa")) {
                tvCategoryBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEDD5")));
                tvCategoryBadge.setTextColor(Color.parseColor("#EA580C"));
            } else if (catLower.contains("nhắc") || catLower.contains("tiền")) {
                tvCategoryBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F3E8FF")));
                tvCategoryBadge.setTextColor(Color.parseColor("#7C3AED"));
            } else if (catLower.contains("quy")) {
                tvCategoryBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E7FF")));
                tvCategoryBadge.setTextColor(Color.parseColor("#4338CA"));
            } else {
                tvCategoryBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0F2EF")));
                tvCategoryBadge.setTextColor(Color.parseColor("#148369"));
            }
        }
    }
}

