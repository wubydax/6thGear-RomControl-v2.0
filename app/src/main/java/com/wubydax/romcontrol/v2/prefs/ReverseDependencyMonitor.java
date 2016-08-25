package com.wubydax.romcontrol.v2.prefs;

import android.preference.Preference;

/**
 * Created by Anna Berkovitch on 18/08/2016.
 */

interface ReverseDependencyMonitor {
    void registerReverseDependencyPreference(Preference preference);
}
