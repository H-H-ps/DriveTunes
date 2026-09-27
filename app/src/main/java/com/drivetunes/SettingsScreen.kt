package com.drivetunes

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlin.math.roundToInt

/** Paired Bluetooth devices as (name, address). */
@SuppressLint("MissingPermission")
fun bondedDevices(ctx: Context): List<Pair<String, String>> {
    val adapter = ctx.getSystemService(BluetoothManager::class.java)?.adapter ?: return emptyList()
    return try {
        adapter.bondedDevices.map { (it.name ?: it.address) to it.address }
            .sortedBy { it.first.lowercase() }
    } catch (e: SecurityException) {
        emptyList()
    }
}

private fun hasBtPermission(ctx: Context): Boolean =
    Build.VERSION.SDK_INT < 31 ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) ==
        PackageManager.PERMISSION_GRANTED

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit, onOpenAbout: () -> Unit) {
    val ctx = LocalContext.current
    var showDevices by remember { mutableStateOf(false) }
    var showFolders by remember { mutableStateOf(false) }
    var showAutoPlayMode by remember { mutableStateOf(false) }
    var showSource by remember { mutableStateOf(false) }
    var showDefaultOrder by remember { mutableStateOf(false) }
    var showAutoPlayLog by remember { mutableStateOf(false) }
    var defaultOrder by remember { mutableStateOf(Prefs.defaultOrder(ctx)) }

    val btLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showDevices = true
    }

    val pm = ctx.getSystemService(PowerManager::class.java)
    var ignoring by remember { mutableStateOf(pm?.isIgnoringBatteryOptimizations(ctx.packageName) ?: false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        ignoring = pm?.isIgnoringBatteryOptimizations(ctx.packageName) ?: false
    }

    val bonded = remember(showDevices) { bondedDevices(ctx) }
    val devicesSummary = if (vm.devices.isEmpty()) stringResource(R.string.s_devices_none)
    else vm.devices.joinToString { addr -> bonded.firstOrNull { it.second == addr }?.first ?: addr }
    val folderSummary = when {
        vm.folders.isEmpty() -> stringResource(R.string.s_folder_all)
        vm.folders.size == 1 -> vm.folders.first()
        else -> stringResource(R.string.s_folder_count, vm.folders.size)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .systemBarsPadding()
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text(
                stringResource(R.string.settings),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingRow(
                title = stringResource(R.string.s_auto),
                subtitle = stringResource(R.string.s_auto_desc),
                onClick = { vm.updateAutoPlay(!vm.autoPlay) }
            ) {
                Switch(
                    checked = vm.autoPlay,
                    onCheckedChange = { vm.updateAutoPlay(it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = Violet)
                )
            }

            SettingRow(
                title = stringResource(R.string.s_devices),
                subtitle = devicesSummary,
                onClick = {
                    if (hasBtPermission(ctx)) showDevices = true
                    else btLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            )

            SettingRow(
                title = stringResource(R.string.s_auto_mode),
                subtitle = autoPlayModeLabel(vm.autoPlayMode),
                onClick = { showAutoPlayMode = true }
            )

            SettingRow(
                title = stringResource(R.string.s_autoplay_log),
                subtitle = stringResource(R.string.s_autoplay_log_desc),
                onClick = { showAutoPlayLog = true }
            )

            SettingRow(
                title = stringResource(R.string.s_delay, vm.delaySec),
                subtitle = stringResource(R.string.s_delay_desc)
            ) {}
            Slider(
                value = vm.delaySec.toFloat(),
                onValueChange = { vm.updateDelay(it.roundToInt()) },
                valueRange = 0f..8f,
                steps = 7,
                colors = SliderDefaults.colors(thumbColor = Pink, activeTrackColor = Violet),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            SettingRow(
                title = stringResource(R.string.s_source),
                subtitle = when (vm.source) {
                    LibrarySource.ALL -> stringResource(R.string.s_source_all)
                    LibrarySource.FOLDER -> folderSummary
                    LibrarySource.FAVORITES -> stringResource(R.string.s_source_favorites)
                },
                onClick = { showSource = true }
            )
            if (vm.source == LibrarySource.FOLDER) {
                SettingRow(
                    title = stringResource(R.string.s_folder),
                    subtitle = folderSummary,
                    onClick = { showFolders = true }
                )
            }

            SettingRow(
                title = stringResource(R.string.s_swipe_random),
                subtitle = stringResource(R.string.s_swipe_random_desc),
                onClick = { vm.updateSwipeRandom(!vm.swipeRandom) }
            ) {
                Switch(
                    checked = vm.swipeRandom,
                    onCheckedChange = { vm.updateSwipeRandom(it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = Violet)
                )
            }

            SettingRow(
                title = stringResource(R.string.s_default_order),
                subtitle = defaultOrderLabel(defaultOrder),
                onClick = { showDefaultOrder = true }
            )

            SettingRow(
                title = stringResource(R.string.s_battery),
                subtitle = stringResource(R.string.s_battery_desc)
            ) {
                if (ignoring) {
                    Text(stringResource(R.string.s_battery_ok), color = Cyan)
                } else {
                    Button(onClick = {
                        ctx.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${ctx.packageName}")
                            )
                        )
                    }) { Text(stringResource(R.string.s_battery_allow)) }
                }
            }

            SettingRow(
                title = stringResource(R.string.s_about),
                subtitle = stringResource(R.string.s_about_desc),
                onClick = onOpenAbout
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDevices) {
        AlertDialog(
            onDismissRequest = { showDevices = false },
            confirmButton = { TextButton(onClick = { showDevices = false }) { Text(stringResource(R.string.done)) } },
            title = { Text(stringResource(R.string.s_devices_title)) },
            text = {
                if (bonded.isEmpty()) {
                    Text(stringResource(R.string.s_no_paired))
                } else {
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(bonded) { (name, addr) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { vm.toggleDevice(addr) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = addr in vm.devices, onCheckedChange = { vm.toggleDevice(addr) })
                                Text(name)
                            }
                        }
                    }
                }
            }
        )
    }

    if (showSource) {
        AlertDialog(
            onDismissRequest = { showSource = false },
            confirmButton = { TextButton(onClick = { showSource = false }) { Text(stringResource(R.string.done)) } },
            title = { Text(stringResource(R.string.s_source_title)) },
            text = {
                Column {
                    LibrarySource.entries.forEach { src ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    vm.updateSource(src)
                                    showSource = false
                                    if (src == LibrarySource.FOLDER) showFolders = true
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = vm.source == src, onClick = {
                                vm.updateSource(src)
                                showSource = false
                                if (src == LibrarySource.FOLDER) showFolders = true
                            })
                            Text(
                                when (src) {
                                    LibrarySource.ALL -> stringResource(R.string.s_source_all)
                                    LibrarySource.FOLDER -> stringResource(R.string.s_source_folder)
                                    LibrarySource.FAVORITES -> stringResource(R.string.s_source_favorites)
                                },
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        )
    }

    if (showAutoPlayMode) {
        AlertDialog(
            onDismissRequest = { showAutoPlayMode = false },
            confirmButton = { TextButton(onClick = { showAutoPlayMode = false }) { Text(stringResource(R.string.done)) } },
            title = { Text(stringResource(R.string.s_auto_mode_title)) },
            text = {
                Column {
                    AutoPlayMode.entries.forEach { mode ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    vm.updateAutoPlayMode(mode)
                                    showAutoPlayMode = false
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = vm.autoPlayMode == mode, onClick = {
                                vm.updateAutoPlayMode(mode)
                                showAutoPlayMode = false
                            })
                            Text(autoPlayModeLabel(mode), fontSize = 14.sp)
                        }
                    }
                }
            }
        )
    }

    if (showDefaultOrder) {
        AlertDialog(
            onDismissRequest = { showDefaultOrder = false },
            confirmButton = { TextButton(onClick = { showDefaultOrder = false }) { Text(stringResource(R.string.done)) } },
            title = { Text(stringResource(R.string.s_default_order_title)) },
            text = {
                Column {
                    DefaultOrder.entries.forEach { order ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    defaultOrder = order
                                    Prefs.setDefaultOrder(ctx, order)
                                    showDefaultOrder = false
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = defaultOrder == order, onClick = {
                                defaultOrder = order
                                Prefs.setDefaultOrder(ctx, order)
                                showDefaultOrder = false
                            })
                            Text(defaultOrderLabel(order), fontSize = 14.sp)
                        }
                    }
                }
            }
        )
    }

    if (showAutoPlayLog) {
        val entries = remember(showAutoPlayLog) { AutoPlayLog.entries(ctx) }
        AlertDialog(
            onDismissRequest = { showAutoPlayLog = false },
            confirmButton = { TextButton(onClick = { showAutoPlayLog = false }) { Text(stringResource(R.string.done)) } },
            dismissButton = {
                TextButton(onClick = {
                    AutoPlayLog.clear(ctx)
                    showAutoPlayLog = false
                }) { Text(stringResource(R.string.s_autoplay_log_clear)) }
            },
            title = { Text(stringResource(R.string.s_autoplay_log_title)) },
            text = {
                Column {
                    TextButton(onClick = {
                        val errors = AutoPlayLog.errorEntries(ctx)
                        val body = if (errors.isEmpty())
                            ctx.getString(R.string.s_autoplay_log_empty)
                        else errors.joinToString("\n")
                        val sent = try {
                            ctx.startActivity(
                                Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:")
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf(DEV_EMAIL))
                                    putExtra(Intent.EXTRA_SUBJECT, ctx.getString(R.string.s_autoplay_log_email_subject))
                                    putExtra(Intent.EXTRA_TEXT, body)
                                }
                            )
                            true
                        } catch (e: Exception) {
                            false
                        }
                        if (sent) showAutoPlayLog = false
                    }) { Text(stringResource(R.string.s_autoplay_log_email)) }

                    if (entries.isEmpty()) {
                        Text(stringResource(R.string.s_autoplay_log_empty))
                    } else {
                        LazyColumn(Modifier.heightIn(max = 350.dp)) {
                            items(entries) { line ->
                                Text(line, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        )
    }

    if (showFolders) {
        val folders = remember { Library.folders(ctx) }
        AlertDialog(
            onDismissRequest = { showFolders = false },
            confirmButton = { TextButton(onClick = { showFolders = false }) { Text(stringResource(R.string.done)) } },
            title = { Text(stringResource(R.string.s_folder_title)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    item {
                        // "All" is a separate, single choice: picking it clears any folder selection.
                        FolderRow(stringResource(R.string.s_folder_all), vm.folders.isEmpty()) {
                            vm.clearFolders()
                        }
                    }
                    items(folders) { (path, count) ->
                        FolderCheckRow("$path  ($count)", path in vm.folders) {
                            vm.toggleFolder(path)
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun autoPlayModeLabel(mode: AutoPlayMode): String = stringResource(
    when (mode) {
        AutoPlayMode.RANDOM -> R.string.s_auto_mode_random
        AutoPlayMode.RESUME_LAST -> R.string.s_auto_mode_resume
        AutoPlayMode.HOME_ORDER -> R.string.s_auto_mode_home
        AutoPlayMode.LAST_FROM_START -> R.string.s_auto_mode_last_start
    }
)

@Composable
private fun defaultOrderLabel(order: DefaultOrder): String = stringResource(
    when (order) {
        DefaultOrder.SEQUENTIAL -> R.string.s_default_order_sequential
        DefaultOrder.RANDOM -> R.string.s_default_order_random
    }
)

@Composable
private fun FolderRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, fontSize = 14.sp)
    }
}

@Composable
private fun FolderCheckRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(label, fontSize = 14.sp)
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val base = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(PanelBg)
    Row(
        (if (onClick != null) base.clickable(onClick = onClick) else base).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(subtitle, color = Muted, fontSize = 13.sp)
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}
