package com.example.renthub.ui.room;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renthub.R;

import java.util.ArrayList;
import java.util.List;

public class LandlordRoomListActivity extends AppCompatActivity {

    private RecyclerView rvRooms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_landlord_room_list);

        rvRooms = findViewById(R.id.rvRooms);

        setupRoomList();
    }

    private void setupRoomList() {

        // Dữ liệu mẫu chỉ để hiển thị giao diện
        List<Room> rooms = new ArrayList<>();

        rooms.add(new Room(
                "Phòng A101",
                "Dãy A • Tầng 1",
                "20 m²",
                "2.500.000",
                "Còn trống"
        ));

        rooms.add(new Room(
                "Phòng A102",
                "Dãy A • Tầng 1",
                "24 m²",
                "3.000.000",
                "Đang thuê"
        ));

        rooms.add(new Room(
                "Phòng B201",
                "Dãy B • Tầng 2",
                "22 m²",
                "2.800.000",
                "Còn trống"
        ));

        rooms.add(new Room(
                "Phòng B202",
                "Dãy B • Tầng 2",
                "18 m²",
                "2.300.000",
                "Bảo trì"
        ));

        rvRooms.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rvRooms.setAdapter(new RoomAdapter(rooms));
    }
}