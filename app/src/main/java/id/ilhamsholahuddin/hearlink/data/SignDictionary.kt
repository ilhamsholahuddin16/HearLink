package id.ilhamsholahuddin.hearlink.data

data class SignWord(
    val id: Int,
    val title: String,
    val category: String,
    val description: String,
    val videoId: String,
    val startTime: Int = 0
)

val signDictionary = listOf(
    // 1. Kategori: Abjad (ID: Hx8IU6CfMIM)
    SignWord(1, "A", "Abjad", "Isyarat A", "Hx8IU6CfMIM", 10),
    SignWord(2, "B", "Abjad", "Isyarat B", "Hx8IU6CfMIM", 15),
    SignWord(3, "C", "Abjad", "Isyarat C", "Hx8IU6CfMIM", 20),
    // ... (Lanjutkan pola ini untuk huruf D-Z dengan interval 5 detik)

    // 2. Kategori: Kata Sapa (ID: xnxydJPDD1M)
    SignWord(27, "Halo", "Kata Sapa", "Menyapa", "xnxydJPDD1M", 10),
    SignWord(28, "Selamat Pagi", "Kata Sapa", "Pagi", "xnxydJPDD1M", 25),
    SignWord(29, "Selamat Siang", "Kata Sapa", "Siang", "xnxydJPDD1M", 35),

    // 3. Kategori: Kata Sifat (ID: lio9OmhZa5I)
    SignWord(49, "Ganteng", "Kata Sifat", "Ganteng", "lio9OmhZa5I", 15),
    SignWord(50, "Cantik", "Kata Sifat", "Cantik", "lio9OmhZa5I", 25),
    SignWord(51, "Baik", "Kata Sifat", "Baik", "lio9OmhZa5I", 35),

    // 4. Kategori: Keluarga (ID: 4icuKB1w5Z0)
    SignWord(64, "Ayah", "Keluarga", "Ayah", "4icuKB1w5Z0", 15),
    SignWord(65, "Ibu", "Keluarga", "Ibu", "4icuKB1w5Z0", 25),
    SignWord(66, "Kakak", "Keluarga", "Kakak", "4icuKB1w5Z0", 35),

    // 5. Kategori: Transportasi (ID: lor4YdtK8tU)
    SignWord(77, "Mobil", "Transportasi", "Mobil", "lor4YdtK8tU", 15),
    SignWord(78, "Motor", "Transportasi", "Motor", "lor4YdtK8tU", 25),
    SignWord(79, "Sepeda", "Transportasi", "Sepeda", "lor4YdtK8tU", 35),

    // 6. Kategori: Profesi (ID: MIIh0EVnbJI)
    SignWord(88, "Guru", "Profesi Kerja", "Guru", "MIIh0EVnbJI", 20),
    SignWord(89, "Dokter", "Profesi Kerja", "Dokter", "MIIh0EVnbJI", 35),
    SignWord(90, "Polisi", "Profesi Kerja", "Polisi", "MIIh0EVnbJI", 50),

    // 7. Kategori: Hari (ID: Cls9oklykKo)
    SignWord(110, "Senin", "Hari", "Senin", "Cls9oklykKo", 15),
    SignWord(111, "Selasa", "Hari", "Selasa", "Cls9oklykKo", 25),
    SignWord(112, "Rabu", "Hari", "Rabu", "Cls9oklykKo", 35),

    // 8. Kategori: Angka (ID: 5UN60jB4eKg)
    SignWord(125, "1", "Angka", "Angka 1", "5UN60jB4eKg", 10),
    SignWord(126, "2", "Angka", "Angka 2", "5UN60jB4eKg", 15),
    SignWord(127, "3", "Angka", "Angka 3", "5UN60jB4eKg", 20),

    // 9. Kategori: Olahraga (ID: mssWGGRUMiw)
    SignWord(141, "Sepak Bola", "Olahraga", "Sepak Bola", "mssWGGRUMiw", 20),
    SignWord(142, "Basket", "Olahraga", "Basket", "mssWGGRUMiw", 35),
    SignWord(143, "Badminton", "Olahraga", "Badminton", "mssWGGRUMiw", 50),

    // 10. Kategori: Buah-buahan (ID: Qhx0_ctwd_4)
    SignWord(154, "Apel", "Buah-buahan", "Apel", "Qhx0_ctwd_4", 35),
    SignWord(155, "Jeruk", "Buah-buahan", "Jeruk", "Qhx0_ctwd_4", 47),
    SignWord(156, "Pisang", "Buah-buahan", "Pisang", "Qhx0_ctwd_4", 57)
)
