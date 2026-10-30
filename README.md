# 📦 CourierLens — On-Device Parcel OCR & Shipment Manifest Organizer

**CourierLens** is a modern Android application that automates parcel logging for courier hubs, dispatch centers, mailrooms, and e-commerce sellers. It scans shipping labels using **100% local, on-device OCR**, extracts structured shipment fields automatically without any cloud API costs, stores records in a local SQLite database, and exports ready-to-share **Microsoft Excel (`.xlsx`)** and **CSV** manifests.

---

## ✨ Key Features

- **💸 100% Cost-Cutting On-Device Vision:** Uses bundled Google ML Kit Text Recognition and a custom logistics regex/heuristic engine. Zero cloud API bills, zero network latency, and works 100% offline inside warehouses.
- **📸 Tri-Mode Capture Workflow:**
  - **Live Batch Scanner:** Continuous CameraX viewfinder with an animated laser guide, torch toggle, and haptic feedback for rapid high-volume scanning.
  - **Single Parcel Capture:** Snap a label and immediately review/refine fields in the precision editor.
  - **Multi-Photo Gallery Import:** Select up to 25 pre-taken parcel photos via the Android Photo Picker for sequential background processing.
- **🧠 Smart Logistics Field Extraction:** Automatically identifies and separates:
  - **Serial Number** (Auto-incremented daily batch IDs, e.g., `SN-20261029-001`)
  - **AWB / Tracking Number**
  - **Courier Partner** (Auto-detects *Delhivery, Blue Dart, DTDC, India Post / Speed Post, Ekart, Shadowfax, Xpressbees, FedEx, DHL, UPS*)
  - **Consignee Name & Mobile Number**
  - **Street Address, City, State, PIN/ZIP Code & Country**
  - **Shipper Name, Order/Invoice Reference ID & Weight**
- **👆 Interactive "Tap-to-Field" OCR Palette:** Unclear or non-standard label? Tap any raw recognized text chip in the editor to assign it directly to any database column in one click.
- **📊 Native Excel (`.xlsx`) & CSV Export:** Generates OpenXML `.xlsx` spreadsheets and UTF-8 `.csv` files locally, complete with an Android Share Sheet integration for WhatsApp, Gmail, Google Sheets, or local storage.

---

## 📲 How to Get / Build the APK

### Option 1: Automatic APK Build in GitHub (GitHub Actions)
This repository includes a pre-configured workflow (`.github/workflows/build-apk.yml`) that automatically builds an installable APK whenever you push code to GitHub:
1. Push this project to your GitHub repository.
2. Open your repository on GitHub and click the **Actions** tab.
3. Click the latest **Build Android APK** workflow run (or trigger it manually via **Run workflow**).
4. Scroll down to **Artifacts** and download **`CourierLens-APK`** (`CourierLens-v1.0-debug.apk`).
5. *(Optional)* Push a version tag like `git tag v1.0.0 && git push origin v1.0.0` to automatically publish the APK under your repository's **GitHub Releases** page.

### Option 2: Download APK Directly from Google AI Studio
- Open the **Settings / Export** menu in the top-right corner of Google AI Studio to directly download the compiled APK/AAB or push the repository to GitHub.

---

## 🛠️ Tech Stack & Architecture

- **UI Framework:** Jetpack Compose (Material Design 3, Dark/Light Theme, Edge-to-Edge)
- **Architecture:** MVVM (`ShipmentViewModel` + `ShipmentRepository` + Kotlin `StateFlow` & Coroutines)
- **Camera & Media:** AndroidX CameraX (`Preview`, `ImageCapture`) + Zero-Permission Android Photo Picker
- **On-Device OCR:** Bundled Google ML Kit Text Recognition (`com.google.mlkit:text-recognition`)
- **Local Persistence:** Jetpack Room Database (SQLite with reactive `Flow` queries & full-text search)
- **Export Engine:** Custom zero-dependency OpenXML `.xlsx` zip generator & RFC-4180 `.csv` writer with `FileProvider`

---

## 🚀 Getting Started Locally

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 11+ (JDK 17 or 21 recommended for Gradle 9)
- Android device or emulator running **API 24 (Android 7.0)** or higher

### Build & Run
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/courier-lens-android.git
   cd courier-lens-android
   ```
2. Open the project in **Android Studio** and allow Gradle to sync.
3. Run the app on a physical device or emulator.
   *(Tip: On emulators without a physical camera, use the built-in **"Demo Scan"** button inside the scanner screen to generate realistic shipping labels and test the OCR + Excel export pipeline immediately!)*

### Running Unit Tests
```bash
gradle :app:testDebugUnitTest
```

---

## 📂 Project Structure

```text
app/src/main/java/com/example/
├── data/
│   ├── db/            # Room Database (AppDatabase, ShipmentDao)
│   ├── model/         # ShipmentEntity schema
│   └── repository/    # ShipmentRepository data abstraction
├── export/
│   └── ManifestExportService.kt      # Native .xlsx and .csv file generator
├── ocr/
│   ├── CourierOcrEngine.kt           # Bundled ML Kit Text Recognition wrapper
│   ├── LogisticsDataExtractor.kt     # Regex & heuristics for AWB, PIN, Address, Couriers
│   └── CourierSampleLabelGenerator.kt# Synthetic shipping label generator for testing
├── ui/
│   ├── components/    # ShipmentCard, StatsCards, Badges
│   ├── dialogs/       # ExportManifestDialog (.xlsx / .csv scope & format picker)
│   ├── screens/       # ShipmentListScreen, CameraScannerScreen, ShipmentDetailEditScreen
│   ├── theme/         # Cargo Navy, Logistics Amber & Electric Cyan M3 Theme
│   └── viewmodel/     # ShipmentViewModel state management
└── MainActivity.kt
```

---

## 📄 License
This project is available for personal and commercial logistics workflows under the MIT License.