package com.example.mybudget

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("budget", MODE_PRIVATE) }
    private val items = mutableListOf<JSONObject>()
    private lateinit var balance: TextView
    private lateinit var history: LinearLayout
    private var cashIn = true
    private val money = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val saved = prefs.getString("items", "[]") ?: "[]"
        val arr = JSONArray(saved)
        for (i in 0 until arr.length()) items.add(arr.getJSONObject(i))
        val page = LinearLayout(this).apply { orientation = 1; setPadding(24, 32, 24, 24); setBackgroundColor(Color.rgb(247,248,250)) }
        page.addView(TextView(this).apply { text = "MY BUDGET"; textSize = 20f; setTextColor(Color.DKGRAY) })
        balance = TextView(this).apply { textSize = 32f; setTextColor(Color.BLACK); setPadding(0,18,0,22) }
        page.addView(balance)
        val buttons = LinearLayout(this).apply { orientation = 0 }
        buttons.addView(Button(this).apply { text = "CASH IN"; setTextColor(Color.WHITE); setBackgroundColor(Color.rgb(25,135,84)); setOnClickListener { showEntry(true) } }, LinearLayout.LayoutParams(0, -2, 1f))
        buttons.addView(Button(this).apply { text = "CASH OUT"; setTextColor(Color.WHITE); setBackgroundColor(Color.rgb(220,53,69)); setOnClickListener { showEntry(false) } }, LinearLayout.LayoutParams(0, -2, 1f))
        page.addView(buttons)
        page.addView(TextView(this).apply { text = "RECENT TRANSACTIONS"; textSize = 14f; setPadding(0,24,0,8); setTextColor(Color.DKGRAY) })
        history = LinearLayout(this).apply { orientation = 1 }
        page.addView(history)
        setContentView(ScrollView(this).apply { addView(page) })
        render()
    }

    private fun showEntry(income: Boolean) {
        cashIn = income
        val box = LinearLayout(this).apply { orientation = 1; setPadding(24,12,24,12) }
        val amount = EditText(this).apply { hint = "Amount"; inputType = 8194 }
        val note = EditText(this).apply { hint = "Note (optional)" }
        box.addView(amount); box.addView(note)
        AlertDialog.Builder(this).setTitle(if (income) "CASH IN" else "CASH OUT").setView(box)
            .setPositiveButton("SAVE") { _, _ ->
                val value = amount.text.toString().toDoubleOrNull()
                if (value != null && value > 0) {
                    items.add(JSONObject().put("amount", value).put("income", cashIn).put("note", note.text.toString().ifBlank { if (cashIn) "Cash In" else "Cash Out" }).put("date", java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(java.util.Date())))
                    val arr = JSONArray(); items.forEach { arr.put(it) }
                    prefs.edit().putString("items", arr.toString()).apply()
                    render()
                } else Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("CANCEL", null).show()
    }

    private fun render() {
        val total = items.sumOf { if (it.optBoolean("income")) it.optDouble("amount") else -it.optDouble("amount") }
        balance.text = money.format(total)
        history.removeAllViews()
        if (items.isEmpty()) history.addView(TextView(this).apply { text = "No transactions yet"; setTextColor(Color.GRAY) })
        items.asReversed().forEach { item ->
            val income = item.optBoolean("income")
            history.addView(TextView(this).apply {
                text = "${item.optString("note")}  •  ${item.optString("date")}\n${if (income) "+" else "−"}${money.format(item.optDouble("amount"))}"
                textSize = 15f; setTextColor(if (income) Color.rgb(25,135,84) else Color.rgb(220,53,69)); setPadding(0,12,0,12)
            })
        }
    }
}
