@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.musta.iptv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.musta.iptv.R
import com.musta.iptv.ui.theme.Spacing

/** Stub for screens that arrive in later phases. */
@Composable
fun PlaceholderScreen(screenId: String, titleRes: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.screenMargin),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.placeholder_screen_id, screenId),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(text = stringResource(titleRes), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = stringResource(R.string.placeholder_coming),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.placeholder_back_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
