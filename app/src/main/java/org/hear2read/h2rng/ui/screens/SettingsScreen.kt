///*
// * This file defines the UI layout for the Settings screen.
// *
// * Functions:
// * @Composable
// * OptionsText(text: String, onOptionClick: () -> Unit)
// *  Displays a single option in the Settings screen.
// *  text: The text to be displayed in the option.
// *  onOptionClick: Custom action to be performed when the option is clicked.
// *
// * @Composable
// * SettingsScreen(navController: NavController)
// *  Displays the setting screen
// *  Calls OptionsText to render each option in the Settings screen.
// */
//
//package org.hear2read.h2rng
//
//import android.content.Intent
//import android.widget.Toast
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.interaction.MutableInteractionSource
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.material3.HorizontalDivider
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.material3.ripple
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.core.net.toUri
//import androidx.navigation.NavController
//
//@Composable
//fun OptionsText(text: String, onOptionClick: () -> Unit) {
//    val interactionSource = remember { MutableInteractionSource() }
//
//    Text(
//        text = text,
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable (
//                interactionSource = interactionSource,
//                indication = ripple(),
//                onClick = onOptionClick
//            )
//            .padding(16.dp),
//        fontSize = MaterialTheme.typography.titleLarge.fontSize
//    )
//
//    HorizontalDivider()
//}
//
//@Composable
//fun SettingsScreen(navController: NavController) {
//    val context = LocalContext.current
//
//    Scaffold (
//        topBar = {
//            GlobalTopBar("Hear2Read")
//        }
//    ) { innerPadding ->
//        LazyColumn(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(innerPadding)
//        ) {
//            item {
////                Spacer(modifier = Modifier.height(16.dp))
////                HorizontalDivider()
//                OptionsText("Install voices") {
//                    navController.navigate(Screen.VoiceManager.route)
//                }
//
//                OptionsText("Open Source Licenses") {
//                    // TODO: Add license screen
//                }
//
//                OptionsText("Feedback") {
//                    val intent = Intent(Intent.ACTION_SENDTO).apply {
//                        data = "mailto:".toUri()
//                        putExtra(Intent.EXTRA_EMAIL, arrayOf("postmaster@feedback.hear2read.org"))
//                        putExtra(Intent.EXTRA_SUBJECT, "Feedback: Hear2Read Android Application")
//                    }
//
//                    if (intent.resolveActivity(context.packageManager) != null) {
//                        context.startActivity(intent)
//                    } else {
//                        Toast.makeText(context, "No email app found.", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
//        }
//    }
//}