package com.example.renthub.ui.room;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.renthub.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RentRoomBottomSheetDialog extends BottomSheetDialogFragment {

    private static final String ARG_ROOM = "ARG_ROOM";

    public interface OnRentSuccessListener {
        void onRentSuccess(Room updatedRoom);
    }

    private Room room;
    private OnRentSuccessListener rentSuccessListener;

    private TextView tvSheetRoomName, tvSheetLocation, tvSheetRoomPrice, tvSheetDeposit;
    private TextInputEditText edtRentTenantName, edtRentTenantPhone, edtRentTenantIdCard;
    private TextInputEditText edtRentStartDate, edtRentOccupants, edtRentNote;
    private RadioGroup rgRentDuration;
    private RadioButton rbDuration6Months, rbDuration12Months, rbDurationFlexible;
    private CheckBox cbAgreeTerms;
    private MaterialButton btnSubmitRent;
    private ProgressBar pbRentLoading;
    private ImageView btnCloseSheet;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private final Calendar selectedCalendar = Calendar.getInstance();

    public static RentRoomBottomSheetDialog newInstance(Room room) {
        RentRoomBottomSheetDialog dialog = new RentRoomBottomSheetDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ROOM, room);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnRentSuccessListener(OnRentSuccessListener listener) {
        this.rentSuccessListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            room = (Room) getArguments().getSerializable(ARG_ROOM);
        }
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_bottom_sheet_rent_room, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        bindRoomData();
        loadTenantUserProfile();
        setupListeners();
    }

    private void initViews(View v) {
        tvSheetRoomName = v.findViewById(R.id.tvSheetRoomName);
        tvSheetLocation = v.findViewById(R.id.tvSheetLocation);
        tvSheetRoomPrice = v.findViewById(R.id.tvSheetRoomPrice);
        tvSheetDeposit = v.findViewById(R.id.tvSheetDeposit);

        edtRentTenantName = v.findViewById(R.id.edtRentTenantName);
        edtRentTenantPhone = v.findViewById(R.id.edtRentTenantPhone);
        edtRentTenantIdCard = v.findViewById(R.id.edtRentTenantIdCard);

        edtRentStartDate = v.findViewById(R.id.edtRentStartDate);
        edtRentOccupants = v.findViewById(R.id.edtRentOccupants);
        edtRentNote = v.findViewById(R.id.edtRentNote);

        rgRentDuration = v.findViewById(R.id.rgRentDuration);
        rbDuration6Months = v.findViewById(R.id.rbDuration6Months);
        rbDuration12Months = v.findViewById(R.id.rbDuration12Months);
        rbDurationFlexible = v.findViewById(R.id.rbDurationFlexible);

        cbAgreeTerms = v.findViewById(R.id.cbAgreeTerms);
        btnSubmitRent = v.findViewById(R.id.btnSubmitRent);
        pbRentLoading = v.findViewById(R.id.pbRentLoading);
        btnCloseSheet = v.findViewById(R.id.btnCloseSheet);
    }

    private void bindRoomData() {
        if (room == null) return;

        tvSheetRoomName.setText(room.getName() != null ? room.getName() : "Phòng trọ");
        tvSheetLocation.setText(room.getLocation() != null && !room.getLocation().isEmpty()
                ? "Địa chỉ: " + room.getLocation()
                : "Địa chỉ: Chưa cập nhật");

        String priceStr = room.getPrice() != null ? room.getPrice() : "0";
        tvSheetRoomPrice.setText(priceStr + " đ");
        tvSheetDeposit.setText(priceStr + " đ");

        // Gợi ý ngày vào ở mặc định là ngày mai
        selectedCalendar.add(Calendar.DAY_OF_MONTH, 1);
        updateStartDateDisplay();
    }

    private void loadTenantUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        String phone = documentSnapshot.getString("phone");

                        if (fullName != null && !fullName.isEmpty()) {
                            edtRentTenantName.setText(fullName);
                        }
                        if (phone != null && !phone.isEmpty()) {
                            edtRentTenantPhone.setText(phone);
                        }
                    }
                });
    }

    private void setupListeners() {
        btnCloseSheet.setOnClickListener(v -> dismiss());

        edtRentStartDate.setOnClickListener(v -> showDatePicker());

        btnSubmitRent.setOnClickListener(v -> handleConfirmRent());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedCalendar.set(Calendar.YEAR, year);
                    selectedCalendar.set(Calendar.MONTH, month);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateStartDateDisplay();
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)
        );

        // Không cho phép chọn ngày trong quá khứ
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void updateStartDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        edtRentStartDate.setText(sdf.format(selectedCalendar.getTime()));
    }

    private void handleConfirmRent() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập lại để thực hiện!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (room == null || room.getId() == null) {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy ID phòng trọ!", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullName = edtRentTenantName.getText() != null ? edtRentTenantName.getText().toString().trim() : "";
        String phone = edtRentTenantPhone.getText() != null ? edtRentTenantPhone.getText().toString().trim() : "";
        String idCard = edtRentTenantIdCard.getText() != null ? edtRentTenantIdCard.getText().toString().trim() : "";
        String startDate = edtRentStartDate.getText() != null ? edtRentStartDate.getText().toString().trim() : "";
        String note = edtRentNote.getText() != null ? edtRentNote.getText().toString().trim() : "";

        if (TextUtils.isEmpty(fullName)) {
            edtRentTenantName.setError("Vui lòng nhập họ và tên");
            edtRentTenantName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            edtRentTenantPhone.setError("Vui lòng nhập số điện thoại");
            edtRentTenantPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(startDate)) {
            edtRentStartDate.setError("Vui lòng chọn ngày bắt đầu vào ở");
            return;
        }

        if (!cbAgreeTerms.isChecked()) {
            Toast.makeText(getContext(), "Vui lòng đồng ý với điều khoản thuê phòng", Toast.LENGTH_SHORT).show();
            return;
        }

        final String finalDuration = rbDuration12Months.isChecked() ? "12 tháng"
                : (rbDurationFlexible.isChecked() ? "Linh hoạt" : "6 tháng");

        // Xử lý danh sách người ở cùng
        List<String> occupantsList = new ArrayList<>();
        String occupantsRaw = edtRentOccupants.getText() != null ? edtRentOccupants.getText().toString().trim() : "";
        if (!occupantsRaw.isEmpty()) {
            String[] parts = occupantsRaw.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    occupantsList.add(trimmed);
                }
            }
        }

        setLoading(true);

        String tenantUid = currentUser.getUid();
        String roomId = room.getId();

        // Kiểm tra xem đã gửi yêu cầu đang chờ duyệt cho phòng này chưa
        db.collection("rental_requests")
                .whereEqualTo("roomId", roomId)
                .whereEqualTo("tenantId", tenantUid)
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        setLoading(false);
                        Toast.makeText(getContext(), "Bạn đã gửi yêu cầu cho phòng này rồi, vui lòng chờ chủ trọ duyệt!", Toast.LENGTH_LONG).show();
                        dismiss();
                        return;
                    }

                    saveRentalRequest(tenantUid, fullName, phone, idCard, startDate, finalDuration, note, occupantsList);
                })
                .addOnFailureListener(e -> {
                    saveRentalRequest(tenantUid, fullName, phone, idCard, startDate, finalDuration, note, occupantsList);
                });
    }

    private void saveRentalRequest(String tenantUid, String fullName, String phone, String idCard,
                                   String startDate, String duration, String note, List<String> occupantsList) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("roomId", room.getId());
        requestData.put("roomName", room.getName());
        requestData.put("roomPrice", room.getPrice());
        requestData.put("roomLocation", room.getLocation());
        requestData.put("landlordId", room.getLandlordId());
        requestData.put("landlordName", room.getLandlordName());
        requestData.put("landlordPhone", room.getLandlordPhone());
        requestData.put("tenantId", tenantUid);
        requestData.put("tenantName", fullName);
        requestData.put("tenantPhone", phone);
        requestData.put("tenantIdCard", idCard);
        requestData.put("startDate", startDate);
        requestData.put("duration", duration);
        requestData.put("price", room.getPrice());
        requestData.put("deposit", room.getPrice());
        requestData.put("occupants", occupantsList);
        requestData.put("note", note);
        requestData.put("status", "PENDING"); // Đang chờ chủ trọ phê duyệt
        requestData.put("createdAt", FieldValue.serverTimestamp());

        db.collection("rental_requests")
                .add(requestData)
                .addOnSuccessListener(docRef -> {
                    setLoading(false);
                    Toast.makeText(getContext(), "Đã gửi yêu cầu thuê phòng thành công! Vui lòng theo dõi kết quả xét duyệt.", Toast.LENGTH_LONG).show();

                    if (rentSuccessListener != null) {
                        rentSuccessListener.onRentSuccess(room);
                    }
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(getContext(), "Lỗi gửi yêu cầu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean isLoading) {
        btnSubmitRent.setEnabled(!isLoading);
        pbRentLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
