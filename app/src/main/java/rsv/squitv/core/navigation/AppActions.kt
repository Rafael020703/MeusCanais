package rsv.squitv.core.navigation

sealed class AppAction {
    data class Back(val eventId: String) : AppAction()
    data class Navigate(
        val route: Route,
        val popUpToRoute: Route? = null,
        val inclusive: Boolean = false,
        val launchSingleTop: Boolean = false
    ) : AppAction()
    data object ExitApp : AppAction()
}
