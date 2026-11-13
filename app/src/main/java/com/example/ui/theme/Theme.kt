package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
  primary = IndigoPrimary,
  onPrimary = Color.White,
  primaryContainer = IndigoContainer,
  onPrimaryContainer = IndigoOnContainer,
  secondary = TealSecondary,
  onSecondary = Color.White,
  secondaryContainer = TealContainer,
  onSecondaryContainer = TealOnContainer,
  tertiary = CoralAccent,
  onTertiary = Color.White,
  tertiaryContainer = CoralContainer,
  onTertiaryContainer = CoralOnContainer,
  background = WarmCreamBg,
  onBackground = SlateInk,
  surface = PureWhiteSurface,
  onSurface = SlateInk,
  surfaceVariant = SoftMistSurface,
  onSurfaceVariant = SlateMuted,
  outline = OutlineMist,
  error = RoseError,
  onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
  primary = DarkIndigoPrimary,
  onPrimary = Color(0xFF0A124A),
  primaryContainer = Color(0xFF283593),
  onPrimaryContainer = IndigoContainer,
  secondary = DarkTealSecondary,
  onSecondary = Color(0xFF003730),
  secondaryContainer = Color(0xFF004D40),
  onSecondaryContainer = TealContainer,
  tertiary = DarkCoralTertiary,
  onTertiary = Color(0xFF5C1504),
  tertiaryContainer = Color(0xFF932710),
  onTertiaryContainer = CoralContainer,
  background = DarkNavyBg,
  onBackground = DarkInk,
  surface = DarkSurface,
  onSurface = DarkInk,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkMuted,
  outline = Color(0xFF334155),
  error = Color(0xFFFCA5A5),
  onError = Color(0xFF7F1D1D)
)

val SkillSwapShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SkillSwapTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = SkillSwapTypography,
    shapes = SkillSwapShapes,
    content = content
  )
}
