package com.jameeli.thykra.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jameeli.thykra.ui.kit.EmptyGlyph
import com.jameeli.thykra.ui.kit.EmptyState
import com.jameeli.thykra.ui.kit.clayPhrase

/**
 * A route that resolves but has nothing behind it yet.
 *
 * Build step 03's acceptance is that *every* route in the IA resolves — to an old screen
 * or to a placeholder — so that App Links, the tab bar and back all work before the
 * screens exist. This is the placeholder half of that, and every one of these disappears
 * as its own step lands.
 */
@Composable
fun RoutePlaceholder(
    title: String,
    note: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            headline = clayPhrase("", title),
            body = note,
            glyph = EmptyGlyph.Plate,
        )
    }
}
