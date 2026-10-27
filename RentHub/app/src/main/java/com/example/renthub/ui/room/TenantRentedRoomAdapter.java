package com.example.renthub.ui.room;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renthub.R;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class TenantRentedRoomAdapter extends RecyclerView.Adapter<TenantRentedRoomAdapter.ViewHolder> {

    public interface OnRentedRoomActionListener {
        void onViewDetail(Room room);
    }

    private List<Room> roomList;
    private final OnRentedRoomActionListener listener;

    public TenantRentedRoomAdapter(List<Room> roomList, OnRentedRoomActionListener listener) {
        this.roomList = roomList;
        this.listener = listener;
    }

    public void updateList(List<Room> newList) {
        this.roomList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tenant_rented_room, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Room room = roomList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvRentedRoomName.setText(room.getName() != null ? room.getName() : "Phòng trọ");
        holder.tvRentedRoomLocation.setText(room.getLocation() != null ? room.getLocation() : "Chưa có vị trí");

        String area = room.getArea() != null ? room.getArea() : "";
        if (!area.isEmpty() && !area.contains("m²")) area += " m²";
        int currentCount = room.getCurrentOccupantCount();
        int maxCount = room.getMaxOccupants();
        holder.tvRentedRoomSpecs.setText((!area.isEmpty() ? area + " • " : "") + currentCount + "/" + maxCount + " người");

        String price = room.getPrice() != null ? room.getPrice() : "0";
        if (!price.contains("đ")) price += " đ/tháng";
        holder.tvRentedRoomPrice.setText(price);

        String landlordName = room.getLandlordName() != null && !room.getLandlordName().trim().isEmpty()
                ? room.getLandlordName() : "Chưa có tên";
        holder.tvRentedLandlordName.setText(landlordName);

        String landlordPhone = room.getLandlordPhone() != null && !room.getLandlordPhone().trim().isEmpty()
                ? room.getLandlordPhone() : "Chưa có SĐT";
        holder.tvRentedLandlordPhone.setText(landlordPhone);

        // Click xem chi tiết
        View.OnClickListener detailClickListener = v -> {
            if (listener != null) {
                listener.onViewDetail(room);
            }
        };
        holder.itemView.setOnClickListener(detailClickListener);
        holder.btnRentedRoomDetail.setOnClickListener(detailClickListener);

        // Nút gọi chủ trọ
        holder.btnRentedRoomCall.setOnClickListener(v -> {
            String phone = room.getLandlordPhone();
            if (phone != null && !phone.trim().isEmpty() && !phone.equals("---")) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + phone.trim()));
                context.startActivity(callIntent);
            } else {
                Toast.makeText(context, "Chưa có số điện thoại chủ trọ", Toast.LENGTH_SHORT).show();
            }
        });

        // Nút nhắn tin SMS cho chủ trọ
        holder.btnRentedRoomSms.setOnClickListener(v -> {
            String phone = room.getLandlordPhone();
            if (phone != null && !phone.trim().isEmpty() && !phone.equals("---")) {
                Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                smsIntent.setData(Uri.parse("smsto:" + phone.trim()));
                context.startActivity(smsIntent);
            } else {
                Toast.makeText(context, "Chưa có số điện thoại chủ trọ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return roomList != null ? roomList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRentedRoomName, tvRentedRoomLocation, tvRentedRoomStatus;
        TextView tvRentedRoomSpecs, tvRentedRoomPrice;
        TextView tvRentedLandlordName, tvRentedLandlordPhone;
        MaterialButton btnRentedRoomCall, btnRentedRoomSms, btnRentedRoomDetail;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRentedRoomName = itemView.findViewById(R.id.tvRentedRoomName);
            tvRentedRoomLocation = itemView.findViewById(R.id.tvRentedRoomLocation);
            tvRentedRoomStatus = itemView.findViewById(R.id.tvRentedRoomStatus);
            tvRentedRoomSpecs = itemView.findViewById(R.id.tvRentedRoomSpecs);
            tvRentedRoomPrice = itemView.findViewById(R.id.tvRentedRoomPrice);
            tvRentedLandlordName = itemView.findViewById(R.id.tvRentedLandlordName);
            tvRentedLandlordPhone = itemView.findViewById(R.id.tvRentedLandlordPhone);
            btnRentedRoomCall = itemView.findViewById(R.id.btnRentedRoomCall);
            btnRentedRoomSms = itemView.findViewById(R.id.btnRentedRoomSms);
            btnRentedRoomDetail = itemView.findViewById(R.id.btnRentedRoomDetail);
        }
    }
}
