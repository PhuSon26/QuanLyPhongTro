package com.example.renthub.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.renthub.R;

import java.util.Locale;

public class OtpVerificationActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvTargetAccount, tvResendTimer;
    private EditText edtOtp1, edtOtp2, edtOtp3, edtOtp4;
    private Button btnVerify;

    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        initViews();
        setupTargetAccount();
        setupOtpInputLogic();
        startCountDownTimer(90000); // 1 phút 30 giây (90s)
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTargetAccount = findViewById(R.id.tvTargetAccount);
        tvResendTimer = findViewById(R.id.tvResendTimer);
        edtOtp1 = findViewById(R.id.edtOtp1);
        edtOtp2 = findViewById(R.id.edtOtp2);
        edtOtp3 = findViewById(R.id.edtOtp3);
        edtOtp4 = findViewById(R.id.edtOtp4);
        btnVerify = findViewById(R.id.btnVerify);
    }

    private void setupTargetAccount() {
        if (getIntent() != null && getIntent().hasExtra(ForgotPasswordActivity.EXTRA_ACCOUNT)) {
            String account = getIntent().getStringExtra(ForgotPasswordActivity.EXTRA_ACCOUNT);
            if (!TextUtils.isEmpty(account)) {
                tvTargetAccount.setText(account);
            }
        }
    }

    private void setupOtpInputLogic() {
        // Tự động nhảy ô khi gõ
        addOtpTextWatcher(edtOtp1, edtOtp2, null);
        addOtpTextWatcher(edtOtp2, edtOtp3, edtOtp1);
        addOtpTextWatcher(edtOtp3, edtOtp4, edtOtp2);
        addOtpTextWatcher(edtOtp4, null, edtOtp3);
    }

    private void addOtpTextWatcher(EditText current, EditText next, EditText prev) {
        current.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1 && next != null) {
                    next.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        current.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (current.getText().length() == 0 && prev != null) {
                    prev.requestFocus();
                }
            }
            return false;
        });
    }

    private void startCountDownTimer(long millisInFuture) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        isTimerRunning = true;
        countDownTimer = new CountDownTimer(millisInFuture, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
                tvResendTimer.setText("Gửi lại mã (" + timeFormatted + ")");
                tvResendTimer.setEnabled(false);
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                tvResendTimer.setText("Gửi lại mã");
                tvResendTimer.setEnabled(true);
            }
        }.start();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        tvResendTimer.setOnClickListener(v -> {
            if (!isTimerRunning) {
                Toast.makeText(this, "Đã gửi lại mã OTP mới", Toast.LENGTH_SHORT).show();
                startCountDownTimer(90000);
            }
        });

        btnVerify.setOnClickListener(v -> {
            String o1 = edtOtp1.getText().toString().trim();
            String o2 = edtOtp2.getText().toString().trim();
            String o3 = edtOtp3.getText().toString().trim();
            String o4 = edtOtp4.getText().toString().trim();

            if (o1.isEmpty() || o2.isEmpty() || o3.isEmpty() || o4.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ 4 số mã OTP", Toast.LENGTH_SHORT).show();
                return;
            }

            // Chuyển sang màn hình Đặt mật khẩu mới
            Intent intent = new Intent(OtpVerificationActivity.this, ResetPasswordActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
