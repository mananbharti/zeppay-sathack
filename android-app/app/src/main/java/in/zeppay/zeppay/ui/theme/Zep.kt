@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package `in`.zeppay.zeppay.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zeppay.zeppay.R

/*
 * ZepPay identity: "Blue Current". Near-black base, one blue for action, light blues for
 * information and the hero glow. Palette from the ZepPay website: #080808, #3898EC,
 * #2476CC, #A5D4FF, #4CD5FF. Status colours stay green / red / amber and always come
 * with a word. Every token has ONE job; the full contract is in docs/DESIGN.md.
 * Screens never use a hex value: everything comes from these tokens.
 */

@Immutable
data class ZepColors(
    val bg: Color,                       // the page
    val card: Color,                     // grouped information (lists, payment details)
    val tile: Color,                     // shortcut rows and inputs
    val raised: Color,                   // a surface on a card: avatars, icon wells, the scan card's face
    val line: Color,                     // hairline borders and dividers
    val text: Color, val muted: Color,
    val link: Color,                     // a text action ("See all", "Copy")
    val primary: Color, val onPrimary: Color,       // blue pill: the one next action
    val secondary: Color, val onSecondary: Color,   // quiet pill: any other choice
    val glowA: Color, val glowB: Color,             // the hero band and the live-payment rings only
    val iconBg: Color, val iconFg: Color,           // round icon wells on shortcuts
    val paid: Color, val failed: Color, val unsure: Color,   // status colour (text + tinted pill)
    val bank: Color,                     // dashed "bank's counter" frame
    val camera: Color, val onCamera: Color,          // the camera viewfinder (always dark)
    val qrPaper: Color, val qrInk: Color,            // behind / in a QR: always dark on light, scanners need it
    val dark: Boolean,
)

/** Website palette, named once. Only this file reads them. */
private object Palette {
    val black = Color(0xFF080808)
    val blue = Color(0xFF3898EC)
    val deepBlue = Color(0xFF2476CC)
    val lightBlue = Color(0xFFA5D4FF)
    val cyan = Color(0xFF4CD5FF)
}

/** Night: the default. */
val Dark = ZepColors(
    bg = Palette.black, card = Color(0xFF121418), tile = Color(0xFF121418), raised = Color(0xFF1A2230), line = Color(0xFF242B36),
    text = Color(0xFFF2F6FB), muted = Color(0xFF9AA7B8), link = Palette.lightBlue,
    primary = Palette.blue, onPrimary = Palette.black,
    secondary = Color(0xFF1A2230), onSecondary = Color(0xFFF2F6FB),
    glowA = Palette.blue, glowB = Palette.cyan,
    iconBg = Color(0xFF16263A), iconFg = Palette.lightBlue,
    paid = Color(0xFF5EE6A0), failed = Color(0xFFFF7A86), unsure = Color(0xFFFFC44D),
    bank = Palette.cyan, camera = Palette.black, onCamera = Color(0xFFF2F6FB), qrPaper = Color(0xFFFFFFFF), qrInk = Palette.black, dark = true,
)

/** Daylight: for bright sun. Same roles, black ink on blue-white. */
val Light = ZepColors(
    bg = Color(0xFFF5F8FC), card = Color(0xFFFFFFFF), tile = Color(0xFFFFFFFF), raised = Color(0xFFE3EEFA), line = Color(0xFFD5E1EE),
    text = Palette.black, muted = Color(0xFF4A5566), link = Color(0xFF1B5FA8),
    primary = Palette.deepBlue, onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFE3EEFA), onSecondary = Color(0xFF0B2A4A),
    glowA = Palette.blue, glowB = Palette.cyan,
    iconBg = Color(0xFFD9ECFF), iconFg = Color(0xFF1B5FA8),
    paid = Color(0xFF0B7A4B), failed = Color(0xFFC62F43), unsure = Color(0xFF8F5F00),
    bank = Palette.deepBlue, camera = Palette.black, onCamera = Color(0xFFF2F6FB), qrPaper = Color(0xFFFFFFFF), qrInk = Palette.black, dark = false,
)

private fun bricolage(w: Int) = Font(R.font.bricolage, FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w), FontVariation.width(100f), FontVariation.Setting("opsz", 36f)))
private fun body(w: Int) = Font(R.font.atkinson, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))

