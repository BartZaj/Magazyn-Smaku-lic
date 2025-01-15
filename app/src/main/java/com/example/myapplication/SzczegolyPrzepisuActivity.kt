package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class SzczegolyPrzepisuActivity : AppCompatActivity() {

    private lateinit var nazwaPrzepisuTextView: TextView
    private lateinit var trescPrzepisuTextView: TextView
    private lateinit var produktyRecyclerView: RecyclerView

    private lateinit var firebaseRef: DatabaseReference
    private lateinit var produktyAdapter: ProduktyPrzepisuAdapter

    // Lista produktów przechowywana jako lista map
    private val produktyList = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_szczegoly_przepisu)

        nazwaPrzepisuTextView = findViewById(R.id.nazwaPrzepisuTextView)
        trescPrzepisuTextView = findViewById(R.id.trescPrzepisuTextView)
        produktyRecyclerView = findViewById(R.id.produktyRecyclerView)

        produktyAdapter = ProduktyPrzepisuAdapter(produktyList)
        produktyRecyclerView.layoutManager = LinearLayoutManager(this)
        produktyRecyclerView.adapter = produktyAdapter

        val przepisId = intent.getStringExtra("przepisId") ?: return
        firebaseRef = FirebaseDatabase.getInstance().getReference("przepisy/$przepisId")

        loadSzczegolyPrzepisu()
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

                FirebaseDatabase.getInstance().getReference("produkty/$id/ogolnaIlosc")
                    .get().addOnSuccessListener { ogolnaIloscSnapshot ->
                        val iloscOgolna = ogolnaIloscSnapshot.getValue(Int::class.java) ?: 0
                        val produktMap = mapOf(
                            "nazwa" to nazwaProduktu,
                            "iloscPrzepis" to iloscPrzepis,
                            "iloscOgolna" to iloscOgolna
                        )
                        produktyList.add(produktMap)
                        produktyAdapter.notifyDataSetChanged()
                    }
            }
        }.addOnFailureListener {
            Log.e("Firebase", "Błąd ładowania szczegółów przepisu: ${it.message}")
            Toast.makeText(this, "Błąd ładowania szczegółów przepisu", Toast.LENGTH_SHORT).show()
        }
    }
}