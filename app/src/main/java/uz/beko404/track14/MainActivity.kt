package uz.beko404.track14

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import uz.beko404.track14.data.Track14RepositoryProvider
import uz.beko404.track14.domain.model.ThemeMode
import uz.beko404.track14.presentation.app.Track14App
import uz.beko404.track14.ui.theme.Track14Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode = Track14RepositoryProvider.current.snapshot.currentUser.themeMode
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.System -> systemDark
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }

            Track14Theme(darkTheme = darkTheme) {
                Track14App()
            }
        }
    }
}
