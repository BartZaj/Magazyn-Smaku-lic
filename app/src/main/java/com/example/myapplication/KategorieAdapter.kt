package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class KategorieAdapter(
    private val listaKategorii: List<Pair<String, String>>, // Lista: ID kategorii, Nazwa kategorii
    private val onItemClick: (idKategorii: String, nazwaKategorii: String) -> Unit
) : RecyclerView.Adapter<KategorieAdapter.KategoriaViewHolder>() {

    class KategoriaViewHolder(val view: android.view.View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): KategoriaViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return KategoriaViewHolder(view)
    }

    override fun onBindViewHolder(holder: KategoriaViewHolder, position: Int) {
        val (idKategorii, nazwaKategorii) = listaKategorii[position]

        holder.view.findViewById<TextView>(R.id.nazwaKategoriiTextView).text = nazwaKategorii
        holder.view.setOnClickListener {
            onItemClick(idKategorii, nazwaKategorii)
        }
    }

    override fun getItemCount(): Int = listaKategorii.size
}