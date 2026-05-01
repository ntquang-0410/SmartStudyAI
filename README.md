# SmartStudy AI

Ứng dụng Android học tập thông minh sử dụng **Google Gemini AI** để quét đề bài bằng camera, giải bài tập tự động và tạo quiz luyện tập từ nội dung đã giải.

## Tính năng chính

- **Quét bài (QuetBai)** — chụp ảnh đề bài bằng camera (CameraX) hoặc chọn ảnh từ thư viện, sau đó gửi tới Gemini Vision để OCR + giải.
- **Giải bài tự động (Solve)** — hiển thị lời giải dạng Markdown/LaTeX (render qua `MarkdownToHtml`), hỗ trợ phân loại môn học (`SubjectClassifier`).
- **Tạo Quiz tự động (QuizGeneratorService)** — sinh bộ câu hỏi trắc nghiệm trực tiếp từ nội dung bài đã giải, làm bài và chấm điểm trong `QuizSessionActivity`.
- **Lịch sử quét (LichSu)** — lưu lại các lần quét và cho phép xem chi tiết, mở ảnh full-screen.
- **Nhắc nhở học tập (Reminders)** — đặt lịch nhắc, chạy nền qua `AlarmManager` + `BootReceiver` để khôi phục sau khi khởi động lại máy.
- **Theo dõi phiên học (StudySessionTracker)** — ghi nhận thời lượng học và thông báo trong `NotificationsActivity`.
- **Hồ sơ người dùng** — đăng nhập, đăng ký, quản lý profile (`LoginActivity`, `RegisterActivity`, `ProfileActivity`).

## Công nghệ sử dụng

| Thành phần | Phiên bản / Mô tả |
|---|---|
| Ngôn ngữ | Java (chính) + Kotlin DSL cho Gradle |
| `compileSdk` / `targetSdk` | 36 |
| `minSdk` | 26 (Android 8.0) |
| Java | 11 |
| AI | `com.google.ai.client.generativeai:generativeai:0.9.0` (Gemini) |
| Camera | AndroidX CameraX 1.4.0 |
| UI | Material Components, ViewBinding, ConstraintLayout |
| Async | Guava `ListenableFuture` |

## Cấu trúc dự án

```
app/src/main/java/com/example/final_project/
├── MainActivity.java              # BottomNav: Trang chủ / Quét / Luyện tập
├── SplashActivity.java            # Màn hình khởi động
├── TrangChuFragment.java          # Tab Trang chủ
├── QuetBaiFragment.java           # Tab Quét bài (camera + chọn ảnh)
├── QuizFragment.java              # Tab Luyện tập (danh sách quiz)
├── SolveFragment.java             # Hiển thị lời giải
├── GeminiVisionService.java       # Gọi Gemini Vision (multi-model fallback)
├── GeminiTextRunner.java          # Gọi Gemini text generation
├── QuizGeneratorService.java      # Sinh quiz JSON từ nội dung bài giải
├── ScanHistoryRepository.java     # Lưu lịch sử quét
├── ReminderScheduler.java         # AlarmManager cho nhắc nhở
└── ...
```

## Yêu cầu môi trường

- Android Studio (Hedgehog trở lên)
- JDK 11
- Gradle 9.1.0 (có sẵn `gradlew`)
- Tài khoản Google AI Studio để lấy **Gemini API Key**

## Cấu hình API key

Project đọc API key từ `local.properties` (ưu tiên) hoặc biến môi trường `GEMINI_API_KEY`.

**Cách 1 — `local.properties` (khuyên dùng):**

```properties
GEMINI_API_KEY=your_api_key_here
```

**Cách 2 — biến môi trường:**

```powershell
$env:GEMINI_API_KEY = "your_api_key_here"
```

> File `local.properties` đã được Git ignore. Không commit API key.

## Build & chạy

```powershell
# Build debug APK
.\gradlew.bat assembleDebug

# Cài lên thiết bị/emulator đang kết nối
.\gradlew.bat installDebug

# Chạy unit test
.\gradlew.bat test
```

Hoặc mở project bằng Android Studio → chọn cấu hình `app` → Run.

## Quyền (Permissions)

Khai báo trong `AndroidManifest.xml`:

- `android.permission.CAMERA` — chụp ảnh đề bài
- `android.permission.INTERNET` — gọi Gemini API
- `FileProvider` — chia sẻ ảnh tạm với camera

## Luồng hoạt động chính

1. Người dùng mở tab **Quét bài** → chụp/chọn ảnh đề.
2. `GeminiVisionService` gửi ảnh tới Gemini (thử lần lượt các model trong `MODEL_CANDIDATES`).
3. Kết quả lời giải hiển thị ở `SolveFragment`, lưu vào `ScanHistoryRepository`.
4. Người dùng có thể bấm **Tạo quiz** → `QuizGeneratorService` sinh JSON quiz → mở `QuizSessionActivity` để làm bài.
5. Lịch sử và quiz được lưu cục bộ và có thể xem lại trong tab **Luyện tập** / **Lịch sử**.

## Ghi chú

- Project sử dụng `viewBinding` và `buildConfig`, đảm bảo bật khi thêm module mới.
- Các model Gemini được thử theo thứ tự fallback: `gemini-3-flash-preview` → `gemini-2.0-flash` → `gemini-2.0-flash-lite-preview`.
- Nội dung trả về được render qua `MarkdownToHtml` để hỗ trợ công thức LaTeX.
