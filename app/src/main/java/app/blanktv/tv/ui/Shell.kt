package app.blanktv.tv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.blanktv.tv.R
import app.blanktv.tv.ui.components.FocusBox

/** Every destination in the app. The player route carries the channel id as a path argument. */
object Routes {
    const val HOME = "home"
    const val LIVE = "live"
    const val MOVIES = "movies"
    const val SERIES = "series"
    const val FAVOURITES = "favourites"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val PLAYER = "player/{channelId}"

    fun player(channelId: Long) = "player/$channelId"
}

private data class RailEntry(val route: String, val labelRes: Int, val icon: ImageVector)

private val RAIL = listOf(
    RailEntry(Routes.HOME, R.string.nav_home, Icons.Default.Home),
    RailEntry(Routes.LIVE, R.string.nav_live, Icons.Default.LiveTv),
    RailEntry(Routes.MOVIES, R.string.nav_movies, Icons.Default.Movie),
    RailEntry(Routes.SERIES, R.string.nav_series, Icons.Default.Tv),
    RailEntry(Routes.FAVOURITES, R.string.nav_favourites, Icons.Default.Star),
    RailEntry(Routes.SEARCH, R.string.nav_search, Icons.Default.Search),
    RailEntry(Routes.SETTINGS, R.string.nav_settings, Icons.Default.Settings),
)

/**
 * The persistent chrome: brand + navigation rail on the left, the active screen on the right.
 * Player screens deliberately do not use this, so video gets the whole panel.
 */
@Composable
fun Shell(current: String, onNavigate: (String) -> Unit, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppRail(current = current, onNavigate = onNavigate)
        Box(Modifier.weight(1f).fillMaxHeight()) { content() }
    }
}

@Composable
private fun AppRail(current: String, onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier
            .width(RAIL_WIDTH)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 24.dp, horizontal = 12.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.logo_wordmark),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 8.dp),
        )
        Spacer(Modifier.height(28.dp))
        RAIL.forEach { entry ->
            RailItem(
                entry = entry,
                selected = entry.route == current,
                onClick = { onNavigate(entry.route) },
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun RailItem(entry: RailEntry, selected: Boolean, onClick: () -> Unit) {
    FocusBox(onClick = onClick, modifier = Modifier.fillMaxWidth()) { _ ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = stringResource(entry.labelRes),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (selected) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val RAIL_WIDTH = 208.dp
