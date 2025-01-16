package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProduktyPrzepisuAdapter(private val produktyList: MutableList<Map<String, Any>>) : RecyclerView.Adapter<ProduktyPrzepisuAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_produkt_przepisu, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val produkt = produktyList[position]

        val nazwa = produkt["nazwa"] as? String ?: "Nieznany"
        val iloscPrzepis = produkt["iloscPrzepis"] as? Int ?: 0
        val iloscOgolna = produkt["iloscOgolna"] as? Int ?: 0

        holder.nazwaTextView.text = nazwa
        holder.iloscTextView.text = "Ilość w przepisie: $iloscPrzepis"
        holder.iloscOgolnaTextView.text = "Dostępna ilość: $iloscOgolna"
    }

    override fun getItemCount(): Int = produktyList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nazwaTextView: TextView = itemView.findViewById(R.id.nazwaTextView)
        val iloscTextView: TextView = itemView.findViewById(R.id.iloscTextView)
        val iloscOgolnaTextView: TextView = itemView.findViewById(R.id.iloscOgolnaTextView)
    }
}