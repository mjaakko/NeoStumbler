package xyz.malkki.neostumbler.ui.composables.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.malkki.neostumbler.R

@Composable
fun Warning(@StringRes warningText: Int) {
    Row(
        modifier = Modifier.wrapContentSize().padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.warning_14sp),
            tint = MaterialTheme.colorScheme.onErrorContainer,
            contentDescription = stringResource(id = R.string.warning_icon_description),
        )
        Spacer(modifier = Modifier.width(2.dp))

        Text(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            text = stringResource(id = warningText),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
