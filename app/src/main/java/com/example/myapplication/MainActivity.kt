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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var kategorieRecyclerView: RecyclerView
    private lateinit var kategorieAdapter: KategorieAdapter
    private val listaKategorii = mutableListOf<Pair<String, String>>() // Lista: ID kategorii, Nazwa kategorii

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid

        if (uid == null) {
            Toast.makeText(this, "Użytkownik niezalogowany", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        kategorieRecyclerView = findViewById(R.id.kategorieRecyclerView)
        kategorieRecyclerView.layoutManager = LinearLayoutManager(this)
        kategorieAdapter = KategorieAdapter(listaKategorii) { idKategorii, nazwaKategorii ->
            val intent = Intent(this, ProductsActivity::class.java)
            intent.putExtra("idKategorii", idKategorii) // Przekazujemy ID kategorii
            intent.putExtra("nazwaKategorii", nazwaKategorii)
            startActivity(intent)
        }
        kategorieRecyclerView.adapter = kategorieAdapter

        val firebaseBaza = FirebaseDatabase.getInstance().getReference("users/$uid/kategorie")

        firebaseBaza.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaKategorii.clear()
                for (kategoriaSnapshot in snapshot.children) {
                    val idKategorii = kategoriaSnapshot.key ?: continue
                    val nazwaKategorii = kategoriaSnapshot.child("name").getValue(String::class.java) ?: "Nieznana"
                    listaKategorii.add(idKategorii to nazwaKategorii)
                }
                listaKategorii.sortBy { it.second }

                kategorieAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Błąd podczas pobierania kategorii: ${error.message}")
            }
        })

        val dodajKategorieButton: Button = findViewById(R.id.dodajKategorieButton)
        dodajKategorieButton.setOnClickListener {
            pokazDialogDodawaniaKategorii(firebaseBaza)
        }

        val zobaczPrzepisyButton: Button = findViewById(R.id.zobaczPrzepisyButton)
        zobaczPrzepisyButton.setOnClickListener {
            val intent = Intent(this, ListaPrzepisowActivity::class.java)
            startActivity(intent)
        }
    }

    private fun pokazDialogDodawaniaKategorii(firebaseBaza: DatabaseReference) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_dodaj_kategorie, null)
        val editTextNazwa = dialogView.findViewById<EditText>(R.id.editTextNazwaKategorii)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Dodaj kategorię")
            .setView(dialogView)
            .setPositiveButton("Dodaj") { _, _ ->
                val nazwaKategorii = editTextNazwa.text.toString().trim()
                if (nazwaKategorii.isNotEmpty()) {
                    val nowaKategoriaRef = firebaseBaza.push()
                    nowaKategoriaRef.setValue(mapOf("name" to nazwaKategorii))
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Kategoria dodana pomyślnie!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Błąd podczas dodawania kategorii.", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Nazwa kategorii nie może być pusta.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Anuluj", null)
            .create()

        dialog.show()
    }
}