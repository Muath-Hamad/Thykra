package com.jameeli.thykra.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object ThykraIcons {
    val Lock: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                // Lock body
                moveTo(6f, 10f)
                verticalLineTo(8f)
                curveTo(6f, 4.69f, 8.69f, 2f, 12f, 2f)
                curveTo(15.31f, 2f, 18f, 4.69f, 18f, 8f)
                verticalLineTo(10f)
                horizontalLineTo(19f)
                curveTo(20.1f, 10f, 21f, 10.9f, 21f, 12f)
                verticalLineTo(20f)
                curveTo(21f, 21.1f, 20.1f, 22f, 19f, 22f)
                horizontalLineTo(5f)
                curveTo(3.9f, 22f, 3f, 21.1f, 3f, 20f)
                verticalLineTo(12f)
                curveTo(3f, 10.9f, 3.9f, 10f, 5f, 10f)
                close()
                // Keyhole
                moveTo(12f, 17f)
                curveTo(13.1f, 17f, 14f, 16.1f, 14f, 15f)
                curveTo(14f, 13.9f, 13.1f, 13f, 12f, 13f)
                curveTo(10.9f, 13f, 10f, 13.9f, 10f, 15f)
                curveTo(10f, 16.1f, 10.9f, 17f, 12f, 17f)
                close()
                // Lock shackle cutout
                moveTo(8f, 10f)
                horizontalLineTo(16f)
                verticalLineTo(8f)
                curveTo(16f, 5.79f, 14.21f, 4f, 12f, 4f)
                curveTo(9.79f, 4f, 8f, 5.79f, 8f, 8f)
                verticalLineTo(10f)
                close()
            }
        }.build()
    }

    val Person: ImageVector by lazy {
        ImageVector.Builder(
            name = "Person",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 12f)
                curveTo(14.21f, 12f, 16f, 10.21f, 16f, 8f)
                curveTo(16f, 5.79f, 14.21f, 4f, 12f, 4f)
                curveTo(9.79f, 4f, 8f, 5.79f, 8f, 8f)
                curveTo(8f, 10.21f, 9.79f, 12f, 12f, 12f)
                close()
                moveTo(12f, 14f)
                curveTo(9.33f, 14f, 4f, 15.34f, 4f, 18f)
                verticalLineTo(20f)
                horizontalLineTo(20f)
                verticalLineTo(18f)
                curveTo(20f, 15.34f, 14.67f, 14f, 12f, 14f)
                close()
            }
        }.build()
    }

    val ArrowBack: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowBack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20f, 11f)
                horizontalLineTo(7.83f)
                lineTo(13.42f, 5.41f)
                lineTo(12f, 4f)
                lineTo(4f, 12f)
                lineTo(12f, 20f)
                lineTo(13.41f, 18.59f)
                lineTo(7.83f, 13f)
                horizontalLineTo(20f)
                verticalLineTo(11f)
                close()
            }
        }.build()
    }

    val Add: ImageVector by lazy {
        ImageVector.Builder(
            name = "Add",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 13f)
                horizontalLineTo(13f)
                verticalLineTo(19f)
                horizontalLineTo(11f)
                verticalLineTo(13f)
                horizontalLineTo(5f)
                verticalLineTo(11f)
                horizontalLineTo(11f)
                verticalLineTo(5f)
                horizontalLineTo(13f)
                verticalLineTo(11f)
                horizontalLineTo(19f)
                verticalLineTo(13f)
                close()
            }
        }.build()
    }

    val Close: ImageVector by lazy {
        ImageVector.Builder(
            name = "Close",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 6.41f)
                lineTo(17.59f, 5f)
                lineTo(12f, 10.59f)
                lineTo(6.41f, 5f)
                lineTo(5f, 6.41f)
                lineTo(10.59f, 12f)
                lineTo(5f, 17.59f)
                lineTo(6.41f, 19f)
                lineTo(12f, 13.41f)
                lineTo(17.59f, 19f)
                lineTo(19f, 17.59f)
                lineTo(13.41f, 12f)
                close()
            }
        }.build()
    }

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

    val Timeline: ImageVector by lazy {
        ImageVector.Builder(
            name = "Timeline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(23f, 8f)
                curveTo(23f, 9.1f, 22.1f, 10f, 21f, 10f)
                curveTo(20.82f, 10f, 20.65f, 9.98f, 20.49f, 9.93f)
                lineTo(16.93f, 13.48f)
                curveTo(16.98f, 13.64f, 17f, 13.82f, 17f, 14f)
                curveTo(17f, 15.1f, 16.1f, 16f, 15f, 16f)
                curveTo(13.9f, 16f, 13f, 15.1f, 13f, 14f)
                curveTo(13f, 13.82f, 13.02f, 13.64f, 13.07f, 13.48f)
                lineTo(10.52f, 10.93f)
                curveTo(10.36f, 10.98f, 10.18f, 11f, 10f, 11f)
                curveTo(9.82f, 11f, 9.64f, 10.98f, 9.48f, 10.93f)
                lineTo(4.93f, 15.49f)
                curveTo(4.98f, 15.65f, 5f, 15.82f, 5f, 16f)
                curveTo(5f, 17.1f, 4.1f, 18f, 3f, 18f)
                curveTo(1.9f, 18f, 1f, 17.1f, 1f, 16f)
                curveTo(1f, 14.9f, 1.9f, 14f, 3f, 14f)
                curveTo(3.18f, 14f, 3.35f, 14.02f, 3.51f, 14.07f)
                lineTo(8.07f, 9.52f)
                curveTo(8.02f, 9.36f, 8f, 9.18f, 8f, 9f)
                curveTo(8f, 7.9f, 8.9f, 7f, 10f, 7f)
                curveTo(11.1f, 7f, 12f, 7.9f, 12f, 9f)
                curveTo(12f, 9.18f, 11.98f, 9.36f, 11.93f, 9.52f)
                lineTo(14.48f, 12.07f)
                curveTo(14.64f, 12.02f, 14.82f, 12f, 15f, 12f)
                curveTo(15.18f, 12f, 15.36f, 12.02f, 15.52f, 12.07f)
                lineTo(19.07f, 8.51f)
                curveTo(19.02f, 8.36f, 19f, 8.18f, 19f, 8f)
                curveTo(19f, 6.9f, 19.9f, 6f, 21f, 6f)
                curveTo(22.1f, 6f, 23f, 6.9f, 23f, 8f)
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

    val Link: ImageVector by lazy {
        ImageVector.Builder(
            name = "Link",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3.9f, 12f)
                curveTo(3.9f, 10.29f, 5.29f, 8.9f, 7f, 8.9f)
                horizontalLineTo(11f)
                verticalLineTo(7f)
                horizontalLineTo(7f)
                curveTo(4.24f, 7f, 2f, 9.24f, 2f, 12f)
                curveTo(2f, 14.76f, 4.24f, 17f, 7f, 17f)
                horizontalLineTo(11f)
                verticalLineTo(15.1f)
                horizontalLineTo(7f)
                curveTo(5.29f, 15.1f, 3.9f, 13.71f, 3.9f, 12f)
                close()
                moveTo(8f, 13f)
                horizontalLineTo(16f)
                verticalLineTo(11f)
                horizontalLineTo(8f)
                verticalLineTo(13f)
                close()
                moveTo(17f, 7f)
                horizontalLineTo(13f)
                verticalLineTo(8.9f)
                horizontalLineTo(17f)
                curveTo(18.71f, 8.9f, 20.1f, 10.29f, 20.1f, 12f)
                curveTo(20.1f, 13.71f, 18.71f, 15.1f, 17f, 15.1f)
                horizontalLineTo(13f)
                verticalLineTo(17f)
                horizontalLineTo(17f)
                curveTo(19.76f, 17f, 22f, 14.76f, 22f, 12f)
                curveTo(22f, 9.24f, 19.76f, 7f, 17f, 7f)
                close()
            }
        }.build()
    }

    val Pause: ImageVector by lazy {
        ImageVector.Builder(
            name = "Pause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 19f)
                horizontalLineTo(10f)
                verticalLineTo(5f)
                horizontalLineTo(6f)
                close()
                moveTo(14f, 5f)
                verticalLineTo(19f)
                horizontalLineTo(18f)
                verticalLineTo(5f)
                horizontalLineTo(14f)
                close()
            }
        }.build()
    }

    val Replay: ImageVector by lazy {
        ImageVector.Builder(
            name = "Replay",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 5f)
                verticalLineTo(1f)
                lineTo(7f, 6f)
                lineTo(12f, 11f)
                verticalLineTo(7f)
                curveTo(15.31f, 7f, 18f, 9.69f, 18f, 13f)
                curveTo(18f, 16.31f, 15.31f, 19f, 12f, 19f)
                curveTo(8.69f, 19f, 6f, 16.31f, 6f, 13f)
                horizontalLineTo(4f)
                curveTo(4f, 17.42f, 7.58f, 21f, 12f, 21f)
                curveTo(16.42f, 21f, 20f, 17.42f, 20f, 13f)
                curveTo(20f, 8.58f, 16.42f, 5f, 12f, 5f)
                close()
            }
        }.build()
    }

    val PlayArrow: ImageVector by lazy {
        ImageVector.Builder(
            name = "PlayArrow",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(8f, 5f)
                verticalLineTo(19f)
                lineTo(19f, 12f)
                close()
            }
        }.build()
    }

    val PhotoLibrary: ImageVector by lazy {
        ImageVector.Builder(
            name = "PhotoLibrary",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(22f, 16f)
                verticalLineTo(4f)
                curveTo(22f, 2.9f, 21.1f, 2f, 20f, 2f)
                horizontalLineTo(8f)
                curveTo(6.9f, 2f, 6f, 2.9f, 6f, 4f)
                verticalLineTo(16f)
                curveTo(6f, 17.1f, 6.9f, 18f, 8f, 18f)
                horizontalLineTo(20f)
                curveTo(21.1f, 18f, 22f, 17.1f, 22f, 16f)
                close()
                moveTo(11f, 12f)
                lineTo(13.03f, 14.71f)
                lineTo(16f, 11f)
                lineTo(20f, 16f)
                horizontalLineTo(8f)
                lineTo(11f, 12f)
                close()
                moveTo(2f, 6f)
                verticalLineTo(20f)
                curveTo(2f, 21.1f, 2.9f, 22f, 4f, 22f)
                horizontalLineTo(18f)
                verticalLineTo(20f)
                horizontalLineTo(4f)
                verticalLineTo(6f)
                horizontalLineTo(2f)
                close()
            }
        }.build()
    }
}
