package com.example.idrated

import android.os.Bundle
import android.os.Environment
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var historyListView: ListView
    private val historyFileName = "IDrated_history.json"

    data class IntakeRecord(val timestamp: String, val waterAmount: Float)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        historyListView = findViewById(R.id.historyListView)
        loadHistory()
    }

    private fun loadHistory() {
        val filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(filePath, historyFileName)

        if (!file.exists()) return

        val historyJson = file.readText()
        val records = Gson().fromJson(historyJson, Array<IntakeRecord>::class.java).toList()

        val listItems = records.map { "${it.waterAmount} ml\t\t\t\t\t${it.timestamp}" }
        historyListView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listItems)
    }
}
