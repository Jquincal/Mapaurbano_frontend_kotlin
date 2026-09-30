package com.mapaurbano.mobile.navigation

import androidx.navigation3.runtime.NavKey

class AppNavigator(private val state: NavigationState) {
  fun navigate(route: NavKey) {
    if (route in state.backStacks) {
      state.topLevelRoute = route
    } else {
      state.backStacks.getValue(state.topLevelRoute).add(route)
    }
  }

  fun goBack(): Boolean {
    val currentStack = state.backStacks.getValue(state.topLevelRoute)
    return if (currentStack.last() != state.topLevelRoute) {
      currentStack.removeLastOrNull()
      true
    } else if (state.topLevelRoute != state.startRoute) {
      state.topLevelRoute = state.startRoute
      true
    } else {
      false
    }
  }
}
