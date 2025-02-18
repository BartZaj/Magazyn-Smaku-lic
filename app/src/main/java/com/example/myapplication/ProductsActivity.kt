package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProductsActivity : AppCompatActivity() {

    private lateinit var categoryNameTextView: TextView
    private lateinit var produktyRecyclerView: RecyclerView
    private lateinit var produktyAdapter: ProduktyAdapter
    private val listaProduktow = mutableListOf<Pair<String, Triple<String, String, Boolean>>>() // ID, (Nazwa, Ilość, Ostrzeżenie)
    private lateinit var firebaseBaza: DatabaseReference
    private var idKategorii: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)

        idKategorii = intent.getStringExtra("idKategorii")
        val nazwaKategorii = intent.getStringExtra("nazwaKategorii")

        if (idKategorii == null) {
            Toast.makeText(this, "Błąd: Brak ID kategorii", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        categoryNameTextView = findViewById(R.id.categoryNameTextView)
        categoryNameTextView.text = "Kategoria: $nazwaKategorii"


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
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        val mojMagazynButton: Button = findViewById(R.id.mojMagazynButton)
        mojMagazynButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        produktyAdapter = ProduktyAdapter(listaProduktow) { idProduktu, nazwaProduktu ->
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
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val kategoriaId = idKategorii ?: return

        firebaseBaza.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempProduktow = mutableListOf<Pair<String, Triple<String, String, Boolean>>>() // Tymczasowa lista

                val licznikProduktow = snapshot.childrenCount
                var przetworzoneProdukty = 0

                for (produktSnapshot in snapshot.children) {
                    val idProduktu = produktSnapshot.key ?: continue
                    val nazwaProduktu = produktSnapshot.child("name").getValue(String::class.java) ?: "Nieznany produkt"
                    val jednostka = produktSnapshot.child("unit").getValue(String::class.java) ?: ""
                    var expiry = false

                    val partieRef = firebaseBaza.child(idProduktu).child("partie")
                    partieRef.get().addOnSuccessListener { partieSnapshot ->
                        var iloscOgolna = 0.0
                        val today = Calendar.getInstance()
                        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

                        for (partia in partieSnapshot.children) {
                            val iloscPartii = partia.child("waga").getValue(Double::class.java) ?: 0.0
                            val terminWaznosciString = partia.child("dataWaznosci").getValue(String::class.java) ?: "Brak daty"

                            try {
                                val terminWaznosciCalendar = Calendar.getInstance().apply {
                                    time = dateFormat.parse(terminWaznosciString) ?: Date()
                                }
                                val daysDifference = ((terminWaznosciCalendar.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()

                                if (daysDifference >= 0) {
                                    iloscOgolna += iloscPartii
                                }
                                else
                                {
                                    expiry = true
                                }
                            } catch (e: Exception) {
                                Log.e("ParseError", "Błąd parsowania daty: ${e.message}")
                            }
                        }

                        val roundedWeight = String.format(Locale.US, "%.2f", iloscOgolna).toDouble()

                        // Po zakończeniu przetwarzania partii dla tego produktu, dodajemy do listy
                        tempProduktow.add(Pair(idProduktu, Triple(nazwaProduktu, "$roundedWeight $jednostka", expiry)))

                        // Sprawdzamy, czy wszystkie produkty zostały przetworzone
                        przetworzoneProdukty++
                        if (przetworzoneProdukty == licznikProduktow.toInt()) {
                            tempProduktow.sortBy { it.second.first }

                            // Przypisujemy zaktualizowaną listę do głównej
                            listaProduktow.clear()
                            listaProduktow.addAll(tempProduktow)

                            // Odświeżamy adapter RAZ po zakończeniu pętli
                            produktyAdapter.notifyDataSetChanged()
                        }
                    }
                }
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

        val defaultBlue = ContextCompat.getColor(this, R.color.blue)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(defaultBlue)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(defaultBlue)

    }
}