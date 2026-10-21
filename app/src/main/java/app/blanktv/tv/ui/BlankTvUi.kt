package app.blanktv.tv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.blanktv.tv.data.db.ChannelEntity
import app.blanktv.tv.di.AppContainer
import app.blanktv.tv.di.AppViewModelFactory
import app.blanktv.tv.domain.MediaKind
import app.blanktv.tv.features.catalogue.CatalogueScreen
import app.blanktv.tv.features.favourites.FavouritesScreen
import app.blanktv.tv.features.home.HomeScreen
import app.blanktv.tv.features.live.LiveScreen
import app.blanktv.tv.features.player.PlayerScreen
import app.blanktv.tv.features.search.SearchScreen
import app.blanktv.tv.features.settings.SettingsScreen
import app.blanktv.tv.ui.theme.BlankTvTheme
import app.blanktv.tv.view.LibraryViewModel
import app.blanktv.tv.view.PlayerViewModel

/**
 * Root composable: theme, navigation graph and the two process-scoped ViewModels.
 *
 * Both ViewModels are created here (owner = the Activity) rather than per destination, so every
 * screen shares one subscription set to the library and playback survives navigation.
 */
@Composable
fun BlankTvUi(container: AppContainer) {
    val dark by container.settings.darkTheme.collectAsStateWithLifecycle(initialValue = true)

    BlankTvTheme(darkTheme = dark) {
        val nav = rememberNavController()
        val factory = remember(container) { AppViewModelFactory(container) }
        val library: LibraryViewModel = viewModel(factory = factory)
        val player: PlayerViewModel = viewModel(factory = factory)

        val openChannel: (ChannelEntity) -> Unit = { channel -> nav.navigate(Routes.player(channel.id)) }

        NavHost(navController = nav, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                Shell(Routes.HOME, nav::go) {
                    HomeScreen(
                        vm = library,
                        onOpenChannel = openChannel,
                        onGoSettings = { nav.go(Routes.SETTINGS) },
                    )
                }
            }
            composable(Routes.LIVE) {
                Shell(Routes.LIVE, nav::go) { LiveScreen(vm = library, onOpenChannel = openChannel) }
            }
            composable(Routes.MOVIES) {
                Shell(Routes.MOVIES, nav::go) {
                    CatalogueScreen(vm = library, kind = MediaKind.Movie, onOpenChannel = openChannel)
                }
            }
            composable(Routes.SERIES) {
                Shell(Routes.SERIES, nav::go) {
                    CatalogueScreen(vm = library, kind = MediaKind.Series, onOpenChannel = openChannel)
                }
            }
            composable(Routes.FAVOURITES) {
                Shell(Routes.FAVOURITES, nav::go) {
                    FavouritesScreen(vm = library, onOpenChannel = openChannel)
                }
            }
            composable(Routes.SEARCH) {
                Shell(Routes.SEARCH, nav::go) { SearchScreen(vm = library, onOpenChannel = openChannel) }
            }
            composable(Routes.SETTINGS) {
                Shell(Routes.SETTINGS, nav::go) { SettingsScreen(vm = library, container = container) }
            }
            composable(
                route = Routes.PLAYER,
                arguments = listOf(navArgument("channelId") { type = NavType.LongType }),
            ) { entry ->
                val channelId = entry.arguments?.getLong("channelId") ?: -1L
                PlayerScreen(
                    channelId = channelId,
                    vm = player,
                    container = container,
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}

/** Single-top navigation that keeps Home as the back-stack root. */
private fun NavHostController.go(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
