package com.example.renthub.ui.room;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renthub.R;

import java.util.List;

public class RentalRequestAdapter extends RecyclerView.Adapter<RentalRequestAdapter.ViewHolder> {

    public interface OnRequestClickListener {
        void onRequestClick(RentalRequest request);
    }

    private final List<RentalRequest> requestList;
    private final OnRequestClickListener listener;

    public RentalRequestAdapter(List<RentalRequest> requestList, OnRequestClickListener listener) {
        this.requestList = requestList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rental_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RentalRequest request = requestList.get(position);

        holder.tvReqRoomName.setText(request.getRoomName() != null ? request.getRoomName() : "Yêu cầu thuê phòng");

        String priceLoc = (request.getRoomPrice() != null ? request.getRoomPrice() + " đ/tháng" : "")
                + (request.getRoomLocation() != null && !request.getRoomLocation().isEmpty() ? " • " + request.getRoomLocation() : "");
        holder.tvReqPriceLocation.setText(priceLoc);

        String startDuration = "Dự kiến ở: " + (request.getStartDate() != null ? request.getStartDate() : "---")
                + " (" + (request.getDuration() != null ? request.getDuration() : "6 tháng") + ")";
        holder.tvReqStartDate.setText(startDuration);

        // Hiển thị trạng thái kết quả
        String status = request.getStatus() != null ? request.getStatus().toUpperCase() : "PENDING";
        if ("APPROVED".equals(status) || status.contains("DUYỆT") || status.contains("CHẤP THUẬN")) {
            holder.tvReqStatusBadge.setText("🟢 Đã chấp thuận");
            holder.tvReqStatusBadge.setTextColor(Color.parseColor("#047857"));
            setBadgeBackground(holder.tvReqStatusBadge, "#ECFDF5", "#A7F3D0");
            holder.tvReqResultNote.setText("Chúc mừng! Chủ trọ đã đồng ý");
            holder.tvReqResultNote.setTextColor(Color.parseColor("#047857"));
        } else if ("REJECTED".equals(status) || status.contains("TỪ CHỐI")) {
            holder.tvReqStatusBadge.setText("🔴 Bị từ chối");
            holder.tvReqStatusBadge.setTextColor(Color.parseColor("#B91C1C"));
            setBadgeBackground(holder.tvReqStatusBadge, "#FEF2F2", "#FECACA");
            holder.tvReqResultNote.setText(request.getRejectReason() != null ? request.getRejectReason() : "Chủ trọ đã từ chối");
            holder.tvReqResultNote.setTextColor(Color.parseColor("#B91C1C"));
        } else {
            // Mặc định: Đang chờ duyệt (PENDING)
            holder.tvReqStatusBadge.setText("🟡 Đang chờ duyệt");
            holder.tvReqStatusBadge.setTextColor(Color.parseColor("#B45309"));
            setBadgeBackground(holder.tvReqStatusBadge, "#FFFBEB", "#FDE68A");
            holder.tvReqResultNote.setText("Đang chờ chủ trọ phản hồi...");
            holder.tvReqResultNote.setTextColor(Color.parseColor("#B45309"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRequestClick(request);
            }
        });
    }

    private void setBadgeBackground(TextView tv, String bgColor, String strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(16);
        drawable.setColor(Color.parseColor(bgColor));
        drawable.setStroke(2, Color.parseColor(strokeColor));
        tv.setBackground(drawable);
    }

    @Override
    public int getItemCount() {
        return requestList != null ? requestList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvReqRoomName, tvReqStatusBadge, tvReqPriceLocation, tvReqStartDate, tvReqResultNote;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReqRoomName = itemView.findViewById(R.id.tvReqRoomName);
            tvReqStatusBadge = itemView.findViewById(R.id.tvReqStatusBadge);
            tvReqPriceLocation = itemView.findViewById(R.id.tvReqPriceLocation);
            tvReqStartDate = itemView.findViewById(R.id.tvReqStartDate);
            tvReqResultNote = itemView.findViewById(R.id.tvReqResultNote);
        }
    }
}
