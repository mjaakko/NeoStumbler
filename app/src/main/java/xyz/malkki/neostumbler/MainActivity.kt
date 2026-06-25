package xyz.malkki.neostumbler

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneDecoratorStrategy
import androidx.navigation3.scene.SceneDecoratorStrategyScope
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import org.koin.android.ext.android.inject
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getBooleanFlow
import xyz.malkki.neostumbler.ui.screens.MapScreen
import xyz.malkki.neostumbler.ui.screens.ReportsScreen
import xyz.malkki.neostumbler.ui.screens.SettingsScreen
import xyz.malkki.neostumbler.ui.screens.StatisticsScreen
import xyz.malkki.neostumbler.ui.theme.NeoStumblerTheme

class MainActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_START_SCANNING = "start_scanning"
        const val EXTRA_REQUEST_BACKGROUND_PERMISSION = "request_background_permission"
    }

    private val settings: Settings by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        val dynamicColorFlow =
            settings
                .getBooleanFlow(PreferenceKeys.DYNAMIC_COLOR_THEME, false)
                .stateIn(lifecycleScope, started = SharingStarted.Eagerly, initialValue = null)

        installSplashScreen().setKeepOnScreenCondition { dynamicColorFlow.value == null }

        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        setContent {
            val dynamicColorState = dynamicColorFlow.collectAsState()

            NeoStumblerTheme(dynamicColor = dynamicColorState.value == true) {
                val navigationBackstack = rememberNavBackStack(ReportsNavKey)

                Surface(modifier = Modifier.fillMaxSize()) {
                    SharedTransitionLayout {
                        NavDisplay(
                            entryDecorators =
                                listOf(
                                    rememberSaveableStateHolderNavEntryDecorator(),
                                    rememberViewModelStoreNavEntryDecorator(),
                                ),
                            sceneDecoratorStrategies =
                                listOf(
                                    rememberMainNavigationSceneDecorator(
                                        navigationItems = {
                                            MAIN_NAVIGATION_TABS.forEach { (icon, navKey) ->
                                                NavigationSuiteItem(
                                                    modifier =
                                                        Modifier.sharedElement(
                                                            rememberSharedContentState(navKey),
                                                            LocalNavAnimatedContentScope.current,
                                                        ),
                                                    icon = {
                                                        Icon(
                                                            painter = painterResource(icon),
                                                            contentDescription =
                                                                stringResource(navKey.title),
                                                        )
                                                    },
                                                    label = {
                                                        Text(text = stringResource(navKey.title))
                                                    },
                                                    selected = navigationBackstack.last() == navKey,
                                                    onClick = {
                                                        navigationBackstack[
                                                            navigationBackstack.lastIndex] = navKey
                                                    },
                                                )
                                            }
                                        },
                                        topBar = {
                                            val navEntry = navigationBackstack.last()
                                            if (navEntry is MainNavKey && navEntry.appBar) {
                                                CenterAlignedTopAppBar(
                                                    modifier =
                                                        Modifier.sharedElement(
                                                            rememberSharedContentState("app-bar"),
                                                            LocalNavAnimatedContentScope.current,
                                                        ),
                                                    title = {
                                                        Text(text = stringResource(navEntry.title))
                                                    },
                                                )
                                            }
                                        },
                                    )
                                ),
                            backStack = navigationBackstack,
                            entryProvider =
                                entryProvider {
                                    entry<MapNavKey> { MapScreen() }
                                    entry<ReportsNavKey> { ReportsScreen() }
                                    entry<StatisticsNavKey> { StatisticsScreen() }
                                    entry<SettingsNavKey> { SettingsScreen() }
                                },
                        )
                    }
                }
            }
        }
    }
}

private val MAIN_NAVIGATION_TABS =
    listOf(
        Tab(icon = R.drawable.map_24px, navKey = MapNavKey),
        Tab(icon = R.drawable.list_24px, navKey = ReportsNavKey),
        Tab(icon = R.drawable.monitoring_24px, navKey = StatisticsNavKey),
        Tab(icon = R.drawable.settings_24px, navKey = SettingsNavKey),
    )

private data class Tab(@get:DrawableRes val icon: Int, val navKey: MainNavKey)

private sealed interface MainNavKey : NavKey {
    @get:StringRes val title: Int
    val appBar: Boolean
}

@Serializable
private object MapNavKey : MainNavKey {
    override val title = R.string.map_tab_title

    override val appBar = false
}

@Serializable
private object ReportsNavKey : MainNavKey {
    override val title = R.string.reports_tab_title

    override val appBar = true
}

@Serializable
private object StatisticsNavKey : MainNavKey {
    override val title = R.string.statistics_tab_title

    override val appBar = true
}

@Serializable
private object SettingsNavKey : MainNavKey {
    override val title = R.string.settings_tab_title

    override val appBar = true
}

private class MainNavigationSceneDecoratorStrategy<T : Any>(
    private val navigationItems: @Composable () -> Unit,
    private val topBar: @Composable () -> Unit,
) : SceneDecoratorStrategy<T> {
    override fun SceneDecoratorStrategyScope<T>.decorateScene(scene: Scene<T>): Scene<T> {
        return MainNavigationScene(
            navigationItems = navigationItems,
            topBar = topBar,
            scene = scene,
        )
    }
}

@Composable
private fun <T : Any> rememberMainNavigationSceneDecorator(
    navigationItems: @Composable () -> Unit,
    topBar: @Composable () -> Unit,
): MainNavigationSceneDecoratorStrategy<T> {
    val movableNavigationItems = remember { movableContentOf { navigationItems() } }

    val movableTopBar = remember { movableContentOf { topBar() } }

    return remember(movableNavigationItems, movableTopBar) {
        MainNavigationSceneDecoratorStrategy(
            navigationItems = movableNavigationItems,
            topBar = movableTopBar,
        )
    }
}

private class MainNavigationScene<T : Any>(
    private val scene: Scene<T>,
    private val topBar: @Composable () -> Unit,
    /** List of [NavigationSuiteItem]s */
    private val navigationItems: @Composable () -> Unit,
) : Scene<T> {
    override val key: Any
        get() = scene.key

    override val entries: List<NavEntry<T>>
        get() = scene.entries

    override val previousEntries: List<NavEntry<T>>
        get() = scene.previousEntries

    override val content: @Composable (() -> Unit)
        get() = {
            NavigationSuiteScaffold(
                navigationItemVerticalArrangement = Arrangement.Center,
                navigationItems = { navigationItems() },
            ) {
                Scaffold(
                    topBar = { topBar() },
                    contentWindowInsets =
                        ScaffoldDefaults.contentWindowInsets
                            .exclude(WindowInsets.systemBars)
                            .exclude(WindowInsets.displayCutout),
                    content = { paddingValues ->
                        Column(
                            modifier =
                                Modifier.fillMaxSize().padding(paddingValues = paddingValues),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            scene.content()
                        }
                    },
                )
            }
        }
}
