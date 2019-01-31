package magym.rectangles.domain.model

import android.graphics.Color
import android.graphics.Paint

data class Rect(
    val playerId: Int,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {

    constructor(player: Int, preRect: PreRect) :
            this(player, preRect.left, preRect.top, preRect.right, preRect.bottom)

    val area = (right - left) * (bottom - top)

    val paint: Paint = Paint().apply {
        style = Paint.Style.FILL

        color = when (playerId) {
            0 -> Color.argb(64, 0, 0, 255)
            else -> Color.argb(64, 0, 255, 0)
        }
    }

    val paintFrame: Paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f

        color = when (playerId) {
            0 -> Color.argb(255, 128, 128, 255)
            else -> Color.argb(255, 0, 255, 0)
        }
    }

    val rect: Array<Int> = arrayOf(left, top, right, bottom)

}