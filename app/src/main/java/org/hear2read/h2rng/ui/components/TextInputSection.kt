package org.hear2read.h2rng.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hear2read.h2rng.ui.theme.AppColors

/**
 * Text input section element in the [TTSDemoScreen]. This contains the text that will be sent to
 * the TTS to synthesize.
 *
 * @param text The text to be displayed and synthesised
 * @param onTextChange Function to be called when the text is changed
 * @param modifier [Modifier] of the parent [TTSDemoScreen] used to configure the UI display
 */
@Composable
fun TextInputSection(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Text Input",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textColor,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .semantics { contentDescription = "Text Input Section" }
            )

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .semantics {
                        contentDescription = "Editable text field. Current text: ${if (text.isBlank()) "Empty" else text}"
                    },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.primaryColor,
                    unfocusedBorderColor = AppColors.textSecondaryColor,
                    focusedTextColor = AppColors.textColor,
                    unfocusedTextColor = AppColors.textColor
                ),
                placeholder = {
                    Text(
                        "Enter text here...",
                        color = AppColors.textSecondaryColor
                    )
                }
            )
        }
    }
}