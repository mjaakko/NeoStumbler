package xyz.malkki.neostumbler

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.malkki.neostumbler.ui.composables.ManageStorage
import xyz.malkki.neostumbler.ui.theme.NeoStumblerTheme

//TODO: investigate if it's possible to use a single activity, e.g. with activity-alias in the manifest
class ManageStorageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NeoStumblerTheme(dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(text = stringResource(R.string.manage_storage_title)) },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                                actions = {

                                }
                            )
                        },
                        content = {
                            Box(modifier = Modifier.padding(paddingValues = it)) {
                                Box(modifier = Modifier.padding(all = 16.dp)) {
                                    ManageStorage()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}