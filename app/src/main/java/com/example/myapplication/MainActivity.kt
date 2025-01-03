package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var produktyRecyclerView: RecyclerView
    private lateinit var produktyAdapter: ProduktyAdapter
    private val listaProduktow = mutableListOf<Pair<String, String>>() // Lista: ID produktu, Nazwa produktu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicjalizacja RecyclerView
        produktyRecyclerView = findViewById(R.id.produktyRecyclerView)
        produktyRecyclerView.layoutManager = LinearLayoutManager(this)
        produktyAdapter = ProduktyAdapter(listaProduktow) { idProduktu, nazwaProduktu ->
            val intent = Intent(this, ProduktDetailsActivity::class.java)
            intent.putExtra("idProduktu", idProduktu) // Przekazuje ID produktu
            intent.putExtra("nazwaProduktu", nazwaProduktu) // Przekazuje nazwe produktu
            startActivity(intent)
        }
        produktyRecyclerView.adapter = produktyAdapter

        val firebaseBaza = FirebaseDatabase.getInstance().getReference("produkty")

        // Pobieranie listy produktow z Firebase
        firebaseBaza.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaProduktow.clear()
                for (produktSnapshot in snapshot.children) {
                    val idProduktu = produktSnapshot.key ?: continue
                    val nazwaProduktu = produktSnapshot.child("nazwa").getValue(String::class.java) ?: "Nieznany"
                    listaProduktow.add(idProduktu to nazwaProduktu)
                }
                produktyAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Błąd podczas pobierania produktów: ${error.message}")
            }
        })

        val dodajProduktButton: Button = findViewById(R.id.dodajProduktButton)
        dodajProduktButton.setOnClickListener {
            pokazDialogDodawania(firebaseBaza)
        }
    }

    private fun pokazDialogDodawania(firebaseBaza: DatabaseReference) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_dodaj_produkt, null)
        val editTextNazwa = dialogView.findViewById<EditText>(R.id.editTextNazwaProduktu)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Dodaj produkt")
            .setView(dialogView)
            .setPositiveButton("Dodaj") { _, _ ->
                val nazwaProduktu = editTextNazwa.text.toString().trim()
                if (nazwaProduktu.isNotEmpty()) {
                    val nowyProduktRef = firebaseBaza.push()
                    nowyProduktRef.setValue(mapOf("nazwa" to nazwaProduktu))
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