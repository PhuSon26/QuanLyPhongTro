package com.example.renthub.ui.room;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renthub.R;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class LandlordRequestAdapter extends RecyclerView.Adapter<LandlordRequestAdapter.ViewHolder> {

    public interface OnLandlordActionListener {
        void onApprove(RentalRequest request);
        void onReject(RentalRequest request);
    }

    private final List<RentalRequest> requestList;
    private final OnLandlordActionListener listener;

    public LandlordRequestAdapter(List<RentalRequest> requestList, OnLandlordActionListener listener) {
        this.requestList = requestList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_landlord_rental_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RentalRequest request = requestList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvLandlordReqRoomName.setText(request.getRoomName() != null ? request.getRoomName() : "Yêu cầu thuê phòng");

        String price = request.getRoomPrice() != null ? request.getRoomPrice() : "0";
        if (!price.contains("đ") && !price.contains("VND")) {
            price = price + "đ/tháng";
        }
        holder.tvLandlordReqRoomPrice.setText(price);

        holder.tvLandlordReqTenantName.setText("Người thuê: " + (request.getTenantName() != null ? request.getTenantName() : "Ẩn danh"));
        holder.tvLandlordReqTenantPhone.setText("SĐT: " + (request.getTenantPhone() != null ? request.getTenantPhone() : "Chưa cập nhật"));

        String startDate = request.getStartDate() != null ? request.getStartDate() : "Sớm nhất";
        String duration = request.getDuration() != null ? request.getDuration() : "6 tháng";
        holder.tvLandlordReqStartDate.setText("Dự kiến vào ở: " + startDate + " (Hạn thuê: " + duration + ")");

        if (request.getOccupants() != null && !request.getOccupants().isEmpty()) {
            StringBuilder sb = new StringBuilder("Người ở cùng: ");
            for (int i = 0; i < request.getOccupants().size(); i++) {
                sb.append(request.getOccupants().get(i));
                if (i < request.getOccupants().size() - 1) sb.append(", ");
            }
            holder.tvLandlordReqOccupants.setText(sb.toString());
            holder.tvLandlordReqOccupants.setVisibility(View.VISIBLE);
        } else {
            holder.tvLandlordReqOccupants.setText("Người ở cùng: Không có (ở 1 mình)");
            holder.tvLandlordReqOccupants.setVisibility(View.VISIBLE);
        }

        if (request.getNote() != null && !request.getNote().trim().isEmpty()) {
            holder.tvLandlordReqNote.setText("Ghi chú: " + request.getNote());
            holder.tvLandlordReqNote.setVisibility(View.VISIBLE);
        } else {
            holder.tvLandlordReqNote.setVisibility(View.GONE);
        }

        // Gọi điện thoại
        holder.btnCallApplicant.setOnClickListener(v -> {
            String phone = request.getTenantPhone();
            if (phone != null && !phone.isEmpty()) {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                context.startActivity(dialIntent);
            } else {
                Toast.makeText(context, "Không tìm thấy số điện thoại của người thuê", Toast.LENGTH_SHORT).show();
            }
        });

        // Duyệt thuê
        holder.btnApproveRequest.setOnClickListener(v -> {
            if (listener != null) {
                listener.onApprove(request);
            }
        });

        // Từ chối
        holder.btnRejectRequest.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReject(request);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requestList != null ? requestList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLandlordReqRoomName, tvLandlordReqRoomPrice, tvLandlordReqStatus;
        TextView tvLandlordReqTenantName, tvLandlordReqTenantPhone;
        TextView tvLandlordReqStartDate, tvLandlordReqOccupants, tvLandlordReqNote;
        ImageButton btnCallApplicant;
        MaterialButton btnRejectRequest, btnApproveRequest;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLandlordReqRoomName = itemView.findViewById(R.id.tvLandlordReqRoomName);
            tvLandlordReqRoomPrice = itemView.findViewById(R.id.tvLandlordReqRoomPrice);
            tvLandlordReqStatus = itemView.findViewById(R.id.tvLandlordReqStatus);
            tvLandlordReqTenantName = itemView.findViewById(R.id.tvLandlordReqTenantName);
            tvLandlordReqTenantPhone = itemView.findViewById(R.id.tvLandlordReqTenantPhone);
            tvLandlordReqStartDate = itemView.findViewById(R.id.tvLandlordReqStartDate);
            tvLandlordReqOccupants = itemView.findViewById(R.id.tvLandlordReqOccupants);
            tvLandlordReqNote = itemView.findViewById(R.id.tvLandlordReqNote);
            btnCallApplicant = itemView.findViewById(R.id.btnCallApplicant);
            btnRejectRequest = itemView.findViewById(R.id.btnRejectRequest);
            btnApproveRequest = itemView.findViewById(R.id.btnApproveRequest);
        }
    }
}
