package net.matthewrease.netdot.live

import androidx.lifecycle.LiveData
import net.matthewrease.netdot.Player

/**
 * Observable map between player IDs and their data.
 */
class LivePlayers: LiveData<HashMap<Int, Player>>(HashMap()) {
    override fun getValue(): HashMap<Int, Player> = super.getValue()!!
    public override fun postValue(value: HashMap<Int, Player>?) = super.postValue(value)
    public override fun setValue(value: HashMap<Int, Player>) = super.setValue(value)
}
