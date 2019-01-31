package magym.rectangles.presentation.screen.surface

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import magym.rectangles.R
import magym.rectangles.domain.model.Player
import magym.rectangles.domain.model.PreRect
import magym.rectangles.domain.model.Rect
import magym.rectangles.presentation.base.surface.BaseSurfaceView
import magym.rectangles.util.extension.log
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class GameSurfaceView(context: Context, attributeSet: AttributeSet) : BaseSurfaceView(context, attributeSet) {

    // TODO Вынести стринги

    private lateinit var view: MainView

    var cell = 60
        set(value) {
            field = value
            initMapSize()
        }
    var nX = 0
        private set
    var nY = 0
        private set
    private var offsetX = 0f
    private var offsetY = 0f
    private var sizeXCurrent = 0f
    private val ratio get() = sizeXCurrent / measuredWidth// todo: Edit size PaintFrames

    private val paintFillGray = Paint().apply {
        style = Paint.Style.FILL
        color = Color.GRAY
    }

    private var cachePaintPreRect = Paint()
    private var cachePreRect = PreRect(0, 0, 0, 0)

    private val preRect = PreRect(0, 0, 0, 0)
    private val testPreRect = PreRect(0, 0, 0, 0)

    private val rects: MutableList<Rect> = ArrayList()

    private val players: Array<Player> = arrayOf(Player(0, ""), Player(1, ""))
    private var gameMode = 0
    private var touchCounter = 0

    private var startX = 0f
    private var startY = 0f
    private var preX = arrayOf(0f, 0f)
    private var preY = arrayOf(0f, 0f)
    private var preHypDistance = 0

    private var currentPlayerId = 0
    private var firstDice = 0
    private var secondDice = 0

    // Индекс следующего игрока
    private val nextPlayer = iterator {
        var i = 0

        while (true) {
            yield(i)
            i++

            if (i >= players.size || rects.size == players.size) i = 0
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initMapSize()
    }

    fun setIActivity(view: MainView) {
        this.view = view
    }

    fun createNewMap(gameMode: Int, firstPlayerName: String = "Игрок 1", secondPlayerName: String = "Игрок 2") {
        this.gameMode = gameMode
        players[0].name = firstPlayerName
        players[1].name = secondPlayerName

        recreateMap()
    }

    fun recreateMap() {
        fun addStartRects() {
            rects.add(Rect(1, 1, 1, 3, 3))
            rects.add(Rect(0, nX - 3, nY - 3, nX - 1, nY - 1))
        }

        clearMap()
        addStartRects()
        nextMove()
    }

    fun clearMap() {
        rects.clear()
        currentPlayerId = 0
        view.titleToolbar = " "
        initMapSize()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.drawColor(Color.WHITE)

        drawGrid(canvas)
        drawRects(canvas)
        drawPreRect(canvas)
    }

    private fun initMapSize() {
        nX = measuredWidth / cell
        nY = measuredHeight / cell
        offsetX = (cell + measuredWidth - (nX) * cell) / 8f
        offsetY = (cell + measuredHeight - (nY) * cell) / 8f
        sizeXCurrent = measuredWidth.toFloat()
    }

    private fun drawGrid(canvas: Canvas) {
        // Вертикаль
        for (i in 1 until nX) {
            canvas.drawLineWithOffset(
                i * cell, cell, i * cell, cell * nY - cell,
                paintFillGray
            )
        }

        // Горизонталь
        for (i in 1 until nY) {
            canvas.drawLineWithOffset(
                cell, i * cell, cell * nX - cell, i * cell,
                paintFillGray
            )
        }
    }

    private fun drawRects(canvas: Canvas) {
        fun drawRect(rect: Rect, paint: Paint) {
            canvas.drawRectWithOffset(
                rect.left * cell, rect.top * cell, rect.right * cell, rect.bottom * cell,
                paint
            )
        }

        // Рисуем рамки после, т.к. имеется наложение фигур друг на друга
        rects.forEach { drawRect(it, it.paint) }
        rects.filter { it.playerId == currentPlayerId }.forEach { drawRect(it, it.paintFrame) }
    }

    private fun drawPreRect(canvas: Canvas) {
        // Кэширование пре ректа для исключения лишних проверок корректного ввода пре ректа (оптимизация)
        val paintPreRect =
            if (preRect.equals(cachePreRect)) {
                cachePaintPreRect
            } else {
                val newPaint = preRect.getPaint(preRectIsCorrect(preRect))
                cachePaintPreRect = newPaint
                preRect.copyPreRect(cachePreRect)
                newPaint
            }

        if (!preRect.rectIsNull) {
            canvas.drawRectWithOffset(
                preRect.left * cell, preRect.top * cell,
                preRect.right * cell, preRect.bottom * cell,
                paintPreRect
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        fun swiping(event: MotionEvent) {
            if (preX[0] != 0f && preX[1] != 0f && preY[0] != 0f && preY[1] != 0f) {
                offsetX += ((event.getX(0) - preX[0]) / ratio)
                offsetY += ((event.getY(0) - preY[0]) / ratio)
            }

            for (i in 0..1) {
                preX[i] = event.getX(i)
                preY[i] = event.getY(i)
            }
        }

        fun resizing(event: MotionEvent) {
            val distanceX = abs(event.getX(0) - event.getX(1)).toDouble()
            val distanceY = abs(event.getY(0) - event.getY(1)).toDouble()
            val hypDistance = sqrt(Math.pow(distanceX, 2.0) + Math.pow(distanceY, 2.0)).toInt()

            if (preHypDistance != 0) {
                sizeXCurrent += (hypDistance - preHypDistance) * ratio
            }

            preHypDistance = hypDistance
        }

        fun setValuesOfTouch(event: MotionEvent) {
            startX = event.x
            startY = event.y

            for (i in 0..1) {
                preX[i] = 0f
                preY[i] = 0f
            }

            preHypDistance = 0
        }

        val actionMask = event.actionMasked

        when (actionMask) {
            MotionEvent.ACTION_DOWN -> {
                touchCounter++
                setValuesOfTouch(event)
            }
            MotionEvent.ACTION_POINTER_DOWN -> touchCounter++
            MotionEvent.ACTION_MOVE -> {
                if (touchCounter == 2) {
                    preRect.clear()
                    //swiping(event)
                    //resizing(event)
                } else fillPreRect(
                    posInMapToValueInArray(startX, true), posInMapToValueInArray(startY, false),
                    posInMapToValueInArray(event.x, true), posInMapToValueInArray(event.y, false)
                )
            }
            MotionEvent.ACTION_UP -> {
                touchCounter--
                fillRect()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                touchCounter--
                preHypDistance = 0
            }
        }

        return true
    }

    private fun fillPreRect(startX: Int, startY: Int, endX: Int, endY: Int) {
        val boundedEnd = preRect.bounding(firstDice, secondDice, startX, startY, endX, endY)

        val newStartX = min(startX, boundedEnd[0])
        val newEndX = max(startX, boundedEnd[0])
        val newStartY = min(startY, boundedEnd[1])
        val newEndY = max(startY, boundedEnd[1])

        preRect.left = newStartX
        preRect.top = newStartY
        preRect.right = newEndX
        preRect.bottom = newEndY
    }

    private fun fillRect() {
        if (preRectIsCorrect(preRect)) {
            rects.add(Rect(currentPlayerId, preRect))
            nextMove()
        }

        preRect.clear()
    }

    // Определение точки касания на экране в позицию в игровом поле
    private fun posInMapToValueInArray(position: Float, isX: Boolean) = when {
        // Выход за левую и верхную границу
        position < (cell + offsetX) * ratio -> 1
        // Выход за правую границу
        isX && position > ((cell * nX + offsetX) * ratio) - ((cell) * ratio) -> nX - 1
        // Выход за нижнюю границу
        !isX && position > ((cell * nY + offsetY) * ratio) - ((cell) * ratio) -> nY - 1
        // Определение Х в пределах поля
        isX -> ((position + (cell / 2 - offsetX) * ratio) / (cell * ratio)).toInt()
        // Определение Y в пределах поля
        else -> ((position + (cell / 2 - offsetY) * ratio) / (cell * ratio)).toInt()
    }

    private fun nextMove() {
        // Проверка на невозможность ходить
        fun checkInabilityToWalk(): Boolean {
            fun fillTestPreRect(i: Int, j: Int) {
                testPreRect.left = i
                testPreRect.top = j
                testPreRect.right = i + firstDice
                testPreRect.bottom = j + secondDice
            }

            for (i in 1..nX - 2) {
                for (j in 1..nY - 2) {
                    fillTestPreRect(i, j)
                    if (preRectIsCorrect(testPreRect)) {
                        return false
                    }

                    fillTestPreRect(i, j)
                    if (preRectIsCorrect(testPreRect)) {
                        return false
                    }
                }
            }

            return true
        }

        // Проверка на возможность ходить кубом 1х1
        fun checkAbilityToWalk(): Boolean {
            for (i in 1..nX - 2) {
                for (j in 1..nY - 2) {
                    fillTestPreRectByUnit(i, j)

                    if (preRectIsCorrect(testPreRect)) {
                        return true
                    }
                }
            }

            return false
        }

        fun getRandDice() = 1 + (Math.random() * 6).toInt()

        currentPlayerId = nextPlayer.next()
        firstDice = getRandDice()
        secondDice = getRandDice()

        scope.launch(Main) {
            view.titleToolbar = "Ходит ${players[currentPlayerId].name}. Кости: $firstDice, $secondDice"
        }

        if (checkInabilityToWalk()) {
            when (gameMode) {
                0 -> view.createAlertDialogEndGame(findResults())
                1 -> {
                    if (checkAbilityToWalk()) {
                        scope.launch {
                            delay(100)
                            nextMove()
                        }
                    } else {
                        //currentPlayerId = nextPlayer.next(); fillMapByUnit()
                        scope.launch(Main) {
                            view.createAlertDialogEndGame(findResults())
                        }
                    }
                }
            }
        }
    }

    // Заполняем пустое оставшееся пространство единицами
    private fun fillMapByUnit() {
        for (i in 1..nX - 2) {
            for (j in 1..nY - 2) {
                fillTestPreRectByUnit(i, j)

                if (preRectIsCorrect(testPreRect)) {
                    rects.add(Rect(currentPlayerId, testPreRect))
                }
            }
        }
    }

    // Создаём квадрат 1х1
    private fun fillTestPreRectByUnit(i: Int, j: Int) {
        testPreRect.left = i
        testPreRect.top = j
        testPreRect.right = i + 1
        testPreRect.bottom = j + 1
    }

    fun findResults(isSurrender: Boolean = false): String {
        // Поиск суммарной площади игроков
        val area = arrayOf(0, 0)
        rects.forEach { area[it.playerId] += it.area }
        if (area[0] == area[1]) return context.getString(R.string.draw)
        val maxArea = max(area[0], area[1])
        val minArea = min(area[0], area[1])

        val nextPlayerId = nextPlayer.next()

        if (isSurrender) {
            return "${players[currentPlayerId].name} сдаётся, \nпобеждает ${players[nextPlayerId].name} \nсо счётом ${area[nextPlayerId]} : ${area[currentPlayerId]}"
        }

        return when (gameMode) {
            0 -> "${players[currentPlayerId].name} не может ходить, \nпобеждает ${players[nextPlayerId].name} \nсо счётом ${area[currentPlayerId]} : ${area[nextPlayerId]}"
            1 -> "Побеждает ${if (maxArea == area[0]) players[0].name else players[1].name} \nсо счётом $maxArea : $minArea"
            else -> {
                "Exception!. This game mode does not exist".log()
                "<End game>"
            }
        }
    }

    private fun preRectIsCorrect(preRect: PreRect) = preRect.rectIsCorrect(rects, currentPlayerId, nX, nY)

    private fun Canvas.drawLineWithOffset(startX: Int, startY: Int, stopX: Int, stopY: Int, paint: Paint) =
        this.drawLine(
            (startX + offsetX) * ratio, (startY + offsetY) * ratio,
            (stopX + offsetX) * ratio, (stopY + offsetY) * ratio, paint
        )

    private fun Canvas.drawRectWithOffset(left: Int, top: Int, right: Int, bottom: Int, paint: Paint) =
        this.drawRect(
            (left + offsetX) * ratio, (top + offsetY) * ratio,
            (right + offsetX) * ratio, (bottom + offsetY) * ratio, paint
        )

}