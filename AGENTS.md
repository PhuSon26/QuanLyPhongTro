# RentHub - Project Instructions for Agents

## Tech Stack & Architecture
- Language: Java (Android Studio structure).
- UI Framework: XML Layouts, Material Design Components, RecyclerViews.
- Local Storage: SQLite Database Helpers.
- Cloud Services: Firebase Authentication, Firebase Realtime Database / Firestore, Firebase Storage.

## Coding Rules
- Tất cả comment và mô tả JavaDoc phải viết bằng tiếng Việt.
- Tuân thủ mô hình MVP hoặc MVVM, tách biệt logic UI (Activity/Fragment) và xử lý Data (Repository/SQLite Helper).
- Các class Adapter cho RecyclerView phải tối ưu bằng ViewHolder pattern.
- Mọi thao tác truy vấn Database hoặc Network call bắt buộc phải xử lý bất đồng bộ (AsyncTask/ExecutorService) để tránh treo UI.