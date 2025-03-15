package com.example.idrated

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(private val historyList: List<HistoryItem>) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    // Step 2: Create the ViewHolder class
    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTimeTextView: TextView = itemView.findViewById(R.id.dateTimeTextView)
        val waterIntakeTextView: TextView = itemView.findViewById(R.id.waterIntakeTextView)
    }

    // Step 3: Inflate the item layout and create ViewHolder instances
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    // Step 4: Bind data to the ViewHolder
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val historyItem = historyList[position]
        holder.dateTimeTextView.text = historyItem.dateTime
        holder.waterIntakeTextView.text = "${historyItem.waterIntake} mL"
    }

    // Step 5: Return the total number of items
    override fun getItemCount(): Int = historyList.size
}
