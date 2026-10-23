package com.example.renthub.ui.room;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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

    public RoomAdapter(List<Room> roomList) {
        this(roomList, null);
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
        holder.tvRoomArea.setText(room.getArea().contains("m²") ? room.getArea() : room.getArea() + " m²");

        String priceText = room.getPrice().contains("đ") ? room.getPrice() : room.getPrice() + "đ/tháng";
        holder.tvRoomPrice.setText(priceText);

        holder.tvRoomStatus.setText(room.getStatus());

        int cardBgColor;
        int textColor;

        String status = room.getStatus() != null ? room.getStatus().toLowerCase() : "";
        if (status.contains("thuê") || status.contains("occupied")) {
            cardBgColor = Color.parseColor("#E8F0FE");
            textColor = Color.parseColor("#2563A6");
            holder.tvRoomStatus.setText("Đang thuê");
        } else if (status.contains("trì") || status.contains("maintenance")) {
            cardBgColor = Color.parseColor("#FFF3E0");
            textColor = Color.parseColor("#B7791F");
            holder.tvRoomStatus.setText("Bảo trì");
        } else {
            cardBgColor = Color.parseColor("#E2F5EA");
            textColor = Color.parseColor("#148369");
            holder.tvRoomStatus.setText("Còn trống");
        }

        holder.cardRoomStatus.setCardBackgroundColor(cardBgColor);
        holder.tvRoomStatus.setTextColor(textColor);

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
        TextView tvRoomName, tvRoomLocation, tvRoomArea, tvRoomPrice, tvRoomStatus;
        MaterialCardView cardRoomStatus;
        MaterialButton btnViewRoom, btnDeleteRoom;

        RoomViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomName = itemView.findViewById(R.id.tvRoomName);
            tvRoomLocation = itemView.findViewById(R.id.tvRoomLocation);
            tvRoomArea = itemView.findViewById(R.id.tvRoomArea);
            tvRoomPrice = itemView.findViewById(R.id.tvRoomPrice);
            tvRoomStatus = itemView.findViewById(R.id.tvRoomStatus);
            cardRoomStatus = itemView.findViewById(R.id.cardRoomStatus);

            btnViewRoom = itemView.findViewById(R.id.btnViewRoom);
            btnDeleteRoom = itemView.findViewById(R.id.btnDeleteRoom);
        }
    }
}