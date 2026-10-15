package app.chummy

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

enum class Cadence(val label: String, val days: Int, val color: Long) {
    DAILY("daily", 1, 0xFFE5484D),
    WEEKLY("weekly", 7, 0xFFC93DD6),
    BIWEEKLY("biweekly", 14, 0xFF6E56CF),
    MONTHLY("monthly", 30, 0xFF0B8BE6),
    QUARTERLY("quarterly", 91, 0xFFA07553),
    ANNUAL("annual", 365, 0xFF8B8D98),
}

/** [last] is the epoch day of the most recent catchup. */
data class Friend(val id: Long, val name: String, val cadence: Cadence, val last: Long) {
    val due get() = last + cadence.days
    /** Elapsed time relative to cadence: 1.0 means due today, above 1 is overdue. */
    fun score(today: Long) = (today - last).toDouble() / cadence.days
}

fun today() = LocalDate.now().toEpochDay()

class Store(context: Context) {
    private val prefs = context.getSharedPreferences("friends", Context.MODE_PRIVATE)

    fun load(): List<Friend> = runCatching {
        val arr = JSONArray(prefs.getString("list", "[]"))
        List(arr.length()) {
            val o = arr.getJSONObject(it)
            Friend(o.getLong("id"), o.getString("name"), Cadence.valueOf(o.getString("cadence")), o.getLong("last"))
        }
    }.getOrDefault(emptyList())

    fun save(friends: List<Friend>) {
        val arr = JSONArray()
        friends.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("name", it.name)
                    .put("cadence", it.cadence.name).put("last", it.last)
            )
        }
        prefs.edit().putString("list", arr.toString()).apply()
    }
}
