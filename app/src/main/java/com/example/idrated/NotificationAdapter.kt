package com.example.idrated

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.idrated.HydrationReminderWorker.NotificationEntry
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter :
    ListAdapter<NotificationEntry, NotificationAdapter.NotificationViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NotificationEntry>() {
            override fun areItemsTheSame(oldItem: NotificationEntry, newItem: NotificationEntry): Boolean {
                return oldItem.timestamp == newItem.timestamp
            }

            override fun areContentsTheSame(oldItem: NotificationEntry, newItem: NotificationEntry): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.notificationMessage)
        private val timestampText: TextView = itemView.findViewById(R.id.notificationTimestamp)
        private val relativeTimeText: TextView = itemView.findViewById(R.id.notificationTime)

        fun bind(notification: NotificationEntry) {
            messageText.text = notification.message
            timestampText.text = formatTimestamp(notification.timestamp)
            relativeTimeText.text = getRelativeTime(notification.timestamp)
        }

        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

        private fun getRelativeTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            return when {
                seconds < 60 -> "Just now"
                minutes < 60 -> "$minutes min${if (minutes != 1L) "s" else ""} ago"
                hours < 24 -> "$hours hr${if (hours != 1L) "s" else ""} ago"
                else -> "$days day${if (days != 1L) "s" else ""} ago"
            }
        }
    }
}

