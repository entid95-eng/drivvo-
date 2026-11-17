package com.example.ui.silhouettes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.model.IconStyle
import com.example.model.VehicleCategory
import com.example.model.VehicleClass

fun parseHexColor(hex: String, fallback: Color = Color(0xFF1D63DC)): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrElse { fallback }
}

@Composable
fun VehicleSilhouette(
    vehicleClass: VehicleClass,
    iconStyle: IconStyle,
    accentColor: Color,
    modifier: Modifier = Modifier,
    surfaceColor: Color = MaterialTheme.colorScheme.surface,
    onSurfaceColor: Color = MaterialTheme.colorScheme.onSurface,
    showGroundLine: Boolean = true
) {
    Canvas(modifier = modifier.aspectRatio(2.0f)) {
        val sx = size.width / 120f
        val sy = size.height / 60f
        withTransform({
            scale(scaleX = sx, scaleY = sy, pivot = Offset.Zero)
        }) {
            if (showGroundLine && iconStyle != IconStyle.SOLID) {
                drawLine(
                    color = onSurfaceColor.copy(alpha = 0.14f),
                    start = Offset(6f, 54f),
                    end = Offset(114f, 54f),
                    strokeWidth = 1.2f,
                    cap = StrokeCap.Round
                )
            }
            when (vehicleClass.category) {
                VehicleCategory.CAR -> drawCar(vehicleClass, iconStyle, accentColor, surfaceColor, onSurfaceColor)
                VehicleCategory.MOTO -> drawMoto(vehicleClass, iconStyle, accentColor, surfaceColor, onSurfaceColor)
            }
        }
    }
}

