package org.hear2read.h2rng.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hear2read.h2rng.data.H2RVoice


/**
 * Voice download list item displayed in the [VoiceManagerScreen]. Displays the name of the voices
 * available, along with the download size. If voice is not downloaded, displays download button,
 * else displays delete button. Also displays the download progress while downloading
 *
 * @param h2RVoice [H2RVoice] item representing the voice associated with the list item
 * @param onDownload Function called to download voice
 * @param onDelete Function called to delete voice
 */
@Composable
fun VoiceDownloadItem(
    h2RVoice: H2RVoice,
    onDownload: (H2RVoice) -> Unit,
    onDelete: (H2RVoice) -> Unit
    ) {
    val isDownloaded = h2RVoice.isDownloaded
    val isDownloading = h2RVoice.isDownloading()
    val interactionSource = remember { MutableInteractionSource() }

    val displayText: String
    val displayIcon: ImageVector
    val onClickAction: (H2RVoice) -> Unit
    var enabled = true

    if (isDownloaded) {
        displayText = "Delete"
        displayIcon = Icons.Default.Delete
        onClickAction = { voiceItem ->
            onDelete(voiceItem)
        }
    }
    else if (!isDownloading) {
        displayText = "Download"
        displayIcon = Icons.Default.Download
        onClickAction = { voiceItem ->
            onDownload(voiceItem)
        }
    }
    else {
        enabled = false
        displayText = "Downloading"
        displayIcon = Icons.Default.Download
        onClickAction = { }
    }
//    else {
//        displayIcon = Icons.Default.Refresh
//        displayText = "Retry"
//        onClickAction = { voiceItem ->
//            onDelete(voiceItem)
//            onDownload(voiceItem)
//        }
//    }

    Row (
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
        ) {
            Text(
                text = h2RVoice.locale.displayName,
                fontSize = 32.sp,
                color = Color.White,
//                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                maxLines = 1 // Optional: if voice names can be very long
            )

            if (isDownloading) {
                Spacer(modifier = Modifier.height(8.dp)) // Add vertical space

                LinearProgressIndicator(
                    progress = { h2RVoice.downloadProgress },
                    modifier = Modifier
                        .fillMaxWidth(0.6f),
                )
            }
        }

        Row (
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format("%.1f", (h2RVoice.fileSize / 1e6)) + " MB",
                modifier = Modifier.padding(8.dp),
                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                color = Color.White
            )

            Icon(
                displayIcon,
                contentDescription = displayText,
                modifier = Modifier
                    .clickable(
                        enabled = enabled,
                        interactionSource = interactionSource,
                        indication = ripple()
                    ) {
                        onClickAction(h2RVoice)
                    }
                    .padding(top = 16.dp, start = 0.dp, bottom = 16.dp, end = 16.dp),
                tint = if (enabled)
                    Color.White.copy(alpha = 0.8f)
//                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                else
                    Color.White.copy(alpha = 0.4f)
            )
        }
    }
    HorizontalDivider()
}