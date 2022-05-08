package net.matthewrease.netdot.live

import androidx.lifecycle.LiveData

class LiveString(value: String): LiveData<String>(value) {
    override fun getValue(): String = super.getValue()!!
    public override fun postValue(value: String) = super.postValue(value)
    public override fun setValue(value: String) = super.setValue(value)
}
