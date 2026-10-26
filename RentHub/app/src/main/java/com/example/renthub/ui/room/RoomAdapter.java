package com.example.renthub.ui.room;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.renthub.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {

    private List<Room> roomList;
    private OnRoomActionListener listener;

    public interface OnRoomActionListener {
        void onView(Room room);
        void onDelete(Room room);
    }

    public RoomAdapter(List<Room> roomList, OnRoomActionListener listener) {
        this.roomList = roomList;
        this.listener = listener;
    }

    public void updateList(List<Room> newList) {
        this.roomList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_landlord_room, parent, false);
        return new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        Room room = roomList.get(position);

        holder.tvRoomName.setText(room.getName());
        holder.tvRoomLocation.setText(room.getLocation());

        String landlordName = room.getLandlordName() != null ? room.getLandlordName() : "Chủ trọ";
        holder.tvLandlordName.setText("Chủ trọ: " + landlordName);

        // HIỂN THỊ SỐ NGƯỜI Ở
        int currentCount = room.getCurrentOccupantCount();
        int maxCount = room.getMaxOccupants();
        holder.tvOccupancy.setText("Số người ở: " + currentCount + "/" + maxCount);

        String price = room.getPrice() != null ? room.getPrice() : "";
        holder.tvRoomPrice.setText(price.contains("đ") ? price : price + "đ/tháng");

        if (room.getImageUrl() != null && !room.getImageUrl().trim().isEmpty()) {
            holder.imgRoomCover.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(holder.itemView.getContext())
                    .load(room.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.rh_ic_room)
                    .into(holder.imgRoomCover);
        } else {
            holder.imgRoomCover.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            holder.imgRoomCover.setImageResource(R.drawable.rh_ic_room);
        }

        String status = room.getStatus() != null ? room.getStatus().toLowerCase() : "";

        // BỔ SUNG NHẢY THẺ TRẠNG THÁI "ĐÃ ĐỦ NGƯỜI"
        if (status.contains("đủ") || room.isFull()) {
            holder.cardRoomStatus.setCardBackgroundColor(Color.parseColor("#FCECEC"));
            holder.tvRoomStatus.setTextColor(Color.parseColor("#C74444"));
            holder.tvRoomStatus.setText("Đã đủ người");
        } else if (status.contains("thuê") || status.contains("occupied")) {
            holder.cardRoomStatus.setCardBackgroundColor(Color.parseColor("#E8F0FE"));
            holder.tvRoomStatus.setTextColor(Color.parseColor("#2563A6"));
            holder.tvRoomStatus.setText("Đang thuê");
        } else if (status.contains("trì") || status.contains("maintenance")) {
            holder.cardRoomStatus.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
            holder.tvRoomStatus.setTextColor(Color.parseColor("#B7791F"));
            holder.tvRoomStatus.setText("Bảo trì");
        } else {
            holder.cardRoomStatus.setCardBackgroundColor(Color.parseColor("#E2F5EA"));
            holder.tvRoomStatus.setTextColor(Color.parseColor("#148369"));
            holder.tvRoomStatus.setText("Còn trống");
        }

        holder.btnViewRoom.setOnClickListener(v -> {
            if (listener != null) listener.onView(room);
        });

        holder.btnDeleteRoom.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(room);
        });
    }

    @Override
    public int getItemCount() {
        return roomList != null ? roomList.size() : 0;
    }

    static class RoomViewHolder extends RecyclerView.ViewHolder {
        ImageView imgRoomCover;
        TextView tvRoomName, tvRoomLocation, tvLandlordName, tvOccupancy, tvRoomPrice, tvRoomStatus;
        MaterialCardView cardRoomStatus;
        MaterialButton btnViewRoom, btnDeleteRoom;

        RoomViewHolder(@NonNull View itemView) {
            super(itemView);
            imgRoomCover = itemView.findViewById(R.id.imgRoomCover);
            tvRoomName = itemView.findViewById(R.id.tvRoomName);
            tvRoomLocation = itemView.findViewById(R.id.tvRoomLocation);
            tvLandlordName = itemView.findViewById(R.id.tvLandlordName);
            tvOccupancy = itemView.findViewById(R.id.tvOccupancy);
            tvRoomPrice = itemView.findViewById(R.id.tvRoomPrice);
            tvRoomStatus = itemView.findViewById(R.id.tvRoomStatus);
            cardRoomStatus = itemView.findViewById(R.id.cardRoomStatus);

            btnViewRoom = itemView.findViewById(R.id.btnViewRoom);
            btnDeleteRoom = itemView.findViewById(R.id.btnDeleteRoom);
        }
    }
}