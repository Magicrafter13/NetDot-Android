package net.matthewrease.netdot.live

import androidx.lifecycle.LiveData

class LiveBoolean(value: Boolean): LiveData<Boolean>(value) {
    override fun getValue(): Boolean = super.getValue()!!
    public override fun postValue(value: Boolean) = super.postValue(value)
    public override fun setValue(value: Boolean) = super.setValue(value)
}
