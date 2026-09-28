package org.hear2read.h2rng.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
 * Response time card element in the [TTSDemoScreen]. This displays the time taken to start playback
 * after play button is clicked. The time is displayed as milliseconds (ms)
 *
 * @param responseTime The response time in milliseconds. If null, response time is not diplayed
 */
@Composable
fun ResponseTimeCard(responseTime: Long?, realTimeFactor: Float?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = if (responseTime != null) {
                    "Response time: ${responseTime} milliseconds"
                } else {
                    "Response time: Not measured yet"
                }
            },
        colors = CardDefaults.cardColors(containerColor = AppColors.surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Response Time:",
                fontSize = 16.sp,
                color = AppColors.textSecondaryColor,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (responseTime != null) "${responseTime}ms" else "—",
                fontSize = 20.sp,
                color = Color.White,
//                color = AppColors.primaryColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = if (realTimeFactor != null) {
                    "Synthesis speed: ${realTimeFactor} X"
                } else {
                    "Synthesis speed: Not measured yet"
                }
            },
        colors = CardDefaults.cardColors(containerColor = AppColors.surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Synthesis speed:",
                fontSize = 16.sp,
                color = AppColors.textSecondaryColor,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (realTimeFactor != null) "${realTimeFactor}" else "—",
                fontSize = 20.sp,
                color = Color.White,
//                color = AppColors.primaryColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}