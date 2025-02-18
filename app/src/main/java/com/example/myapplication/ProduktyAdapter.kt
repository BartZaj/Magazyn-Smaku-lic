package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProduktyAdapter(
    private val listaProduktow: MutableList<Pair<String,Triple<String, String, Boolean>>>, // ID, Nazwa, Ilość
    private val onProduktClick: (String, String) -> Unit
) : RecyclerView.Adapter<ProduktyAdapter.ProduktViewHolder>() {

    class ProduktViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProduktViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProduktViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProduktViewHolder, position: Int) {
        val (idProduktu, data) = listaProduktow[position]
        val (nazwaProduktu, iloscOgolna, expiry) = data
            holder.view.findViewById<TextView>(R.id.produktNameTextView).text = nazwaProduktu
            holder.view.findViewById<TextView>(R.id.produktIloscTextView).text = iloscOgolna

        if (expiry) {
            holder.view.findViewById<ImageView>(R.id.expiryIcon).visibility = View.VISIBLE
        } else {
            holder.view.findViewById<ImageView>(R.id.expiryIcon).visibility = View.GONE
        }

        holder.view.setOnClickListener {
            onProduktClick(idProduktu, nazwaProduktu)
        }
    }

    override fun getItemCount(): Int = listaProduktow.size
}