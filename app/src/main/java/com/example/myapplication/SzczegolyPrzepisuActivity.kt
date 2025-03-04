package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SzczegolyPrzepisuActivity : AppCompatActivity() {

    private lateinit var nazwaPrzepisuTextView: TextView
    private lateinit var trescPrzepisuTextView: TextView
    private lateinit var produktyRecyclerView: RecyclerView
    private lateinit var iloscPosilkowEditText: EditText // Pole do wprowadzania ilości posiłków

    private lateinit var firebaseRef: DatabaseReference
    private lateinit var produktyAdapter: ProduktyPrzepisuAdapter

    private val produktyList = mutableListOf<Map<String, Any>>()
    private var iloscPosilkow: Int = 1
    private var uid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_szczegoly_przepisu)

        nazwaPrzepisuTextView = findViewById(R.id.nazwaPrzepisuTextView)
        trescPrzepisuTextView = findViewById(R.id.trescPrzepisuTextView)
        produktyRecyclerView = findViewById(R.id.produktyRecyclerView)
        iloscPosilkowEditText = findViewById(R.id.iloscPosilkowEditText)

        produktyAdapter = ProduktyPrzepisuAdapter(produktyList) { idProduktu, idKategorii ->
            val intent = Intent(this, ProduktDetailsActivity::class.java).apply {
                putExtra("uid", uid)
                putExtra("idProduktu", idProduktu)
                putExtra("idKategorii", idKategorii)
                putExtra("nazwaProduktu", produktyList.find { it["id"] == idProduktu }?.get("nazwa").toString())
            }
            startActivity(intent)
        }

        produktyRecyclerView.layoutManager = LinearLayoutManager(this)
        produktyRecyclerView.adapter = produktyAdapter

        uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Brak zalogowanego użytkownika", Toast.LENGTH_SHORT).show()
            return
        }

        val przepisId = intent.getStringExtra("przepisId") ?: return
        firebaseRef = FirebaseDatabase.getInstance().getReference("users/$uid/przepisy/$przepisId")

        loadSzczegolyPrzepisu()

        iloscPosilkowEditText.addTextChangedListener { editable ->
            val iloscPosilkow = editable.toString().toIntOrNull() ?: 1
            przeliczProdukty(iloscPosilkow)
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
                val idKategorii = produkt.child("id_kategorii").getValue(String::class.java) ?: "Nieznany"

                FirebaseDatabase.getInstance().getReference("users/$uid/kategorie/$idKategorii/produkty/$id/ogolnaIlosc")
                    .get().addOnSuccessListener { ogolnaIloscSnapshot ->
                        val iloscOgolna = ogolnaIloscSnapshot.getValue(Int::class.java) ?: 0

                        val produktMap = mutableMapOf(
                            "nazwa" to nazwaProduktu,
                            "iloscPrzepis" to iloscPrzepis,
                            "iloscPrzepisBazowa" to iloscPrzepis,
                            "iloscOgolna" to iloscOgolna,
                            "id" to id,
                            "idKategorii" to idKategorii
                        )

                        produktyList.add(produktMap)

                        produktyList.sortBy { it["nazwa"].toString() }
                        produktyAdapter.notifyDataSetChanged()
                    }
            }
        }.addOnFailureListener {
            Log.e("Firebase", "Błąd ładowania szczegółów przepisu: ${it.message}")
            Toast.makeText(this, "Błąd ładowania szczegółów przepisu", Toast.LENGTH_SHORT).show()
        }
    }

    private fun przeliczProdukty(iloscPosilkow: Int) {
        produktyList.forEach { produkt ->
            val iloscPrzepisBazowa = produkt["iloscPrzepisBazowa"] as? Int ?: 0
            val iloscPoZmianie = iloscPrzepisBazowa * iloscPosilkow
            val updatedProdukt = produkt.toMutableMap()
            updatedProdukt["iloscPrzepis"] = iloscPoZmianie
            val index = produktyList.indexOf(produkt)
            produktyList[index] = updatedProdukt
        }

        produktyAdapter.notifyDataSetChanged()
    }

    override fun onStart() {
        super.onStart()
        reaload()
    }

    private fun reaload() {

        // Zaktualizuj ogólną ilość produktu dla wszystkich produktów w przepisie
        produktyList.forEach { produkt ->
            val idKategorii = produkt["idKategorii"] as? String ?: return@forEach
            val idProduktu = produkt["id"] as? String ?: return@forEach

            FirebaseDatabase.getInstance().getReference("users/$uid/kategorie/$idKategorii/produkty/$idProduktu/ogolnaIlosc")
                .get().addOnSuccessListener { ogolnaIloscSnapshot ->
                    val ogolnaIlosc = ogolnaIloscSnapshot.getValue(Int::class.java) ?: 0

                    val updatedProdukt = produkt.toMutableMap()
                    updatedProdukt["iloscOgolna"] = ogolnaIlosc

                    val index = produktyList.indexOf(produkt)
                    produktyList[index] = updatedProdukt

                    produktyAdapter.notifyDataSetChanged()
                }
        }
    }
}