package com.example.renthub.ui.issue;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renthub.R;
import com.example.renthub.ui.room.Room;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Màn hình danh sách theo dõi và quản lý sự cố phòng trọ
 * Phân quyền chặt chẽ:
 * - Chủ trọ: Chỉ tải các sự cố thuộc các phòng do chính chủ trọ này quản lý (landlordId khớp với currentUserId).
 *   Tuyệt đối không tải các sự cố chưa gán phòng hoặc thuộc phòng của chủ trọ khác.
 * - Người thuê: Chỉ xem các sự cố do chính mình tạo (tenantId khớp với currentUserId).
 *
 * Tính năng bổ sung:
 * - Lọc theo từng phòng thuộc quyền quản lý.
 * - Sắp xếp theo Mức độ ưu tiên cao nhất hoặc Thời gian gửi.
 * - Xem chi tiết đầy đủ mô tả, hình ảnh và tương tác xử lý.
 */
public class IssueListActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private Spinner spinnerRoomFilter;
    private Spinner spinnerSortFilter;
    private TextView tvFilterSummary;
    private RecyclerView recyclerView;
    private View layoutEmptyState;
    private TextView tvEmptyTitle, tvEmptySubtitle;
    private ProgressBar pbLoading;
    private FloatingActionButton fabAddIssue;

    private IssueAdapter adapter;
    private final List<Issue> rawIssueList = new ArrayList<>();
    private final List<Issue> displayedIssueList = new ArrayList<>();

    // Danh sách và map quản lý phòng thuộc quyền quản lý của người dùng hiện tại
    private final Set<String> myManagedRoomIds = new HashSet<>();
    private final Map<String, String> roomNamesMap = new HashMap<>();
    private final List<String> filterRoomIds = new ArrayList<>();
    private final List<String> filterRoomDisplayNames = new ArrayList<>();
    private ArrayAdapter<String> roomSpinnerAdapter;

    // Trạng thái bộ lọc
    private String selectedFilterRoomId = "ALL";
    private int selectedSortOption = 0; // 0: Ưu tiên cao nhất, 1: Mới nhất, 2: Cũ nhất

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId = "";
    private boolean isLandlord = false;

    private ListenerRegistration issueListener;

    private ArrayList<String> extraRoomIds = new ArrayList<>();
    private ArrayList<String> extraRoomNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_issue_list);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        getIntentExtras();
        initViews();
        setupRecyclerView();
        setupSpinners();
        setupListeners();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            checkUserRoleAndLoadData();
        } else {
            Toast.makeText(this, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Nhận dữ liệu truyền qua Intent từ các màn hình trước nếu có
     */
    private void getIntentExtras() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("EXTRA_ROOM_IDS")) {
                extraRoomIds = intent.getStringArrayListExtra("EXTRA_ROOM_IDS");
            }
            if (intent.hasExtra("EXTRA_ROOM_NAMES")) {
                extraRoomNames = intent.getStringArrayListExtra("EXTRA_ROOM_NAMES");
            }
        }
    }

    /**
     * Ánh xạ các View trên giao diện
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        spinnerRoomFilter = findViewById(R.id.spinnerRoomFilter);
        spinnerSortFilter = findViewById(R.id.spinnerSortFilter);
        tvFilterSummary = findViewById(R.id.tvFilterSummary);
        recyclerView = findViewById(R.id.recyclerViewIssues);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        pbLoading = findViewById(R.id.pbLoading);
        fabAddIssue = findViewById(R.id.fabAddIssue);

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * Cài đặt RecyclerView và Adapter
     */
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new IssueAdapter(displayedIssueList);
        recyclerView.setAdapter(adapter);

        // Chuyển sang màn hình chi tiết khi nhấn vào một sự cố
        adapter.setOnItemClickListener(issue -> {
            Intent intent = new Intent(IssueListActivity.this, IssueDetailActivity.class);
            intent.putExtra("ISSUE_ID", issue.getIssueId());
            startActivity(intent);
        });
    }

    /**
     * Khởi tạo danh sách cho Spinner Lọc phòng và Sắp xếp
     */
    private void setupSpinners() {
        // 1. Spinner Lọc phòng
        filterRoomIds.clear();
        filterRoomDisplayNames.clear();
        filterRoomIds.add("ALL");
        filterRoomDisplayNames.add("Tất cả phòng");

        roomSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filterRoomDisplayNames);
        spinnerRoomFilter.setAdapter(roomSpinnerAdapter);

        spinnerRoomFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < filterRoomIds.size()) {
                    selectedFilterRoomId = filterRoomIds.get(position);
                    applyFilterAndSort();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 2. Spinner Sắp xếp
        String[] sortOptions = new String[]{
                "⚡ Ưu tiên cao nhất",
                "🕒 Mới nhất",
                "🕒 Cũ nhất"
        };
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sortOptions);
        spinnerSortFilter.setAdapter(sortAdapter);

        spinnerSortFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSortOption = position;
                applyFilterAndSort();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * Cài đặt các sự kiện bấm nút
     */
    private void setupListeners() {
        fabAddIssue.setOnClickListener(v -> {
            Intent intent = new Intent(IssueListActivity.this, CreateIssueActivity.class);
            intent.putStringArrayListExtra("EXTRA_ROOM_IDS", extraRoomIds);
            intent.putStringArrayListExtra("EXTRA_ROOM_NAMES", extraRoomNames);
            startActivity(intent);
        });
    }

    /**
     * Kiểm tra role người dùng trong collection 'users'
     */
    private void checkUserRoleAndLoadData() {
        pbLoading.setVisibility(View.VISIBLE);

        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String role = documentSnapshot.getString("role");
                    isLandlord = role != null && (role.equalsIgnoreCase("LANDLORD")
                            || role.equalsIgnoreCase("CHỦ TRỌ")
                            || role.toLowerCase().contains("chủ"));

                    if (isLandlord) {
                        toolbar.setTitle("Quản lý sự cố phòng trọ");
                        fabAddIssue.setVisibility(View.GONE);
                        // Chủ trọ: Chỉ tải các phòng do Chủ trọ này quản lý trước
                        loadManagedRoomsForLandlord();
                    } else {
                        toolbar.setTitle("Theo dõi sự cố đã báo");
                        fabAddIssue.setVisibility(View.VISIBLE);
                        // Người thuê: Tải phòng người thuê đang ở và các sự cố của chính mình
                        loadRentedRoomsForTenant();
                    }
                })
                .addOnFailureListener(e -> {
                    // Mặc định an toàn cho người thuê nếu lỗi tải role
                    isLandlord = false;
                    toolbar.setTitle("Theo dõi sự cố đã báo");
                    fabAddIssue.setVisibility(View.VISIBLE);
                    loadRentedRoomsForTenant();
                });
    }

    /**
     * Tải danh sách phòng do Chủ trọ hiện tại quản lý (landlordId == currentUserId)
     */
    private void loadManagedRoomsForLandlord() {
        db.collection("rooms")
                .whereEqualTo("landlordId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    myManagedRoomIds.clear();
                    roomNamesMap.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Room room = doc.toObject(Room.class);
                        if (room != null) {
                            String roomId = doc.getId();
                            myManagedRoomIds.add(roomId);

                            String roomName = room.getName() != null && !room.getName().trim().isEmpty()
                                    ? room.getName().trim()
                                    : "Phòng " + roomId;
                            roomNamesMap.put(roomId, roomName);
                        }
                    }

                    adapter.setRoomNamesMap(roomNamesMap);

                    // Nếu chủ trọ chưa tạo phòng nào thì không có sự cố nào cần hiển thị
                    if (myManagedRoomIds.isEmpty()) {
                        pbLoading.setVisibility(View.GONE);
                        rawIssueList.clear();
                        updateRoomFilterOptions();
                        applyFilterAndSort();

                        if (tvEmptyTitle != null) tvEmptyTitle.setText("Chưa có sự cố nào");
                        if (tvEmptySubtitle != null) tvEmptySubtitle.setText("Bạn chưa tạo phòng trọ nào hoặc chưa có sự cố phát sinh cho các phòng của bạn.");
                        return;
                    }

                    // Tải các sự cố thuộc về những phòng do chủ trọ này quản lý
                    loadIssuesForLandlord();
                })
                .addOnFailureListener(e -> {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi nạp danh sách phòng của chủ trọ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Tải danh sách sự cố dành riêng cho Chủ trọ hiện tại:
     * - Chỉ lấy các sự cố có roomId thuộc về myManagedRoomIds.
     * - Tuyệt đối không nạp các sự cố chưa gán phòng hoặc thuộc về chủ trọ khác.
     */
    private void loadIssuesForLandlord() {
        if (issueListener != null) issueListener.remove();

        pbLoading.setVisibility(View.VISIBLE);

        // Firestore hỗ trợ whereIn tối đa 30 phần tử
        if (myManagedRoomIds.size() <= 30) {
            List<String> roomList = new ArrayList<>(myManagedRoomIds);
            issueListener = db.collection("issues")
                    .whereIn("roomId", roomList)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener((value, error) -> {
                        pbLoading.setVisibility(View.GONE);

                        if (error != null) {
                            Toast.makeText(this, "Lỗi tải sự cố: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (value != null) {
                            rawIssueList.clear();
                            for (QueryDocumentSnapshot doc : value) {
                                Issue issue = doc.toObject(Issue.class);
                                issue.setIssueId(doc.getId());

                                String rId = issue.getRoomId();
                                // 1. Tuyệt đối không nạp các sự cố chưa gán phòng
                                if (rId == null || rId.trim().isEmpty()) {
                                    continue;
                                }

                                // 2. Tuyệt đối không nạp sự cố thuộc về phòng của chủ trọ khác
                                if (!myManagedRoomIds.contains(rId.trim())) {
                                    continue;
                                }

                                // 3. Nếu document có lưu trường landlordId, kiểm tra trùng khớp với currentUserId
                                String lId = doc.getString("landlordId");
                                if (lId != null && !lId.trim().isEmpty() && !lId.equals(currentUserId)) {
                                    continue;
                                }

                                rawIssueList.add(issue);
                            }

                            updateRoomFilterOptions();
                            applyFilterAndSort();
                        }
                    });
        } else {
            // Khi chủ trọ quản lý hơn 30 phòng: Lắng nghe toàn bộ sự cố và lọc chính xác phía ứng dụng
            issueListener = db.collection("issues")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener((value, error) -> {
                        pbLoading.setVisibility(View.GONE);

                        if (error != null) {
                            Toast.makeText(this, "Lỗi tải sự cố: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (value != null) {
                            rawIssueList.clear();
                            for (QueryDocumentSnapshot doc : value) {
                                Issue issue = doc.toObject(Issue.class);
                                issue.setIssueId(doc.getId());

                                String rId = issue.getRoomId();
                                // 1. Tuyệt đối không nạp nếu chưa gán phòng
                                if (rId == null || rId.trim().isEmpty()) {
                                    continue;
                                }

                                // 2. Tuyệt đối không nạp nếu không nằm trong danh sách phòng của chủ trọ này
                                if (!myManagedRoomIds.contains(rId.trim())) {
                                    continue;
                                }

                                // 3. Nếu có lưu landlordId thì phải trùng khớp với currentUserId
                                String lId = doc.getString("landlordId");
                                if (lId != null && !lId.trim().isEmpty() && !lId.equals(currentUserId)) {
                                    continue;
                                }

                                rawIssueList.add(issue);
                            }

                            updateRoomFilterOptions();
                            applyFilterAndSort();
                        }
                    });
        }
    }

    /**
     * Tải danh mục phòng người thuê đang ở
     */
    private void loadRentedRoomsForTenant() {
        db.collection("rooms")
                .whereEqualTo("tenantId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    myManagedRoomIds.clear();
                    roomNamesMap.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Room room = doc.toObject(Room.class);
                        if (room != null) {
                            String roomId = doc.getId();
                            myManagedRoomIds.add(roomId);
                            String roomName = room.getName() != null && !room.getName().trim().isEmpty()
                                    ? room.getName().trim()
                                    : "Phòng " + roomId;
                            roomNamesMap.put(roomId, roomName);
                        }
                    }

                    adapter.setRoomNamesMap(roomNamesMap);
                    loadIssuesForTenant();
                })
                .addOnFailureListener(e -> loadIssuesForTenant());
    }

    /**
     * Tải danh sách sự cố của Người thuê (theo tenantId == currentUserId)
     */
    private void loadIssuesForTenant() {
        if (issueListener != null) issueListener.remove();

        pbLoading.setVisibility(View.VISIBLE);

        issueListener = db.collection("issues")
                .whereEqualTo("tenantId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    pbLoading.setVisibility(View.GONE);

                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải sự cố: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        rawIssueList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Issue issue = doc.toObject(Issue.class);
                            issue.setIssueId(doc.getId());
                            rawIssueList.add(issue);
                        }

                        updateRoomFilterOptions();
                        applyFilterAndSort();
                    }
                });
    }

    /**
     * Cập nhật danh sách phòng vào Spinner Lọc phòng:
     * - Chỉ hiển thị các phòng thuộc quyền quản lý của chủ trọ hiện tại.
     */
    private void updateRoomFilterOptions() {
        filterRoomIds.clear();
        filterRoomDisplayNames.clear();

        filterRoomIds.add("ALL");
        filterRoomDisplayNames.add("Tất cả phòng (" + rawIssueList.size() + ")");

        // Duyệt qua tất cả các phòng của chủ trọ này để thêm vào dropdown
        for (String rId : myManagedRoomIds) {
            filterRoomIds.add(rId);
            String displayName = roomNamesMap.containsKey(rId) ? roomNamesMap.get(rId) : "Phòng " + rId;
            filterRoomDisplayNames.add(displayName);
        }

        roomSpinnerAdapter.notifyDataSetChanged();

        // Đảm bảo không bị out of bounds khi dữ liệu phòng thay đổi
        int previousIndex = filterRoomIds.indexOf(selectedFilterRoomId);
        if (previousIndex >= 0) {
            spinnerRoomFilter.setSelection(previousIndex, false);
        } else {
            selectedFilterRoomId = "ALL";
            spinnerRoomFilter.setSelection(0, false);
        }
    }

    /**
     * Áp dụng Bộ lọc theo Phòng và Sắp xếp theo Mức độ ưu tiên
     */
    private void applyFilterAndSort() {
        List<Issue> filtered = new ArrayList<>();

        // 1. Lọc theo Phòng
        for (Issue issue : rawIssueList) {
            if ("ALL".equals(selectedFilterRoomId) || selectedFilterRoomId.equals(issue.getRoomId())) {
                filtered.add(issue);
            }
        }

        // 2. Sắp xếp
        if (selectedSortOption == 0) {
            // Sắp xếp theo Mức độ ưu tiên cao nhất
            Collections.sort(filtered, (i1, i2) -> {
                int score1 = calculatePriorityScore(i1);
                int score2 = calculatePriorityScore(i2);
                if (score1 != score2) {
                    return Integer.compare(score2, score1); // Điểm cao hơn xếp trước
                }
                // Nếu cùng độ ưu tiên, sự cố mới hơn sẽ được xếp lên trước
                if (i1.getCreatedAt() != null && i2.getCreatedAt() != null) {
                    return i2.getCreatedAt().compareTo(i1.getCreatedAt());
                }
                return 0;
            });
        } else if (selectedSortOption == 1) {
            // Sắp xếp Mới nhất lên đầu
            Collections.sort(filtered, (i1, i2) -> {
                if (i1.getCreatedAt() != null && i2.getCreatedAt() != null) {
                    return i2.getCreatedAt().compareTo(i1.getCreatedAt());
                }
                return 0;
            });
        } else if (selectedSortOption == 2) {
            // Sắp xếp Cũ nhất lên đầu
            Collections.sort(filtered, (i1, i2) -> {
                if (i1.getCreatedAt() != null && i2.getCreatedAt() != null) {
                    return i1.getCreatedAt().compareTo(i2.getCreatedAt());
                }
                return 0;
            });
        }

        // 3. Cập nhật giao diện danh sách
        displayedIssueList.clear();
        displayedIssueList.addAll(filtered);
        adapter.notifyDataSetChanged();

        // 4. Cập nhật dòng tóm tắt và hiển thị Empty State nếu rỗng
        if (displayedIssueList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            tvFilterSummary.setText("Không có sự cố nào phù hợp");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
            tvFilterSummary.setText("Đang hiển thị " + displayedIssueList.size() + " sự cố");
        }
    }

    /**
     * Tính toán thang điểm ưu tiên cho sự cố (Điểm càng cao -> Ưu tiên càng lớn)
     * Thang điểm đánh giá:
     * - Khẩn cấp / Điện / Nước chưa xử lý: 95 - 100 điểm
     * - Chờ xử lý thông thường: 70 điểm
     * - Đang xử lý: 40 điểm
     * - Đã giải quyết: 10 điểm
     */
    private int calculatePriorityScore(Issue issue) {
        if (issue == null) return 0;

        // Nếu có trường priority được gán trực tiếp
        String priority = issue.getPriority();
        if (priority != null && !priority.trim().isEmpty()) {
            String p = priority.trim().toUpperCase();
            if (p.contains("KHẨN") || p.contains("URGENT")) return 100;
            if (p.contains("CAO") || p.contains("HIGH")) return 80;
            if (p.contains("TRUNG") || p.contains("MEDIUM")) return 50;
            if (p.contains("THẤP") || p.contains("LOW")) return 20;
        }

        // Tính theo trạng thái và tính chất cấp bách của sự cố
        String status = issue.getStatus() != null ? issue.getStatus().trim().toUpperCase() : "PENDING";
        if (status.contains("DONE") || status.contains("RESOLVED") || status.contains("ĐÃ")) {
            return 10;
        } else if (status.contains("PROCESSING") || status.contains("PROCESS") || status.contains("ĐANG")) {
            return 40;
        } else {
            // Chờ xử lý (PENDING)
            String type = issue.getType() != null ? issue.getType().toLowerCase() : "";
            if (type.contains("điện") || type.contains("nước")) {
                return 95; // Sự cố rò rỉ nước, hỏng điện cần khắc phục khẩn cấp
            }
            return 70; // Các sự cố khác chờ tiếp nhận
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (issueListener != null) {
            issueListener.remove();
        }
    }
}
