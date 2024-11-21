package com.example.myapplication
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("products")

        // Dodanie przykładowego wpisu
        val product = mapOf("name" to "Pomarańcza", "price" to 4.2)
        ref.child("9").setValue(product).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("Firebase", "Dane zapisane pomyślnie!")
            } else {
                Log.e("Firebase", "Błąd podczas zapisywania: ${task.exception}")
            }
        }
    }
}