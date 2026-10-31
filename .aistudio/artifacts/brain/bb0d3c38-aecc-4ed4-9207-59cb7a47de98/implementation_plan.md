# Camera Viewfinder Fix & Precision Focus/Zoom Upgrade

Eliminates the solid black patch obstructing the center of the camera scanner and upgrades the viewfinder with interactive tap-to-focus and a zoom control slider for reading small courier labels.

## User Review & Critical Decisions

> [!IMPORTANT]
> Based on your confirmed preferences, this update includes:
> - **Confirmed Decision 1 (Clear Cutout Overlay)**: Keep the high-contrast corner brackets and dimmed outer surround while making the center scanning window 100% transparent over the live camera feed.
> - **Confirmed Decision 2 (Tap-to-Focus & Zoom Controls)**: Add interactive tap-to-focus (with an animated focus ring indicator) and a quick zoom slider (`1.0x` – `3.0x` / linear zoom) for small barcodes and fine-print PIN codes.

---

## 1. Overview & Root Cause Analysis

- **Why the Black Patch Happened**: The scanner overlay `Canvas` previously drew a semi-transparent black rectangle over the screen and then called `drawRoundRect(..., blendMode = BlendMode.Clear)` without an isolated offscreen compositing layer. On Android hardware-accelerated canvases, `BlendMode.Clear` punches through the entire Compose layer down to the window background (`Color.Black`), creating an opaque black rectangle right over the `PreviewView` camera stream.
- **How We Fix It**: Replace `BlendMode.Clear` with a geometric **Difference Path** (`Path.op(fullScreenPath, cutoutWindowPath, PathOperation.Difference)`). This draws the dimmed surround *only* outside the central scanning window, leaving the center region completely untouched so the live CameraX feed shines through with zero compositing artifacts on all Android versions.

---

## 2. User Experience & Visual Design

- **Clear Scanning Window**:
  - Central rounded rectangle (`82%` width × `45%` height) is 100% unobstructed.
  - Outer area is shaded with a `45%` dark scrim (`#73000000`) to help users align the courier packet.
  - Electric Cyan (`#38BDF8`) corner brackets and a subtle animated laser line guide label placement without blocking text.
- **Interactive Tap-to-Focus**:
  - Tapping anywhere on the camera preview triggers CameraX `FocusMeteringAction` (`FLAG_AF | FLAG_AE`) at that exact coordinate using `previewView.meteringPointFactory`.
  - Displays a crisp animated Logistics Amber (`#F59E0B`) focus reticle ring at the tapped point that fades out once locked.
- **Smooth Zoom Slider & Quick Presets**:
  - Compact zoom bar positioned just above the bottom shutter dock with `1x`, `2x`, and a continuous slider (`0f` to `1f` linear zoom) so operators can zoom in on small return addresses or dense AWB barcodes without moving the phone closer.

---

## 3. Technical Architecture & Component Flow

```
┌──────────────────────────────────────────────────────────────────────┐
│                        CameraScannerScreen                           │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ Layer 1: CameraX PreviewView (Live Hardware Camera Stream)     │  │
│  │  - Touch Input Listener -> previewView.meteringPointFactory    │  │
│  │  - Triggers cameraControl.startFocusAndMetering(action)        │  │
│  └──────────────────────────────┬─────────────────────────────────┘  │
│                                 ▼                                    │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ Layer 2: Difference-Path Scrim & Reticle Canvas                │  │
│  │  - Outer Path (Full Screen) MINUS Inner Cutout (Rounded Rect)  │  │
│  │  - Zero BlendMode.Clear -> 100% Transparent Center Window      │  │
│  │  - Draws Cyan Corner Brackets + Animated Tap-to-Focus Ring     │  │
│  └──────────────────────────────┬─────────────────────────────────┘  │
│                                 ▼                                    │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ Layer 3: Zoom & Shutter Control Dock                           │  │
│  │  - Zoom Slider + 1x / 2x Quick Pills -> setLinearZoom(value)   │  │
│  │  - Single / Batch Mode Toggle, Torch, Shutter, Gallery & Demo  │  │
│  └────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘
```

### Interactive State Mapping
- **`zoomLevel: Float` (`0f..1f`)**: Bound to the zoom slider and `1x` / `2x` preset pills; calls `camera?.cameraControl?.setLinearZoom(zoomLevel)`.
- **`focusTapOffset: Offset?`**: Updated via `Modifier.pointerInput` tap detector on the preview area; converts screen coordinates into a `MeteringPoint`, initiates auto-focus/auto-exposure, and renders a temporary target indicator on the overlay canvas.
