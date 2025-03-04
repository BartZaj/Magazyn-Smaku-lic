package com.example.myapplication

import android.media.Image
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecipeAdapter(
    private val przepisy: MutableList<Map<String, String>>,
    private val onClick: (Map<String, String>) -> Unit,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nazwaTextView: TextView = itemView.findViewById(R.id.nazwaPrzepisuTextView)
        val usunButton: ImageButton = itemView.findViewById(R.id.usunPrzepisButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_przepis, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val przepis = przepisy[position]
        val nazwa = przepis["nazwa"] ?: "Nieznany"
        val id = przepis["id"] ?: return

        holder.nazwaTextView.text = nazwa
        holder.itemView.setOnClickListener {
            onClick(przepis)
        }

        holder.usunButton.setOnClickListener {
            onDelete(id)
        }
    }

    override fun getItemCount() = przepisy.size
}