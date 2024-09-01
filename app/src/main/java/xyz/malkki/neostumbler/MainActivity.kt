package xyz.malkki.neostumbler

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.malkki.neostumbler.ui.screens.MapScreen
import xyz.malkki.neostumbler.ui.screens.ReportsScreen
import xyz.malkki.neostumbler.ui.screens.StatisticsScreen
import xyz.malkki.neostumbler.ui.screens.settings.SettingsScreen
import xyz.malkki.neostumbler.ui.theme.NeoStumblerTheme

class MainActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_START_SCANNING = "start_scanning"
        const val EXTRA_REQUEST_BACKGROUND_PERMISSION = "request_background_permission"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NeoStumblerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(text = getString(R.string.app_name)) },
                                colors = TopAppBarDefaults.smallTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                                actions = {

                                }
                            )
                         },
                        content = {
                            val selectedTabIndex = remember { mutableIntStateOf(1) }

                            val items = listOf(
                                stringResource(R.string.map_tab_title) to rememberVectorPainter(Icons.Filled.Place),
                                stringResource(R.string.reports_tab_title) to rememberVectorPainter(Icons.Filled.List),
                                stringResource(R.string.statistics_tab_title) to painterResource(id = R.drawable.statistics_24),
                                stringResource(R.string.settings_tab_title)  to rememberVectorPainter(Icons.Filled.Settings),
                            )

                            Column(modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues = it)) {
                                Column(modifier = Modifier
                                    .weight(1.0f)
                                    .padding(16.dp)) {
                                    when (selectedTabIndex.intValue) {
                                        0 -> {
                                            MapScreen()
                                        }
                                        1 -> {
                                            ReportsScreen()
                                        }
                                        2 -> {
                                            StatisticsScreen()
                                        }
                                        3 -> {
                                            SettingsScreen()
                                        }
                                    }
                                }

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
                            }
                        }
                    )
                }
            }
        }
    }
}