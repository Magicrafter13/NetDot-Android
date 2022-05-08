package net.matthewrease.netdot.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View

class PlayerSquare(context: Context, attrs: AttributeSet?): View(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = measuredWidth
        val height = measuredHeight
        val edge: Int = if (width > height) height else width
        setMeasuredDimension(edge, edge)
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val edge: Int = if (w > h) h else w
        minimumWidth = edge
        minimumHeight = edge
        /*layoutParams.width = edge
        layoutParams.height = edge*/
    }
}
