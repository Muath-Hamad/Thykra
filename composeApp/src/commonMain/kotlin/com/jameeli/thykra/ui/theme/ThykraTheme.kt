package com.jameeli.thykra.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object ThykraColors {
    val SkyBlue = Color(0xFF1B7FCC)
    val OceanBlue = Color(0xFF4FA3E0)
    val SunriseOrange = Color(0xFFF47820)
    val Sandy = Color(0xFFF5E6C8)
    val WarmWhite = Color(0xFFFDF8EF)
    val DeepNavy = Color(0xFF1A1A2E)
    val MutedSlate = Color(0xFF5C6B7A)
    val SoftRed = Color(0xFFE05252)
}

private val ThykraColorScheme = lightColorScheme(
    primary = ThykraColors.SkyBlue,
    onPrimary = Color.White,
    primaryContainer = ThykraColors.OceanBlue,
    onPrimaryContainer = Color.White,
    inversePrimary = ThykraColors.OceanBlue,
    secondary = ThykraColors.Sandy,
    onSecondary = ThykraColors.DeepNavy,
    secondaryContainer = ThykraColors.Sandy,
    onSecondaryContainer = ThykraColors.DeepNavy,
    tertiary = ThykraColors.SunriseOrange,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF0E0),
    onTertiaryContainer = ThykraColors.DeepNavy,
    background = ThykraColors.WarmWhite,
    onBackground = ThykraColors.DeepNavy,
    surface = ThykraColors.WarmWhite,
    onSurface = ThykraColors.DeepNavy,
    surfaceVariant = ThykraColors.Sandy,
    onSurfaceVariant = ThykraColors.MutedSlate,
    surfaceTint = ThykraColors.SkyBlue,
    error = ThykraColors.SoftRed,
    onError = Color.White,
    errorContainer = Color(0xFFFCE4E4),
    onErrorContainer = Color(0xFF8B2020),
    outline = Color(0xFFBCA98E),
    outlineVariant = Color(0xFFE0D4BF),
    inverseSurface = ThykraColors.DeepNavy,
    inverseOnSurface = ThykraColors.WarmWhite,
    surfaceContainerLowest = Color(0xFFFFFBF5),
    surfaceContainerLow = ThykraColors.WarmWhite,
    surfaceContainer = Color(0xFFFAF2E5),
    surfaceContainerHigh = Color(0xFFF5ECDA),
    surfaceContainerHighest = Color(0xFFF0E6D0),
)

// Typography using default sans-serif for now.
// TODO: Replace with Nunito (display/headline/title) and Inter (body/label) custom fonts
private val ThykraTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

@Composable
fun ThykraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ThykraColorScheme,
        typography = ThykraTypography,
        content = content
    )
}
