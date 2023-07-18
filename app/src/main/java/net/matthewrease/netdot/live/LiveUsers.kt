package net.matthewrease.netdot.live

import androidx.lifecycle.LiveData
import net.matthewrease.netdot.data.User

/**
 * Observable map between player IDs and their data.
 */
class LiveUsers: LiveData<HashMap<Int, User>>(HashMap()) {
    override fun getValue(): HashMap<Int, User> = super.getValue()!!
    public override fun postValue(value: HashMap<Int, User>?) = super.postValue(value)
    public override fun setValue(value: HashMap<Int, User>) = super.setValue(value)
}
