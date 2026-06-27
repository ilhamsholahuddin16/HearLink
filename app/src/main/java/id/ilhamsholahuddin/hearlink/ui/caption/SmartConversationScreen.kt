package id.ilhamsholahuddin.hearlink.ui.caption

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.ilhamsholahuddin.hearlink.data.ConversationTurn
import id.ilhamsholahuddin.hearlink.service.conversation.SmartConversationEngine.Companion.SPEAKER_ME
import id.ilhamsholahuddin.hearlink.service.conversation.SmartConversationEngine.Companion.SPEAKER_OTHER
import id.ilhamsholahuddin.hearlink.viewmodel.SaveStatus
import id.ilhamsholahuddin.hearlink.viewmodel.SmartConversationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartConversationScreen(
    vm: SmartConversationViewModel = viewModel()
) {
    val turns           by vm.turns.collectAsStateWithLifecycle()
    val isSessionActive by vm.isSessionActive.collectAsStateWithLifecycle()
    val isListening     by vm.isListening.collectAsStateWithLifecycle()
    val isOtherSpeaking by vm.isOtherSpeaking.collectAsStateWithLifecycle()
    val durationMs      by vm.sessionDurationMs.collectAsStateWithLifecycle()
    val saveStatus      by vm.saveStatus.collectAsStateWithLifecycle()

    var showSaveDialog  by remember { mutableStateOf(false) }
    var titleInput      by remember { mutableStateOf("") }
    var inputText       by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    // Auto-scroll ke bawah saat turn baru masuk
    LaunchedEffect(turns.size) {
        if (turns.isNotEmpty()) listState.animateScrollToItem(turns.size - 1)
    }

    // Auto reset save status setelah 2.5 detik
    LaunchedEffect(saveStatus) {
        if (saveStatus is SaveStatus.Success || saveStatus is SaveStatus.Error) {
            kotlinx.coroutines.delay(2500)
            vm.resetSaveStatus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header ─────────────────────────────────────────────────────────────
        SessionHeader(
            isSessionActive = isSessionActive,
            isListening     = isListening,
            durationMs      = durationMs,
            onStop          = { vm.stopSession() },
            onSave          = { showSaveDialog = true }
        )

        // ── Konten Utama ───────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when {
                // 1. Sesi belum pernah dimulai → Placeholder + Tombol Start
                !isSessionActive && turns.isEmpty() -> {
                    StartSessionPlaceholder(onStart = { vm.startSession() })
                }

                // 2. Sesi baru selesai (ada riwayat chat) → Tampilkan chat + tombol aksi
                !isSessionActive && turns.isNotEmpty() -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Chat history (non-scrollable preview dalam mode selesai)
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            items(turns, key = { it.timestampMs }) { turn ->
                                ConversationBubble(turn = turn)
                            }
                        }

                        // ── Tombol setelah sesi selesai ───────────────────────
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Tombol Reset Chat
                            OutlinedButton(
                                onClick = { vm.clearChat() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Reset Chat")
                            }

                            // Tombol Mulai Lagi
                            Button(
                                onClick = { vm.startSession() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Mulai Lagi")
                            }
                        }
                    }
                }

                // 3. Sesi sedang aktif → Chat + Input
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(turns, key = { it.timestampMs }) { turn ->
                            ConversationBubble(turn = turn)
                        }
                        // Indikator lawan bicara sedang bicara
                        if (isOtherSpeaking) {
                            item { TypingIndicator() }
                        }
                    }
                }
            }

            // Snackbar sukses simpan
            if (saveStatus is SaveStatus.Success) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "✅ Percakapan berhasil disimpan!",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // ── Input Chat (hanya saat sesi AKTIF) ────────────────────────────────
        AnimatedVisibility(
            visible = isSessionActive,
            enter   = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit    = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Column {
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tombol Mikrofon (lawan bicara)
                    IconButton(
                        onClick = { vm.toggleListening() },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (isListening) MaterialTheme.colorScheme.errorContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                    ) {
                        Icon(
                            if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isListening) "Berhenti Rekam" else "Mulai Rekam",
                            modifier = Modifier.size(24.dp),
                            tint = if (isListening) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // TextField input pengguna
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ketik pesan Anda…", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank()) {
                                vm.sendTypedMessage(inputText)
                                inputText = ""
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    // Tombol Kirim
                    FloatingActionButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                vm.sendTypedMessage(inputText)
                                inputText = ""
                            }
                        },
                        modifier       = Modifier.size(48.dp),
                        shape          = CircleShape,
                        containerColor = if (inputText.isNotBlank())
                                             MaterialTheme.colorScheme.primary
                                         else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Kirim",
                            modifier = Modifier.size(20.dp),
                            tint = if (inputText.isNotBlank())
                                       MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // ── Dialog Simpan ─────────────────────────────────────────────────────────
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Simpan Percakapan") },
            text = {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Judul (opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    vm.saveSession(titleInput)
                    showSaveDialog = false
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Batal") }
            }
        )
    }
}

