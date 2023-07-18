package net.matthewrease.netdot

import android.content.Context
import android.content.res.Configuration
//import android.graphics.Canvas
import android.graphics.Color
//import android.graphics.Paint
//import android.graphics.Rect
//import android.os.Build
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.TextView
import net.matthewrease.netdot.data.User
import net.matthewrease.netdot.ui.PlayerSquare
import kotlin.math.ceil
//import kotlin.math.min

class PlayersView(context: Context, attrs: AttributeSet?): ViewGroup(context, attrs) {
    private companion object {
        //const val DEFAULT_FONT_SIZE: Float = 40f
        const val MAX_PLAYERS_PER_ROW_PORTRAIT: Int = 3
        const val MAX_PLAYERS_PER_ROW_LANDSCAPE: Int = 5
        const val PADDING: Int = 8
    }

    private data class PlayerInfo(
        var index: Int,
        var name: String,
        var color: Int = Color.BLACK,
        val label: TextView,
        val view: PlayerSquare,
        var new: Boolean = true,
        var stale: Boolean = false
    )

    private val portrait: Boolean = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    //private val bounds = Rect()
    //private val defTextPaint: Paint = Paint()
    //private val playerTextPaint = HashMap<Int, Paint>()
    private val userData = HashMap<Int, PlayerInfo>()

    private var rows: Int = 0
    private var columns: Int = 0
    private var playersPerRow: Int = 0
    private var boxLength: Int = 0
    private var textParams = LayoutParams(0, 0)
    private var squareParams = LayoutParams(0, 0)

    private fun recalculate(w: Int) {
        if (userData.size == 0) {
            rows = 0
            columns = 0
            playersPerRow = 0
        }
        else {
            rows = (userData.size - 1) / (if (portrait) MAX_PLAYERS_PER_ROW_PORTRAIT else MAX_PLAYERS_PER_ROW_LANDSCAPE) + 1
            columns = (userData.size - 1) / rows + 1
            playersPerRow = ceil(userData.size.toFloat() / rows).toInt()
            boxLength = w / playersPerRow
            textParams.width = boxLength
            textParams.height = (boxLength * 0.25f).toInt()
            squareParams.width = boxLength
            squareParams.height = boxLength
        }
    }

    /*override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        with (canvas) {
            playerData.forEach { (_, data) ->
                val column = data.index % playersPerRow
                val row = data.index / playersPerRow
                drawText(
                    data.name,
                    column * boxLength + boxLength / 2.0f,
                    row * (boxLength * 1.25f) + boxLength * 0.25f,
                    defTextPaint
                )
            }
        }
    }*/

    override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
        // Remove old views
        userData
            .filter { (_, data) -> data.stale }
            .forEach { (id, data) ->
                removeViewInLayout(data.label)
                removeViewInLayout(data.view)
                //removeDetachedView(data.view, false)
                userData.remove(id)
            }

        // Update grid layout
        recalculate(p3 - p1)

        // Reposition all views
        userData.forEach { (_, data) ->
            //val column: Int = if (portrait) index % playersPerRow else index / playersPerColumn
            //val row: Int = if (portrait) index / playersPerRow else index % playersPerColumn
            /*with (playerTextPaint[id]) {
                if (this != null) {
                    getTextBounds(data.name, 0, data.name.length, bounds)
                    textSize = DEFAULT_FONT_SIZE * min((boxLength - PADDING * 2) / bounds.width().toFloat(), boxLength * 0.25f / bounds.height())
                }
            }*/
            // Add new views
            if (data.new) {
                data.new = false
                addViewInLayout(data.label, -1, textParams, false)
                addViewInLayout(data.view, -1, squareParams, true)
            }
            // Get grid location
            val column = data.index % playersPerRow
            val row = data.index / playersPerRow
            // Set properties for name label
            with (data.label) {
                println("TEXT VIEW FOR ${data.index} HAS $text BUT SHOULD HAVE ${data.name}")
                //text = data.name
                //maxLines = 1
                left = column * boxLength + PADDING
                right = (column + 1) * boxLength - PADDING
                top = (row * boxLength * 1.25f + PADDING).toInt()
                bottom = (row * boxLength * 1.25f + boxLength * 0.25f - PADDING).toInt()
                /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM)
                }*/
                /*paint.getTextBounds(data.name, 0, data.name.length, bounds)
                textSize = textSize * min((this.width).toFloat() / bounds.width(), (this.height).toFloat() / bounds.height())*/
            }
            // Set properties for color box
            with (data.view) {
                //setBackgroundColor(data.color)
                left = column * boxLength + PADDING
                right = (column + 1) * boxLength - PADDING
                top = (row * boxLength * 1.25f + boxLength * 0.25f + PADDING).toInt()
                bottom = ((row + 1) * boxLength * 1.25f - PADDING).toInt()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        setMeasuredDimension(
            resolveSize(columns * boxLength, widthMeasureSpec),
            resolveSize((rows * boxLength * 1.25f).toInt(), heightMeasureSpec)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalculate(w)
    }

    fun updateUsers(users: HashMap<Int, User>) {
        println("INFO PLAYERS UPDATED ${users.size} ${users[0]?.color}")
        // Mark old entries for removal
        val old = userData.filterKeys { !users.containsKey(it) }
        old .forEach { it.value.stale = true }
        // Update existing entries
        userData.forEach { (id, data) ->
            val player = users[id]
            if (player != null) {
                data.name = player.name
                data.color = player.color
                data.label.text = player.name
                data.view.setBackgroundColor(player.color)
            }
        }
        // Add new entries
        val new = users.filterKeys { !userData.containsKey(it) }
        new.forEach { (id, user) ->
            userData[id] = PlayerInfo(
                -1,
                user.name,
                user.color,
                TextView(context),
                PlayerSquare(context, null)
            ).also {
                it.label.maxLines = 1
                it.label.text = user.name
                it.view.setBackgroundColor(user.color)
            }
        }
        // Update indices
        userData
            .filter { (_, data) -> !data.stale }
            .toSortedMap()
            .values
            .withIndex()
            .forEach { (index, data) -> data.index = index }
        // Update layout if number of players has changed
        if (old.isNotEmpty() || new.isNotEmpty())
            requestLayout()
    }

    init {
        // FOR DEBUG ONLY
        val debugView = 16
        if (debugView > 0) {
            val map = HashMap<Int, User>()
            for (i in 0 until debugView)
                map[i] = User().also {
                    with (it) {
                        color = Color.HSVToColor(floatArrayOf(i * 360.0f / debugView, 0.85f, 0.90f))
                        name = if (i == 0) "Server" else "Client $i"
                    }
                }
                /*map[i] = PlayerInfo(
                    i,
                    if (i == 0) "Server" else "Client $i",
                    Color.HSVToColor(floatArrayOf(i * 360.0f / debugView, 0.85f, 0.90f)),
                    TextView(context),
                    PlayerSquare(context, null)
                )*/
            updateUsers(map)
            //requestLayout()
        }

        /*val colors = context.theme.obtainStyledAttributes(R.style.Theme_NetDot, intArrayOf(
            R.attr.gameText
        ))

        // Hardcoded text paint settings and bounds
        with (defTextPaint) {
            color = colors.getColor(0, Color.MAGENTA)
            style = Paint.Style.FILL_AND_STROKE
            textAlign = Paint.Align.CENTER
            textSize = DEFAULT_FONT_SIZE
        }

        colors.recycle()*/
    }
}
