package id.ilhamsholahuddin.hearlink.ui.caption

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.ilhamsholahuddin.hearlink.data.Transcript
import id.ilhamsholahuddin.hearlink.viewmodel.CaptionViewModel
import id.ilhamsholahuddin.hearlink.viewmodel.HomeViewModel
import androidx.compose.foundation.clickable
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.runtime.DisposableEffect

@Composable
fun CaptionScreen(
    vm: CaptionViewModel = viewModel(),
    onSmartModeActive: (Boolean) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Live Caption", "Smart Mode")

    // Beritahu parent kapanpun tab berganti
    LaunchedEffect(selectedTab) {
        onSmartModeActive(selectedTab == 1)
    }
    // Cleanup saat meninggalkan layar
    DisposableEffect(Unit) {
        onDispose { onSmartModeActive(false) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> LiveCaptionContent(vm = vm)
            1 -> SmartConversationScreen()
        }
    }
}

@Composable
private fun LiveCaptionContent(vm: CaptionViewModel) {

        // Dapatkan HomeViewModel yang sama dengan HomeScreen (satu Activity scope)
        // untuk bisa pause/resume Sound Alert saat mic digunakan oleh SpeechRecognizer
        val homeVm: HomeViewModel = viewModel()

        // Pause Sound Alert (AudioRecord) saat layar ini aktif agar tidak konflik mic
        DisposableEffect(Unit) {
            homeVm.pauseSoundAlertForExternalMicUse()
            onDispose {
                // Saat meninggalkan layar, pastikan SpeechRecognizer berhenti
                // lalu kembalikan Sound Alert ke state sebelumnya
                if (vm.isListening.value) {
                    vm.toggleListening()
                }
                homeVm.resumeSoundAlertIfEnabled()
            }
        }

        val transcription by vm.transcription.collectAsStateWithLifecycle()
        val isListening by vm.isListening.collectAsStateWithLifecycle()
        val decibelLevel by vm.decibelLevel.collectAsStateWithLifecycle()
        val allTranscripts by vm.allTranscripts.collectAsStateWithLifecycle()
        var showSaveDialog by remember { mutableStateOf(false) }
        var transcriptTitleInput by remember { mutableStateOf("") }
        var selectedTranscript by remember { mutableStateOf<Transcript?>(null) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Live Caption",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
            )

            // Visualizer Level Suara
            DecibelVisualizer(
                decibelLevel = decibelLevel,
                isListening = isListening,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Area Transkripsi
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 24.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                if (transcription.isBlank()) {
                    Text(
                        text = if (isListening) "Mendengarkan..." else "Tekan tombol mikrofon untuk mulai merekam suara...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Start
                    )
                } else {
                    Text(
                        text = transcription.trim(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start
                    )
                }
            }

            // Tombol Aksi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tombol Simpan
                OutlinedButton(
                    onClick = {
                        transcriptTitleInput = ""
                        showSaveDialog = true
                    },
                    enabled = transcription.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan")
                }

                // Tombol Hapus
                OutlinedButton(
                    onClick = { vm.clearTranscription() },
                    enabled = transcription.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hapus")
                }
            }

            // Tombol Mikrofon Utama
            FloatingActionButton(
                onClick = { vm.toggleListening() },
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                contentColor = if (isListening) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isListening) "Stop" else "Mulai",
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = if (isListening) "Ketuk untuk berhenti" else "Ketuk untuk merekam",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Riwayat Transkrip tersimpan
            if (allTranscripts.isNotEmpty()) {
                Divider(modifier = Modifier.padding(horizontal = 24.dp))
                Text(
                    text = "Riwayat Transkrip",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allTranscripts) { transcript ->
                        TranscriptItem(
                            transcript = transcript,
                            onClick = { selectedTranscript = transcript },
                            onDelete = { vm.deleteTranscript(it) }
                        )
                    }
                }
            }
        }

        // Dialog Simpan Transkrip
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Simpan Transkrip") },
                text = {
                    OutlinedTextField(
                        value = transcriptTitleInput,
                        onValueChange = { transcriptTitleInput = it },
                        label = { Text("Judul (opsional)") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        vm.saveTranscript(transcriptTitleInput)
                        showSaveDialog = false
                    }) { Text("Simpan") }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) { Text("Batal") }
                }
            )
        }

        // Dialog Detail Transkrip
        selectedTranscript?.let { transcript ->
            TranscriptDetailDialog(
                transcript = transcript,
                onDismiss = { selectedTranscript = null },
                onDelete = {
                    vm.deleteTranscript(it)
                    selectedTranscript = null
                }
            )
        }
    }

    @Composable
    private fun DecibelVisualizer(
        decibelLevel: Float,
        isListening: Boolean,
        modifier: Modifier = Modifier
    ) {
        // decibelLevel kini berupa rmsdB dari SpeechRecognizer (range ~0–10)
        // Normalisasi berbeda dari SoundAlertService yang menggunakan dB (30–90)
        val normalizedLevel = if (isListening) (decibelLevel.coerceIn(0f, 10f) / 10f) else 0f
        val animatedLevel by animateFloatAsState(targetValue = normalizedLevel, label = "dB")

        val barColor = when {
            normalizedLevel > 0.7f -> Color(0xFFFF3B30)
            normalizedLevel > 0.4f -> Color(0xFFFFCC00)
            else -> MaterialTheme.colorScheme.primary
        }

        Card(
            modifier = modifier.height(80.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(20) { index ->
                    val barHeight = if (isListening) {
                        val factor = 0.2f + (index % 5) * 0.15f
                        (animatedLevel * factor * 50).dp + 4.dp
                    } else 4.dp

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(barHeight)
                            .clip(RoundedCornerShape(4.dp))
                            .background(barColor.copy(alpha = 0.7f + index * 0.01f))
                    )
                }
            }
        }
    }

    @Composable
    private fun TranscriptItem(
        transcript: Transcript,
        onClick: () -> Unit,
        onDelete: (Transcript) -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transcript.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = transcript.content.take(60) + if (transcript.content.length > 60) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                IconButton(onClick = { onDelete(transcript) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    @Composable
    private fun TranscriptDetailDialog(
        transcript: Transcript,
        onDismiss: () -> Unit,
        onDelete: (Transcript) -> Unit
    ) {
        val context = LocalContext.current
        val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }
        val dateString = dateFormat.format(Date(transcript.createdAt))

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = transcript.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = transcript.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = {
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Transkrip", transcript.content)
                        clipboard.setPrimaryClip(clip)
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Salin")
                    }
                    IconButton(onClick = {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, transcript.content)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Bagikan")
                    }
                }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onDelete(transcript) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Hapus",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Tutup")
                    }
                }
            }
        )
    }