// ── Placeholder (sebelum sesi dimulai) ───────────────────────────────────────

@Composable
private fun StartSessionPlaceholder(onStart: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.08f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💬", fontSize = 64.sp)
        Spacer(Modifier.height(20.dp))
        Text(
            "Smart Conversation Mode",
            style     = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Anda mengetik → Suara (TTS)\nLawan bicara berbicara → Teks (STT)",
            style      = MaterialTheme.typography.bodySmall,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign  = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(36.dp))

        // Tombol START di tengah layar dengan efek pulsasi
        Box(
            modifier = Modifier
                .scale(scale)
                .size(88.dp)
                .background(
                    brush = Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick  = onStart,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Mulai Sesi",
                    modifier           = Modifier.size(44.dp),
                    tint               = Color.White
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Ketuk untuk mulai",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Session Header ────────────────────────────────────────────────────────────

@Composable
private fun SessionHeader(
    isSessionActive : Boolean,
    isListening     : Boolean,
    durationMs      : Long,
    onStop          : () -> Unit,
    onSave          : () -> Unit
) {
    Surface(
        color          = if (isSessionActive) MaterialTheme.colorScheme.primaryContainer
                         else MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Smart Conversation",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                if (isSessionActive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(
                                    if (isListening) Color(0xFFFF3B30) else Color(0xFFFFCC00),
                                    CircleShape
                                )
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            formatDuration(durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        "Mode percakapan dua arah",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSessionActive) {
                IconButton(onClick = onSave) {
                    Icon(Icons.Default.Save, contentDescription = "Simpan")
                }
                IconButton(onClick = onStop) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ── Bubble Chat ───────────────────────────────────────────────────────────────

@Composable
private fun ConversationBubble(turn: ConversationTurn) {
    val isMe    = turn.speakerLabel == SPEAKER_ME
    val timeStr = remember(turn.timestampMs) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(turn.timestampMs))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        if (!isMe) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) { Text("👤", fontSize = 14.sp) }
            Spacer(Modifier.width(6.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 270.dp)
        ) {
            Text(
                if (isMe) "Saya" else "Lawan Bicara",
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
            Box(
                modifier = Modifier
                    .background(
                        brush = if (isMe) Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            )
                        ) else Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        ),
                        shape = RoundedCornerShape(
                            topStart    = if (isMe) 16.dp else 4.dp,
                            topEnd      = if (isMe) 4.dp  else 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd   = 16.dp
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    turn.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMe) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                timeStr,
                style    = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }

        if (isMe) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) { Text("🙋", fontSize = 14.sp) }
        }
    }
}

// ── Typing Indicator ──────────────────────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val alpha by infiniteTransition.animateFloat(
        initialValue  = 0.3f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "typing_alpha"
    )
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center
        ) { Text("👤", fontSize = 12.sp) }
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                "● ● ●",
                style = MaterialTheme.typography.labelMedium.copy(
                    color         = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    letterSpacing = 3.sp
                )
            )
        }
    }
}

// ── Helper ────────────────────────────────────────────────────────────────────

private fun formatDuration(ms: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
