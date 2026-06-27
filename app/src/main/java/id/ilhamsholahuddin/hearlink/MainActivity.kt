package id.ilhamsholahuddin.hearlink

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.ilhamsholahuddin.hearlink.navigation.Screen
import id.ilhamsholahuddin.hearlink.ui.components.FlashAlertOverlay
import id.ilhamsholahuddin.hearlink.ui.components.HearLinkBottomBar
import id.ilhamsholahuddin.hearlink.ui.home.HomeScreen
import id.ilhamsholahuddin.hearlink.ui.sos.SOSViewModel
import id.ilhamsholahuddin.hearlink.ui.caption.CaptionScreen
import id.ilhamsholahuddin.hearlink.ui.communicator.CommunicatorScreen
import id.ilhamsholahuddin.hearlink.ui.library.LibraryScreen
import id.ilhamsholahuddin.hearlink.ui.library.LibraryDetailScreen
import id.ilhamsholahuddin.hearlink.ui.profile.ProfileScreen
import id.ilhamsholahuddin.hearlink.ui.theme.HearLinkTheme
import id.ilhamsholahuddin.hearlink.viewmodel.CaptionViewModel

class MainActivity : ComponentActivity() {

    // Daftar semua izin yang dibutuhkan aplikasi
    private val requiredPermissions = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.VIBRATE)
        add(Manifest.permission.CAMERA)
        // POST_NOTIFICATIONS hanya diperlukan di Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    // Launcher untuk meminta banyak izin sekaligus
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "Semua izin diberikan. HearLink siap digunakan!", Toast.LENGTH_SHORT).show()
        } else {
            val denied = permissions.filterValues { !it }.keys
            Toast.makeText(
                this,
                "Beberapa izin ditolak: ${denied.joinToString { it.substringAfterLast('.') }}. Beberapa fitur mungkin tidak berfungsi.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Minta semua izin saat aplikasi pertama dibuka
        permissionLauncher.launch(requiredPermissions)

        setContent {
            HearLinkTheme {
                HearLinkApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HearLinkApp(
    sosViewModel: SOSViewModel = viewModel(),
    captionViewModel: CaptionViewModel = viewModel()
) {
    val context = LocalContext.current
    // Inisialisasi NavController untuk mengatur perpindahan halaman
    val navController = rememberNavController()

    val isCountdownActive by sosViewModel.isCountdownActive.collectAsStateWithLifecycle()
    val countdownValue by sosViewModel.countdownValue.collectAsStateWithLifecycle()
    val alertLevel by captionViewModel.alertLevel.collectAsStateWithLifecycle()

    // Sembunyikan SOS saat Smart Conversation Mode aktif
    var isSmartModeActive by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            // Sembunyikan tombol SOS saat berada di Smart Conversation Mode
            if (!isSmartModeActive) {
                FloatingActionButton(
                    onClick = { sosViewModel.startSOSCountdown() },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = "SOS")
                }
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    thickness = 1.dp
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "© 2026 Ilham Sholahuddin (231011403034)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                HearLinkBottomBar(navController = navController)
            }
        }
    ) { innerPadding ->
        // NavHost mengatur halaman mana yang tampil berdasarkan rute
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Home.route) { HomeScreen(navController, onTriggerSOS = { sosViewModel.startSOSCountdown() }) }
                composable(Screen.Caption.route) {
                    CaptionScreen(onSmartModeActive = { isSmartModeActive = it })
                }
                composable(Screen.Communicator.route) { CommunicatorScreen() }
                composable(Screen.Library.route) { LibraryScreen(navController) }
                composable("library_detail/{id}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id")?.toIntOrNull() ?: 0
                    LibraryDetailScreen(id = id, navController = navController)
                }
                composable(Screen.Profile.route) { ProfileScreen() }
            }
            // Overlay flash alert global — tampil di atas semua konten
            val prefs = LocalContext.current.getSharedPreferences("hearlink_settings", android.content.Context.MODE_PRIVATE)
            val isFlashEnabled = prefs.getBoolean("flash_enabled", true)
            if (isFlashEnabled) {
                FlashAlertOverlay(alertLevel = alertLevel)
            }
        }
    }

    if (isCountdownActive) {
        AlertDialog(
            onDismissRequest = { sosViewModel.cancelSOS() },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Peringatan",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SOS DARURAT",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Mengirim pesan darurat dalam...",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = countdownValue.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "HearLink akan mengirimkan koordinat lokasi GPS dan info profil Anda ke semua kontak darurat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                Button(
                    onClick = { sosViewModel.cancelSOS() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("BATAL")
                }
            }
        )
    }
}