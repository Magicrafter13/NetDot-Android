package net.matthewrease.netdot.live

import androidx.lifecycle.LiveData
import net.matthewrease.netdot.grid.GridPoint
import net.matthewrease.netdot.shapes.Dot

/**
 * Observable map between indexes and player colors for drawing by the UI.
 * If 8x8 grid, then map[0] is the horizontal line of the dot at 0,0
 * but map[64] is the vertical line of the dot at 0,0.
 */
class LiveDotMap: LiveData<HashMap<GridPoint, Dot>>(HashMap()) {
    override fun getValue(): HashMap<GridPoint, Dot> = super.getValue()!!
    public override fun postValue(value: HashMap<GridPoint, Dot>?) = super.postValue(value)
    public override fun setValue(value: HashMap<GridPoint, Dot>) = super.setValue(value)
}
