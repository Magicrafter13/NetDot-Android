package net.matthewrease.netdot

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat
import net.matthewrease.netdot.grid.GridPoint
import net.matthewrease.netdot.shapes.Dot
import kotlin.math.abs
import kotlin.math.floor

class DotGrid(context: Context, attrs: AttributeSet?): View(context, attrs), GestureDetector.OnGestureListener {
    companion object {
        const val PADDING: Int = 20
    }

    private val gestureDetector: GestureDetectorCompat = GestureDetectorCompat(this.context, this)
    private val dotPaint = Paint()
    private val noLinePaint = Paint()
    private val playerLinePaint = HashMap<Int, Paint>()
    private val noBoxPaint = Paint()
    private val playerBoxPaint = HashMap<Int, Paint>()
    private val gridPoint = GridPoint(-1, -1)

    private var safeWidth: Int = 0
    private var safeHeight: Int = 0
    private var boxLength: Int = 0
    private var safeX: Int = 0
    private var safeY: Int = 0
    private var gridWidth: Int = 0
    private var gridHeight: Int = 0
    private var ownedDots: HashMap<GridPoint, Dot> = HashMap() // Key is row * (gridWidth + 1) + column, and value is Color value

    var moveX: Int = -1
    var moveY: Int = -1
    var moveVertical = false

    private fun recalculate(w: Int, h: Int) {
        // Landscape
        if (w > h) {
            safeHeight = height - PADDING * 2
            boxLength = safeHeight / gridHeight
            safeWidth = boxLength * gridWidth
            // If game is too wide for landscape, shrink it to fit
            if (safeWidth > width - PADDING * 2) {
                safeWidth = width - PADDING * 2
                boxLength = safeWidth / gridWidth
                safeHeight = boxLength * gridHeight
                safeX = PADDING
                safeY = height / 2 - safeHeight / 2
            }
            // (Normal)
            else {
                safeX = width / 2 - safeWidth / 2
                safeY = PADDING
            }
        }
        // Portrait
        else {
            safeWidth = width - PADDING * 2
            boxLength = safeWidth / gridWidth
            safeHeight = boxLength * gridHeight
            // If game is too tall for portrait, shrink it to fit
            if (safeHeight > height - PADDING * 2) {
                safeHeight = height - PADDING * 2
                boxLength = safeHeight / gridHeight
                safeWidth = boxLength * gridWidth
                safeX = width / 2 - safeWidth / 2
                safeY = PADDING
            }
            // (Normal)
            else {
                safeX = PADDING
                safeY = height / 2 - safeHeight / 2
            }
        }
        noLinePaint.strokeWidth = boxLength * 0.1f
        playerLinePaint.forEach { (_, paint) -> paint.strokeWidth = noLinePaint.strokeWidth }
    }

    fun reset(dimX: Int, dimY: Int) {
        gridWidth = dimX - 1
        gridHeight = dimY - 1
        ownedDots.clear()
        recalculate(width, height)
        invalidate()
    }

    fun updateDots(dots: HashMap<GridPoint, Dot>) {
        println("DOTS UPDATED ${dots.size}")
        ownedDots = dots
        invalidate()
    }

