@file:OptIn(ExperimentalMaterial3Api::class)

package uz.beko404.track14.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.beko404.track14.R
import uz.beko404.track14.core.designsystem.Manrope
import uz.beko404.track14.core.designsystem.T14Icon
import uz.beko404.track14.core.designsystem.T14Paths
import uz.beko404.track14.core.designsystem.T14Theme
import uz.beko404.track14.core.designsystem.Track14Colors
import uz.beko404.track14.core.device.ExternalLinks
import uz.beko404.track14.domain.repository.Session
import uz.beko404.track14.feature.messageRes

/**
 * "Davom etish uchun kiring" sheet from the canvas. Shown over the current screen when an action needs
 * an account; closes itself and calls [onSignedIn] once Firebase reports the session.
 */
@Composable
fun SignInSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSignedIn: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val auth by authViewModel.state.collectAsStateWithLifecycle()
    val tokenClient = rememberGoogleIdTokenClient()
    LaunchedEffect(visible, auth.session) {
        if (visible && auth.session is Session.SignedIn) {
            onDismiss()
            onSignedIn()
        }
    }
    if (!visible) return

    val c = T14Theme.colors
    val context = LocalContext.current
    val privacyUrl = stringResource(R.string.privacy_url)
    ModalBottomSheet(
        onDismissRequest = { authViewModel.dismissError(); onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color(0x8C111827),
        dragHandle = {
            Box(Modifier.padding(top = 12.dp).size(width = 40.dp, height = 5.dp).clip(RoundedCornerShape(999.dp)).background(if (c.dark) Color(0xFF334155) else Color(0xFFCBD5E1)))
        },
    ) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(c.blueSoft), contentAlignment = Alignment.Center) {
                T14Icon(T14Paths.Lock, size = 32.dp, tint = Track14Colors.Blue600)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.signin_title), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                Text(stringResource(R.string.signin_body), fontSize = 15.sp, lineHeight = 23.sp, color = c.textBody)
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.amberSoft)
                    .border(1.dp, c.amberBorder, RoundedCornerShape(16.dp)).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                T14Icon(T14Paths.Info, size = 22.dp, tint = if (c.dark) c.amberText else Color(0xFFB45309))
                Text(stringResource(R.string.signin_same_account), fontSize = 14.sp, lineHeight = 21.sp, color = if (c.dark) c.amberText else Color(0xFF78350F))
            }
            auth.error?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            Row(
                Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)
                    .border(1.5.dp, if (c.dark) c.fieldBorder else Color(0xFFCBD5E1), RoundedCornerShape(16.dp))
                    .clickable(enabled = !auth.isSigningIn) { authViewModel.signIn { tokenClient.getIdToken() } },
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (auth.isSigningIn) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Track14Colors.Blue600)
                } else {
                    Box(Modifier.size(26.dp).clip(CircleShape).background(if (c.dark) Color(0xFFF1F5F9) else Color(0xFF0F172A)), contentAlignment = Alignment.Center) {
                        Text("G", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (c.dark) Color(0xFF0B1220) else Color.White)
                    }
                }
                Text(stringResource(R.string.sign_in_google), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(
                stringResource(R.string.signin_consent),
                fontSize = 12.sp, lineHeight = 18.sp, color = c.blueSoftText, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { ExternalLinks.openTrustedUrl(context, privacyUrl) }.padding(4.dp),
            )
        }
    }
}
