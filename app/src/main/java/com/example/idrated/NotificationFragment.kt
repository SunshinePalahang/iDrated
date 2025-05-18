import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.idrated.HydrationReminderWorker.NotificationEntry
import com.example.idrated.NotificationAdapter
import com.example.idrated.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class NotificationFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoNotifications: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvNotifications)
        tvNoNotifications = view.findViewById(R.id.tvNoNotifications)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val notifications = loadNotifications()
        if (notifications.isEmpty()) {
            tvNoNotifications.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvNoNotifications.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            recyclerView.adapter = NotificationAdapter(notifications)
        }
    }

    private fun loadNotifications(): List<NotificationEntry> {
        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("notificationHistory", "[]")
        val gson = Gson()
        val type = object : TypeToken<List<NotificationEntry>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}
