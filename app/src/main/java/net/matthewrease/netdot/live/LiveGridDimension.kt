package net.matthewrease.netdot.live

import androidx.lifecycle.LiveData
import net.matthewrease.netdot.grid.Grid

class LiveGridDimension(width: Int, height: Int): LiveData<Grid.Dimension>(Grid.Dimension(width, height)) {
    override fun getValue(): Grid.Dimension = super.getValue()!!
    public override fun postValue(value: Grid.Dimension) = super.postValue(value)
    public override fun setValue(value: Grid.Dimension) = super.setValue(value)
}
