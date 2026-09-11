package ru.pdd.academy.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ru.pdd.academy.R

@Composable
fun AppearanceSettings(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        Triple("system", R.string.theme_system, Icons.Rounded.SettingsBrightness),
        Triple("light", R.string.theme_light, Icons.Rounded.LightMode),
        Triple("dark", R.string.theme_dark, Icons.Rounded.DarkMode)
    )
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.appearance_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.appearance_description), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        options.forEach { (key, label, icon) ->
            val checked = selected == key
            Surface(shape = MaterialTheme.shapes.medium,
                color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                Row(Modifier.fillMaxWidth().selectable(checked, role = Role.RadioButton, onClick = { onSelect(key) })
                    .padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(icon, null, tint = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                        color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                    RadioButton(selected = checked, onClick = null)
                }
            }
        }
    }
}
