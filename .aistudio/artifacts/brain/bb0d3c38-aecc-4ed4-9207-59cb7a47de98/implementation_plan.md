# CourierLens AI - On-Device Parcel OCR & Shipment Organizer

A 100% cost-free, on-device courier label scanner and shipment manifest organizer that extracts tracking numbers, addresses, PIN codes, and consignee details locally without cloud API costs, with direct Excel (.xlsx) and CSV export.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and UX choices have been synthesized directly from your clarified requirements:
> - **100% On-Device Zero-Cost OCR**: Powered entirely by Google ML Kit on-device Vision Text Recognition and a dedicated courier logistics pattern matching engine. Zero recurring cloud API charges, zero latency, and fully functional offline.
> - **Tri-Mode Capture & Ingestion**: Full support for (1) continuous Live Camera batch scanning with real-time bounding overlays, (2) Single-Package precision capture with instant field refinement, and (3) Multi-image photo gallery importer for pre-taken parcel photos.
> - **Spreadsheet Exporters**: Native Microsoft Excel (.xlsx) formatted XML/workbook export and standardized CSV output, shareable directly via Android Sheets, WhatsApp, Gmail, or Cloud Drive.

---

## 1. Overview & Core Concept

- **What It Does**: Automates the daily logistics grind for warehouse operators, dispatch managers, e-commerce sellers, and courier franchises. Users point their camera at parcel shipping labels (or pick photos from gallery); the app instantly parses key shipment metadata into structured fields (AWB/Tracking #, Receiver Name, Phone, Address, PIN code, City, State, Country, Reference/Order #, Weight, Courier Partner) and logs them into a searchable Room database.
- **Target Audience / Persona**: Courier agents, dispatch clerks, small e-commerce business owners, delivery hubs, and administrative mailroom staff processing 10 to 500+ packages a day.
- **Key Value**: Saves hours of manual typing, eliminates transcription errors, runs 100% locally with zero server costs, and generates clean Excel spreadsheets ready for courier portals and accounting.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Dashboard & Manifest Hub**:
   - Header with quick statistics: Total Scanned Today, Pending Dispatch, Unique Cities, and Recent Batches.
   - Prominent Quick-Action Bar: **"Batch Scan"**, **"Single Scan"**, **"Import Photos"**, and **"Export Manifest"**.
   - Search & Filter Bar: Instant search by AWB, Name, Phone, City, or PIN code, with status chips (All, Ready, Dispatched, Delivered).
   - Shipment Cards: High-density logistics cards displaying AWB badge, recipient name, city + PIN code, timestamp, and thumbnail. Tap to inspect/edit; swipe or long-press to batch select.

2. **Live Viewfinder & Batch Capture**:
   - CameraX live feed with edge guide brackets and real-time text recognition bounding highlights.
   - Quick "Capture & Next" mode: Audio/haptic beep upon successful AWB + PIN detection, preview bubble at the bottom, and auto-incrementing serial counter.
   - Continuous scanning counter showing scanned parcels in the current batch.

3. **Single Scan & Instant Precision Editor**:
   - Immediate extraction preview sheet displaying extracted fields with confidence indicators:
     - Serial Number (auto-incremented or extracted)
     - AWB / Tracking No (with auto-detected courier: Delhivery, BlueDart, DTDC, FedEx, Speed Post, Ekart, etc.)
     - Consignee Name & Contact Phone
     - Shipper / Sender details
     - Address Line 1 & 2
     - City, PIN Code, State, Country
     - Reference / Order ID / Invoice No
     - Weight / Dimensions & Package Type
   - Interactive OCR Raw Text Inspector: Allows tapping any detected raw text line to assign it directly to a field.

4. **Batch Gallery Import**:
   - Android Photo Picker allows selecting multiple parcel photos at once.
   - Background sequential processing queue with progress indicator and review screen.

5. **Export & Share Manifest**:
   - Export modal offering **Excel (.xlsx)** or **CSV (.csv)** formats.
   - Column selector toggle (choose which fields to include in the spreadsheet).
   - Date range and status filters.
   - Direct Android Share sheet to email, send via WhatsApp, or save to Device Downloads.

### Visual Identity & Theme

- **Aesthetic Direction**: High-contrast, clean logistics utilitarian design. Crisp typography, legible data tables, and rapid-glance color badges.
- **Color Palette**:
  - *Primary*: Deep Cargo Navy (`#0F172A` / `#1E293B`)
  - *Accent & Brand*: Logistics Amber & Electric Cyan (`#F59E0B` & `#0284C7`)
  - *Status Badges*: Success Emerald (`#10B981`), Processing Amber (`#F59E0B`), Alert Rose (`#F43F5E`)
  - *Backgrounds*: Slate Frost (`#F8FAFC` light, `#0B0F19` dark)
  - *Surfaces*: Pure white elevated cards (`#FFFFFF` light, `#161F30` dark)
- **Typography & Hierarchy**:
  - High-legibility monospaced display for Tracking Numbers and PIN codes (`JetBrains Mono` / Monospace).
  - Clean M3 Sans-Serif (`Roboto` / Inter) for headings and metadata labels.
- **Haptics & Feedback**:
  - Distinctive tactile vibration and subtle chime on successful AWB & PIN detection.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: 100% On-Device ML Kit Text Recognition vs. Cloud Vision API**
  - *Chosen Approach*: On-device Google ML Kit Text Recognition bundled with an intelligent logistics rule-based heuristic parser.
  - *Why*: User explicitly mandated 100% cost cutting. Zero cloud API calls means zero billings, 50ms instant latency, total privacy, and 100% offline capability inside warehouses with spotty internet.
  - *Trade-off*: Complex or highly distorted handwritten labels are handled via an interactive "Tap-to-Field" raw text inspector so users can tap any recognized text chunk to assign it in seconds.

- **Decision 2: Tri-Mode Capture Architecture (Batch, Single, Gallery)**
  - *Chosen Approach*: Unified CameraX + Android Photo Picker interface with a switchable capture mode.
  - *Why*: Accommodates both high-volume speed scanning (50 packages in 2 minutes) and high-accuracy single package dispatch logging.

- **Decision 3: Native Spreadsheet Generation (.xlsx and .csv)**
  - *Chosen Approach*: Structured XML-based Microsoft Excel (.xlsx) generator and RFC-4180 compliant CSV generator with Android FileProvider URI sharing.
  - *Why*: Directly opens in Microsoft Excel, Google Sheets, LibreOffice, and logistics CRM systems without third-party heavy dependencies or server calls.

- **Decision 4: Local Room Database with Full-Text Search (FTS)**
  - *Chosen Approach*: Jetpack Room Database with reactive Kotlin Flows and search indices.
  - *Why*: Super-fast queries even with thousands of archived shipments, persistent across app restarts and reboots.

---

## 4. Technical Architecture & Data Strategy

### System Architecture & Data Flow Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Capture Layer                                 │
│  ┌───────────────────────┐  ┌──────────────────────┐  ┌─────────────┐  │
│  │ Live CameraX Scanner  │  │ Single Parcel Camera │  │ Photo Picker│  │
│  │ (Continuous Viewfind) │  │  (Precision Freeze)  │  │  (Multi-Img)│  │
│  └───────────┬───────────┘  └──────────┬───────────┘  └──────┬──────┘  │
└──────────────┼─────────────────────────┼─────────────────────┼─────────┘
               │                         │                     │
               ▼                         ▼                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                   On-Device Logistics Vision Engine                    │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ Google ML Kit On-Device Text Recognizer (Zero Cost / Offline)    │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
│                                     ▼                                  │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ Courier Logistics Heuristic & Regex Extraction Engine            │  │
│  │  - AWB / Tracking Patterns (Delhivery, BlueDart, DTDC, FedEx...) │  │
│  │  - Postal Code Extractor (6-digit PIN, ZIP codes)                │  │
│  │  - Address, City, State, Country Entity Separator                │  │
│  │  - Consignee Name & Phone Number Heuristics                      │  │
│  │  - Order / Invoice / Ref No & Weight Parsing                     │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
└─────────────────────────────────────┼──────────────────────────────────┘
                                      │
                                      ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       ViewModel & Repository Layer                     │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ ShipmentViewModel (StateFlow, Search, Filter, Batch Operations)  │  │
│  └───────────────────┬──────────────────────────────┬───────────────┘  │
│                      ▼                              ▼                  │
│       ┌───────────────────────────┐  ┌──────────────────────────────┐  │
│       │ ShipmentRepository        │  │ ManifestExportService        │  │
│       │ (Room DAO Operations)     │  │ (.xlsx & .csv File Generator)│  │
│       └──────────────┬────────────┘  └──────────────┬───────────────┘  │
└──────────────────────┼──────────────────────────────┼──────────────────┘
                       ▼                              ▼
┌──────────────────────────────┐              ┌──────────────────────────┐
│ SQLite / Room Local Database │              │ Android FileProvider     │
│ (ShipmentEntity Table)       │              │ (Share to Excel/WhatsApp)│
└──────────────────────────────┘              └──────────────────────────┘
```

### Core Data Entities

- **`ShipmentEntity`**:
  - `id`: Long (Auto-generated Primary Key)
  - `serialNumber`: String (Auto-incremented batch counter, e.g. "SN-2026-0001")
  - `awbNumber`: String (Indexed Air Waybill / Tracking number)
  - `courierPartner`: String (Auto-detected: Delhivery, Blue Dart, DTDC, Speed Post, FedEx, DHL, Ekart, etc.)
  - `consigneeName`: String (Receiver name)
  - `consigneePhone`: String (Phone / Mobile)
  - `addressLine`: String (Street address / Building)
  - `city`: String (Destination city)
  - `state`: String (Destination state / province)
  - `pincode`: String (Postal code / PIN code)
  - `country`: String (Default: India / International)
  - `shipperName`: String (Sender / Vendor name)
  - `referenceNumber`: String (Order ID / Invoice #)
  - `weight`: String (e.g. "0.5 kg", "2.1 lbs")
  - `status`: String ("Scanned", "Manifested", "Dispatched", "Delivered")
  - `rawOcrText`: String (Full text for audit and manual field correction)
  - `photoUri`: String? (Local cached image file path)
  - `createdAt`: Long (Epoch timestamp)
  - `batchId`: String (Grouping batches by day or session)

### Component & Screen Layout

1. **`MainScaffold`**: Navigation with top app bar (stats, search, multi-select action mode) and floating capture action button.
2. **`ShipmentListScreen`**: High-efficiency filterable list with sticky date headers, statistics summary cards, and batch export actions.
3. **`LiveCameraScannerScreen`**: Fullscreen CameraX viewfinder with bounding boxes, sound/vibration feedback, and quick review footer.
4. **`ShipmentDetailEditScreen`**: Interactive field editor with "Tap OCR Text" palette for one-click correction and label image preview.
5. **`ExportManifestDialog`**: Date-range filtering, format toggle (Excel vs CSV), column selection, and instant share intent.

---

## 5. Verification & Implementation Steps

1. Configure CameraX and ML Kit Text Recognition dependencies in `build.gradle.kts`.
2. Define the Room Database schema (`ShipmentEntity`, `ShipmentDao`, `ShipmentDatabase`) and Repository.
3. Build the Courier Logistics OCR parser with comprehensive regex patterns for major carriers, Indian PIN codes, international ZIPs, phone numbers, and addresses.
4. Construct the CameraX live preview with real-time text analysis and batch capture mode.
5. Implement the Excel (.xlsx XML) and CSV manifest generator with Android FileProvider sharing.
6. Assemble the Jetpack Compose Material 3 UI with dark/light themes, search/filtering, and detail editor.
7. Run compilation and unit tests to verify database operations, regex parsing accuracy, and export formatting.
