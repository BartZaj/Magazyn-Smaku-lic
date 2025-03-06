package com.example.myapplication

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class RecipeProductsAdapter(
    private val produktyList: MutableList<Map<String, Any>>,
    private val onItemClick: (String, String, String) -> Unit
) : RecyclerView.Adapter<RecipeProductsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_produkt_przepisu, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val produkt = produktyList[position]

        val nazwa = produkt["nazwa"] as? String ?: "Nieznany"
        val iloscPrzepis = produkt["iloscPrzepis"] as? Double ?: 0.0
        val iloscOgolna = produkt["iloscOgolna"] as? Double ?: 0.0
        val jednostka = produkt["jednostka"] as? String ?: "szt."
        val idProduktu = produkt["id"] as? String ?: ""
        val idKategorii = produkt["idKategorii"] as? String ?: ""

        holder.nazwaTextView.text = nazwa

        val context = holder.itemView.context
        val defaultBlue = ContextCompat.getColor(context, R.color.blue)

        if (iloscPrzepis > iloscOgolna) {
            holder.nazwaTextView.setTextColor(Color.RED)
            holder.iloscTextView.setTextColor(Color.RED)
            holder.iloscOgolnaTextView.setTextColor(Color.RED)
        } else {
            holder.nazwaTextView.setTextColor(defaultBlue)
            holder.iloscTextView.setTextColor(defaultBlue)
            holder.iloscOgolnaTextView.setTextColor(defaultBlue)
        }

        holder.iloscTextView.text = "Ilość w przepisie: $iloscPrzepis $jednostka"
        holder.iloscOgolnaTextView.text = "Dostępna ilość: $iloscOgolna $jednostka"

        holder.itemView.setOnClickListener {
                onItemClick(idProduktu,nazwa, idKategorii)
        }
    }

    override fun getItemCount(): Int = produktyList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nazwaTextView: TextView = itemView.findViewById(R.id.nazwaTextView)
        val iloscTextView: TextView = itemView.findViewById(R.id.iloscTextView)
        val iloscOgolnaTextView: TextView = itemView.findViewById(R.id.iloscOgolnaTextView)
    }
}