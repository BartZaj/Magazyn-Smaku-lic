package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProductsActivity : AppCompatActivity() {

    private lateinit var produktyRecyclerView: RecyclerView
    private lateinit var produktyAdapter: ProduktyAdapter
    private val listaProduktow = mutableListOf<Pair<String, String>>() // Lista: ID produktu, Nazwa produktu
    private lateinit var firebaseBaza: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)

        val idKategorii = intent.getStringExtra("idKategorii")
        val nazwaKategorii = intent.getStringExtra("nazwaKategorii")

        if (idKategorii == null) {
            Toast.makeText(this, "Błąd: Brak ID kategorii", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        title = "Produkty: $nazwaKategorii"

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Użytkownik niezalogowany", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        firebaseBaza = FirebaseDatabase.getInstance().getReference("users/$uid/kategorie/$idKategorii/produkty")

        produktyRecyclerView = findViewById(R.id.produktyRecyclerView)
        produktyRecyclerView.layoutManager = LinearLayoutManager(this)

        val dodajProduktButton: Button = findViewById(R.id.dodajProduktButton)
        dodajProduktButton.setOnClickListener {
            pokazDialogDodawaniaProduktu()
        }

        val zobaczPrzepisyButton: Button = findViewById(R.id.zobaczPrzepisyButton)
        zobaczPrzepisyButton.setOnClickListener {
            val intent = Intent(this, ListaPrzepisowActivity::class.java)
            startActivity(intent)

            // Używamy FLAG_ACTIVITY_CLEAR_TOP, aby usunąć inne aktywności w tle
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

            // Zakończenie bieżącej aktywności (aby nie wrócić do niej)
            finish()
        }

        val mojMagazynButton: Button = findViewById(R.id.mojMagazynButton)
        mojMagazynButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            // Używamy FLAG_ACTIVITY_CLEAR_TOP, aby usunąć inne aktywności w tle
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            // Zakończenie bieżącej aktywności (aby nie wrócić do niej)
            finish()
        }

        produktyAdapter = ProduktyAdapter(listaProduktow) { idProduktu, nazwaProduktu ->
            // Przejście do ProduktDetailsActivity
            val intent = Intent(this, ProduktDetailsActivity::class.java).apply {
                putExtra("uid", uid)
                putExtra("idKategorii", idKategorii)
                putExtra("idProduktu", idProduktu)
                putExtra("nazwaProduktu", nazwaProduktu)
            }
            startActivity(intent)
        }

        produktyRecyclerView.adapter = produktyAdapter

        loadProducts()
    }

    private fun loadProducts() {
        firebaseBaza.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaProduktow.clear()
                for (produktSnapshot in snapshot.children) {
                    val idProduktu = produktSnapshot.key ?: continue
                    val nazwaProduktu = produktSnapshot.child("name").getValue(String::class.java) ?: "Nieznany produkt"
                    listaProduktow.add(idProduktu to nazwaProduktu)
                }
                listaProduktow.sortBy { it.second }

                produktyAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Błąd podczas pobierania produktów: ${error.message}")
            }
        })
    }

    private fun pokazDialogDodawaniaProduktu() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_dodaj_produkt, null)
        val editTextNazwa = dialogView.findViewById<EditText>(R.id.editTextNazwaProduktu)
        val spinnerJednostka = dialogView.findViewById<Spinner>(R.id.spinnerJednostka)

        val jednostki = listOf("g", "kg", "szt", "l", "ml")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, jednostki)
        spinnerJednostka.adapter = adapter

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Dodaj produkt")
            .setView(dialogView)
            .setPositiveButton("Dodaj") { _, _ ->
                val nazwaProduktu = editTextNazwa.text.toString().trim()
                val jednostka = spinnerJednostka.selectedItem.toString()

                if (nazwaProduktu.isNotEmpty()) {
                    val nowyProduktRef = firebaseBaza.push()
                    nowyProduktRef.setValue(mapOf("name" to nazwaProduktu, "unit" to jednostka))
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Produkt dodany pomyślnie!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Błąd podczas dodawania produktu.", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Nazwa produktu nie może być pusta.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Anuluj", null)
            .create()

        dialog.show()
    }
}