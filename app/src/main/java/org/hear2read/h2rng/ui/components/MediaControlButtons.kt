package org.hear2read.h2rng.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hear2read.h2rng.ui.theme.AppColors

/**
 * Media control buttons displayed in the [TTSDemoScreen] to control playback. The buttons are:
 * - Play/Pause: Plays or pauses playback. On pause, the playback is reset at sentence level
 * - Previous: Plays the previous sentence, if present
 * - Next: Plays the next sentence, if present
 * - Clear/Reset: Clears text if present, else resets to default text
 *
 * @param isPlaying mutableState<boolean> that signals if TTS is playing or not
 * @param hasText mutableState<boolean> that signals if the input text is empty or not
 * @param onPlayPause Function to be called on play/pause button being clicked
 * @param onPrevious Function to be called on previous button being clicked
 * @param onNext Function to be called on next button being clicked
 * @param onClearReset Function to be called on clear/reset button being clicked
 */
@Composable
fun MediaControlButtons(
    isPlaying: Boolean,
    hasText: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClearReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Playback Controls",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textColor,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .semantics { contentDescription = "Playback Controls Section" }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Button
                IconButton(
                    onClick = onPrevious,
//                    enabled = hasPrevious,
                    modifier = Modifier
                        .size(56.dp)
                        .semantics {
                            contentDescription = //if (hasPrevious) {
                                "Previous sentence button"
//                            } else {
//                                "Previous sentence button, disabled"
//                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = null,
//                        tint = if (hasPrevious) AppColors.buttonBackgroundColor else AppColors.buttonDisabledColor,
                        tint = AppColors.buttonBackgroundColor,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play/Pause Button
                FloatingActionButton(
                    onClick = onPlayPause,
                    containerColor = if (hasText) AppColors.buttonBackgroundColor else AppColors.buttonDisabledColor,
                    modifier = Modifier
                        .size(72.dp)
                        .semantics {
                            contentDescription = if (isPlaying) {
                                "Pause playback button"
                            } else {
                                "Play text to speech button"
                            }
                        },
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Next Button
                IconButton(
                    onClick = onNext,
//                    enabled = hasNext,
                    modifier = Modifier
                        .size(56.dp)
                        .semantics {
                            contentDescription = //if (hasNext) {
                                "Next sentence button"
//                            } else {
//                                "Next sentence button, disabled"
//                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = null,
//                        tint = if (hasNext) AppColors.buttonBackgroundColor else AppColors.buttonDisabledColor,
                        tint = AppColors.buttonBackgroundColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Clear/Reset Button
            OutlinedButton(
                onClick = onClearReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .semantics {
                        contentDescription = if (hasText) {
                            "Clear text button"
                        } else {
                            "Reset to sample text button"
                        }
                    },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppColors.buttonBackgroundColor
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = if (hasText) Icons.Default.Clear else Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasText) "Clear Text" else "Reset to Sample",
                    fontSize = 16.sp
                )
            }
        }
    }
}