    fun updatePlayers(players: HashMap<Int, Player>) {
        println("PLAYERS UPDATED ${players.size} ${players[0]?.color}")
        players.forEach { (playerID, player) ->
            playerLinePaint[playerID] = Paint(noLinePaint).also { it.color = player.color }
            playerBoxPaint[playerID] = Paint(noBoxPaint).also { it.color = player.color }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalculate(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        with (canvas) {
            // Draw dots
            for (row in 0..gridHeight) {
                for (column in 0..gridWidth) {
                    drawCircle(
                        (safeX + column * boxLength).toFloat(),
                        (safeY + row * boxLength).toFloat(),
                        boxLength * 0.1f,
                        dotPaint
                    )
                    /*drawLine(
                        (safeX + column * boxLength).toFloat(),
                        (safeY + row * boxLength).toFloat(),
                        (safeX + (column + 1) * boxLength).toFloat(),
                        (safeY + (row + 1) * boxLength).toFloat(),
                        noLinePaint
                    )
                    drawLine(
                        (safeX + (column + 1) * boxLength).toFloat(),
                        (safeY + row * boxLength).toFloat(),
                        (safeX + column * boxLength).toFloat(),
                        (safeY + (row + 1) * boxLength).toFloat(),
                        noLinePaint
                    )*/
                    gridPoint.set(column, row)
                    // Draw horizontal lines
                    if (column < gridWidth) {
                        drawLine(
                            (safeX + column * boxLength).toFloat() + boxLength * 0.2f,
                            (safeY + row * boxLength).toFloat(),
                            (safeX + (column + 1) * boxLength).toFloat() - boxLength * 0.2f,
                            (safeY + row * boxLength).toFloat(),
                            playerLinePaint[ownedDots[gridPoint]?.right?.getOwner()] ?: noLinePaint
                        )
                    }
                    // Draw vertical lines
                    if (row < gridHeight) {
                        drawLine(
                            (safeX + column * boxLength).toFloat(),
                            (safeY + row * boxLength).toFloat() + boxLength * 0.2f,
                            (safeX + column * boxLength).toFloat(),
                            (safeY + (row + 1) * boxLength).toFloat() - boxLength * 0.2f,
                            playerLinePaint[ownedDots[gridPoint]?.down?.getOwner()] ?: noLinePaint
                        )
                    }
                    // Draw box
                    if (column < gridWidth && row < gridHeight) {
                        val owner = ownedDots[gridPoint]?.box?.getOwner()
                        if (owner != null && owner >= 0)
                            drawRect(
                                (safeX + column * boxLength).toFloat() + boxLength * 0.2f,
                                (safeY + row * boxLength).toFloat() + boxLength * 0.2f,
                                (safeX + (column + 1) * boxLength).toFloat() - boxLength * 0.2f,
                                (safeY + (row + 1) * boxLength).toFloat() - boxLength * 0.2f,
                                playerBoxPaint[owner] ?: noBoxPaint
                            )
                    }
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return if (gestureDetector.onTouchEvent(event)) performClick() else super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDown(p0: MotionEvent?): Boolean {
        if (p0 != null) {
            if (p0.x >= safeX - boxLength / 2 && p0.x < safeX + safeWidth + boxLength / 2) {
                if (p0.y >= safeY - boxLength / 2 && p0.y < safeY + safeHeight + boxLength / 2) {
                    val column: Int = floor((p0.x - safeX) / boxLength).toInt()
                    val row: Int = floor((p0.y - safeY) / boxLength).toInt()
                    /*println("Testing tap at ${p0.x},${p0.y} ($column,$row)")
                    println("${p0.x - safeX} -> ${floor(p0.x - safeX)} -> ${floor(p0.x - safeX).toInt()}")
                    println("${floor((p0.x - safeX) / boxLength)} ${floor((p0.x - safeX) / boxLength).toInt()}")*/
                    if (column < -1 || column > gridWidth + 1 || row < -1 || row > gridHeight + 1)
                        return false
                    // Left or Right outer edge
                    if (column == -1 || column == gridWidth) {
                        if (row !in 0 until gridHeight)
                            return false
                        val offX: Int = (if (column == -1) -(p0.x - safeX) else (p0.x - safeX - safeWidth)).toInt()
                        val offY: Int = abs((p0.y - safeY).toInt() % boxLength - boxLength / 2)
                        //println("offsets: $offX,$offY")
                        if (offX + offY >= boxLength / 2)
                            return false
                        //println("TAP AT ${if (column == -1) 0 else gridWidth},$row VER")
                        moveX = if (column == -1) 0 else gridWidth
                        moveY = row
                        moveVertical = true
                        return true
                    }
                    // Top or Bottom outer edge
                    if (row == -1 || row == gridHeight) {
                        if (column !in 0 until gridWidth)
                            return false
                        val offX: Int = abs((p0.x - safeX).toInt() % boxLength - boxLength / 2)
                        val offY: Int = (if (row == -1) -(p0.y - safeY) else (p0.y - safeY - safeHeight)).toInt()
                        if (offX + offY >= boxLength / 2)
                            return false
                        //println("TAP AT $column,${if (row == -1) 0 else gridHeight} HOR")
                        moveX = column
                        moveY = if (row == -1) 0 else gridHeight
                        moveVertical = false
                        return true
                    }
                    // Inside grid
                    //println("\n\nINSIDE GRID\n")
                    moveX = column + 1
                    moveY = row
                    moveVertical = true
                    val offX: Int = ((p0.x - safeX) % boxLength).toInt()
                    val offY: Int = ((p0.y - safeY) % boxLength).toInt()
                    //println("offsets: $offX,$offY")
                    // Up
                    //println("$offY < $boxLength / 2 (${boxLength / 2})")
                    if (offY < boxLength / 2) {
                        if (offY + abs(offX - boxLength / 2) < boxLength / 2) {
                            //println("TAP AT $column,$row HOR")
                            moveX = column
                            moveVertical = false
                            return true
                        }
                    }
                    // Down
                    else {
                        //println("${boxLength - offY} + ${abs(offX - boxLength / 2)} < $boxLength / 2 (${boxLength / 2})")
                        if (boxLength - offY + abs(offX - boxLength / 2) < boxLength / 2) {
                            //println("TAP AT $column,${row + 1} HOR")
                            moveX = column
                            moveY = row + 1
                            moveVertical = false
                            return true
                        }
                    }
                    // Left
                    //println("$offX < $boxLength / 2 (${boxLength / 2})")
                    if (offX < boxLength / 2) {
                        if (offX + abs(offY - boxLength / 2) < boxLength / 2) {
                            //println("TAP AT $column,$row VER")
                            moveX = column
                            return true
                        }
                    }
                    // Right (no check necessary - literally has to be true if the previous 3 weren't
                    /*else {
                        if (boxLength / 2 - offX + abs(offY - boxLength / 2) < boxLength / 2) {*/
                            //println("TAP AT ${column + 1},$row VER")
                            /*return true
                        }
                    }*/
                    return true
                }
            }
        }
        return false
    }

    override fun onShowPress(p0: MotionEvent?) {
        //TODO("Not yet implemented")
    }

    override fun onSingleTapUp(p0: MotionEvent?): Boolean {
        return true
        //TODO("Not yet implemented")
    }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        //TODO("Not yet implemented")
        return false
    }

    override fun onLongPress(p0: MotionEvent?) {
        //TODO("Not yet implemented")
    }

    override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        //TODO("Not yet implemented")
        return false
    }

    init {
        val light = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO
        val colors = context.theme.obtainStyledAttributes(R.style.Theme_NetDot, intArrayOf(
            R.attr.gameBack,
            R.attr.gameDot,
            R.attr.gameLine
        ))

        // Draw background
        setBackgroundColor(colors.getColor(0, Color.MAGENTA)) // gameBack
        with (dotPaint) {
            color = colors.getColor(1, Color.MAGENTA) // gameDot
            style = Paint.Style.FILL
        }
        with (noLinePaint) {
            color = colors.getColor(2, Color.MAGENTA) // gameLine
            strokeWidth = 5f
            style = Paint.Style.STROKE
        }
        with (noBoxPaint) {
            color = Color.MAGENTA
            style = Paint.Style.FILL
        }

        gridWidth = 7
        gridHeight = 7

        colors.recycle()
    }
}
