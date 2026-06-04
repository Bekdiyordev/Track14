package uz.beko404.track14

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import uz.beko404.track14.presentation.app.Track14App
import uz.beko404.track14.ui.theme.Track14Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Track14Theme {
                Track14App()
            }
        }
    }
}