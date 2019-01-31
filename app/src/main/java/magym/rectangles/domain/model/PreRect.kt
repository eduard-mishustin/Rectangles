package magym.rectangles.domain.model

import android.graphics.Color
import android.graphics.Paint
import java.lang.Math.pow
import kotlin.math.sqrt

data class PreRect(
    var left: Int,
    var top: Int,
    var right: Int,
    var bottom: Int
) {

    // Все стороны в левом верхнем углу
    val rectIsNull get() = left == top && top == right && right == bottom && bottom == 0

    // Обе стороны не равны нулю
    private val rectIsNotNull get() = left != right && top != bottom

    private val paint: Paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    override fun toString(): String {
        return "[left = $left, right = $right, top = $top, bottom = $bottom]"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PreRect) return false
        return left == other.left && top == other.top && right == other.right && bottom == other.bottom
    }

    override fun hashCode() = 30 * left + 31 * top + 11 * right + 9 * bottom

    fun copyPreRect(new: PreRect) {
        new.left = left
        new.right = right
        new.top = top
        new.bottom = bottom
    }

    fun getPaint(isCorrect: Boolean): Paint {
        if (isCorrect) paint.color = Color.DKGRAY
        else paint.color = Color.RED
        return paint
    }

    fun clear() {
        left = 0
        top = 0
        right = 0
        bottom = 0
    }

    // Привязка к ближайшей точке
    fun bounding(firstDice: Int, secondDice: Int, startX: Int, startY: Int, endX: Int, endY: Int): Array<Int> {
        // Возможные точки для привязки
        val firstSides = arrayOf(
            startX + firstDice, startX - firstDice, startX + firstDice, startX - firstDice,
            startX + secondDice, startX - secondDice, startX + secondDice, startX - secondDice
        )
        val secondSides = arrayOf(
            startY + secondDice, startY - secondDice, startY - secondDice, startY + secondDice,
            startY + firstDice, startY - firstDice, startY - firstDice, startY + firstDice
        )

        // Гипотенуза от второй точки касания до возможной точке привязки
        fun hyp(i: Int) =
            sqrt(pow((endX - firstSides[i]).toDouble(), 2.0) + pow((endY - secondSides[i]).toDouble(), 2.0))

        var minHyp = hyp(0)
        var newHyp: Double
        var k = 0

        // Поиск минимальной гипотенузы
        for (i in 1..7) {
            newHyp = hyp(i)

            if (newHyp < minHyp) {
                minHyp = newHyp
                k = i
            }
        }

        return arrayOf(firstSides[k], secondSides[k])
    }

    // Проверка на соответствие всем условиям при построении прямоугольника
    fun rectIsCorrect(rects: List<Rect>, currentPlayer: Int, nX: Int, nY: Int): Boolean {
        if (!rectIsNotNull || !inScreen(nX, nY)) return false
        for (currentRect in rects) if (intersectionRects(currentRect)) return false
        for (currentRect in rects) if (equalsSide(currentPlayer, currentRect)) return true

        return false
    }

    // В пределах экрана
    private fun inScreen(nX: Int, nY: Int) =
        left in 1 until nX && right in 1 until nX && top in 1 until nY && bottom in 1 until nY

    // Совпадение по длине с числами на кубике. (С привязками не нужно)
    private fun isInDice(firstDice: Int, secondDice: Int) =
        right - left == firstDice && bottom - top == secondDice || right - left == secondDice && bottom - top == firstDice

    // Проверка на касание с прямоугольником тогоже цвета
    private fun equalsSide(currentPlayer: Int, r: Rect): Boolean {
        val vert = (top + 1)..bottom
        val horiz = (left + 1)..right
        val rVert = (r.top + 1)..r.bottom
        val rHoriz = (r.left + 1)..r.right

        val touchLeftSide = right == r.left && (top in rVert || bottom in rVert || r.top in vert || r.bottom in vert)
        val touchRightSide = left == r.right && (top in rVert || bottom in rVert || r.top in vert || r.bottom in vert)
        val touchTopSide = bottom == r.top && (left in rHoriz || right in rHoriz || r.left in horiz || r.right in horiz)
        val touchBotSide = top == r.bottom && (left in rHoriz || right in rHoriz || r.left in horiz || r.right in horiz)

        return (currentPlayer == r.playerId) && (touchLeftSide || touchRightSide || touchTopSide || touchBotSide)
    }

    // Пересечение прямоугольников
    private fun intersectionRects(r: Rect): Boolean {
        val vert = (top + 1)..(bottom - 1)
        val horiz = (left + 1)..(right - 1)
        val rVert = (r.top + 1)..(r.bottom - 1)
        val rHoriz = (r.left + 1)..(r.right - 1)
        val horizStrictly = left..right
        val vertStrictly = top..bottom

        val cornerPreRectInRect = left in rHoriz && bottom in rVert || right in rHoriz && top in rVert ||
                left in rHoriz && top in rVert || right in rHoriz && bottom in rVert
        val cornerRectInPreRect = r.left in horiz && r.top in vert || r.right in horiz && r.bottom in vert ||
                r.left in horiz && r.bottom in vert || r.right in horiz && r.top in vert
        val sidePreRectInRect = r.left in horizStrictly && r.right in horizStrictly &&
                (top in rVert || bottom in rVert || r.top in vert || r.bottom in vert)
        val sideRectInPreRect = r.top in vertStrictly && r.bottom in vertStrictly &&
                (left in rHoriz || right in rHoriz || r.left in horiz || r.right in horiz)
        val rectsCoincidence = r.left == left && r.right == right && r.top == top && r.bottom == bottom

        return cornerPreRectInRect || cornerRectInPreRect || sidePreRectInRect || sideRectInPreRect || rectsCoincidence
    }

}