package id.ilhamsholahuddin.hearlink.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

// Sealed class untuk menyimpan rute, nama, dan ikon untuk Bottom Navigation
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Caption : Screen("caption", "Caption", Icons.Default.Mic)
    object Communicator : Screen("communicator", "Chat", Icons.Default.Chat)
    object Library : Screen("library", "Library", Icons.Default.MenuBook)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
}