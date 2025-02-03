package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.google.firebase.auth.FirebaseAuth

class ListaPrzepisowActivity : AppCompatActivity() {

    private lateinit var przepisyRecyclerView: RecyclerView
    private lateinit var przepisyAdapter: PrzepisyAdapter
    private lateinit var firebaseRef: DatabaseReference

    // Lista przepisów jako lista map
    private val przepisyList = mutableListOf<Map<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_przepisow)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Użytkownik niezalogowany", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        firebaseRef = FirebaseDatabase.getInstance().getReference("users/$uid/przepisy")

        przepisyRecyclerView = findViewById(R.id.przepisyRecyclerView)
        przepisyAdapter = PrzepisyAdapter(przepisyList, { przepis ->
            val intent = Intent(this, SzczegolyPrzepisuActivity::class.java)
            intent.putExtra("przepisId", przepis["id"])
            startActivity(intent) }, { recipeId -> deleteRecipe(recipeId)
        })

        val dodajPrzepisButton: Button = findViewById(R.id.dodajPrzepisButton)
        dodajPrzepisButton.setOnClickListener {
            val intent = Intent(this, DodajPrzepisActivity::class.java)
            startActivity(intent)
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

        przepisyRecyclerView.layoutManager = LinearLayoutManager(this)
        przepisyRecyclerView.adapter = przepisyAdapter

        loadPrzepisy()
    }

    private fun loadPrzepisy() {
        firebaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                przepisyList.clear()
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val nazwa = child.child("nazwa").getValue(String::class.java) ?: "Nieznany"
                    val przepisMap = mapOf("id" to id, "nazwa" to nazwa)
                    przepisyList.add(przepisMap)
                }
                przepisyList.sortBy { it["nazwa"].toString() }

                przepisyAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Błąd pobierania przepisów: ${error.message}")
                //Toast.makeText(this@ListaPrzepisowActivity, "Błąd pobierania przepisów", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteRecipe(recipeId: String) {
        firebaseRef.child(recipeId).removeValue().addOnSuccessListener {
            przepisyList.removeAll { it["id"] == recipeId }
            przepisyAdapter.notifyDataSetChanged()
            Toast.makeText(this, "Przepis usunięty", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Błąd usuwania przepisu", Toast.LENGTH_SHORT).show()
        }
    }
}