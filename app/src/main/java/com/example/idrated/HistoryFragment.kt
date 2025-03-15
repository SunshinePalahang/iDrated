package com.example.idrated

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private var historyList = mutableListOf<HistoryItem>()
    private var historyRef: DatabaseReference? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_history, container, false)

        // Initialize RecyclerView
        recyclerView = rootView.findViewById(R.id.recyclerViewHistory)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize HistoryAdapter with the empty list
        adapter = HistoryAdapter(historyList)
        recyclerView.adapter = adapter

        // Get the Firebase reference to the current user's history
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            historyRef = FirebaseDatabase.getInstance().getReference("users/$userId/history")
            fetchHistoryData()
        } else {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        }

        return rootView
    }

    private fun fetchHistoryData() {
        historyRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                historyList.clear()
                for (dataSnapshot in snapshot.children) {
                    val historyItem = dataSnapshot.getValue(HistoryItem::class.java)
                    if (historyItem != null) {
                        historyList.add(historyItem)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load history: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    companion object {
        fun newInstance(): HistoryFragment = HistoryFragment()
    }
}
