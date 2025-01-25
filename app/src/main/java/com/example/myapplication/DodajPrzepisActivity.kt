package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth

data class ProduktWybrany(
    val id: String,
    val name: String,
    var ilosc: Int,
    val id_kategorii: String
)

class DodajPrzepisActivity : AppCompatActivity() {

    private lateinit var nazwaPrzepisuEditText: EditText
    private lateinit var trescPrzepisuEditText: EditText
    private lateinit var produktyLinearLayout: LinearLayout
    private lateinit var dodajButton: Button
    private lateinit var anulujButton: Button

    private lateinit var firebaseRef: DatabaseReference

    private val wybraneProdukty = mutableListOf<ProduktWybrany>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dodaj_przepis)

        nazwaPrzepisuEditText = findViewById(R.id.nazwaPrzepisuEditText)
        trescPrzepisuEditText = findViewById(R.id.trescPrzepisuEditText)
        produktyLinearLayout = findViewById(R.id.produktyLinearLayout)
        dodajButton = findViewById(R.id.dodajPrzepisButton)
        anulujButton = findViewById(R.id.anulujButton)

        firebaseRef = FirebaseDatabase.getInstance().getReference("przepisy")

        loadProductsFromFirebase()

        dodajButton.setOnClickListener {
            dodajPrzepisDoFirebase()
        }

        anulujButton.setOnClickListener {
            finish()
        }
    }

    private fun loadProductsFromFirebase() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        if (uid != null) {
            FirebaseDatabase.getInstance().getReference("users/$uid/kategorie")
                .get().addOnSuccessListener { snapshot ->
                    val allProducts = mutableListOf<Triple<String, String, String>>() // Lista do przechowywania ID, nazwy i ID kategorii

                    for (categorySnapshot in snapshot.children) {
                        val idKategorii = categorySnapshot.key ?: continue
                        val produktySnapshot = categorySnapshot.child("produkty")
                        for (child in produktySnapshot.children) {
                            val id = child.key ?: continue
                            val nazwa = child.child("name").getValue(String::class.java) ?: "Nieznany"
                            allProducts.add(Triple(id, nazwa, idKategorii)) // Dodajemy (ID, nazwa, ID kategorii)
                        }
                    }

                    allProducts.sortBy { it.second }

                    produktyLinearLayout.removeAllViews()
                    for ((id, nazwa, idKategorii) in allProducts) {
                        val productView = createProductView(id, nazwa, idKategorii)
                        produktyLinearLayout.addView(productView)
                    }
                }
        } else {
            Toast.makeText(this, "Błąd: użytkownik niezalogowany", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createProductView(id: String, nazwa: String, idKategorii: String): View {
        val productView = LayoutInflater.from(this).inflate(R.layout.item_produkt_do_wyboru, null)

        val checkBox: CheckBox = productView.findViewById(R.id.produktCheckBox)
        val nazwaTextView: TextView = productView.findViewById(R.id.nazwaProduktuTextView)
        val gramaturaEditText: EditText = productView.findViewById(R.id.gramaturaEditText)

        nazwaTextView.text = nazwa

        gramaturaEditText.isEnabled = false

        checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                gramaturaEditText.isEnabled = true
                val gramatura = gramaturaEditText.text.toString().toIntOrNull() ?: 0
                if (gramatura > 0) {
                    val produkt = ProduktWybrany(id, nazwa, gramatura, idKategorii)
                    wybraneProdukty.add(produkt)
                }
            } else {
                gramaturaEditText.isEnabled = false
                gramaturaEditText.setText("")
                wybraneProdukty.removeAll { it.name == nazwa }
            }
        }

        gramaturaEditText.addTextChangedListener {
            val gramatura = it.toString().toIntOrNull() ?: 0
            if (checkBox.isChecked) {
                val produkt = wybraneProdukty.find { it.name == nazwa }
                if (produkt != null) {
                    produkt.ilosc = gramatura
                } else if (gramatura > 0) {
                    wybraneProdukty.add(ProduktWybrany(id, nazwa, gramatura, idKategorii))
                }
            }
        }

        return productView
    }

    private fun dodajPrzepisDoFirebase() {
        val nazwaPrzepisu = nazwaPrzepisuEditText.text.toString().trim()
        val trescPrzepisu = trescPrzepisuEditText.text.toString().trim()

        if (nazwaPrzepisu.isEmpty() || trescPrzepisu.isEmpty()) {
            Toast.makeText(this, "Uzupełnij wszystkie pola", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        if (uid != null) {
            val przepisId = FirebaseDatabase.getInstance().getReference("users/$uid/przepisy").push().key
            if (przepisId != null) {
                val produktyMap = wybraneProdukty.associate { produkt ->
                    produkt.id to mapOf(
                        "name" to produkt.name,
                        "ilosc" to produkt.ilosc,
                        "id_kategorii" to produkt.id_kategorii
                    )
                }

                val przepisData = mapOf(
                    "nazwa" to nazwaPrzepisu,
                    "tresc" to trescPrzepisu,
                    "produkty" to produktyMap
                )

                FirebaseDatabase.getInstance().getReference("users/$uid/przepisy/$przepisId")
                    .setValue(przepisData).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Przepis dodany pomyślnie!", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this, "Błąd podczas dodawania przepisu.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        } else {
            Toast.makeText(this, "Błąd: użytkownik niezalogowany", Toast.LENGTH_SHORT).show()
        }
    }
}
