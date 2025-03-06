package com.example.myapplication

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private val listaKategorii: List<Pair<String, String>>, // Lista: ID kategorii, Nazwa kategorii
    private val onItemClick: (idKategorii: String, nazwaKategorii: String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(val view: android.view.View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): CategoryViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val (idKategorii, nazwaKategorii) = listaKategorii[position]

        holder.view.findViewById<TextView>(R.id.nazwaKategoriiTextView).text = nazwaKategorii
        holder.view.setOnClickListener {
            onItemClick(idKategorii, nazwaKategorii)
        }
    }

    override fun getItemCount(): Int = listaKategorii.size
}