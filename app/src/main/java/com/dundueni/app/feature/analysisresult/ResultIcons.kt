package com.dundueni.app.feature.analysisresult

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class ResultIcon { SHIELD, WARNING, BACK, CLOSE, PROFILE, MESSAGE, CHECK, LINK, PHONE, SHARE, SAVE, SEARCH, FAMILY, CLOCK, AI, TIP }

/** Small vector icons drawn locally: no new icon-library dependency. */
@Composable
internal fun ResultGlyph(kind: ResultIcon, color: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        scale(this.size.width / 24f, this.size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(1.7f, cap = StrokeCap.Round)
            fun line(x1: Float, y1: Float, x2: Float, y2: Float, tint: Color = color) =
                drawLine(tint, Offset(x1, y1), Offset(x2, y2), 1.7f, StrokeCap.Round)
            fun check(tint: Color = color) { line(7f, 12f, 10.5f, 15.5f, tint); line(10.5f, 15.5f, 17f, 8.5f, tint) }
            when (kind) {
                ResultIcon.SHIELD -> {
                    drawPath(Path().apply { moveTo(12f, 2f); lineTo(21f, 6f); lineTo(20f, 14f); quadraticTo(18f, 20f, 12f, 23f); quadraticTo(6f, 20f, 4f, 14f); lineTo(3f, 6f); close() }, color)
                    check(Color.White)
                }
                ResultIcon.WARNING -> {
                    drawPath(Path().apply { moveTo(12f, 2f); lineTo(23f, 22f); lineTo(1f, 22f); close() }, color)
                    line(12f, 8f, 12f, 14f, Color.White); drawCircle(Color.White, 1f, Offset(12f, 18f))
                }
                ResultIcon.BACK -> { line(4f, 12f, 20f, 12f); line(4f, 12f, 10f, 6f); line(4f, 12f, 10f, 18f) }
                ResultIcon.CLOSE -> { line(6f, 6f, 18f, 18f); line(18f, 6f, 6f, 18f) }
                ResultIcon.CHECK -> check()
                ResultIcon.PROFILE, ResultIcon.FAMILY -> {
                    drawCircle(color, 3f, Offset(12f, 7f), style = stroke)
                    drawPath(Path().apply { moveTo(5f, 21f); lineTo(5f, 17f); quadraticTo(12f, 10f, 19f, 17f); lineTo(19f, 21f); close() }, color, style = stroke)
                }
                ResultIcon.MESSAGE -> {
                    drawPath(Path().apply { moveTo(3f, 3f); lineTo(21f, 3f); lineTo(21f, 18f); lineTo(8f, 18f); lineTo(3f, 22f); close() }, color, style = stroke)
                    line(7f, 8f, 17f, 8f); line(7f, 12f, 14f, 12f)
                }
                ResultIcon.SAVE -> {
                    drawPath(Path().apply { moveTo(6f, 3f); lineTo(18f, 3f); lineTo(18f, 22f); lineTo(12f, 18f); lineTo(6f, 22f); close() }, color, style = stroke)
                }
                ResultIcon.SEARCH -> { drawCircle(color, 7f, Offset(10f, 10f), style = stroke); line(15f, 15f, 22f, 22f) }
                ResultIcon.CLOCK -> { drawCircle(color, 9f, Offset(12f, 12f), style = stroke); line(12f, 6f, 12f, 12f); line(12f, 12f, 17f, 15f) }
                ResultIcon.SHARE -> {
                    line(6f, 12f, 18f, 5f); line(6f, 12f, 18f, 19f)
                    listOf(Offset(5f, 12f), Offset(19f, 4f), Offset(19f, 20f)).forEach { drawCircle(color, 2.5f, it) }
                }
                ResultIcon.LINK -> {
                    drawPath(Path().apply { moveTo(10f, 8f); quadraticTo(4f, 3f, 3f, 9f); quadraticTo(2f, 12f, 7f, 15f); lineTo(10f, 17f) }, color, style = stroke)
                    drawPath(Path().apply { moveTo(14f, 7f); lineTo(18f, 10f); quadraticTo(24f, 14f, 20f, 19f); quadraticTo(17f, 22f, 13f, 18f) }, color, style = stroke)
                    line(8f, 8f, 17f, 17f)
                }
                ResultIcon.PHONE -> {
                    drawPath(Path().apply { moveTo(4f, 3f); lineTo(9f, 3f); lineTo(10f, 8f); lineTo(7f, 10f); quadraticTo(10f, 16f, 15f, 17f); lineTo(17f, 14f); lineTo(22f, 16f); lineTo(21f, 21f); quadraticTo(6f, 23f, 2f, 5f); close() }, color)
                }
                ResultIcon.AI -> {
                    drawRoundRect(color, Offset(5f, 6f), androidx.compose.ui.geometry.Size(14f, 13f), androidx.compose.ui.geometry.CornerRadius(3f), style = stroke)
                    line(12f, 2f, 12f, 6f); line(2f, 10f, 5f, 10f); line(19f, 10f, 22f, 10f)
                    drawCircle(color, 1f, Offset(9f, 11f)); drawCircle(color, 1f, Offset(15f, 11f)); line(9f, 15f, 15f, 15f)
                }
                ResultIcon.TIP -> { drawCircle(color, 6f, Offset(12f, 9f), style = stroke); line(9f, 15f, 9f, 19f); line(15f, 15f, 15f, 19f); line(9f, 19f, 15f, 19f); line(10f, 22f, 14f, 22f) }
            }
        }
    }
}
