package com.example.renthub.ui.home;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.renthub.R;

public class TenantHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        setupTenantUI();
    }

    private void setupTenantUI() {
        setText(R.id.tvHomeRole, "NGƯỜI THUÊ");
        setText(R.id.tvGreeting, "Xin chào, Người thuê!");
        setText(R.id.tvHomeSubtitle,
                "Không gian sống của bạn, trong tầm tay");

        setText(R.id.tvHeroTitle, "Quản lý phòng thuê");
        setText(R.id.tvHeroSubtitle,
                "Theo dõi phòng đang thuê và các khoản thanh toán.");
        setText(R.id.btnHomeViewRooms, "Phòng của tôi");

        setText(R.id.tvStat1, "01");
        setText(R.id.tvStat2, "01");
        setText(R.id.tvStat3, "00");

        setText(R.id.tvStatLabel1, "Phòng đang thuê");
        setText(R.id.tvStatLabel2, "Hóa đơn mới");
        setText(R.id.tvStatLabel3, "Sự cố");

        setText(R.id.tvRoomSectionTitle, "Thông tin của bạn");

        setText(R.id.tvRecentRoom1, "Phòng A102");
        setText(R.id.tvRecentRoomDesc1,
                "24 m² • 3.000.000đ/tháng");
        setText(R.id.tvRecentStatus1, "Đang thuê");

        setText(R.id.tvRecentRoom2, "Hóa đơn tháng này");
        setText(R.id.tvRecentRoomDesc2,
                "Tổng tiền cần thanh toán");
        setText(R.id.tvRecentStatus2, "Chưa thanh toán");
    }

    private void setText(int id, String text) {
        TextView view = findViewById(id);
        view.setText(text);
    }
}