package com.example.renthub.ui.invoice;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.renthub.R;
import com.example.renthub.ui.settings.SettingsActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.DecimalFormat;

public class TenantInvoiceDetailActivity extends AppCompatActivity {

    public static final String EXTRA_INVOICE = "EXTRA_INVOICE";

    private ImageButton btnBack;
    private TextView tvRoomAndMonth, tvDetailRoomPrice, tvDetailElectricUsage;
    private TextView tvDetailElectricTotal, tvDetailWaterUsage, tvDetailWaterTotal, tvDetailGrandTotal;
    private TextView tvInvoiceStatus;
    private MaterialCardView cardInvoiceStatus;
    private View layoutVietQRSection;
    private ImageView imgVietQR;

    private Invoice invoice;
    private FirebaseFirestore db;
    private ListenerRegistration invoiceListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_invoice_detail);

        db = FirebaseFirestore.getInstance();

        initViews();
        getIntentData();
        setupBottomNavListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (invoiceListener != null) {
            invoiceListener.remove();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvRoomAndMonth = findViewById(R.id.tvRoomAndMonth);
        tvInvoiceStatus = findViewById(R.id.tvInvoiceStatus);
        cardInvoiceStatus = findViewById(R.id.cardInvoiceStatus);

        tvDetailRoomPrice = findViewById(R.id.tvDetailRoomPrice);
        tvDetailElectricUsage = findViewById(R.id.tvDetailElectricUsage);
        tvDetailElectricTotal = findViewById(R.id.tvDetailElectricTotal);
        tvDetailWaterUsage = findViewById(R.id.tvDetailWaterUsage);
        tvDetailWaterTotal = findViewById(R.id.tvDetailWaterTotal);
        tvDetailGrandTotal = findViewById(R.id.tvDetailGrandTotal);

        layoutVietQRSection = findViewById(R.id.layoutVietQRSection);
        imgVietQR = findViewById(R.id.imgVietQR);
    }

    private void setupBottomNavListeners() {
        View btnNavRoom = findViewById(R.id.btnNavRoom);
        if (btnNavRoom != null) {
            btnNavRoom.setOnClickListener(v -> finish());
        }

        View btnNavSettings = findViewById(R.id.btnNavSettings);
        if (btnNavSettings != null) {
            btnNavSettings.setOnClickListener(v -> {
                startActivity(new Intent(TenantInvoiceDetailActivity.this, SettingsActivity.class));
                finish();
            });
        }
    }

    private void getIntentData() {
        if (getIntent().hasExtra(EXTRA_INVOICE)) {
            invoice = (Invoice) getIntent().getSerializableExtra(EXTRA_INVOICE);
            if (invoice != null) {
                displayInvoiceData();
                listenToInvoiceUpdates();
            }
        }
    }

    private void listenToInvoiceUpdates() {
        if (invoice == null || invoice.getId() == null) return;

        invoiceListener = db.collection("invoices").document(invoice.getId())
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) return;

                    Invoice updatedInvoice = snapshot.toObject(Invoice.class);
                    if (updatedInvoice != null) {
                        updatedInvoice.setId(snapshot.getId());
                        this.invoice = updatedInvoice;
                        displayInvoiceData();
                    }
                });
    }

    private void displayInvoiceData() {
        DecimalFormat fmt = new DecimalFormat("#,###");

        String roomName = (invoice.getRoomName() != null) ? invoice.getRoomName() : "Phòng";
        String monthYear = (invoice.getMonthYear() != null) ? invoice.getMonthYear() : "";
        tvRoomAndMonth.setText(roomName + " - Tháng " + monthYear);

        tvDetailRoomPrice.setText(fmt.format(invoice.getRoomPrice()) + " đ");

        tvDetailElectricUsage.setText("Tiền điện (" + invoice.getElectricUsage() + " kWh)");
        tvDetailElectricTotal.setText(fmt.format(invoice.getTotalElectric()) + " đ");

        tvDetailWaterUsage.setText("Tiền nước (" + invoice.getWaterUsage() + " m³)");
        tvDetailWaterTotal.setText(fmt.format(invoice.getTotalWater()) + " đ");

        tvDetailGrandTotal.setText(fmt.format(invoice.getTotalAmount()) + " đ");

        boolean isPaid = "PAID".equalsIgnoreCase(invoice.getStatus());

        if (isPaid) {
            tvInvoiceStatus.setText("Đã thanh toán");
            tvInvoiceStatus.setTextColor(Color.parseColor("#148369"));
            cardInvoiceStatus.setCardBackgroundColor(Color.parseColor("#E2F5EA"));

            if (layoutVietQRSection != null) {
                layoutVietQRSection.setVisibility(View.GONE);
            }
        } else {
            tvInvoiceStatus.setText("Chưa thanh toán");
            tvInvoiceStatus.setTextColor(Color.parseColor("#C74444"));
            cardInvoiceStatus.setCardBackgroundColor(Color.parseColor("#FCECEC"));

            if (layoutVietQRSection != null) {
                layoutVietQRSection.setVisibility(View.VISIBLE);
                loadVietQR();
            }
        }
    }

    private void loadVietQR() {
        String bankId = "MB";
        String accountNo = "0333017880";
        String accountName = "LE PHU SON";

        double amount = invoice.getTotalAmount();
        String roomClean = (invoice.getRoomName() != null) ? invoice.getRoomName().replaceAll(" ", "") : "";
        String monthClean = (invoice.getMonthYear() != null) ? invoice.getMonthYear().replaceAll("/", "") : "";
        String addInfo = "Phong " + roomClean + " T" + monthClean;

        String qrUrl = String.format("https://img.vietqr.io/image/%s-%s-compact2.png?amount=%.0f&addInfo=%s&accountName=%s",
                bankId, accountNo, amount, addInfo, accountName);

        Glide.with(this)
                .load(qrUrl)
                .placeholder(R.drawable.rh_ic_room)
                .into(imgVietQR);
    }
}