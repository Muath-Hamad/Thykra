package com.jameeli.thykra.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.jameeli.thykra.resources.Res
import com.jameeli.thykra.resources.archivo_expanded_bold
import com.jameeli.thykra.resources.archivo_expanded_bold_italic
import com.jameeli.thykra.resources.archivo_expanded_semibold
import com.jameeli.thykra.resources.readex_pro_bold
import com.jameeli.thykra.resources.readex_pro_medium
import com.jameeli.thykra.resources.readex_pro_regular
import com.jameeli.thykra.resources.readex_pro_semibold
import org.jetbrains.compose.resources.Font

/**
 * The two bundled families — 529 KB in `composeResources/font/`, both SIL OFL 1.1
 * (licences in `docs/licenses/fonts/`).
 *
 * [display] is Archivo cut at `wdth=125` (Expanded) in SemiBold, Bold and Bold Italic,
 * Latin only — display slots and the day numeral. The italic face carries the one clay
 * phrase per headline in English.
 *
 * [text] is Readex Pro at 400 / 500 / 600 / 700, Latin **and Arabic** — 700 is what the
 * Arabic typography instance uses for display slots, where Archivo has no Arabic to give., so every Arabic string in
 * the app renders from the same family as the Latin one, and Arabic-Indic digits (U+0660)
 * are available for the chapter numeral.
 *
 * Both are static instances rather than the variable originals: `minSdk` is 24 and
 * `FontVariation` axis selection only lands on API 26+, so a static cut is the one that
 * renders correctly on every device the app supports. Part 1 sanctions this fallback.
 */
@Immutable
data class ThykraFonts(
    val display: FontFamily,
    val text: FontFamily,
)

@Composable
fun rememberThykraFonts(): ThykraFonts {
    val archivoSemiBold = Font(Res.font.archivo_expanded_semibold, FontWeight.SemiBold, FontStyle.Normal)
    val archivoBold = Font(Res.font.archivo_expanded_bold, FontWeight.Bold, FontStyle.Normal)
    val archivoBoldItalic = Font(Res.font.archivo_expanded_bold_italic, FontWeight.Bold, FontStyle.Italic)
    val readexRegular = Font(Res.font.readex_pro_regular, FontWeight.Normal, FontStyle.Normal)
    val readexMedium = Font(Res.font.readex_pro_medium, FontWeight.Medium, FontStyle.Normal)
    val readexSemiBold = Font(Res.font.readex_pro_semibold, FontWeight.SemiBold, FontStyle.Normal)
    val readexBold = Font(Res.font.readex_pro_bold, FontWeight.Bold, FontStyle.Normal)
    return remember(
        archivoSemiBold, archivoBold, archivoBoldItalic,
        readexRegular, readexMedium, readexSemiBold, readexBold,
    ) {
        ThykraFonts(
            display = FontFamily(archivoSemiBold, archivoBold, archivoBoldItalic),
            text = FontFamily(readexRegular, readexMedium, readexSemiBold, readexBold),
        )
    }
}
