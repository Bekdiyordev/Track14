package uz.beko404.track14.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import uz.beko404.track14.R
import uz.beko404.track14.data.auth.GoogleIdTokenClient

/** Google account picker bound to the current Activity context. */
@Composable
fun rememberGoogleIdTokenClient(): GoogleIdTokenClient {
    val context = LocalContext.current
    val webClientId = stringResource(R.string.google_web_client_id)
    return remember(context, webClientId) { GoogleIdTokenClient(context, webClientId) }
}
