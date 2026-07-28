package org.hear2read.h2rng.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hear2read.h2rng.ui.theme.AppColors
import org.hear2read.h2rng.utils.getScriptCharacter
import java.util.Locale

/**
 * Language list item for each language in the [LanguageSelectionScreen]. Initialized by sending
 * the Locale of the language and the function to be called on click. Contains an icon of the
 * letter "ka" in the script corresponding to the language and the name of the language. Used to
 * open the corresponding [TTSDemoScreen]
 *
 * @param langLocale [Locale] of the language
 * @param onClick Function to be called on click
 */
@Composable
fun LanguageListItem(langLocale: Locale, onClick: () -> Unit) {
    val languageName = remember(langLocale) { langLocale.getDisplayLanguage(Locale.ENGLISH) }
    val scriptCharacter = remember(langLocale) { getScriptCharacter(langLocale.language) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "$languageName language. Double tap to select and open demo."
            },
        colors = CardDefaults.cardColors(containerColor = AppColors.surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Script Character Icon
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color = AppColors.backgroundColor,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = scriptCharacter,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
//                    color = AppColors.primaryColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Language Name
            Text(
                text = languageName,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textSecondaryColor,
                modifier = Modifier.weight(1f)
            )

            // Arrow Icon
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
//                tint = AppColors.textSecondaryColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}