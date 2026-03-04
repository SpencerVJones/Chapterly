package com.example.jonesspencer_ce07.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.jonesspencer_ce07.ui.books.BooksRoute
import com.example.jonesspencer_ce07.ui.books.BooksListViewModel
import com.example.jonesspencer_ce07.ui.detail.BookDetailRoute
import com.example.jonesspencer_ce07.ui.detail.BookDetailViewModel

const val BOOKS_ROUTE = "books"
const val BOOK_DETAIL_ROUTE = "book_detail"
const val BOOK_ID_ARG = "bookId"

private const val BOOK_DETAIL_ROUTE_TEMPLATE = "$BOOK_DETAIL_ROUTE/{$BOOK_ID_ARG}"

fun buildBookDetailRoute(bookId: String): String {
    return "$BOOK_DETAIL_ROUTE/${Uri.encode(bookId)}"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = BOOKS_ROUTE
    ) {
        composable(route = BOOKS_ROUTE) {
            val viewModel: BooksListViewModel = hiltViewModel()
            BooksRoute(
                onBookClick = { bookId ->
                    navController.navigate(buildBookDetailRoute(bookId))
                },
                viewModel = viewModel
            )
        }

        composable(
            route = BOOK_DETAIL_ROUTE_TEMPLATE,
            arguments = listOf(navArgument(BOOK_ID_ARG) { type = NavType.StringType })
        ) {
            val viewModel: BookDetailViewModel = hiltViewModel()
            BookDetailRoute(
                onBackClicked = { navController.popBackStack() },
                viewModel = viewModel
            )
        }
    }
}
