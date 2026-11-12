package com.example.renthub.ui.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renthub.R;
import com.example.renthub.ui.room.Room;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapter danh sách chọn phòng trọ để gửi thông báo chỉ định theo phòng.
 * Sử dụng ViewHolder pattern tối ưu theo chuẩn AGENTS.md.
 */
public class RoomSelectAdapter extends RecyclerView.Adapter<RoomSelectAdapter.RoomSelectViewHolder> {

    public interface OnSelectionChangeListener {
        void onSelectionChanged(int selectedCount);
    }

    private final List<Room> roomList;
    private final Set<String> selectedRoomIds = new HashSet<>();
    private final OnSelectionChangeListener selectionChangeListener;

    public RoomSelectAdapter(List<Room> roomList, OnSelectionChangeListener listener) {
        this.roomList = roomList;
        this.selectionChangeListener = listener;
    }

    @NonNull
    @Override
    public RoomSelectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_room_select, parent, false);
        return new RoomSelectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomSelectViewHolder holder, int position) {
        Room room = roomList.get(position);
        holder.bind(room);
    }

    @Override
    public int getItemCount() {
        return roomList != null ? roomList.size() : 0;
    }

    /**
     * Chọn hoặc bỏ chọn toàn bộ các phòng
     */
    public void selectAll(boolean select) {
        selectedRoomIds.clear();
        if (select && roomList != null) {
            for (Room room : roomList) {
                if (room.getId() != null) {
                    selectedRoomIds.add(room.getId());
                }
            }
        }
        notifyDataSetChanged();
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(selectedRoomIds.size());
        }
    }

    /**
     * Lấy danh sách các phòng được chọn
     */
    public List<Room> getSelectedRooms() {
        List<Room> result = new ArrayList<>();
        if (roomList != null) {
            for (Room r : roomList) {
                if (r.getId() != null && selectedRoomIds.contains(r.getId())) {
                    result.add(r);
                }
            }
        }
        return result;
    }

    class RoomSelectViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCheckBox cbSelectRoom;
        private final TextView tvRoomName;
        private final TextView tvTenantInfo;

        public RoomSelectViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelectRoom = itemView.findViewById(R.id.cbSelectRoom);
            tvRoomName = itemView.findViewById(R.id.tvRoomName);
            tvTenantInfo = itemView.findViewById(R.id.tvTenantInfo);
        }

        public void bind(Room room) {
            if (room == null) return;

            String name = room.getName() != null ? room.getName() : "Phòng " + room.getId();
            tvRoomName.setText(name);

            if (room.getTenantName() != null && !room.getTenantName().trim().isEmpty()) {
                tvTenantInfo.setText("Người thuê: " + room.getTenantName().trim());
            } else {
                tvTenantInfo.setText("Phòng trống (chưa có người thuê)");
            }

            boolean isChecked = room.getId() != null && selectedRoomIds.contains(room.getId());
            cbSelectRoom.setOnCheckedChangeListener(null);
            cbSelectRoom.setChecked(isChecked);

            View.OnClickListener toggleListener = v -> {
                if (room.getId() == null) return;
                if (selectedRoomIds.contains(room.getId())) {
                    selectedRoomIds.remove(room.getId());
                    cbSelectRoom.setChecked(false);
                } else {
                    selectedRoomIds.add(room.getId());
                    cbSelectRoom.setChecked(true);
                }
                if (selectionChangeListener != null) {
                    selectionChangeListener.onSelectionChanged(selectedRoomIds.size());
                }
            };

            itemView.setOnClickListener(toggleListener);
            cbSelectRoom.setOnClickListener(toggleListener);
        }
    }
}

