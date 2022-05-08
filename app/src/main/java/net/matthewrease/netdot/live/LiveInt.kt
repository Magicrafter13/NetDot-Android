package net.matthewrease.netdot.live

import androidx.lifecycle.LiveData

class LiveInt(value: Int): LiveData<Int>(value) {
    override fun getValue(): Int = super.getValue()!!
    public override fun postValue(value: Int) = super.postValue(value)
    public override fun setValue(value: Int) = super.setValue(value)
}
