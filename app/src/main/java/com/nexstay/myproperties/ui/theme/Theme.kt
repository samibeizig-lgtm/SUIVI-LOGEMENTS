package com.nexstay.myproperties.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Palette minimaliste : encre, ivoire, bronze.
private val Ink = Color(0xFF191D24)
private val InkSoft = Color(0xFF3C424D)
private val Ivory = Color(0xFFFAF9F6)
private val Paper = Color(0xFFFFFFFF)
private val Bronze = Color(0xFFA98554)
private val BronzeSoft = Color(0xFFF0E7DB)
private val Mist = Color(0xFFE8E6E1)
private val Slate = Color(0xFF6E7480)

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Ivory,
    primaryContainer = Mist,
    onPrimaryContainer = Ink,
    secondary = Slate,
    onSecondary = Ivory,
    secondaryContainer = Mist,
    onSecondaryContainer = Ink,
    tertiary = Bronze,
    onTertiary = Ivory,
    tertiaryContainer = BronzeSoft,
    onTertiaryContainer = Ink,
    background = Ivory,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Ivory,
    onSurfaceVariant = Slate,
    outline = Color(0xFFD5D2CB),
    outlineVariant = Mist,
    error = Color(0xFF9B3B34),
    onError = Ivory
)

private val DarkColors = darkColorScheme(
    primary = Ivory,
    onPrimary = Ink,
    primaryContainer = InkSoft,
    onPrimaryContainer = Ivory,
    secondary = Color(0xFFB6BAC2),
    onSecondary = Ink,
    secondaryContainer = InkSoft,
    onSecondaryContainer = Ivory,
    tertiary = Color(0xFFCBA97C),
    onTertiary = Ink,
    tertiaryContainer = Color(0xFF4A3D2C),
    onTertiaryContainer = BronzeSoft,
    background = Color(0xFF121418),
    onBackground = Ivory,
    surface = Color(0xFF1B1E24),
    onSurface = Ivory,
    surfaceVariant = Color(0xFF1B1E24),
    onSurfaceVariant = Color(0xFF9AA0AA),
    outline = Color(0xFF3C424D),
    outlineVariant = Color(0xFF2A2E36),
    error = Color(0xFFE08B84),
    onError = Ink
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val AppTypography = Typography().let { base ->
    base.copy(
        headlineMedium = base.headlineMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp
        ),
        titleLarge = base.titleLarge.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.25).sp
        ),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(letterSpacing = 0.4.sp),
        labelMedium = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.2.sp
        )
    )
}

@Composable
fun MyPropertiesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content
    )
}
