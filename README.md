# MEFABZ Scanner - Invoice Voice Reader

Production-grade Android app that scans **MEFABZ-only invoices**, extracts products + page number, and reads them aloud with synchronized UI highlighting.

## Stack
- Kotlin 2.3.10
- Jetpack Compose 1.10.3 + Material 3 (dark-only theme)
- MVVM + Clean Architecture + Hilt DI
- CameraX 1.5.3 (`camera-compose`) - pinned for AGP 8.8.1 compatibility
- ML Kit Text Recognition (`com.google.mlkit:text-recognition`) - free, on-device OCR
- Android TextToSpeech (Google engine) with utterance range tracking

## Setup
1. Open `/Users/rockykumar/project/mefabz voice ` in Android Studio.
2. Ensure `local.properties` points to your Android SDK:
   ```properties
   sdk.dir=/path/to/Android/sdk
   ```
3. Sync Gradle and run on a device/emulator with camera support.

## Core Behavior
- Scans invoice using camera preview + capture.
- Runs free on-device OCR with ML Kit (no API key, no paid model call).
- Rejects non-MEFABZ invoices.
- Extracts only product descriptions and strict numeric footer page number.
- Displays results with **Page X** neon header.
- Auto-plays narration:
  - `MEFABZ Invoice detected. Products listed are: ... Page X.`
- Highlights currently spoken product line via TTS utterance range callbacks.

## Key Source Paths
- App entry: `/Users/rockykumar/project/mefabz voice /app/src/main/java/com/mefabz/scanner/MainActivity.kt`
- Scanner UI: `/Users/rockykumar/project/mefabz voice /app/src/main/java/com/mefabz/scanner/feature/scanner/presentation/ScannerScreen.kt`
- Result UI: `/Users/rockykumar/project/mefabz voice /app/src/main/java/com/mefabz/scanner/feature/scanner/presentation/ResultScreen.kt`
- ViewModel: `/Users/rockykumar/project/mefabz voice /app/src/main/java/com/mefabz/scanner/feature/scanner/presentation/ScannerViewModel.kt`
- OCR data source: `/Users/rockykumar/project/mefabz voice /app/src/main/java/com/mefabz/scanner/data/remote/OcrInvoiceDataSource.kt`
- TTS manager: `/Users/rockykumar/project/mefabz voice /app/src/main/java/com/mefabz/scanner/core/tts/GoogleTtsManager.kt`
