@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.musta.iptv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.musta.iptv.R
import com.musta.iptv.ui.components.rememberInitialFocus
import com.musta.iptv.ui.theme.Spacing

data class LanguageOption(val tag: String, val nativeName: String)

val languageOptions = listOf(
    LanguageOption("ar", "العربية"),
    LanguageOption("en", "English"),
    LanguageOption("ru", "Русский"),
)

/** S01. Language picker, shown on first launch. */
@Composable
fun LanguageScreen(currentTag: String?, onSelect: (String) -> Unit) {
    val firstFocus = rememberInitialFocus()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.screenMargin),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.language_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.language_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.lg))
        Column(
            modifier = Modifier.width(Spacing.listWidth),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            languageOptions.forEachIndexed { index, option ->
                ListItem(
                    selected = option.tag == currentTag,
                    onClick = { onSelect(option.tag) },
                    headlineContent = { Text(option.nativeName) },
                    modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                )
            }
        }
    }
}
