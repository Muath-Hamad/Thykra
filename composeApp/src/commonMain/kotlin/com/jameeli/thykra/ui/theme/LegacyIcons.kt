package com.jameeli.thykra.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The four pre-Editions glyphs the screens still waiting on build steps 03-11 use, kept
 * here so [ThykraIcons] is exactly the set the design signed off.
 *
 * None of them survives its screen:
 *
 * - `Edit` and `CameraAlt` are on the profile screen; the Me screen replaces both with the
 *   text row "Edit name and photo" (step 11).
 * - `Logout` and `Block` are destructive verbs, and destructive verbs are always text
 *   labels — which is why the signed-off set has no door and no bin (steps 09 and 11).
 *
 * They are also the old filled Material drawings rather than 1.75 dp strokes, so they do
 * not belong beside the new set.
 */
@Deprecated(
    "Pre-Editions glyph. Not part of the signed-off set; its screen replaces it with a text label.",
    level = DeprecationLevel.WARNING,
)
object LegacyIcons {

    val Edit: ImageVector by lazy {
        ImageVector.Builder(
            name = "Edit",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 17.25f)
                verticalLineTo(21f)
                horizontalLineTo(6.75f)
                lineTo(17.81f, 9.94f)
                lineTo(14.06f, 6.19f)
                lineTo(3f, 17.25f)
                close()
                moveTo(20.71f, 7.04f)
                curveTo(21.1f, 6.65f, 21.1f, 6.02f, 20.71f, 5.63f)
                lineTo(18.37f, 3.29f)
                curveTo(17.98f, 2.9f, 17.35f, 2.9f, 16.96f, 3.29f)
                lineTo(15.13f, 5.12f)
                lineTo(18.88f, 8.87f)
                lineTo(20.71f, 7.04f)
                close()
            }
        }.build()
    }

    val CameraAlt: ImageVector by lazy {
        ImageVector.Builder(
            name = "CameraAlt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 15.2f)
                curveTo(13.77f, 15.2f, 15.2f, 13.77f, 15.2f, 12f)
                curveTo(15.2f, 10.23f, 13.77f, 8.8f, 12f, 8.8f)
                curveTo(10.23f, 8.8f, 8.8f, 10.23f, 8.8f, 12f)
                curveTo(8.8f, 13.77f, 10.23f, 15.2f, 12f, 15.2f)
                close()
                moveTo(9f, 2f)
                lineTo(7.17f, 4f)
                horizontalLineTo(4f)
                curveTo(2.9f, 4f, 2f, 4.9f, 2f, 6f)
                verticalLineTo(18f)
                curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
                horizontalLineTo(20f)
                curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
                verticalLineTo(6f)
                curveTo(22f, 4.9f, 21.1f, 4f, 20f, 4f)
                horizontalLineTo(16.83f)
                lineTo(15f, 2f)
                horizontalLineTo(9f)
                close()
                moveTo(12f, 17f)
                curveTo(9.24f, 17f, 7f, 14.76f, 7f, 12f)
                curveTo(7f, 9.24f, 9.24f, 7f, 12f, 7f)
                curveTo(14.76f, 7f, 17f, 9.24f, 17f, 12f)
                curveTo(17f, 14.76f, 14.76f, 17f, 12f, 17f)
                close()
            }
        }.build()
    }

    val Logout: ImageVector by lazy {
        ImageVector.Builder(
            name = "Logout",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(17f, 7f)
                lineTo(15.59f, 8.41f)
                lineTo(18.17f, 11f)
                horizontalLineTo(8f)
                verticalLineTo(13f)
                horizontalLineTo(18.17f)
                lineTo(15.59f, 15.58f)
                lineTo(17f, 17f)
                lineTo(22f, 12f)
                close()
                moveTo(4f, 5f)
                horizontalLineTo(12f)
                verticalLineTo(3f)
                horizontalLineTo(4f)
                curveTo(2.9f, 3f, 2f, 3.9f, 2f, 5f)
                verticalLineTo(19f)
                curveTo(2f, 20.1f, 2.9f, 21f, 4f, 21f)
                horizontalLineTo(12f)
                verticalLineTo(19f)
                horizontalLineTo(4f)
                verticalLineTo(5f)
                close()
            }
        }.build()
    }

    val Block: ImageVector by lazy {
        ImageVector.Builder(
            name = "Block",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                close()
                moveTo(4f, 12f)
                curveTo(4f, 7.58f, 7.58f, 4f, 12f, 4f)
                curveTo(13.85f, 4f, 15.55f, 4.63f, 16.9f, 5.69f)
                lineTo(5.69f, 16.9f)
                curveTo(4.63f, 15.55f, 4f, 13.85f, 4f, 12f)
                close()
                moveTo(12f, 20f)
                curveTo(10.15f, 20f, 8.45f, 19.37f, 7.1f, 18.31f)
                lineTo(18.31f, 7.1f)
                curveTo(19.37f, 8.45f, 20f, 10.15f, 20f, 12f)
                curveTo(20f, 16.42f, 16.42f, 20f, 12f, 20f)
                close()
            }
        }.build()
    }
}
