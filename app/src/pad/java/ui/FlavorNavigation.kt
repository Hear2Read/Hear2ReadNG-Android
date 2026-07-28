import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import org.hear2read.h2rng.ui.screens.VoiceManagerScreen

/***
 * PAD specific extension adding voice manager screen
 *
 */
fun NavGraphBuilder.addFlavorSpecificScreens(
    navController: NavHostController
) {
    composable("install_voices") {
        VoiceManagerScreen(
            navController = navController
        )
    }
}