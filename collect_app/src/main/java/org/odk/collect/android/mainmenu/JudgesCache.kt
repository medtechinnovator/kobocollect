package org.odk.collect.android.mainmenu

/**
 * In-memory cache for the judges list. Populated when the main menu loads so the judge picker
 * can show the list immediately when opened (no loading spinner).
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
