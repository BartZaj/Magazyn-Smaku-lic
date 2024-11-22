package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var helloTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        helloTextView = findViewById(R.id.helloTextView)

        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("products")

        // Dodanie przykładowego wpisu
        val product = mapOf("name" to "Pomarańcza", "price" to 4.2)
        ref.child("1").setValue(product).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("Firebase", "Dane zapisane pomyślnie!")
            } else {
                Log.e("Firebase", "Błąd podczas zapisywania: ${task.exception}")
            }
        }

        // Pobierz dane z Firebase
        ref.child("1").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Pobieramy dane z bazy
                val productName = snapshot.child("name").getValue(String::class.java)
                val productPrice = snapshot.child("price").getValue(Double::class.java)

                // Wyświetlamy dane w TextView
                if (productName != null && productPrice != null) {
                    helloTextView.text = "$productName, $productPrice"
                } else {
                    helloTextView.text = "Nie znaleziono danych"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Błąd podczas pobierania danych: ${error.message}")
                helloTextView.text = "Błąd podczas pobierania danych"
            }
        })
    }
}