private fun DrawScope.drawCar(
    vehicleClass: VehicleClass,
    style: IconStyle,
    accent: Color,
    surface: Color,
    onSurface: Color
) {
    val bodyPath = Path()
    val cabinGlassPath = Path()
    val wheelRadius: Float
    val frontWheelX = 92f
    val rearWheelX = 30f
    val wheelY: Float
    var roofRailPath: Path? = null

    when (vehicleClass) {
        VehicleClass.SEDAN -> {
            wheelRadius = 8.6f
            wheelY = 44.5f
            bodyPath.apply {
                moveTo(10f, 40f)
                lineTo(10f, 32f)
                quadraticTo(10f, 27f, 17f, 26f)
                lineTo(31f, 24f)
                lineTo(46f, 14f)
                quadraticTo(51f, 11f, 61f, 11f)
                lineTo(79f, 11f)
                quadraticTo(86f, 11f, 92f, 16f)
                lineTo(99f, 23f)
                lineTo(109f, 25f)
                quadraticTo(114f, 26.5f, 114f, 32f)
                lineTo(114f, 40f)
                quadraticTo(114f, 43f, 110f, 43f)
                lineTo(103f, 43f)
                lineTo(81f, 43f)
                lineTo(41f, 43f)
                lineTo(13f, 43f)
                quadraticTo(10f, 43f, 10f, 40f)
                close()
            }
            cabinGlassPath.apply {
                moveTo(36f, 24f)
                lineTo(48f, 15.5f)
                lineTo(64f, 15.5f)
                lineTo(64f, 24f)
                close()
                moveTo(67f, 15.5f)
                lineTo(78f, 15.5f)
                quadraticTo(83f, 15.5f, 89f, 21.5f)
                lineTo(92f, 24f)
                lineTo(67f, 24f)
                close()
            }
        }

        VehicleClass.CROSSOVER -> {
            wheelRadius = 9.6f
            wheelY = 44f
            bodyPath.apply {
                moveTo(11f, 38f)
                lineTo(11f, 27f)
                quadraticTo(11f, 21f, 16f, 19f)
                lineTo(24f, 11f)
                quadraticTo(28f, 9f, 35f, 9f)
                lineTo(75f, 9f)
                quadraticTo(82f, 9f, 87f, 13f)
                lineTo(97f, 21f)
                lineTo(109f, 23.5f)
                quadraticTo(114f, 25f, 114f, 31f)
                lineTo(114f, 38f)
                quadraticTo(114f, 41f, 110f, 41f)
                lineTo(14f, 41f)
                quadraticTo(11f, 41f, 11f, 38f)
                close()
            }
            roofRailPath = Path().apply {
                moveTo(31f, 8.5f)
                lineTo(34f, 6.8f)
                lineTo(77f, 6.8f)
                lineTo(80f, 8.5f)
            }
            cabinGlassPath.apply {
                moveTo(27f, 20f)
                lineTo(34f, 12.5f)
                lineTo(54f, 12.5f)
                lineTo(54f, 20f)
                close()
                moveTo(57f, 12.5f)
                lineTo(74f, 12.5f)
                quadraticTo(79f, 12.5f, 85f, 17f)
                lineTo(89f, 20f)
                lineTo(57f, 20f)
                close()
            }
        }

        else -> { // WAGON
            wheelRadius = 8.6f
            wheelY = 44.5f
            bodyPath.apply {
                moveTo(9.5f, 40f)
                lineTo(10f, 28f)
                quadraticTo(11f, 18f, 17f, 12.5f)
                quadraticTo(21f, 10.5f, 28f, 10.5f)
                lineTo(77f, 10.5f)
                quadraticTo(84f, 10.5f, 90f, 15.5f)
                lineTo(98f, 23f)
                lineTo(109f, 25f)
                quadraticTo(114f, 26.5f, 114f, 32f)
                lineTo(114f, 40f)
                quadraticTo(114f, 43f, 110f, 43f)
                lineTo(13f, 43f)
                quadraticTo(9.5f, 43f, 9.5f, 40f)
                close()
            }
            roofRailPath = Path().apply {
                moveTo(21f, 10f)
                lineTo(24f, 8.5f)
                lineTo(79f, 8.5f)
                lineTo(82f, 10f)
            }
            cabinGlassPath.apply {
                moveTo(19f, 22f)
                lineTo(23f, 14f)
                lineTo(42f, 14f)
                lineTo(42f, 22f)
                close()
                moveTo(45f, 14f)
                lineTo(63f, 14f)
                lineTo(63f, 22f)
                lineTo(45f, 22f)
                close()
                moveTo(66f, 14f)
                lineTo(77f, 14f)
                quadraticTo(82f, 14f, 88f, 19.5f)
                lineTo(91f, 22f)
                lineTo(66f, 22f)
                close()
            }
        }
    }

    when (style) {
        IconStyle.SOLID -> {
            drawPath(bodyPath, color = accent, style = Fill)
            roofRailPath?.let {
                drawPath(it, color = accent, style = Stroke(width = 1.8f, cap = StrokeCap.Round))
            }
            drawCarWheelCutout(rearWheelX, wheelY, wheelRadius, surface, accent)
            drawCarWheelCutout(frontWheelX, wheelY, wheelRadius, surface, accent)
            drawPath(cabinGlassPath, color = surface.copy(alpha = 0.35f), style = Fill)
        }

        IconStyle.OUTLINE -> {
            drawPath(bodyPath, color = accent.copy(alpha = 0.08f), style = Fill)
            drawPath(
                bodyPath,
                color = accent,
                style = Stroke(width = 2.0f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            roofRailPath?.let {
                drawPath(it, color = accent, style = Stroke(width = 1.6f, cap = StrokeCap.Round))
            }
            drawPath(
                cabinGlassPath,
                color = onSurface.copy(alpha = 0.65f),
                style = Stroke(width = 1.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // Beltline & wheel wells
            drawCarWheelOutline(rearWheelX, wheelY, wheelRadius, surface, accent)
            drawCarWheelOutline(frontWheelX, wheelY, wheelRadius, surface, accent)
        }

        IconStyle.TWO_TONE -> {
            // Subtle lower shadow
            drawOval(
                color = onSurface.copy(alpha = 0.12f),
                topLeft = Offset(14f, 50.5f),
                size = Size(95f, 4.5f)
            )
            drawPath(bodyPath, color = accent, style = Fill)
            // Dark roof/glass insert
            drawPath(cabinGlassPath, color = Color(0xFF182235), style = Fill)
            roofRailPath?.let {
                drawPath(it, color = Color(0xFF334155), style = Stroke(width = 1.8f, cap = StrokeCap.Round))
            }
            // Lower sill trim
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(40f, 41.5f),
                end = Offset(82f, 41.5f),
                strokeWidth = 2.2f,
                cap = StrokeCap.Round
            )
            // Headlight & taillight accents
            drawRoundRect(
                color = Color(0xFFFEF08A),
                topLeft = Offset(107f, 26f),
                size = Size(6f, 3.2f),
                cornerRadius = CornerRadius(1.5f, 1.5f)
            )
            drawRoundRect(
                color = Color(0xFFEF4444),
                topLeft = Offset(10.5f, 26.5f),
                size = Size(4.5f, 3.2f),
                cornerRadius = CornerRadius(1.5f, 1.5f)
            )
            drawCarWheelTwoTone(rearWheelX, wheelY, wheelRadius, surface)
            drawCarWheelTwoTone(frontWheelX, wheelY, wheelRadius, surface)
        }
    }
}

private fun DrawScope.drawCarWheelCutout(cx: Float, cy: Float, r: Float, surface: Color, accent: Color) {
    drawCircle(color = surface, radius = r + 1.8f, center = Offset(cx, cy))
    drawCircle(color = accent, radius = r, center = Offset(cx, cy))
    drawCircle(color = surface, radius = r * 0.48f, center = Offset(cx, cy))
}

private fun DrawScope.drawCarWheelOutline(cx: Float, cy: Float, r: Float, surface: Color, accent: Color) {
    drawCircle(color = surface, radius = r + 1.5f, center = Offset(cx, cy))
    drawCircle(color = accent, radius = r, center = Offset(cx, cy), style = Stroke(width = 2.0f))
    drawCircle(color = accent.copy(alpha = 0.65f), radius = r * 0.45f, center = Offset(cx, cy), style = Stroke(width = 1.2f))
}

private fun DrawScope.drawCarWheelTwoTone(cx: Float, cy: Float, r: Float, surface: Color) {
    drawCircle(color = surface, radius = r + 1.6f, center = Offset(cx, cy))
    drawCircle(color = Color(0xFF111827), radius = r, center = Offset(cx, cy))
    drawCircle(color = Color(0xFFCBD5E1), radius = r * 0.56f, center = Offset(cx, cy))
    drawCircle(color = Color(0xFF334155), radius = r * 0.22f, center = Offset(cx, cy))
}

private fun DrawScope.drawMoto(
    vehicleClass: VehicleClass,
    style: IconStyle,
    accent: Color,
    surface: Color,
    onSurface: Color
) {
    val rearX: Float
    val rearY: Float
    val frontX: Float
    val frontY: Float
    val wheelR: Float

    when (vehicleClass) {
        VehicleClass.SCOOTER -> {
            wheelR = 7.8f
            rearX = 29f
            rearY = 45.5f
            frontX = 91f
            frontY = 45.5f
        }
        else -> {
            wheelR = 9.8f
            rearX = 27f
            rearY = 43.5f
            frontX = 93f
            frontY = 43.5f
        }
    }

    // Draw wheels first as base layer
    when (style) {
        IconStyle.SOLID -> {
            drawMotoWheelSolid(rearX, rearY, wheelR, accent, surface)
            drawMotoWheelSolid(frontX, frontY, wheelR, accent, surface)
        }
        IconStyle.OUTLINE -> {
            drawMotoWheelOutline(rearX, rearY, wheelR, accent)
            drawMotoWheelOutline(frontX, frontY, wheelR, accent)
        }
        IconStyle.TWO_TONE -> {
            drawOval(
                color = onSurface.copy(alpha = 0.12f),
                topLeft = Offset(16f, 51f),
                size = Size(88f, 4f)
            )
            drawMotoWheelTwoTone(rearX, rearY, wheelR)
            drawMotoWheelTwoTone(frontX, frontY, wheelR)
        }
    }

    // Swingarm & Front Fork lines
    val forkTop = when (vehicleClass) {
        VehicleClass.TOUR_ENDURO -> Offset(78f, 14f)
        VehicleClass.TOURING -> Offset(78f, 16f)
        VehicleClass.SCOOTER -> Offset(81f, 17f)
        VehicleClass.SPORT -> Offset(79f, 18f)
        VehicleClass.NAKED -> Offset(79f, 16f)
        else -> Offset(79f, 16f)
    }

    val chassisColor = when (style) {
        IconStyle.SOLID -> accent
        IconStyle.OUTLINE -> onSurface.copy(alpha = 0.78f)
        IconStyle.TWO_TONE -> Color(0xFF1E293B)
    }

    // Front Fork & Rear Swingarm
    drawLine(chassisColor, start = forkTop, end = Offset(frontX, frontY), strokeWidth = 2.8f, cap = StrokeCap.Round)
    drawLine(chassisColor, start = Offset(49f, 37f), end = Offset(rearX, rearY), strokeWidth = 2.6f, cap = StrokeCap.Round)

    // Engine / Frame block (non-scooter)
    val enginePath = Path()
    val paintedBodyPath = Path()
    val darkTrimPath = Path()
    var windscreenPath: Path? = null
    var luggagePath: Path? = null

    when (vehicleClass) {
        VehicleClass.NAKED -> {
            // Exposed compact engine & trellis frame
            enginePath.apply {
                moveTo(47f, 27f)
                lineTo(74f, 25f)
                lineTo(73f, 38f)
                lineTo(52f, 39f)
                close()
            }
            // Sculpted fuel tank + short tail cowl
            paintedBodyPath.apply {
                moveTo(52f, 24f)
                quadraticTo(62f, 16f, 77f, 20f)
                lineTo(76f, 26f)
                lineTo(53f, 26f)
                close()
                // Compact streetfighter tail
                moveTo(16f, 21f)
                lineTo(37f, 24f)
                lineTo(35f, 28f)
                lineTo(19f, 24f)
                close()
            }
            // Flat saddle + bar + round headlight
            darkTrimPath.apply {
                moveTo(35f, 24f)
                quadraticTo(45f, 27f, 54f, 24f)
                lineTo(54f, 27f)
                lineTo(35f, 27f)
                close()
            }
            // Upright handlebar & headlight
            drawLine(chassisColor, Offset(77f, 17f), Offset(83f, 14f), strokeWidth = 2.3f, cap = StrokeCap.Round)
            drawCircle(
                color = if (style == IconStyle.TWO_TONE) Color(0xFFFEF08A) else accent,
                radius = 3.2f,
                center = Offset(87f, 23f),
                style = if (style == IconStyle.OUTLINE) Stroke(1.6f) else Fill
            )
        }

        VehicleClass.SPORT -> {
            // Full aerodynamic fairing + tucked tail
            paintedBodyPath.apply {
                moveTo(47f, 27f)
                quadraticTo(58f, 15f, 77f, 18f)
                lineTo(95f, 21f)
                quadraticTo(99f, 24f, 94f, 30f)
                lineTo(73f, 38f)
                lineTo(51f, 38f)
                close()
                // Sharp upswept sport tail
                moveTo(13f, 15f)
                lineTo(38f, 24f)
                lineTo(36f, 29f)
                lineTo(16f, 19f)
                close()
            }
            // Tucked bubble windscreen
            windscreenPath = Path().apply {
                moveTo(76f, 18f)
                quadraticTo(82f, 12f, 90f, 17f)
                lineTo(87f, 20f)
                close()
            }
            // Low racing saddle
            darkTrimPath.apply {
                moveTo(36f, 24f)
                quadraticTo(44f, 27f, 52f, 24f)
                lineTo(51f, 28f)
                lineTo(36f, 28f)
                close()
            }
        }

        VehicleClass.TOUR_ENDURO -> {
            // Engine + skid plate
            enginePath.apply {
                moveTo(48f, 27f)
                lineTo(75f, 26f)
                lineTo(73f, 40f)
                lineTo(52f, 40f)
                close()
            }
            // High tank + signature front "beak" fender + handguards
            paintedBodyPath.apply {
                moveTo(52f, 24f)
                quadraticTo(62f, 15f, 78f, 19f)
                lineTo(91f, 20f)
                lineTo(105f, 23.5f) // Adventure beak!
                lineTo(91f, 25f)
                lineTo(75f, 27f)
                lineTo(52f, 27f)
                close()
            }
            // Tall rally windscreen
            windscreenPath = Path().apply {
                moveTo(81f, 18f)
                lineTo(86f, 7.5f)
                lineTo(90f, 11f)
                lineTo(88f, 19f)
                close()
            }
            // Rear top-box / rack silhouette
            luggagePath = Path().apply {
                moveTo(14f, 12f)
                lineTo(31f, 12f)
                lineTo(31f, 23f)
                lineTo(14f, 23f)
                close()
            }
            // Stepped adventure saddle
            darkTrimPath.apply {
                moveTo(32f, 23f)
                lineTo(53f, 24f)
                lineTo(53f, 28f)
                lineTo(32f, 27f)
                close()
            }
            // High bar + handguard
            drawLine(chassisColor, Offset(77f, 15f), Offset(83f, 12f), strokeWidth = 2.4f, cap = StrokeCap.Round)
        }

        VehicleClass.SCOOTER -> {
            // Step-through body + wide legshield + rear CVT
            paintedBodyPath.apply {
                moveTo(19f, 24f)
                lineTo(44f, 25f)
                lineTo(53f, 36f)
                lineTo(73f, 36f)
                lineTo(78f, 18f)
                lineTo(88f, 21f)
                lineTo(86f, 38f)
                lineTo(49f, 40f)
                lineTo(22f, 34f)
                close()
            }
            windscreenPath = Path().apply {
                moveTo(79f, 18f)
                lineTo(83f, 9f)
                lineTo(87f, 13f)
                lineTo(85f, 19f)
                close()
            }
            darkTrimPath.apply {
                moveTo(22f, 22f)
                quadraticTo(35f, 21f, 47f, 24.5f)
                lineTo(45f, 27.5f)
                lineTo(21f, 26f)
                close()
            }
        }

        VehicleClass.TOURING -> {
            // Broad front fairing + plush tourer tank & bodywork
            paintedBodyPath.apply {
                moveTo(50f, 24f)
                quadraticTo(62f, 16f, 79f, 17f)
                lineTo(98f, 20f)
                quadraticTo(103f, 24f, 97f, 33f)
                lineTo(75f, 39f)
                lineTo(49f, 38f)
                close()
            }
            // Tall touring windscreen
            windscreenPath = Path().apply {
                moveTo(81f, 17f)
                lineTo(87f, 6.5f)
                lineTo(93f, 11f)
                lineTo(91f, 18.5f)
                close()
            }
            // Integrated hard side pannier + rear top-case with passenger backrest!
            luggagePath = Path().apply {
                // Top case
                moveTo(13f, 13f)
                lineTo(27f, 13f)
                lineTo(27f, 24f)
                lineTo(13f, 24f)
                close()
                // Hard side pannier
                moveTo(16f, 25f)
                lineTo(39f, 25f)
                lineTo(38f, 36f)
                lineTo(17f, 35f)
                close()
            }
            // Plush stepped tourer seat
            darkTrimPath.apply {
                moveTo(27f, 18f)
                lineTo(33f, 23f)
                lineTo(52f, 24f)
                lineTo(51f, 28f)
                lineTo(27f, 25f)
                close()
            }
        }

        else -> Unit
    }

    when (style) {
        IconStyle.SOLID -> {
            if (!enginePath.isEmpty) drawPath(enginePath, color = accent, style = Fill)
            drawPath(paintedBodyPath, color = accent, style = Fill)
            drawPath(darkTrimPath, color = accent, style = Fill)
            windscreenPath?.let { drawPath(it, color = accent.copy(alpha = 0.72f), style = Fill) }
            luggagePath?.let { drawPath(it, color = accent, style = Fill) }
        }

        IconStyle.OUTLINE -> {
            if (!enginePath.isEmpty) {
                drawPath(enginePath, color = onSurface.copy(alpha = 0.6f), style = Stroke(1.5f, join = StrokeJoin.Round))
            }
            drawPath(paintedBodyPath, color = accent.copy(alpha = 0.10f), style = Fill)
            drawPath(paintedBodyPath, color = accent, style = Stroke(2.0f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(darkTrimPath, color = onSurface.copy(alpha = 0.75f), style = Stroke(1.5f, join = StrokeJoin.Round))
            windscreenPath?.let {
                drawPath(it, color = accent.copy(alpha = 0.8f), style = Stroke(1.4f, join = StrokeJoin.Round))
            }
            luggagePath?.let {
                drawPath(it, color = accent.copy(alpha = 0.85f), style = Stroke(1.7f, join = StrokeJoin.Round))
            }
        }

        IconStyle.TWO_TONE -> {
            if (!enginePath.isEmpty) drawPath(enginePath, color = Color(0xFF475569), style = Fill)
            drawPath(paintedBodyPath, color = accent, style = Fill)
            drawPath(darkTrimPath, color = Color(0xFF0F172A), style = Fill)
            windscreenPath?.let { drawPath(it, color = Color(0xFF93C5FD).copy(alpha = 0.75f), style = Fill) }
            luggagePath?.let {
                drawPath(it, color = Color(0xFF334155), style = Fill)
                drawPath(it, color = accent.copy(alpha = 0.7f), style = Stroke(1.1f))
            }
        }
    }
}

private fun DrawScope.drawMotoWheelSolid(cx: Float, cy: Float, r: Float, accent: Color, surface: Color) {
    drawCircle(color = accent, radius = r, center = Offset(cx, cy))
    drawCircle(color = surface, radius = r * 0.56f, center = Offset(cx, cy))
    drawCircle(color = accent, radius = r * 0.2f, center = Offset(cx, cy))
}

private fun DrawScope.drawMotoWheelOutline(cx: Float, cy: Float, r: Float, accent: Color) {
    drawCircle(color = accent, radius = r, center = Offset(cx, cy), style = Stroke(2.1f))
    drawCircle(color = accent.copy(alpha = 0.6f), radius = r * 0.56f, center = Offset(cx, cy), style = Stroke(1.2f))
}

private fun DrawScope.drawMotoWheelTwoTone(cx: Float, cy: Float, r: Float) {
    drawCircle(color = Color(0xFF111827), radius = r, center = Offset(cx, cy))
    drawCircle(color = Color(0xFFE2E8F0), radius = r * 0.58f, center = Offset(cx, cy))
    drawCircle(color = Color(0xFF334155), radius = r * 0.32f, center = Offset(cx, cy))
}
