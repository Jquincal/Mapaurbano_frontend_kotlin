package com.mapaurbano.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer

@Composable
fun rememberNavigationState(
  startRoute: NavKey,
  topLevelRoutes: Set<NavKey>,
): NavigationState {
  val topLevelRoute =
    rememberSerializable(
      startRoute,
      topLevelRoutes,
      serializer = MutableStateSerializer(NavKeySerializer()),
    ) {
      mutableStateOf(startRoute)
    }
  val backStacks = topLevelRoutes.associateWith { route -> rememberNavBackStack(route) }

  return remember(startRoute, topLevelRoutes) {
    NavigationState(
      startRoute = startRoute,
      topLevelRoute = topLevelRoute,
      backStacks = backStacks,
    )
  }
}

class NavigationState(
  val startRoute: NavKey,
  topLevelRoute: MutableState<NavKey>,
  val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
  var topLevelRoute: NavKey by topLevelRoute

  @Composable
  fun toDecoratedEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>,
  ): List<NavEntry<NavKey>> {
    val decoratedEntries =
      backStacks.mapValues { (_, stack) ->
        rememberDecoratedNavEntries(
          backStack = stack,
          entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
          entryProvider = entryProvider,
        )
      }

    val activeRoutes =
      if (topLevelRoute == startRoute) {
        listOf(startRoute)
      } else {
        listOf(startRoute, topLevelRoute)
      }
    return activeRoutes.flatMap { route -> decoratedEntries[route].orEmpty() }
  }
}
