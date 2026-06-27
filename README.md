<div align="center">

# HearLink
### *Connecting Beyond Sound*

**Aplikasi Android aksesibilitas untuk penyandang disabilitas pendengaran**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-API%2024+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material Design 3](https://img.shields.io/badge/Material%20Design-3-757575?style=for-the-badge&logo=materialdesign&logoColor=white)](https://m3.material.io/)
[![Room](https://img.shields.io/badge/Room-2.8.4-FF6F00?style=for-the-badge&logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)

</div>

---

## 📖 Deskripsi

**HearLink** adalah aplikasi Android yang dirancang untuk membantu penyandang **disabilitas pendengaran (Deaf & Hard of Hearing)** dalam berkomunikasi, memahami percakapan, mengenali suara penting di lingkungan sekitar, serta meningkatkan keamanan dan kemandirian melalui teknologi aksesibilitas Android.

Aplikasi menggabungkan teknologi **Speech-to-Text**, **Text-to-Speech**, **Sound Alert**, **Emergency SOS**, serta **Sign Language Library** dalam satu platform yang mudah digunakan, intuitif, dan dapat diakses oleh semua kalangan.

> **Proyek ini merupakan Proyek Akhir dari Ilham Sholahuddin (231011403034)**

---

## 🎯 Tujuan

| Tujuan | Keterangan |
|--------|-----------|
| 🎙️ **Live Caption** | Mengubah ucapan menjadi teks secara realtime menggunakan Speech Recognition |
| 📢 **Text-to-Speech** | Membantu pengguna menyampaikan pesan suara kepada orang lain |
| 🔔 **Sound Alert** | Memberikan notifikasi visual dan haptic terhadap suara penting di sekitar |
| 🤟 **Sign Library** | Menyediakan media pembelajaran bahasa isyarat Indonesia (BISINDO) |
| 🚨 **Emergency SOS** | Menyediakan fitur keamanan darurat berbasis GPS dan SMS |

---

## 👥 Target Pengguna

- 🦻 Penyandang Disabilitas Pendengaran (Deaf & Hard of Hearing)
- 👨‍👩‍👧 Keluarga dan pendamping penyandang disabilitas pendengaran
- 👨‍🏫 Guru atau tenaga pendidik
- 🤝 Masyarakat umum yang berinteraksi dengan penyandang disabilitas pendengaran

---

## ✨ Fitur Utama

### 🏠 Home Dashboard
- Sambutan dengan nama pengguna
- Shortcut cepat ke Live Caption, Communicator, Sign Library, dan Transcript
- Status dan toggle Sound Alert langsung dari Home

### 🎤 Caption
- **Live Caption** — Transkripsi suara ke teks secara realtime dengan indikator level suara
- **Transcript Saver** — Simpan, lihat riwayat, edit judul, dan hapus transkrip
- **Sound Alert** — Deteksi level amplitudo audio dan beri notifikasi visual + haptic
- **Smart Conversation Mode** — Mode percakapan dua arah cerdas antara pengguna dan lawan bicara

### 💬 Communicator
- **Quick Phrase** — Kalimat siap pakai per kategori (Identitas, Percakapan, Tempat Umum, Darurat)
- **Custom Text TTS** — Ketik pesan bebas, diputar sebagai suara dengan kecepatan & volume yang dapat disesuaikan
- **Favorite Phrase** — Tandai dan akses cepat kalimat favorit
- **Phrase History** — Riwayat kalimat yang pernah digunakan

### 📚 Library (Sign Language)
- Kamus bahasa isyarat BISINDO dengan kategori: Alfabet (A–Z), Angka (0–9), Salam & Perkenalan, Keluarga, Tempat Umum, dan Darurat
- Pemutaran video/GIF isyarat
- Search berdasarkan kata kunci
- Tambahkan ke Favorit

### 👤 Profile
- **Deaf ID Card** — Kartu identitas digital berisi nama, foto profil, golongan darah, dan catatan alergi
- **Emergency Contact** — Kelola hingga 5 kontak darurat (nama + nomor + WhatsApp flag)
- **Settings** — Ukuran font, Dark/Light Mode, kecepatan TTS, volume, bahasa TTS, dan sensitivitas Sound Alert

### 🚨 Emergency SOS (Global FAB)
- Tombol SOS selalu tersedia di semua halaman
- Alur: Dialog konfirmasi → Countdown 5 detik (bisa dibatalkan) → Ambil GPS → Kirim SMS otomatis + WhatsApp ke semua kontak darurat
- Format pesan berisi nama dan koordinat lokasi Google Maps

---

## 🏗️ Arsitektur

HearLink menggunakan **Clean Architecture** dengan pola **MVVM**:

```
UI Layer (Jetpack Compose)
        ↓
ViewModel (StateFlow / LiveData)
        ↓
Repository
        ↓
Android Services (Speech, TTS, Sound, Location, SMS)
        ↓
Room Database (Local Persistence)
```

### Struktur Package

```
id.ilhamsholahuddin.hearlink
│
├── data/
│   ├── AppDatabase.kt          # Room Database instance
│   ├── Dao.kt                  # DAO interfaces
│   ├── Entities.kt             # Room Entities (User, Transcript, dll.)
│   ├── Repository.kt           # Repository pattern (6 repository)
│   └── SignDictionary.kt       # Data kamus bahasa isyarat
│
├── service/
│   ├── conversation/           # SmartConversationEngine
│   ├── location/               # LocationService (GPS)
│   ├── notification/           # NotificationService
│   ├── sound/                  # SoundAlertService
│   ├── speech/                 # SpeechRecognitionService
│   ├── tts/                    # TextToSpeechService
│   └── vibration/              # HapticService
│
├── navigation/
│   └── NavGraph.kt             # Navigasi utama + rute
│
├── ui/
│   ├── caption/                # CaptionScreen + sub-fitur
│   ├── communicator/           # CommunicatorScreen + sub-fitur
│   ├── components/             # Komponen reusable (BottomBar, FlashAlert, dll.)
│   ├── home/                   # HomeScreen
│   ├── library/                # LibraryScreen + LibraryDetailScreen
│   ├── profile/                # ProfileScreen
│   ├── settings/               # SettingsScreen
│   ├── sos/                    # SOSViewModel
│   └── theme/                  # Color, Typography, Shape (Material 3)
│
├── viewmodel/                  # ViewModel per fitur
└── MainActivity.kt             # Entry point, Scaffold global + FAB SOS
```

---

## 🛠️ Teknologi

| Teknologi | Versi | Digunakan Untuk |
|-----------|-------|-----------------|
| **Kotlin** | 2.0.21 | Bahasa Pemrograman Utama |
| **Jetpack Compose** | BOM 2024.09.00 | UI Framework |
| **Material Design 3** | Latest | Komponen UI Modern |
| **MVVM** | — | Arsitektur Aplikasi |
| **Room Database** | 2.8.4 | Database Lokal |
| **Navigation Compose** | 2.9.8 | Sistem Navigasi Antar Halaman |
| **Coroutines + Flow** | Latest | Pemrograman Asinkron |
| **StateFlow** | — | State Management |
| **SpeechRecognizer API** | Android | Live Caption (Speech-to-Text) |
| **TextToSpeech API** | Android | Quick Communicator (Text-to-Speech) |
| **MediaRecorder / AudioRecord** | Android | Sound Alert (Deteksi Level Suara) |
| **Vibrator / VibrationEffect** | Android | Haptic Pattern Notification |
| **Fused Location Provider** | 21.3.0 | GPS untuk Emergency SOS |
| **SmsManager** | Android | Pengiriman SMS Darurat |
| **KSP** | 2.0.21-1.0.27 | Kotlin Symbol Processing (Room) |

---

## 🔐 Permissions

Aplikasi memerlukan permission berikut:

| Permission | Keperluan |
|------------|-----------|
| `RECORD_AUDIO` | Live Caption & Sound Alert |
| `SEND_SMS` | Emergency SOS |
| `ACCESS_FINE_LOCATION` | GPS untuk SOS |
| `ACCESS_COARSE_LOCATION` | GPS fallback |
| `READ_CONTACTS` | Akses kontak darurat |
| `VIBRATE` | Haptic notification |
| `CAMERA` | Foto profil (Deaf ID Card) |
| `POST_NOTIFICATIONS` | *(Android 13+)* Notifikasi sistem |

---

## 🚀 Cara Menjalankan Proyek

### Prasyarat

- Android Studio **Ladybug** atau lebih baru
- JDK 17+
- Android SDK API level **24** (Android 7.0) atau lebih tinggi
- Perangkat/Emulator dengan microphone aktif

### Langkah Instalasi

1. **Clone repository**
   ```bash
   git clone https://github.com/ilhamsholahuddin16/HearLink.git
   ```

2. **Buka di Android Studio**
   ```
   File → Open → Pilih folder HearLink
   ```

3. **Sync Gradle**
   ```
   Android Studio akan otomatis melakukan sync. Tunggu hingga selesai.
   ```

4. **Jalankan aplikasi**
   ```
   Run → Run 'app' (Shift + F10)
   ```

5. **Izinkan semua permission** saat diminta pertama kali

---

## 📱 Navigasi Aplikasi

```
╔══════════════════════════════════════════════════════╗
║                                                🚨 SOS║
║  🏠 Home  🎤 Caption  💬 Communicator  📚 Library  👤 Profile ║
╚══════════════════════════════════════════════════════╝
```

## 👨‍💻 Developer

| | |
|---|---|
| **Nama** | Ilham Sholahuddin |
| **NIM** | 231011403034 |
| **GitHub** | [@ilhamsholahuddin16](https://github.com/ilhamsholahuddin16) |

---
---

<div align="center">

*"HearLink bertujuan menjadi aplikasi aksesibilitas terpercaya yang membantu penyandang disabilitas pendengaran berkomunikasi secara efektif, memahami lingkungan sekitar, dan merasa aman dalam kehidupan sehari-hari."*

**⭐ Jika proyek ini bermanfaat, jangan lupa beri bintang!**

</div>
