package id.ilhamsholahuddin.hearlink.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import id.ilhamsholahuddin.hearlink.navigation.Screen
import id.ilhamsholahuddin.hearlink.viewmodel.HomeViewModel
import id.ilhamsholahuddin.hearlink.service.vibration.HapticPattern
import kotlin.math.roundToInt

// Solid, distinct card colors — no reliance on colorScheme containers that can conflict
private val CardMic    = Color(0xFF1A73E8) // Google Blue
private val CardChat   = Color(0xFF00897B) // Teal
private val CardBook   = Color(0xFF3949AB) // Indigo
private val CardSOS    = Color(0xFFD32F2F) // Deep Red

@Composable
fun HomeScreen(
    navController: NavController,
    onTriggerSOS: () -> Unit = {},
    vm: HomeViewModel = viewModel()
) {
    var isVisible by remember { mutableStateOf(false) }
    val isSoundAlertEnabled by vm.isSoundAlertEnabled.collectAsStateWithLifecycle()
    val decibelLevel by vm.decibelLevel.collectAsStateWithLifecycle()
    val soundAlertError by vm.soundAlertError.collectAsStateWithLifecycle()
    val isVibrationEnabled by vm.isVibrationEnabled.collectAsStateWithLifecycle()
    val isFlashEnabled by vm.isFlashEnabled.collectAsStateWithLifecycle()
    val selectedHapticPattern by vm.selectedHapticPattern.collectAsStateWithLifecycle()
    
    var showSettingsSheet by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.onPermissionGranted()
    }

    LaunchedEffect(Unit) { isVisible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Selamat Datang,",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Text(
            text = "Pengguna HearLink",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(500)) +
                    slideInVertically(animationSpec = tween(500), initialOffsetY = { 50 })
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // ---- Sound Alert Card ----
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            soundAlertError != null -> MaterialTheme.colorScheme.errorContainer
                            isSoundAlertEnabled -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSoundAlertEnabled)
                                    Icons.Default.NotificationsActive
                                else
                                    Icons.Default.NotificationsOff,
                                contentDescription = "Sound Alert",
                                tint = when {
                                    soundAlertError != null -> MaterialTheme.colorScheme.onErrorContainer
                                    isSoundAlertEnabled -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        soundAlertError != null -> "Sound Alert Error"
                                        isSoundAlertEnabled -> "Sound Alert Aktif"
                                        else -> "Sound Alert Nonaktif"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = when {
                                        soundAlertError != null -> MaterialTheme.colorScheme.onErrorContainer
                                        isSoundAlertEnabled -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Text(
                                    text = when {
                                        soundAlertError != null -> soundAlertError!!
                                        isSoundAlertEnabled -> "Mendeteksi suara di sekitar — ${decibelLevel.roundToInt()} dB"
                                        else -> "Ketuk untuk mengaktifkan"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when {
                                        soundAlertError != null -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                        isSoundAlertEnabled -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    }
                                )
                            }
                            IconButton(onClick = { showSettingsSheet = true }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Pengaturan Sound Alert",
                                    tint = if (isSoundAlertEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isSoundAlertEnabled,
                                onCheckedChange = { _ ->
                                    if (isSoundAlertEnabled) {
                                        // Sedang aktif → matikan
                                        vm.toggleSoundAlert()
                                    } else {
                                        // Sedang nonaktif → aktifkan (minta izin jika belum ada)
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        vm.toggleSoundAlert()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        // Decibel progress bar — shown only when active and no error
                        if (isSoundAlertEnabled && soundAlertError == null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            // Normalize dB 0–90 to 0f–1f
                            val progress = (decibelLevel / 90f).coerceIn(0f, 1f)
                            val barColor = when {
                                decibelLevel > 70 -> MaterialTheme.colorScheme.error
                                decibelLevel > 50 -> Color(0xFFF57C00) // Orange
                                else -> MaterialTheme.colorScheme.primary
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = barColor,
                                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                            )
                        }
                    }
                }

                // ---- Pintasan Cepat ----
                Text(
                    text = "Pintasan Cepat",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ShortcutCard(
                        modifier = Modifier.weight(1f),
                        title = "Live Caption",
                        icon = Icons.Default.Mic,
                        cardColor = CardMic,
                        onClick = {
                            navController.navigate(Screen.Caption.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        }
                    )
                    ShortcutCard(
                        modifier = Modifier.weight(1f),
                        title = "Komunikator",
                        icon = Icons.AutoMirrored.Filled.Chat,
                        cardColor = CardChat,
                        onClick = {
                            navController.navigate(Screen.Communicator.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ShortcutCard(
                        modifier = Modifier.weight(1f),
                        title = "Kamus Isyarat",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        cardColor = CardBook,
                        onClick = {
                            navController.navigate(Screen.Library.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        }
                    )
                    ShortcutCard(
                        modifier = Modifier.weight(1f),
                        title = "SOS Darurat",
                        icon = Icons.Default.ReportProblem, // ReportProblem is single-color unlike Warning
                        cardColor = CardSOS,
                        onClick = onTriggerSOS
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    cardColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Force tint to pure white — prevents icon's intrinsic color from bleeding through
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}
