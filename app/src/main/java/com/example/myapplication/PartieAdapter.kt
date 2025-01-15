package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PartieAdapter(
    private val partieList: List<Pair<String, Pair<Int, String>>>, // Lista partii (ID, (waga, dataWaznosci))
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<PartieAdapter.PartieViewHolder>() {

    class PartieViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val batchWeightTextView: TextView = view.findViewById(R.id.batchWeightTextView)
        val batchExpiryDateTextView: TextView = view.findViewById(R.id.batchExpiryDateTextView)
        val deleteButton: Button = view.findViewById(R.id.deleteBatchButton)
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
        holder.deleteButton.setOnClickListener {
            onDeleteClick(batchId)
        }
    }

    override fun getItemCount(): Int = partieList.size
}