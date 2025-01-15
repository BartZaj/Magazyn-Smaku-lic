package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProduktyPrzepisuAdapter(
    private val produkty: List<Map<String, Any>>
) : RecyclerView.Adapter<ProduktyPrzepisuAdapter.ProduktViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProduktViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_produkt_przepisu, parent, false)
        return ProduktViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProduktViewHolder, position: Int) {
        val produkt = produkty[position]
        val nazwa = produkt["nazwa"] as? String ?: "Nieznany"
        val iloscPrzepis = produkt["iloscPrzepis"] as? Int ?: 0
        val iloscOgolna = produkt["iloscOgolna"] as? Int ?: 0

        holder.nazwaTextView.text = nazwa
        holder.iloscTextView.text = "$iloscPrzepis / $iloscOgolna"
    }

    override fun getItemCount() = produkty.size

    class ProduktViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nazwaTextView: TextView = itemView.findViewById(R.id.nazwaProduktuTextView)
        val iloscTextView: TextView = itemView.findViewById(R.id.iloscProduktuTextView)
    }
}