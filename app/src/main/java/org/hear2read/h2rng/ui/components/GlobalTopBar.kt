package org.hear2read.h2rng.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hear2read.h2rng.R
import org.hear2read.h2rng.ui.theme.AppColors

/**
 * Global top bar to be used in all screens. The text is configurable and will generally be the
 * title of the screen
 *
 * @param text Text to be displayed in the top bar
 * @param onBack Function to be called on back button being pressed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalTopBar(text: String, onBack: () -> Unit) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
                ) {
                Image(
                    // TODO change this to the horizontal logo
                    painter = painterResource(id = R.drawable.hear2read_logo),
                    contentDescription = "Hear2Read Logo",
//                    modifier = Modifier
//                        .padding(end = 8.dp)
                )
                Spacer(Modifier.weight(1f))
                Text(text,
                    fontSize = 20.sp,
//                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .semantics {
                            contentDescription = "$text Screen"
                        }
                        .padding(end = 8.dp))
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier.semantics {
                    contentDescription = "Navigate back"
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = AppColors.backgroundColor
//                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
//            containerColor = AppColors.primaryColor,
            containerColor = Color.White,
//            titleContentColor = Color.White
            titleContentColor = AppColors.backgroundColor
        )
    )
}