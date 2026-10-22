package com.example.renthub.ui.room;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renthub.R;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class RoomAdapter
        extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {

    private final List<Room> rooms;

    public RoomAdapter(List<Room> rooms) {
        this.rooms = rooms;
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_landlord_room, parent, false);

        return new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull RoomViewHolder holder, int position) {

        Room room = rooms.get(position);

        holder.tvRoomName.setText(room.getName());
        holder.tvRoomLocation.setText(room.getLocation());
        holder.tvRoomArea.setText(room.getArea());
        holder.tvRoomPrice.setText(room.getPrice() + "đ/tháng");
        holder.tvRoomStatus.setText(room.getStatus());

        int backgroundColor;
        int textColor;

        switch (room.getStatus()) {
            case "Đang thuê":
                backgroundColor = Color.parseColor("#E8F0FE");
                textColor = Color.parseColor("#2563A6");
                break;

            case "Bảo trì":
                backgroundColor = Color.parseColor("#FFF3E0");
                textColor = Color.parseColor("#B7791F");
                break;

            default:
                backgroundColor = Color.parseColor("#E2F5EA");
                textColor = Color.parseColor("#148369");
                break;
        }

        holder.cardStatus.setCardBackgroundColor(backgroundColor);
        holder.tvRoomStatus.setTextColor(textColor);
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    static class RoomViewHolder extends RecyclerView.ViewHolder {

        TextView tvRoomName;
        TextView tvRoomLocation;
        TextView tvRoomArea;
        TextView tvRoomPrice;
        TextView tvRoomStatus;
        MaterialCardView cardStatus;

        RoomViewHolder(@NonNull View itemView) {
            super(itemView);

            tvRoomName = itemView.findViewById(R.id.tvRoomName);
            tvRoomLocation = itemView.findViewById(R.id.tvRoomLocation);
            tvRoomArea = itemView.findViewById(R.id.tvRoomArea);
            tvRoomPrice = itemView.findViewById(R.id.tvRoomPrice);
            tvRoomStatus = itemView.findViewById(R.id.tvRoomStatus);
            cardStatus = itemView.findViewById(R.id.cardRoomStatus);
        }
    }
}