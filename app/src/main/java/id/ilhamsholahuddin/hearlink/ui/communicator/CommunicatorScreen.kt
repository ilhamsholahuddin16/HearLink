package id.ilhamsholahuddin.hearlink.ui.communicator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.ilhamsholahuddin.hearlink.data.CustomPhrase
import id.ilhamsholahuddin.hearlink.data.FavoritePhrase
import id.ilhamsholahuddin.hearlink.viewmodel.CommunicatorViewModel

@Composable
fun CommunicatorScreen(
    vm: CommunicatorViewModel = viewModel()
) {
    val favoritePhrases by vm.favoritePhrases.collectAsStateWithLifecycle()
    val customPhrases by vm.customPhrases.collectAsStateWithLifecycle()
    var ttsSpeed by remember { mutableStateOf(1.0f) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Frasa Cepat", "Teks Kustom", "Favorit")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Komunikator",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, style = MaterialTheme.typography.labelLarge) }
                )
            }
        }

        when (selectedTab) {
            0 -> QuickPhraseTab(
                customPhrases = customPhrases,
                favoritePhrases = favoritePhrases,
                ttsSpeed = ttsSpeed,
                onSpeak = { vm.speak(it, ttsSpeed) },
                onToggleFavorite = { phrase ->
                    val existing = favoritePhrases.find { it.phrase == phrase }
                    if (existing != null) vm.removeFavorite(existing)
                    else vm.addFavorite(phrase, "quick")
                },
                onAddCustomPhrase = { vm.addCustomPhrase(it) },
                onEditCustomPhrase = { cp, newText -> vm.updateCustomPhrase(cp, newText) },
                onDeleteCustomPhrase = { vm.deleteCustomPhrase(it) }
            )
            1 -> CustomTextTab(
                ttsSpeed = ttsSpeed,
                onSpeedChange = { ttsSpeed = it },
                onSpeak = { vm.speak(it, ttsSpeed) },
                onSaveCustomPhrase = { vm.addCustomPhrase(it) }
            )
            2 -> FavoriteTab(
                favorites = favoritePhrases,
                onSpeak = { vm.speak(it, ttsSpeed) },
                onDelete = { vm.removeFavorite(it) },
                onEdit = { fav, newText -> vm.updateFavorite(fav, newText) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Tab: Frasa Cepat
// ---------------------------------------------------------------------------

@Composable
private fun QuickPhraseTab(
    customPhrases: List<CustomPhrase>,
    favoritePhrases: List<FavoritePhrase>,
    ttsSpeed: Float,
    onSpeak: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onAddCustomPhrase: (String) -> Unit,
    onEditCustomPhrase: (CustomPhrase, String) -> Unit,
    onDeleteCustomPhrase: (CustomPhrase) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        PhraseInputDialog(
            title = "Tambah Frasa Baru",
            initialText = "",
            onConfirm = { text ->
                onAddCustomPhrase(text)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        // Custom phrases (editable & deletable)
        items(customPhrases, key = { "custom_${it.id}" }) { cp ->
            val isFavorite = favoritePhrases.any { it.phrase == cp.phrase }
            var showEditDialog by remember { mutableStateOf(false) }
            if (showEditDialog) {
                PhraseInputDialog(
                    title = "Edit Frasa",
                    initialText = cp.phrase,
                    onConfirm = { newText ->
                        onEditCustomPhrase(cp, newText)
                        showEditDialog = false
                    },
                    onDismiss = { showEditDialog = false }
                )
            }
            PhraseCard(
                text = cp.phrase,
                isFavorite = isFavorite,
                onSpeak = { onSpeak(cp.phrase) },
                onToggleFavorite = { onToggleFavorite(cp.phrase) },
                canEdit = true,
                onEdit = { showEditDialog = true },
                onDelete = { onDeleteCustomPhrase(cp) }
            )
        }

        // Add custom phrase button
        item {
            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tambah Frasa Kustom")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Tab: Teks Kustom
// ---------------------------------------------------------------------------

@Composable
private fun CustomTextTab(
    ttsSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onSpeak: (String) -> Unit,
    onSaveCustomPhrase: (String) -> Unit
) {
    var customText by remember { mutableStateOf("") }
    var savedConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(savedConfirmation) {
        if (savedConfirmation) {
            kotlinx.coroutines.delay(2000)
            savedConfirmation = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = customText,
            onValueChange = { customText = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            label = { Text("Ketik pesan Anda...") },
            shape = RoundedCornerShape(16.dp)
        )

        // TTS speed slider
        Column {
            Text(
                "Kecepatan Suara: ${String.format("%.1fx", ttsSpeed)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Slider(
                value = ttsSpeed,
                onValueChange = onSpeedChange,
                valueRange = 0.5f..2.0f,
                steps = 5
            )
        }

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onSpeak(customText) },
                enabled = customText.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Putar Suara")
            }

            OutlinedButton(
                onClick = {
                    onSaveCustomPhrase(customText)
                    savedConfirmation = true
                    customText = ""
                },
                enabled = customText.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simpan Frasa")
            }
        }

        if (savedConfirmation) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "✓ Frasa berhasil disimpan ke Frasa Cepat",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Tab: Favorit
// ---------------------------------------------------------------------------

@Composable
private fun FavoriteTab(
    favorites: List<FavoritePhrase>,
    onSpeak: (String) -> Unit,
    onDelete: (FavoritePhrase) -> Unit,
    onEdit: (FavoritePhrase, String) -> Unit
) {
    if (favorites.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Belum ada frasa favorit.\nTambahkan dari tab Frasa Cepat.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(favorites, key = { it.id }) { fav ->
                var showEditDialog by remember { mutableStateOf(false) }
                if (showEditDialog) {
                    PhraseInputDialog(
                        title = "Edit Frasa Favorit",
                        initialText = fav.phrase,
                        onConfirm = { newText ->
                            onEdit(fav, newText)
                            showEditDialog = false
                        },
                        onDismiss = { showEditDialog = false }
                    )
                }
                PhraseCard(
                    text = fav.phrase,
                    isFavorite = true,
                    onSpeak = { onSpeak(fav.phrase) },
                    onToggleFavorite = { onDelete(fav) },
                    canEdit = true,
                    onEdit = { showEditDialog = true },
                    onDelete = { onDelete(fav) }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared: PhraseCard
// ---------------------------------------------------------------------------

@Composable
private fun PhraseCard(
    text: String,
    isFavorite: Boolean,
    onSpeak: () -> Unit,
    onToggleFavorite: () -> Unit,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Frasa") },
            text = { Text("Hapus \"$text\"?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        onClick = onSpeak
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )

            // Edit button (only for editable cards)
            if (canEdit) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Favorite toggle
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Hapus favorit" else "Tambah favorit",
                    tint = if (isFavorite) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared: Phrase Input Dialog (Add / Edit)
// ---------------------------------------------------------------------------

@Composable
private fun PhraseInputDialog(
    title: String,
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Teks frasa") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onConfirm(text.trim()) },
                enabled = text.isNotBlank()
            ) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
