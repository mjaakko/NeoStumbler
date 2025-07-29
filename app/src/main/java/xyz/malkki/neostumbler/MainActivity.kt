package xyz.malkki.neostumbler

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
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

        installSplashScreen().apply { setKeepOnScreenCondition { dynamicColorFlow.value == null } }

        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        setContent {
            val dynamicColorState = dynamicColorFlow.collectAsState()

            val navigationBackstack = rememberNavBackStack(ReportsNavKey)

            NeoStumblerTheme(dynamicColor = dynamicColorState.value == true) {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize()) {
                    val tabs =
                        listOf(
                            Tab(
                                title = stringResource(R.string.map_tab_title),
                                icon = rememberVectorPainter(Icons.Filled.Place),
                                navKey = MapNavKey,
                            ),
                            Tab(
                                title = stringResource(R.string.reports_tab_title),
                                icon = rememberVectorPainter(Icons.AutoMirrored.Default.List),
                                navKey = ReportsNavKey,
                            ),
                            Tab(
                                title = stringResource(R.string.statistics_tab_title),
                                icon = painterResource(id = R.drawable.statistics_24),
                                navKey = StatisticsNavKey,
                            ),
                            Tab(
                                title = stringResource(R.string.settings_tab_title),
                                icon = rememberVectorPainter(Icons.Filled.Settings),
                                navKey = SettingsNavKey,
                            ),
                        )

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(text = stringResource(R.string.app_name)) },
                                colors =
                                    TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                actions = {},
                            )
                        },
                        bottomBar = {
                            NavigationBar {
                                tabs.forEach { (title, icon, navKey) ->
                                    NavigationBarItem(
                                        icon = { Icon(icon, contentDescription = title) },
                                        label = { Text(title) },
                                        selected = navigationBackstack.last() == navKey,
                                        onClick = {
                                            navigationBackstack.removeLastOrNull()
                                            navigationBackstack.add(navKey)
                                        },
                                    )
                                }
                            }
                        },
                        content = {
                            Column(modifier = Modifier.fillMaxSize().padding(paddingValues = it)) {
                                NavDisplay(
                                    entryDecorators =
                                        listOf(
                                            rememberSavedStateNavEntryDecorator(),
                                            rememberViewModelStoreNavEntryDecorator(),
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
                        },
                        contentWindowInsets =
                            WindowInsets.systemBars.exclude(WindowInsets.displayCutout),
                    )
                }
            }
        }
    }

    private data class Tab(val title: String, val icon: Painter, val navKey: NavKey)

    @Serializable private object MapNavKey : NavKey

    @Serializable private object ReportsNavKey : NavKey

    @Serializable private object StatisticsNavKey : NavKey

    @Serializable private object SettingsNavKey : NavKey
}
