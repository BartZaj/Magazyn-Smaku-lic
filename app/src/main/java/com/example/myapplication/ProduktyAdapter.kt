package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProduktyAdapter(
    private val listaProduktow: List<Pair<String, String>>, // Lista: ID produktu, Nazwa produktu
    private val onItemClick: (idProduktu: String, nazwaProduktu: String) -> Unit
) : RecyclerView.Adapter<ProduktyAdapter.ProduktViewHolder>() {

    class ProduktViewHolder(val view: android.view.View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ProduktViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.element_produktu, parent, false)
        return ProduktViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProduktViewHolder, position: Int) {
        val (idProduktu, nazwaProduktu) = listaProduktow[position]

        holder.view.findViewById<TextView>(R.id.nazwaProduktuTextView).text = nazwaProduktu
        holder.view.setOnClickListener {
            onItemClick(idProduktu, nazwaProduktu)
        }
    }

    override fun getItemCount(): Int = listaProduktow.size
}