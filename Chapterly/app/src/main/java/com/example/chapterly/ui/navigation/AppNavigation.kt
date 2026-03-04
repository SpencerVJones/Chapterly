package com.example.chapterly.ui.navigation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.chapterly.ui.auth.AuthRoute
import com.example.chapterly.ui.bookdetail.BookDetailRoute
import com.example.chapterly.ui.bookdetail.BookDetailViewModel
import com.example.chapterly.ui.booklist.BookListRoute
import com.example.chapterly.ui.booklist.BookListViewModel
import com.example.chapterly.ui.product.ClubsRoute
import com.example.chapterly.ui.product.CreatorModeRoute
import com.example.chapterly.ui.product.NotificationsRoute
import com.example.chapterly.ui.product.ReadingHubRoute
import com.example.chapterly.ui.product.RecommendationsRoute

const val BOOK_LIST_ROUTE = "books"
const val FAVORITES_ROUTE = "favorites"
const val ACCOUNT_ROUTE = "account"
const val READING_HUB_ROUTE = "reading_hub"
const val RECOMMENDATIONS_ROUTE = "recommendations"
const val CLUBS_ROUTE = "clubs"
const val NOTIFICATIONS_ROUTE = "notifications"
const val CREATOR_MODE_ROUTE = "creator_mode"
const val BOOK_DETAIL_ROUTE = "book_detail"
const val BOOK_ID_ARG = "bookId"

private const val BOOK_DETAIL_ROUTE_TEMPLATE = "$BOOK_DETAIL_ROUTE/{$BOOK_ID_ARG}"

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val topLevelDestinations =
    listOf(
        TopLevelDestination(
            route = BOOK_LIST_ROUTE,
            label = "Explore",
            icon = Icons.Outlined.Explore,
        ),
        TopLevelDestination(
            route = FAVORITES_ROUTE,
            label = "Favorites",
            icon = Icons.Outlined.BookmarkBorder,
        ),
        TopLevelDestination(
            route = ACCOUNT_ROUTE,
            label = "Account",
            icon = Icons.Outlined.PersonOutline,
        ),
    )

fun buildBookDetailRoute(bookId: String): String {
    return "$BOOK_DETAIL_ROUTE/${Uri.encode(bookId)}"
}

@Composable
fun AppNavigation() {
    hiltViewModel<SessionViewModel>()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            if (currentDestination.isTopLevelDestination()) {
                ChapterlyBottomBar(
                    currentDestination = currentDestination,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BOOK_LIST_ROUTE,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(route = BOOK_LIST_ROUTE) {
                val viewModel: BookListViewModel = hiltViewModel()
                BookListRoute(
                    onBookClick = { bookId ->
                        navController.navigate(buildBookDetailRoute(bookId))
                    },
                    viewModel = viewModel,
                    screenTitle = "Chapterly",
                    defaultFavoritesOnly = false,
                    lockFavoritesFilter = false,
                )
            }

            composable(route = FAVORITES_ROUTE) {
                val viewModel: BookListViewModel = hiltViewModel()
                BookListRoute(
                    onBookClick = { bookId ->
                        navController.navigate(buildBookDetailRoute(bookId))
                    },
                    viewModel = viewModel,
                    screenTitle = "Favorites",
                    defaultFavoritesOnly = true,
                    lockFavoritesFilter = true,
                )
            }

            composable(route = ACCOUNT_ROUTE) {
                AuthRoute(
                    onOpenReadingHub = { navController.navigate(READING_HUB_ROUTE) },
                    onOpenRecommendations = { navController.navigate(RECOMMENDATIONS_ROUTE) },
                    onOpenClubs = { navController.navigate(CLUBS_ROUTE) },
                    onOpenNotifications = { navController.navigate(NOTIFICATIONS_ROUTE) },
                    onOpenCreatorMode = { navController.navigate(CREATOR_MODE_ROUTE) },
                )
            }

            composable(route = READING_HUB_ROUTE) {
                ReadingHubRoute(
                    onBackClicked = { navController.popBackStack() },
                    onBookClick = { bookId ->
                        navController.navigate(buildBookDetailRoute(bookId))
                    },
                )
            }

            composable(route = RECOMMENDATIONS_ROUTE) {
                RecommendationsRoute(
                    onBackClicked = { navController.popBackStack() },
                    onBookClick = { bookId ->
                        navController.navigate(buildBookDetailRoute(bookId))
                    },
                )
            }

            composable(route = CLUBS_ROUTE) {
                ClubsRoute(
                    onBackClicked = { navController.popBackStack() },
                )
            }

            composable(route = NOTIFICATIONS_ROUTE) {
                NotificationsRoute(
                    onBackClicked = { navController.popBackStack() },
                )
            }

            composable(route = CREATOR_MODE_ROUTE) {
                CreatorModeRoute(
                    onBackClicked = { navController.popBackStack() },
                )
            }

            composable(
                route = BOOK_DETAIL_ROUTE_TEMPLATE,
                arguments = listOf(navArgument(BOOK_ID_ARG) { type = NavType.StringType }),
            ) {
                val viewModel: BookDetailViewModel = hiltViewModel()
                BookDetailRoute(
                    onBackClicked = { navController.popBackStack() },
                    viewModel = viewModel,
                )
            }
        }
    }
}

@Composable
private fun ChapterlyBottomBar(
    currentDestination: NavDestination?,
    onNavigate: (String) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFF2EEF8),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                topLevelDestinations.forEach { destination ->
                    val selected = currentDestination.isRouteInHierarchy(destination.route)
                    BottomBarItem(
                        label = destination.label,
                        icon = destination.icon,
                        selected = selected,
                        onClick = { onNavigate(destination.route) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (selected) Color(0xFFE1D8F7) else Color.Transparent,
        ) {
            Box(
                modifier =
                    Modifier
                        .width(86.dp)
                        .height(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) Color(0xFF2A2352) else Color(0xFF54506A),
                )
            }
        }
        Text(
            text = label,
            color = if (selected) Color(0xFF2A2352) else Color(0xFF54506A),
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

private fun NavDestination?.isRouteInHierarchy(route: String): Boolean {
    return this?.hierarchy?.any { destination -> destination.route == route } == true
}

private fun NavDestination?.isTopLevelDestination(): Boolean {
    return topLevelDestinations.any { destination -> this.isRouteInHierarchy(destination.route) }
}
