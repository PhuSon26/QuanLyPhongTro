package com.example.renthub.ui.invoice;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renthub.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.DecimalFormat;
import java.util.List;

public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.ViewHolder> {

    private List<Invoice> list;
    private OnInvoiceClickListener listener;

    public interface OnInvoiceClickListener {
        void onItemClick(Invoice invoice);
        void onTogglePaidStatus(Invoice invoice);
        void onDeleteClick(Invoice invoice);
    }

    public InvoiceAdapter(List<Invoice> list, OnInvoiceClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    public void updateList(List<Invoice> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invoice, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Invoice inv = list.get(position);

        // ĐỊNH DẠNG HIỂN THỊ TÊN PHÒNG VÀ TÊN NGƯỜI THUÊ
        String roomTitle = (inv.getRoomName() != null && !inv.getRoomName().trim().isEmpty())
                ? inv.getRoomName()
                : "Phòng trọ";

        if (inv.getTenantName() != null && !inv.getTenantName().trim().isEmpty()) {
            roomTitle += " (" + inv.getTenantName() + ")";
        }

        holder.tvRoomName.setText(roomTitle);
        holder.tvMonthYear.setText("Tháng " + (inv.getMonthYear() != null ? inv.getMonthYear() : ""));

        DecimalFormat formatter = new DecimalFormat("#,###");
        holder.tvTotalAmount.setText(formatter.format(inv.getTotalAmount()) + " đ");

        if ("PAID".equalsIgnoreCase(inv.getStatus())) {
            holder.tvStatus.setText("Đã thanh toán");
            holder.tvStatus.setTextColor(Color.parseColor("#148369"));
            holder.cardStatus.setCardBackgroundColor(Color.parseColor("#E2F5EA"));
            holder.btnToggleStatus.setText("Đánh dấu Chưa thu");
        } else {
            holder.tvStatus.setText("Chưa thanh toán");
            holder.tvStatus.setTextColor(Color.parseColor("#C74444"));
            holder.cardStatus.setCardBackgroundColor(Color.parseColor("#FCECEC"));
            holder.btnToggleStatus.setText("Xác nhận Đã thu tiền");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(inv);
        });

        holder.btnToggleStatus.setOnClickListener(v -> {
            if (listener != null) listener.onTogglePaidStatus(inv);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(inv);
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomName, tvMonthYear, tvTotalAmount, tvStatus;
        MaterialCardView cardStatus;
        MaterialButton btnToggleStatus, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomName = itemView.findViewById(R.id.tvRoomName);
            tvMonthYear = itemView.findViewById(R.id.tvMonthYear);
            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            cardStatus = itemView.findViewById(R.id.cardStatus);
            btnToggleStatus = itemView.findViewById(R.id.btnToggleStatus);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}