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

## 🛠️ Tech Stack & Architecture

- **UI Framework:** Jetpack Compose (Material Design 3, Dark/Light Theme, Edge-to-Edge)
- **Architecture:** MVVM (`ShipmentViewModel` + `ShipmentRepository` + Kotlin `StateFlow` & Coroutines)
- **Camera & Media:** AndroidX CameraX (`Preview`, `ImageCapture`) + Zero-Permission Android Photo Picker
- **On-Device OCR:** Bundled Google ML Kit Text Recognition (`com.google.mlkit:text-recognition`)
- **Local Persistence:** Jetpack Room Database (SQLite with reactive `Flow` queries & full-text search)
- **Export Engine:** Custom zero-dependency OpenXML `.xlsx` zip generator & RFC-4180 `.csv` writer with `FileProvider`

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 11+
- Android device or emulator running **API 24 (Android 7.0)** or higher

### Build & Run
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/courier-lens-android.git
   cd courier-lens-android