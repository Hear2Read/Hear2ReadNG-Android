package ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hear2read.h2rng.ui.components.VoiceManagerBtn
import org.hear2read.h2rng.ui.theme.AppColors

/**
 * Local implementation of the VoiceManagerBtn interface. Does nothing as this screen is not
 * present in the "local" flavor
 *
 * @constructor
 */
class VoiceManagerBtnImpl: VoiceManagerBtn {
    /**
     * Empty composable
     *
     * @param onInstallVoiceClick unused onClick behaviour, set to empty lambda in AppNavigation
     */
    @Composable
    override fun floatingButton(onInstallVoiceClick: () -> Unit) {
        // no button in local version
        // Crashlytics test
//        FloatingActionButton(
//            onClick = { throw RuntimeException("Test Crash") },
//            containerColor = Color.White,
//            modifier = Modifier.semantics {
//                contentDescription = "Install voice button. Opens screen to install additional TTS voices."
//            }
//        ) {
//            Row(verticalAlignment = Alignment.CenterVertically,
//                modifier = Modifier.padding(horizontal = 8.dp)) {
//                Icon(
//                    imageVector = Icons.Default.Add,
//                    contentDescription = null,
//                    tint = AppColors.surfaceColor
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                Text(
//                    text = "ADD VOICES",
//                    fontSize = 20.sp,
//                    modifier = Modifier.padding(end = 8.dp)
//                )
//            }
//        }
    }
}