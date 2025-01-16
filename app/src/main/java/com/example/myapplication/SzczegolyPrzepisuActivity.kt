package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class SzczegolyPrzepisuActivity : AppCompatActivity() {

    private lateinit var nazwaPrzepisuTextView: TextView
    private lateinit var trescPrzepisuTextView: TextView
    private lateinit var produktyRecyclerView: RecyclerView
    private lateinit var iloscPosilkowEditText: EditText // Pole do wprowadzania ilości posiłków

    private lateinit var firebaseRef: DatabaseReference
    private lateinit var produktyAdapter: ProduktyPrzepisuAdapter

    // Lista produktów przechowywana jako lista map
    private val produktyList = mutableListOf<Map<String, Any>>()

    private var iloscPosilkow: Int = 1 // Początkowa wartość ilości posiłków (domyślnie 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_szczegoly_przepisu)

        // Inicjalizacja widoków
        nazwaPrzepisuTextView = findViewById(R.id.nazwaPrzepisuTextView)
        trescPrzepisuTextView = findViewById(R.id.trescPrzepisuTextView)
        produktyRecyclerView = findViewById(R.id.produktyRecyclerView)
        iloscPosilkowEditText = findViewById(R.id.iloscPosilkowEditText) // Inicjalizacja pola do ilości posiłków

        // Ustawienie adaptera RecyclerView
        produktyAdapter = ProduktyPrzepisuAdapter(produktyList)
        produktyRecyclerView.layoutManager = LinearLayoutManager(this)
        produktyRecyclerView.adapter = produktyAdapter

        // Pobranie ID przepisu z intencji
        val przepisId = intent.getStringExtra("przepisId") ?: return
        firebaseRef = FirebaseDatabase.getInstance().getReference("przepisy/$przepisId")

        loadSzczegolyPrzepisu() // Załaduj szczegóły przepisu

        // Obserwacja zmiany ilości posiłków
        iloscPosilkowEditText.addTextChangedListener { editable ->
            val iloscPosilkow = editable.toString().toIntOrNull() ?: 1
            przeliczProdukty(iloscPosilkow) // Przelicz ilości produktów
        }
    }

    private fun loadSzczegolyPrzepisu() {
        firebaseRef.get().addOnSuccessListener { snapshot ->
            val nazwa = snapshot.child("nazwa").getValue(String::class.java) ?: "Nieznany"
            val tresc = snapshot.child("tresc").getValue(String::class.java) ?: "Brak treści"

            nazwaPrzepisuTextView.text = nazwa
            trescPrzepisuTextView.text = tresc

            produktyList.clear()
            val produktySnapshot = snapshot.child("produkty")
            produktySnapshot.children.forEach { produkt ->
                val id = produkt.key ?: return@forEach
                val nazwaProduktu = produkt.child("name").getValue(String::class.java) ?: "Nieznany"
                val iloscPrzepis = produkt.child("ilosc").getValue(Int::class.java) ?: 0

                // Pobieranie ogólnej ilości produktu z bazy
                FirebaseDatabase.getInstance().getReference("produkty/$id/ogolnaIlosc")
                    .get().addOnSuccessListener { ogolnaIloscSnapshot ->
                        val iloscOgolna = ogolnaIloscSnapshot.getValue(Int::class.java) ?: 0

                        // Dodanie produktu do listy z zachowaniem bazowej ilości
                        val produktMap = mutableMapOf(
                            "nazwa" to nazwaProduktu,
                            "iloscPrzepis" to iloscPrzepis, // Aktualna ilość w przepisie
                            "iloscPrzepisBazowa" to iloscPrzepis, // Bazowa ilość
                            "iloscOgolna" to iloscOgolna
                        )

                        produktyList.add(produktMap)
                        produktyAdapter.notifyDataSetChanged() // Odświeżenie adaptera
                    }
            }
        }.addOnFailureListener {
            Log.e("Firebase", "Błąd ładowania szczegółów przepisu: ${it.message}")
            Toast.makeText(this, "Błąd ładowania szczegółów przepisu", Toast.LENGTH_SHORT).show()
        }
    }

    // Funkcja przeliczająca ilość produktów na podstawie ilości posiłków
    private fun przeliczProdukty(iloscPosilkow: Int) {
        // Zmieniamy ilości produktów w zależności od wartości wprowadzonej w polu
        produktyList.forEach { produkt ->
            val iloscPrzepisBazowa = produkt["iloscPrzepisBazowa"] as? Int ?: 0 // Bazowa ilość z bazy
            val iloscPoZmianie = iloscPrzepisBazowa * iloscPosilkow // Mnożymy przez ilość posiłków

            // Tworzymy kopię mapy, aby móc ją zmodyfikować
            val updatedProdukt = produkt.toMutableMap()

            // Zaktualizowanie ilości w przepisie
            updatedProdukt["iloscPrzepis"] = iloscPoZmianie

            // Zaktualizowanie oryginalnej mapy w liście
            val index = produktyList.indexOf(produkt)
            produktyList[index] = updatedProdukt
        }

        // Powiadamiamy adapter o zmianie danych, aby odświeżyć widok
        produktyAdapter.notifyDataSetChanged()
    }
}