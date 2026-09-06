package com.jameeli.thykra.ui.me

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.resources.Res
import com.jameeli.thykra.resources.me_clear
import com.jameeli.thykra.resources.me_clear_cache
import com.jameeli.thykra.resources.me_clear_confirm_body
import com.jameeli.thykra.resources.me_clear_confirm_title
import com.jameeli.thykra.resources.me_edit_profile
import com.jameeli.thykra.resources.me_language
import com.jameeli.thykra.resources.me_language_choices
import com.jameeli.thykra.resources.me_sign_out
import com.jameeli.thykra.resources.me_sign_out_confirm_many
import com.jameeli.thykra.resources.me_sign_out_confirm_one
import com.jameeli.thykra.resources.me_sign_out_confirm_title
import com.jameeli.thykra.resources.me_wifi_only
import com.jameeli.thykra.resources.me_wifi_only_help
import org.jetbrains.compose.resources.stringResource
import com.jameeli.thykra.navigation.LocalThykraChrome
import com.jameeli.thykra.ui.kit.AssistChip
import com.jameeli.thykra.ui.kit.Avatar
import com.jameeli.thykra.ui.kit.AvatarSize
import com.jameeli.thykra.ui.kit.AvatarUser
import com.jameeli.thykra.ui.kit.ConfirmDialog
import com.jameeli.thykra.ui.kit.Segmented
import com.jameeli.thykra.ui.kit.SegmentedOption
import com.jameeli.thykra.ui.kit.SheetAction
import com.jameeli.thykra.ui.kit.SheetDivider
import com.jameeli.thykra.ui.kit.ThykraButton
import com.jameeli.thykra.ui.kit.ThykraButtonVariant
import com.jameeli.thykra.ui.kit.ThykraSheet
import com.jameeli.thykra.ui.kit.ToastTone
import com.jameeli.thykra.ui.kit.toAvatarUser
import com.jameeli.thykra.ui.theme.HapticKind
import com.jameeli.thykra.ui.theme.ThemeMode
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.theme.rememberHaptics
import com.jameeli.thykra.ui.theme.thykra

/**
 * Design part 3 §11.
 *
 * Everything the app remembers about you, and the three switches that change how it
 * behaves. Sign out sits 100 dp below the last of them so it is never a mis-tap from the
 * tab bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(
    viewModel: MeViewModel,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile by viewModel.profile.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val wifiOnly by viewModel.wifiOnlyUploads.collectAsState()
    val cacheBytes by viewModel.cacheBytes.collectAsState()
    val message by viewModel.message.collectAsState()
    val uploadsPending by viewModel.uploadsPending.collectAsState()
    val chrome = LocalThykraChrome.current
    val haptic = rememberHaptics()

    var editOpen by remember { mutableStateOf(false) }
    var languageOpen by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }

    LaunchedEffect(profile) {
        chrome.currentUser = profile?.toAvatarUser()
    }

    LaunchedEffect(message) {
        message?.let {
            chrome.toast.show(it)
            viewModel.consumeMessage()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(24.dp))

        Avatar(
            user = profile?.toAvatarUser() ?: AvatarUser("", ""),
            size = AvatarSize.Xl,
            decorative = false,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = profile?.displayName.orEmpty(),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = listOfNotNull(profile?.email, stats).joinToString(" · "),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.thykra.textMeta,
        )

        ThykraButton(
            label = stringResource(Res.string.me_edit_profile),
            onClick = { editOpen = true },
            variant = ThykraButtonVariant.Text,
        )

        Section("Appearance")
        Segmented(
            options = listOf(
                SegmentedOption("System"),
                SegmentedOption("Paper"),
                SegmentedOption("Darkroom"),
            ),
            selectedIndex = when (themeMode) {
                ThemeMode.System -> 0
                ThemeMode.Paper -> 1
                ThemeMode.Darkroom -> 2
            },
            onSelect = { index ->
                haptic(HapticKind.Tick)
                onThemeModeChange(
                    when (index) {
                        0 -> ThemeMode.System
                        1 -> ThemeMode.Paper
                        else -> ThemeMode.Darkroom
                    },
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Section("Language")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(Res.string.me_language_choices),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            AssistChip(label = viewModel.languageLabel(), onClick = { languageOpen = true })
        }

        Section("Uploads")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.me_wifi_only),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    // The copy says exactly what the switch does, because "Wi-Fi only"
                    // on its own would read as "nothing uploads on data".
                    text = stringResource(Res.string.me_wifi_only_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.thykra.textMeta,
                )
            }
            Switch(checked = wifiOnly, onCheckedChange = viewModel::setWifiOnlyUploads)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.me_clear_cache),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = cacheBytes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.thykra.textMeta,
                )
            }
            ThykraButton(
                stringResource(Res.string.me_clear),
                { confirmClear = true },
                variant = ThykraButtonVariant.Outlined,
            )
        }

        Section("About")
        SheetAction("Terms · Privacy", { viewModel.openTerms() }, icon = ThykraIcons.Info)
        SheetDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Thykra · ذكرى",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = viewModel.versionLabel(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.thykra.textMeta,
            )
        }

        // 100 dp of nothing, so signing out is never a mis-tap from the tab bar.
        Spacer(Modifier.height(100.dp))
        ThykraButton(
            label = stringResource(Res.string.me_sign_out),
            onClick = { confirmSignOut = true },
            variant = ThykraButtonVariant.Text,
            destructive = true,
        )
        Spacer(Modifier.height(32.dp))
    }

    if (editOpen) {
        EditProfileSheet(
            initialName = profile?.displayName.orEmpty(),
            onDismiss = { editOpen = false },
            onSave = { name ->
                editOpen = false
                viewModel.updateName(name)
            },
        )
    }

    if (languageOpen) {
        ThykraSheet(onDismiss = { languageOpen = false }, title = stringResource(Res.string.me_language)) {
            SheetAction("English", {
                languageOpen = false
                viewModel.setLanguage("en")
            })
            SheetDivider()
            SheetAction("العربية", {
                languageOpen = false
                viewModel.setLanguage("ar")
            })
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = stringResource(Res.string.me_clear_confirm_title),
            body = stringResource(Res.string.me_clear_confirm_body, cacheBytes),
            confirmLabel = stringResource(Res.string.me_clear),
            onConfirm = {
                confirmClear = false
                viewModel.clearCache()
            },
            onDismiss = { confirmClear = false },
        )
    }

    if (confirmSignOut) {
        // Only worth a dialog when there is something to lose.
        if (uploadsPending > 0) {
            ConfirmDialog(
                title = stringResource(Res.string.me_sign_out_confirm_title),
                body = if (uploadsPending == 1) {
                    stringResource(Res.string.me_sign_out_confirm_one)
                } else {
                    stringResource(Res.string.me_sign_out_confirm_many, uploadsPending)
                },
                confirmLabel = stringResource(Res.string.me_sign_out),
                onConfirm = {
                    confirmSignOut = false
                    onSignOut()
                },
                onDismiss = { confirmSignOut = false },
            )
        } else {
            LaunchedEffect(Unit) {
                confirmSignOut = false
                onSignOut()
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(20.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .padding(top = 16.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}