/** Headings and numerals: Bricolage Grotesque, normal width. */
val Display = FontFamily(bricolage(700), bricolage(600))
/** Atkinson Hyperlegible Next: built for low-vision readers. Every sentence. */
val Body = FontFamily(body(400), body(700), body(800))

object ZType {
    val amount = TextStyle(fontFamily = Display, fontWeight = FontWeight(700), fontSize = 64.sp, lineHeight = 66.sp, letterSpacing = (-1.5).sp)
    val hero = TextStyle(fontFamily = Display, fontWeight = FontWeight(700), fontSize = 38.sp, lineHeight = 42.sp, letterSpacing = (-0.8).sp)
    val title = TextStyle(fontFamily = Display, fontWeight = FontWeight(700), fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.4).sp)
    val label = TextStyle(fontFamily = Display, fontWeight = FontWeight(600), fontSize = 21.sp, lineHeight = 25.sp)
    val body = TextStyle(fontFamily = Body, fontWeight = FontWeight(400), fontSize = 18.sp, lineHeight = 26.sp)
    val bodyBold = body.copy(fontWeight = FontWeight(700))
    val small = TextStyle(fontFamily = Body, fontWeight = FontWeight(400), fontSize = 16.sp, lineHeight = 23.sp)
    val button = TextStyle(fontFamily = Body, fontWeight = FontWeight(800), fontSize = 20.sp, lineHeight = 24.sp)
    val mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp, lineHeight = 21.sp)
}

/*
 * Shapes, each with one role:
 *   Pill (full radius)          = an action. Blue = the next step; quiet = any other choice.
 *   Small tinted pill, no edge  = a status (Paid / Not confirmed). Never pressable.
 *   Card (24dp)                 = grouped information.
 *   Round icon well + label     = a shortcut (Home quick actions). Rows (20dp) = list shortcuts.
 *   Inputs are 18dp with a hairline.
 *   Scan card (32dp, blue frame)= the ONE hero object on Home. Rings = the live payment.
 *   Small circle                = a person.
 *   Dashed cyan frame           = the bank's counter, where ZepPay steps aside.
 */
object Shapes {
    val pill = CircleShape.let { RoundedCornerShape(50) }
    val card = RoundedCornerShape(24.dp)
    val tile = RoundedCornerShape(20.dp)
    val input = RoundedCornerShape(18.dp)
    val hero = RoundedCornerShape(32.dp)
}

/*
 * Motion, each with one role:
 *   press   = pill/tile scales to 96%: your tap registered.
 *   forward = next screen rises in; back = it sinks away.
 *   rings   = the live-payment rings ripple outward ONLY while the carrier is working.
 *   outcome = Paid badge pops with one ring; Failed shakes once; Not confirmed stays still.
 * With the system's "remove animations" on, every motion is instant.
 */
object Motion {
    val press = spring<Float>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)
    val emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    fun <T> shutter() = tween<T>(340, easing = emphasized)
    fun <T> pop() = spring<T>(dampingRatio = 0.45f, stiffness = 420f)
}

/**
 * The same night tokens as ARGB ints, for views drawn outside Compose (the overlay card over
 * the bank's screen). It is always night: it floats over the carrier's own dialog.
 */
object ZepArgb {
    val bg get() = Dark.bg.toArgb()
    val card get() = Dark.card.toArgb()
    val line get() = Dark.line.toArgb()
    val text get() = Dark.text.toArgb()
    val muted get() = Dark.muted.toArgb()
    val primary get() = Dark.primary.toArgb()
    val raised get() = Dark.raised.toArgb()
    val bank get() = Dark.bank.toArgb()
}

val LocalZep = staticCompositionLocalOf { Dark }
val LocalReduceMotion = staticCompositionLocalOf { false }

object Zep {
    val c: ZepColors @Composable get() = LocalZep.current
    val reduceMotion: Boolean @Composable get() = LocalReduceMotion.current
}

@Composable
fun ZepTheme(mode: String, content: @Composable () -> Unit) {
    // Night-first; "auto" follows the system, "light" is the daylight variant.
    val dark = when (mode) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    val ctx = LocalContext.current
    val reduce = remember { try { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f } catch (_: Exception) { false } }
    CompositionLocalProvider(LocalZep provides if (dark) Dark else Light, LocalReduceMotion provides reduce, content = content)
}
