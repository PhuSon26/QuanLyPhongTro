package com.example.renthub.ui.invoice;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renthub.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class InvoiceListActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private MaterialButton btnCreateInvoice, btnFilterAll, btnFilterUnpaid, btnFilterPaid;
    private TextView tvTotalCollected, tvTotalPending, tvEmptyState;
    private RecyclerView rvInvoices;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private InvoiceAdapter adapter;
    private final List<Invoice> allInvoices = new ArrayList<>();
    private final List<Invoice> displayedInvoices = new ArrayList<>();
    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice_list);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerView();
        setupListeners();
        updateFilterButtonStyles(); // Đặt màu nút ban đầu
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInvoicesFromFirestore();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnCreateInvoice = findViewById(R.id.btnCreateInvoice);
        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterUnpaid = findViewById(R.id.btnFilterUnpaid);
        btnFilterPaid = findViewById(R.id.btnFilterPaid);

        tvTotalCollected = findViewById(R.id.tvTotalCollected);
        tvTotalPending = findViewById(R.id.tvTotalPending);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        rvInvoices = findViewById(R.id.rvInvoices);
    }

    private void setupRecyclerView() {
        adapter = new InvoiceAdapter(displayedInvoices, new InvoiceAdapter.OnInvoiceClickListener() {
            @Override
            public void onItemClick(Invoice invoice) {
                Intent intent = new Intent(InvoiceListActivity.this, AddEditInvoiceActivity.class);
                intent.putExtra(AddEditInvoiceActivity.EXTRA_INVOICE, invoice);
                startActivity(intent);
            }

            @Override
            public void onTogglePaidStatus(Invoice invoice) {
                String newStatus = "PAID".equalsIgnoreCase(invoice.getStatus()) ? "UNPAID" : "PAID";
                db.collection("invoices").document(invoice.getId())
                        .update("status", newStatus)
                        .addOnSuccessListener(aVoid -> {
                            invoice.setStatus(newStatus);
                            calculateTotalsAndFilter();
                            Toast.makeText(InvoiceListActivity.this, "Đã cập nhật trạng thái!", Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onDeleteClick(Invoice invoice) {
                db.collection("invoices").document(invoice.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            allInvoices.remove(invoice);
                            calculateTotalsAndFilter();
                            Toast.makeText(InvoiceListActivity.this, "Đã xóa hóa đơn!", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        rvInvoices.setLayoutManager(new LinearLayoutManager(this));
        rvInvoices.setAdapter(adapter);
    }

    private void setupListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (btnCreateInvoice != null) {
            btnCreateInvoice.setOnClickListener(v -> {
                Intent intent = new Intent(InvoiceListActivity.this, AddEditInvoiceActivity.class);
                startActivity(intent);
            });
        }

        if (btnFilterAll != null) btnFilterAll.setOnClickListener(v -> setFilter("ALL"));
        if (btnFilterUnpaid != null) btnFilterUnpaid.setOnClickListener(v -> setFilter("UNPAID"));
        if (btnFilterPaid != null) btnFilterPaid.setOnClickListener(v -> setFilter("PAID"));

        View btnNavRoom = findViewById(R.id.btnNavRoom);
        if (btnNavRoom != null) {
            btnNavRoom.setOnClickListener(v -> finish());
        }

        View btnNavSettings = findViewById(R.id.btnNavSettings);
        if (btnNavSettings != null) {
            btnNavSettings.setOnClickListener(v -> {
                startActivity(new Intent(InvoiceListActivity.this, com.example.renthub.ui.settings.SettingsActivity.class));
                finish();
            });
        }
    }

    private void loadInvoicesFromFirestore() {
        if (mAuth.getCurrentUser() == null) return;
        String landlordId = mAuth.getCurrentUser().getUid();

        db.collection("invoices")
                .whereEqualTo("landlordId", landlordId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allInvoices.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Invoice inv = doc.toObject(Invoice.class);
                        if (inv != null) {
                            inv.setId(doc.getId());
                            allInvoices.add(inv);
                        }
                    }
                    calculateTotalsAndFilter();
                });
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        updateFilterButtonStyles();
        calculateTotalsAndFilter();
    }

    /**
     * CẬP NHẬT MÀU SẮC 3 NÚT LỌC KHU BẤM
     */
    private void updateFilterButtonStyles() {
        int selectedBg = Color.parseColor("#148369");
        int selectedText = Color.parseColor("#FFFFFF");
        int unselectedBg = Color.parseColor("#FFFFFF");
        int unselectedText = Color.parseColor("#718096");
        int strokeColor = Color.parseColor("#E4ECE8");

        styleFilterButton(btnFilterAll, "ALL".equals(currentFilter), selectedBg, selectedText, unselectedBg, unselectedText, strokeColor);
        styleFilterButton(btnFilterUnpaid, "UNPAID".equals(currentFilter), selectedBg, selectedText, unselectedBg, unselectedText, strokeColor);
        styleFilterButton(btnFilterPaid, "PAID".equals(currentFilter), selectedBg, selectedText, unselectedBg, unselectedText, strokeColor);
    }

    private void styleFilterButton(MaterialButton btn, boolean isSelected, int selBg, int selText, int unSelBg, int unSelText, int stroke) {
        if (btn == null) return;
        if (isSelected) {
            btn.setBackgroundTintList(ColorStateList.valueOf(selBg));
            btn.setTextColor(selText);
            btn.setStrokeWidth(0);
        } else {
            btn.setBackgroundTintList(ColorStateList.valueOf(unSelBg));
            btn.setTextColor(unSelText);
            btn.setStrokeColor(ColorStateList.valueOf(stroke));
            btn.setStrokeWidth(2);
        }
    }

    private void calculateTotalsAndFilter() {
        double totalCollected = 0;
        double totalPending = 0;

        displayedInvoices.clear();

        for (Invoice inv : allInvoices) {
            double amount = inv.getTotalAmount();
            if ("PAID".equalsIgnoreCase(inv.getStatus())) {
                totalCollected += amount;
                if ("ALL".equals(currentFilter) || "PAID".equals(currentFilter)) {
                    displayedInvoices.add(inv);
                }
            } else {
                totalPending += amount;
                if ("ALL".equals(currentFilter) || "UNPAID".equals(currentFilter)) {
                    displayedInvoices.add(inv);
                }
            }
        }

        DecimalFormat fmt = new DecimalFormat("#,###");
        if (tvTotalCollected != null) tvTotalCollected.setText(fmt.format(totalCollected) + " đ");
        if (tvTotalPending != null) tvTotalPending.setText(fmt.format(totalPending) + " đ");

        adapter.notifyDataSetChanged();
        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(displayedInvoices.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
}