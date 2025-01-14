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

data class ProduktWybrany(
    val id: String,
    val name: String,
    var ilosc: Int
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

        loadProduktyFromFirebase()

        dodajButton.setOnClickListener {
            dodajPrzepisDoFirebase()
        }

        anulujButton.setOnClickListener {
            finish()
        }
    }

    private fun loadProduktyFromFirebase() {
        FirebaseDatabase.getInstance().getReference("produkty")
            .get().addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val nazwa = child.child("nazwa").getValue(String::class.java) ?: "Nieznany"
                    val productView = createProductView(id, nazwa)
                    produktyLinearLayout.addView(productView)
                }
            }
    }

    private fun createProductView(id: String, nazwa: String): View {
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
                    val produkt = ProduktWybrany(id, nazwa, gramatura)
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
                    wybraneProdukty.add(ProduktWybrany(id, nazwa, gramatura))
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

        val przepisId = firebaseRef.push().key
        if (przepisId != null) {
            // Tworzymy mapę produktów, gdzie kluczem jest `id` produktu
            val produktyMap = wybraneProdukty.associate { produkt ->
                produkt.id to mapOf(
                    "name" to produkt.name,
                    "ilosc" to produkt.ilosc
                )
            }

            // Tworzymy dane przepisu
            val przepisData = mapOf(
                "nazwa" to nazwaPrzepisu,
                "tresc" to trescPrzepisu,
                "produkty" to produktyMap
            )

            // Zapisujemy do Firebase
            firebaseRef.child(przepisId).setValue(przepisData).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Przepis dodany pomyślnie!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Błąd podczas dodawania przepisu.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}