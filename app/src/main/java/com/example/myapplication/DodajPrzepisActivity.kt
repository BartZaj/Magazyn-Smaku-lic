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

class DodajPrzepisActivity : AppCompatActivity() {

    private lateinit var nazwaPrzepisuEditText: EditText
    private lateinit var trescPrzepisuEditText: EditText
    private lateinit var produktyLinearLayout: LinearLayout
    private lateinit var dodajButton: Button
    private lateinit var anulujButton: Button

    private lateinit var firebaseRef: DatabaseReference

    // Zmieniamy mapę, aby przechowywać nazwy produktów i ich gramatury
    private val wybraneProdukty = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dodaj_przepis)

        // Inicjalizacja widoków
        nazwaPrzepisuEditText = findViewById(R.id.nazwaPrzepisuEditText)
        trescPrzepisuEditText = findViewById(R.id.trescPrzepisuEditText)
        produktyLinearLayout = findViewById(R.id.produktyLinearLayout)
        dodajButton = findViewById(R.id.dodajPrzepisButton)
        anulujButton = findViewById(R.id.anulujButton)

        // Inicjalizacja bazy danych Firebase
        firebaseRef = FirebaseDatabase.getInstance().getReference("przepisy")

        // Ładowanie produktów z Firebase
        loadProduktyFromFirebase()

        // Ustawienie kliknięcia przycisku "Dodaj"
        dodajButton.setOnClickListener {
            dodajPrzepisDoFirebase()
        }

        // Ustawienie kliknięcia przycisku "Anuluj"
        anulujButton.setOnClickListener {
            finish()
        }
    }

    // Funkcja ładująca produkty z Firebase
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

    // Funkcja tworząca widok dla każdego produktu
    private fun createProductView(id: String, nazwa: String): View {
        val productView = LayoutInflater.from(this).inflate(R.layout.item_produkt_do_wyboru, null)

        val checkBox: CheckBox = productView.findViewById(R.id.produktCheckBox)
        val nazwaTextView: TextView = productView.findViewById(R.id.nazwaProduktuTextView)
        val gramaturaEditText: EditText = productView.findViewById(R.id.gramaturaEditText)

        nazwaTextView.text = nazwa

        // Początkowo ustawiamy gramaturę na zablokowaną, aby nie było możliwe jej edytowanie przed zaznaczeniem checkboxa
        gramaturaEditText.isEnabled = false

        // Ustawiamy domyślną gramaturę na podstawie wcześniej zapisanej wartości
        gramaturaEditText.setText(wybraneProdukty[nazwa]?.toString())

        // Obsługa zmiany stanu checkboxa
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Jeśli checkbox jest zaznaczony, umożliwiamy edycję gramatury
                gramaturaEditText.isEnabled = true

                // Jeśli gramatura była już zapisana, przypisujemy ją do pola
                val gramatura = wybraneProdukty[nazwa] ?: 0
                gramaturaEditText.setText(gramatura.toString())
            } else {
                // Jeśli checkbox nie jest zaznaczony, blokujemy edycję gramatury
                gramaturaEditText.isEnabled = false
                gramaturaEditText.setText("") // Możemy wyczyścić gramaturę, jeśli checkbox jest odznaczony
                wybraneProdukty.remove(nazwa) // Usuwamy produkt z listy wybranych produktów
            }
        }

        // Obsługa zmiany gramatury
        gramaturaEditText.addTextChangedListener {
            val gramatura = it.toString().toIntOrNull() ?: 0
            if (gramatura > 0) {
                // Jeśli gramatura jest większa niż 0, przechowujemy ją w mapie
                wybraneProdukty[nazwa] = gramatura
            } else {
                // Jeśli gramatura wynosi 0, usuwamy produkt z mapy
                wybraneProdukty.remove(nazwa)
            }
        }

        return productView
    }

    // Funkcja dodająca przepis do Firebase
    private fun dodajPrzepisDoFirebase() {
        val nazwaPrzepisu = nazwaPrzepisuEditText.text.toString().trim()
        val trescPrzepisu = trescPrzepisuEditText.text.toString().trim()

        // Walidacja pól formularza
        if (nazwaPrzepisu.isEmpty() || trescPrzepisu.isEmpty()) {
            Toast.makeText(this, "Uzupełnij wszystkie pola", Toast.LENGTH_SHORT).show()
            return
        }

        val przepisId = firebaseRef.push().key
        if (przepisId != null) {
            // Tworzenie mapy danych przepisu
            val przepisData = mapOf(
                "nazwa" to nazwaPrzepisu,
                "tresc" to trescPrzepisu,
                "produkty" to wybraneProdukty // Zamiast id, przekazujemy nazwy produktów
            )

            // Zapis do Firebase
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