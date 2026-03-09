package org.odk.collect.android.mainmenu

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import org.odk.collect.android.R

/**
 * Filterable adapter for the username dropdown. Shows "FirstName LastName", then cleanName, then email.
 * Sorted alphabetically by display name. Filters by any match in first name, last name, email, or cleanName.
 */
class UserSearchAdapter(
    context: Context,
    initialJudges: List<Judge> = emptyList()
) : ArrayAdapter<Judge>(context, R.layout.dropdown_item_user, initialJudges), Filterable {

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
        view.findViewById<TextView>(R.id.user_dropdown_secondary).text = judge.cleanName
        view.findViewById<TextView>(R.id.user_dropdown_email).text = judge.email
        return view
    }

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val query = constraint?.toString()?.trim()?.lowercase() ?: ""
            val result = if (query.isEmpty()) {
                judges
            } else {
                judges.filter { judge ->
                    judge.firstName.lowercase().contains(query) ||
                        judge.lastName.lowercase().contains(query) ||
                        judge.email.lowercase().contains(query) ||
                        judge.displayName.lowercase().contains(query) ||
                        judge.cleanName.lowercase().contains(query)
                }
            }
            val sorted = result.sortedBy { it.displayName.lowercase() }
            return FilterResults().apply {
                values = sorted
                count = sorted.size
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            filteredJudges = (results?.values as? List<Judge>) ?: emptyList()
            notifyDataSetChanged()
        }
    }

    /** Replaces the backing list (e.g. after loading from API). Sorts alphabetically by display name. */
    fun setJudges(newJudges: List<Judge>) {
        judges = newJudges.sortedBy { it.displayName.lowercase() }
        filteredJudges = judges
        notifyDataSetChanged()
    }
}
