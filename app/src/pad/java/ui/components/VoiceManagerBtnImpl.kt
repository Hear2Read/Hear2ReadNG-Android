package ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
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
 * Production implementation of the VoiceManagerBtn interface, in the PAD flavours. Shows a
 * [FloatingActionButton] that performs provided onClick behaviour
 *
 * @constructor Create empty VoiceManagerBtn impl
 */
class VoiceManagerBtnImpl: VoiceManagerBtn {
    /**
     * Floating button that will route to the VoiceManagerScreen. The behaviour is encapsulated in
     * the onInstallVoiceClick function specified in the AppNavigation class
     *
     * @param onInstallVoiceClick onClick behaviour of the button
     */
    @Composable
    override fun floatingButton(onInstallVoiceClick: () -> Unit) {
        FloatingActionButton(
            onClick = onInstallVoiceClick,
            containerColor = Color.White,
            modifier = Modifier
                .navigationBarsPadding()
                .semantics {
                contentDescription = "Install voice button. Opens screen to install additional TTS voices."
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = AppColors.surfaceColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ADD VOICES",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }

}