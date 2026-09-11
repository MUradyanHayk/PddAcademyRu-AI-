package ru.pdd.academy.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Light = lightColorScheme(
    primary = Color(0xFF195CDB), onPrimary = Color.White,
    primaryContainer = Color(0xFFE6EDFF), onPrimaryContainer = Color(0xFF153C83),
    secondary = Color(0xFF007E6E), secondaryContainer = Color(0xFFD7F4E8),
    onSecondaryContainer = Color(0xFF005140),
    background = Color(0xFFF5F7FA), surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEAF0F5), onSurface = Color(0xFF182338),
    onSurfaceVariant = Color(0xFF566478), outlineVariant = Color(0xFFDEE4ED)
)
private val Dark = darkColorScheme(
    primary = Color(0xFFAEC6FF), onPrimary = Color(0xFF002C74),
    primaryContainer = Color(0xFF193C77), onPrimaryContainer = Color(0xFFDCE7FF),
    secondary = Color(0xFF72DABD), secondaryContainer = Color(0xFF134C42),
    onSecondaryContainer = Color(0xFFA8EED7),
    background = Color(0xFF101722), surface = Color(0xFF192230),
    surfaceVariant = Color(0xFF273344), onSurface = Color(0xFFEBF0FA),
    onSurfaceVariant = Color(0xFFB2C0D3), outlineVariant = Color(0xFF344255)
)

@Composable
fun AcademyTheme(theme: String, content: @Composable () -> Unit) {
    val dark = theme == "dark" || (theme == "system" && isSystemInDarkTheme())
    MaterialTheme(colorScheme = if (dark) Dark else Light,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp)),
        typography = Typography(
            headlineLarge = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
            headlineMedium = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 33.sp),
            titleLarge = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 28.sp),
            titleMedium = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp),
            bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, lineHeight = 26.sp),
            bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, lineHeight = 23.sp),
            labelLarge = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        ), content = content)
}
