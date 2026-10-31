package com.example.renthub.ui.invoice;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.renthub.R;
import com.example.renthub.ui.room.Room;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AddEditInvoiceActivity extends AppCompatActivity {

    public static final String EXTRA_ROOM = "EXTRA_ROOM";
    public static final String EXTRA_INVOICE = "EXTRA_INVOICE";

    private ImageButton btnBack;
    private TextView tvTitle, tvCalculatedTotal;
    private AutoCompleteTextView autoCompleteRoom;
    private TextInputEditText edtMonthYear, edtRoomPrice;
    private TextInputEditText edtElectricOld, edtElectricNew, edtElectricPrice;
    private TextInputEditText edtWaterOld, edtWaterNew, edtWaterPrice;
    private MaterialButton btnSaveInvoice;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private Room selectedRoom;
    private Invoice editingInvoice;
    private final List<Room> roomList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_invoice);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        getIntentData();
        loadLandlordRooms();
        setupAutoCalculation();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvCalculatedTotal = findViewById(R.id.tvCalculatedTotal);

        autoCompleteRoom = findViewById(R.id.autoCompleteRoom);
        edtMonthYear = findViewById(R.id.edtMonthYear);
        edtRoomPrice = findViewById(R.id.edtRoomPrice);

        edtElectricOld = findViewById(R.id.edtElectricOld);
        edtElectricNew = findViewById(R.id.edtElectricNew);
        edtElectricPrice = findViewById(R.id.edtElectricPrice);

        edtWaterOld = findViewById(R.id.edtWaterOld);
        edtWaterNew = findViewById(R.id.edtWaterNew);
        edtWaterPrice = findViewById(R.id.edtWaterPrice);

        btnSaveInvoice = findViewById(R.id.btnSaveInvoice);
    }

    private void getIntentData() {
        if (getIntent().hasExtra(EXTRA_ROOM)) {
            selectedRoom = (Room) getIntent().getSerializableExtra(EXTRA_ROOM);
            if (selectedRoom != null) {
                tvTitle.setText("Tạo hóa đơn: " + selectedRoom.getName());
                autoCompleteRoom.setText(selectedRoom.getName(), false);
                autoCompleteRoom.setEnabled(false); // Đã chỉ định phòng từ trước

                String priceClean = selectedRoom.getPrice() != null ? selectedRoom.getPrice().replaceAll("[^0-9]", "") : "0";
                edtRoomPrice.setText(priceClean);
            }
        }

        if (getIntent().hasExtra(EXTRA_INVOICE)) {
            editingInvoice = (Invoice) getIntent().getSerializableExtra(EXTRA_INVOICE);
            if (editingInvoice != null) {
                tvTitle.setText("Cập nhật hóa đơn");
                btnSaveInvoice.setText("Cập nhật");

                autoCompleteRoom.setText(editingInvoice.getRoomName() != null ? editingInvoice.getRoomName() : "", false);
                edtMonthYear.setText(editingInvoice.getMonthYear());
                edtRoomPrice.setText(String.format(Locale.getDefault(), "%.0f", editingInvoice.getRoomPrice()));

                edtElectricOld.setText(String.valueOf(editingInvoice.getElectricOld()));
                edtElectricNew.setText(String.valueOf(editingInvoice.getElectricNew()));
                edtElectricPrice.setText(String.format(Locale.getDefault(), "%.0f", editingInvoice.getElectricPrice()));

                edtWaterOld.setText(String.valueOf(editingInvoice.getWaterOld()));
                edtWaterNew.setText(String.valueOf(editingInvoice.getWaterNew()));
                edtWaterPrice.setText(String.format(Locale.getDefault(), "%.0f", editingInvoice.getWaterPrice()));
            }
        }
    }

    /**
     * TẢI DANH SÁCH PHÒNG TRỌ CỦA CHỦ TRỌ ĐỂ CHỌN TRONG DROPDOWN MENU
     */
    private void loadLandlordRooms() {
        if (mAuth.getCurrentUser() == null || selectedRoom != null) return;

        String uid = mAuth.getCurrentUser().getUid();
        db.collection("rooms")
                .whereEqualTo("landlordId", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    roomList.clear();
                    List<String> roomNames = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Room room = doc.toObject(Room.class);
                        if (room != null) {
                            room.setId(doc.getId());
                            roomList.add(room);
                            roomNames.add(room.getName() != null ? room.getName() : "Phòng " + doc.getId());
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, roomNames);
                    autoCompleteRoom.setAdapter(adapter);

                    autoCompleteRoom.setOnItemClickListener((parent, view, position, id) -> {
                        selectedRoom = roomList.get(position);
                        if (selectedRoom != null) {
                            String priceClean = selectedRoom.getPrice() != null ? selectedRoom.getPrice().replaceAll("[^0-9]", "") : "0";
                            edtRoomPrice.setText(priceClean);
                        }
                    });
                });
    }

    private void setupAutoCalculation() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { calculateTotal(); }
            @Override public void afterTextChanged(Editable s) {}
        };

        edtRoomPrice.addTextChangedListener(watcher);
        edtElectricOld.addTextChangedListener(watcher);
        edtElectricNew.addTextChangedListener(watcher);
        edtElectricPrice.addTextChangedListener(watcher);
        edtWaterOld.addTextChangedListener(watcher);
        edtWaterNew.addTextChangedListener(watcher);
        edtWaterPrice.addTextChangedListener(watcher);
    }

    private void calculateTotal() {
        double roomP = parseDouble(edtRoomPrice.getText().toString());
        int eOld = parseInt(edtElectricOld.getText().toString());
        int eNew = parseInt(edtElectricNew.getText().toString());
        double ePrice = parseDouble(edtElectricPrice.getText().toString());

        int wOld = parseInt(edtWaterOld.getText().toString());
        int wNew = parseInt(edtWaterNew.getText().toString());
        double wPrice = parseDouble(edtWaterPrice.getText().toString());

        double eTotal = Math.max(0, eNew - eOld) * ePrice;
        double wTotal = Math.max(0, wNew - wOld) * wPrice;
        double grandTotal = roomP + eTotal + wTotal;

        DecimalFormat formatter = new DecimalFormat("#,###");
        tvCalculatedTotal.setText(formatter.format(grandTotal) + " đ");
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSaveInvoice.setOnClickListener(v -> saveInvoice());
    }

    private void saveInvoice() {
        String roomNameInput = autoCompleteRoom.getText().toString().trim();
        if (roomNameInput.isEmpty()) {
            autoCompleteRoom.setError("Vui lòng chọn hoặc nhập tên phòng trọ");
            return;
        }

        String monthYear = edtMonthYear.getText().toString().trim();
        if (monthYear.isEmpty()) {
            edtMonthYear.setError("Vui lòng nhập kỳ hóa đơn");
            return;
        }

        Invoice invoice = (editingInvoice != null) ? editingInvoice : new Invoice();

        // Cập nhật thông tin phòng
        if (selectedRoom != null) {
            invoice.setRoomId(selectedRoom.getId());
            invoice.setRoomName(selectedRoom.getName());
            invoice.setTenantName(selectedRoom.getTenantName());
        } else {
            invoice.setRoomName(roomNameInput);
        }

        if (mAuth.getCurrentUser() != null) {
            invoice.setLandlordId(mAuth.getCurrentUser().getUid());
        }

        invoice.setMonthYear(monthYear);
        invoice.setRoomPrice(parseDouble(edtRoomPrice.getText().toString()));

        invoice.setElectricOld(parseInt(edtElectricOld.getText().toString()));
        invoice.setElectricNew(parseInt(edtElectricNew.getText().toString()));
        invoice.setElectricPrice(parseDouble(edtElectricPrice.getText().toString()));

        invoice.setWaterOld(parseInt(edtWaterOld.getText().toString()));
        invoice.setWaterNew(parseInt(edtWaterNew.getText().toString()));
        invoice.setWaterPrice(parseDouble(edtWaterPrice.getText().toString()));

        if (editingInvoice == null) {
            invoice.setStatus("UNPAID");
            invoice.setCreatedAt(System.currentTimeMillis());

            db.collection("invoices").add(invoice)
                    .addOnSuccessListener(ref -> {
                        Toast.makeText(this, "Tạo hóa đơn thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            db.collection("invoices").document(invoice.getId()).set(invoice)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Cập nhật hóa đơn thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private double parseDouble(String val) {
        try { return Double.parseDouble(val.trim()); } catch (Exception e) { return 0; }
    }

    private int parseInt(String val) {
        try { return Integer.parseInt(val.trim()); } catch (Exception e) { return 0; }
    }
}