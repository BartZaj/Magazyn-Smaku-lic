package com.example.myapplication

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class PartieAdapter(
    private val partieList: MutableList<Pair<String, Pair<Int, String>>>, // Lista partii (ID, (waga, dataWaznosci))
    private val onDeleteClick: (String) -> Unit,
    private val onEditClick: (String, Int) -> Unit
) : RecyclerView.Adapter<PartieAdapter.PartieViewHolder>() {

    class PartieViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val batchWeightTextView: TextView = view.findViewById(R.id.batchWeightTextView)
        val batchExpiryDateTextView: TextView = view.findViewById(R.id.batchExpiryDateTextView)
        val deleteButton: Button = view.findViewById(R.id.deleteBatchButton)
        val editButton: Button = view.findViewById(R.id.editBatchButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartieViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_partie, parent, false)
        return PartieViewHolder(view)
    }

    override fun onBindViewHolder(holder: PartieViewHolder, position: Int) {
        val (batchId, data) = partieList[position]
        val (weight, expiryDate) = data
        holder.batchWeightTextView.text = "Partia: $weight g"
        holder.batchExpiryDateTextView.text = "Data ważności: $expiryDate"

        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val today = Calendar.getInstance()

        try {
            val expiryDateCalendar = Calendar.getInstance().apply {
                time = dateFormat.parse(expiryDate) ?: Date()
            }

            val daysDifference =
                ((expiryDateCalendar.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()

            when {
                daysDifference < 0 -> { // Po terminie
                    holder.batchWeightTextView.setTextColor(Color.RED)
                    holder.batchExpiryDateTextView.setTextColor(Color.RED)
                }
                daysDifference < 7 -> { // Tydzień lub mniej do końca
                    holder.batchWeightTextView.setTextColor(Color.parseColor("#FFA500")) // Pomarańczowy
                    holder.batchExpiryDateTextView.setTextColor(Color.parseColor("#FFA500")) // Pomarańczowy
                }
            }
        } catch (e: Exception) {
            holder.batchWeightTextView.setTextColor(Color.BLACK)
            holder.batchExpiryDateTextView.setTextColor(Color.BLACK)
        }

        holder.deleteButton.setOnClickListener {
            onDeleteClick(batchId)
        }

        holder.editButton.setOnClickListener {
            onEditClick(batchId, weight)
        }
    }

    override fun getItemCount(): Int = partieList.size
}