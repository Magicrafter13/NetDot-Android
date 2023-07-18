package net.matthewrease.netdot.live

import android.net.nsd.NsdServiceInfo
import androidx.lifecycle.LiveData

class LiveNsd(value: NsdServiceInfo): LiveData<NsdServiceInfo>(value) {
    override fun getValue(): NsdServiceInfo = super.getValue()!!
    public override fun postValue(value: NsdServiceInfo) = super.postValue(value)
    public override fun setValue(value: NsdServiceInfo) = super.setValue(value)
}
