package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView

class ProduktyDoWyboruAdapter(
    private val produkty: List<Pair<String, String>>,
    private val onProductSelected: (String, Int) -> Unit
) : RecyclerView.Adapter<ProduktyDoWyboruAdapter.ProduktViewHolder>() {

    // Mapy przechowujące stany zaznaczenia i gramatury dla każdego produktu
    private val selectedProducts = mutableMapOf<String, Int>()
    private val productSelectionState = mutableMapOf<String, Boolean>()

    inner class ProduktViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.produktCheckBox)
        val nazwaTextView: TextView = view.findViewById(R.id.nazwaProduktuTextView)
        val gramaturaEditText: EditText = view.findViewById(R.id.gramaturaEditText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProduktViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_produkt_do_wyboru, parent, false)
        return ProduktViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProduktViewHolder, position: Int) {
        val (id, nazwa) = produkty[position]
        holder.nazwaTextView.text = nazwa

        // Przywracamy stan zaznaczenia na podstawie zapisanych danych
        val isChecked = productSelectionState[id] == true
        holder.checkBox.isChecked = isChecked

        // Przywracamy poprzednią gramaturę
        val gramatura = selectedProducts[id] ?: 0
        holder.gramaturaEditText.setText(gramatura.toString())

        // Obsługuje zmianę stanu zaznaczenia
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            productSelectionState[id] = isChecked // Zapewnia, że zaznaczenie będzie zachowane

            val gramatura = holder.gramaturaEditText.text.toString().toIntOrNull() ?: 0
            if (isChecked && gramatura > 0) {
                selectedProducts[id] = gramatura // Dodajemy do mapy, jeśli zaznaczone
                onProductSelected(id, gramatura)
            } else {
                selectedProducts.remove(id) // Usuwamy z mapy, jeśli odznaczone
                onProductSelected(id, 0)
            }
        }

        // Obsługuje zmianę gramatury
        holder.gramaturaEditText.addTextChangedListener {
            val gramatura = it.toString().toIntOrNull() ?: 0
            if (productSelectionState[id] == true && gramatura > 0) {
                selectedProducts[id] = gramatura // Zaktualizuj gramaturę, jeśli produkt jest zaznaczony
                onProductSelected(id, gramatura)
            }
        }
    }

    override fun getItemCount() = produkty.size
}