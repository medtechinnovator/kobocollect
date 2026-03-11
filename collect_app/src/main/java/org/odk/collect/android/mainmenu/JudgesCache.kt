package org.odk.collect.android.mainmenu

/**
 * In-memory cache for the judges list. Populated when the main menu loads and refreshed every
 * time the judge picker is opened, so new judges added on the spot are visible without restarting the app.
 */
object JudgesCache {
    @Volatile
    private var judges: List<Judge>? = null

    fun get(): List<Judge>? = judges

    fun set(list: List<Judge>) {
        judges = list
    }

    fun clear() {
        judges = null
    }
}
