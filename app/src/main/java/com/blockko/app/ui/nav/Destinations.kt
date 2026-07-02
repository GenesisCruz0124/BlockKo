package com.blockko.app.ui.nav

sealed class Destination(val route: String) {
    data object Onboarding : Destination("onboarding")
    data object Home : Destination("home")
    data object Apps : Destination("apps")
    data object Blocklist : Destination("blocklist")
    data object Settings : Destination("settings")
    data object About : Destination("about")
}

val BOTTOM_NAV_DESTINATIONS = listOf(Destination.Home, Destination.Apps, Destination.Blocklist, Destination.Settings)
