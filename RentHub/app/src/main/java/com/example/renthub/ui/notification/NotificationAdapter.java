package com.example.renthub.ui.notification;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renthub.R;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter hiển thị danh sách thông báo cho người dùng (Chủ trọ / Người thuê).
 * Tối ưu hóa hiệu năng bằng ViewHolder pattern theo chuẩn dự án.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    public interface OnNotificationClickListener {
        void onItemClick(NotificationModel notification);
        void onDeleteClick(NotificationModel notification);
    }

    private final List<NotificationModel> notificationList;
    private final OnNotificationClickListener listener;

    public NotificationAdapter(List<NotificationModel> notificationList, OnNotificationClickListener listener) {
        this.notificationList = notificationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationModel notification = notificationList.get(position);
        holder.bind(notification, listener);
    }

    @Override
    public int getItemCount() {
        return notificationList != null ? notificationList.size() : 0;
    }

    /**
     * ViewHolder tối ưu hóa việc tái sử dụng view
     */
    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardNotification;
        private final FrameLayout layoutIconBg;
        private final ImageView ivNotificationIcon;
        private final TextView tvNotificationTitle;
        private final TextView tvNotificationBody;
        private final TextView tvNotificationTime;
        private final View viewUnreadDot;
        private final ImageButton btnDeleteNotification;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardNotification = itemView.findViewById(R.id.cardNotification);
            layoutIconBg = itemView.findViewById(R.id.layoutIconBg);
            ivNotificationIcon = itemView.findViewById(R.id.ivNotificationIcon);
            tvNotificationTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvNotificationBody = itemView.findViewById(R.id.tvNotificationBody);
            tvNotificationTime = itemView.findViewById(R.id.tvNotificationTime);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
            btnDeleteNotification = itemView.findViewById(R.id.btnDeleteNotification);
        }

        public void bind(NotificationModel item, OnNotificationClickListener listener) {
            if (item == null) return;

            tvNotificationTitle.setText(item.getTitle() != null ? item.getTitle() : "Thông báo");
            tvNotificationBody.setText(item.getBody() != null ? item.getBody() : "");
            tvNotificationTime.setText(formatTimeAgo(item.getCreatedAt()));

            // Cập nhật trạng thái đọc / chưa đọc
            if (item.isRead()) {
                viewUnreadDot.setVisibility(View.GONE);
                cardNotification.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
                tvNotificationTitle.setTextColor(Color.parseColor("#4A5568"));
            } else {
                viewUnreadDot.setVisibility(View.VISIBLE);
                cardNotification.setCardBackgroundColor(Color.parseColor("#F9FCFB"));
                tvNotificationTitle.setTextColor(Color.parseColor("#14344B"));
            }

            // Tùy chỉnh icon và màu sắc theo loại thông báo
            applyNotificationTypeStyle(item.getType());

            // Xử lý sự kiện click
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });

            btnDeleteNotification.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(item);
                }
            });
        }

        private void applyNotificationTypeStyle(String type) {
            if (type == null) type = NotificationModel.TYPE_SYSTEM;

            switch (type) {
                case NotificationModel.TYPE_ISSUE_NEW:
                    layoutIconBg.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
                    ivNotificationIcon.setImageResource(R.drawable.rh_ic_issue);
                    ivNotificationIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#DC2626")));
                    break;

                case NotificationModel.TYPE_ISSUE_UPDATE:
                    layoutIconBg.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FEF3C7")));
                    ivNotificationIcon.setImageResource(R.drawable.rh_ic_issue);
                    ivNotificationIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#D97706")));
                    break;

                case NotificationModel.TYPE_INVOICE_NEW:
                case NotificationModel.TYPE_INVOICE_REMINDER:
                    layoutIconBg.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0F2EF")));
                    ivNotificationIcon.setImageResource(R.drawable.rh_ic_invoice);
                    ivNotificationIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#148369")));
                    break;

                case NotificationModel.TYPE_ANNOUNCEMENT:
                    layoutIconBg.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EFF6FF")));
                    ivNotificationIcon.setImageResource(R.drawable.rh_ic_feed);
                    ivNotificationIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
                    break;

                case NotificationModel.TYPE_SYSTEM:
                default:
                    layoutIconBg.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F3E8FF")));
                    ivNotificationIcon.setImageResource(R.drawable.rh_ic_bell);
                    ivNotificationIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#7C3AED")));
                    break;
            }
        }

        /**
         * Định dạng thời gian theo kiểu tương đối thân thiện (VD: 5 phút trước)
         */
        private String formatTimeAgo(long timestamp) {
            if (timestamp <= 0) return "";

            long now = System.currentTimeMillis();
            long diff = now - timestamp;

            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (seconds < 60) {
                return "Vừa xong";
            } else if (minutes < 60) {
                return minutes + " phút trước";
            } else if (hours < 24) {
                return hours + " giờ trước";
            } else if (days < 7) {
                return days + " ngày trước";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                return sdf.format(new Date(timestamp));
            }
        }
    }
}

