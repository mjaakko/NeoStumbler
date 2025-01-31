package xyz.malkki.neostumbler

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.ui.screens.MapScreen
import xyz.malkki.neostumbler.ui.screens.ReportsScreen
import xyz.malkki.neostumbler.ui.screens.StatisticsScreen
import xyz.malkki.neostumbler.ui.screens.settings.SettingsScreen
import xyz.malkki.neostumbler.ui.theme.NeoStumblerTheme

private fun Context.useDynamicColor(): Flow<Boolean> =
    (applicationContext as StumblerApplication).settingsStore.data
        .map { prefs ->
            prefs[booleanPreferencesKey(PreferenceKeys.DYNAMIC_COLOR_THEME)] == true
        }

class MainActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_START_SCANNING = "start_scanning"
        const val EXTRA_REQUEST_BACKGROUND_PERMISSION = "request_background_permission"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val dynamicColorFlow = useDynamicColor().stateIn(
            lifecycleScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

        installSplashScreen().apply {
            setKeepOnScreenCondition {
                dynamicColorFlow.value == null
            }
        }

        super.onCreate(savedInstanceState)

        setContent {
            val dynamicColorState = dynamicColorFlow.collectAsState()

            NeoStumblerTheme(dynamicColor = dynamicColorState.value == true) {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val items = listOf(
                        stringResource(R.string.map_tab_title) to rememberVectorPainter(Icons.Filled.Place),
                        stringResource(R.string.reports_tab_title) to rememberVectorPainter(Icons.AutoMirrored.Default.List),
                        stringResource(R.string.statistics_tab_title) to painterResource(id = R.drawable.statistics_24),
                        stringResource(R.string.settings_tab_title) to rememberVectorPainter(Icons.Filled.Settings),
                    )

                    val selectedTabIndex = rememberSaveable { mutableIntStateOf(1) }

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(text = stringResource(R.string.app_name)) },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                                actions = {

                                }
                            )
                        },
                        bottomBar = {
                            NavigationBar {
                                items.forEachIndexed { index, (title, icon) ->
                                    NavigationBarItem(
                                        icon = {
                                            Icon(icon, contentDescription = title)
                                        },
                                        label = { Text(title) },
                                        selected = selectedTabIndex.intValue == index,
                                        onClick = { selectedTabIndex.intValue = index }
                                    )
                                }
                            }
                        },
                        content = {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues = it)
                            ) {
                                when (selectedTabIndex.intValue) {
                                    0 -> MapScreen()
                                    1 -> ReportsScreen()
                                    2 -> StatisticsScreen()
                                    3 -> SettingsScreen()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}