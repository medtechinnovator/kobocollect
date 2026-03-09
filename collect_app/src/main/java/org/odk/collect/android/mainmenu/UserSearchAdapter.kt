package org.odk.collect.android.mainmenu

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import org.odk.collect.android.R

/**
 * Adapter for the username dropdown. Shows "FirstName LastName", then cleanName, then email.
 * Filtering is client-side only: refilter on each keystroke by firstName, lastName, or cleanName.
 */
class UserSearchAdapter(
    context: Context,
    initialJudges: List<Judge> = emptyList()
) : ArrayAdapter<Judge>(context, R.layout.dropdown_item_user, initialJudges) {

    private var judges: List<Judge> = initialJudges
    private var filteredJudges: List<Judge> = initialJudges
    private val inflater = LayoutInflater.from(context)

    override fun getCount(): Int = filteredJudges.size

    override fun getItem(position: Int): Judge = filteredJudges[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.dropdown_item_user, parent, false)
        val judge = getItem(position)
        view.findViewById<TextView>(R.id.user_dropdown_name).text = judge.displayName
        view.findViewById<TextView>(R.id.user_dropdown_secondary).text = judge.safeCleanName
        view.findViewById<TextView>(R.id.user_dropdown_email).text = judge.safeEmail
        return view
    }

    /**
     * Refilter the list on each keystroke. Match on first name, last name, or clean name (case-insensitive).
     * Call from the username field's TextWatcher.
     */
    fun filterBy(query: CharSequence?) {
        val q = query?.toString()?.trim()?.lowercase() ?: ""
        filteredJudges = if (q.isEmpty()) {
            judges
        } else {
            judges.filter { judge ->
                (judge.firstName.orEmpty().lowercase().contains(q)) ||
                    (judge.lastName.orEmpty().lowercase().contains(q)) ||
                    (judge.cleanName.orEmpty().lowercase().contains(q))
            }.sortedBy { it.displayName.lowercase() }
        }
        Log.d(TAG, "Judge filter triggered: query=\"$q\" | fullList=${judges.size} | showing ${filteredJudges.size} judges")
        notifyDataSetChanged()
    }

    /** Replaces the full judges list (e.g. after loading from API). Call filterBy("") after to show all. */
    fun setJudges(newJudges: List<Judge>) {
        judges = newJudges.sortedBy { it.displayName.lowercase() }
        filteredJudges = judges
        Log.d(TAG, "setJudges: ${judges.size} judges loaded (client-side, filter on each keystroke)")
        notifyDataSetChanged()
    }

    private companion object {
        const val TAG = "UserSearchAdapter"
    }
}
