package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.google.firebase.auth.FirebaseAuth

class RecipeListActivity : AppCompatActivity() {

    private lateinit var przepisyRecyclerView: RecyclerView
    private lateinit var przepisyAdapter: RecipeAdapter
    private lateinit var firebaseRef: DatabaseReference

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
        przepisyAdapter = RecipeAdapter(przepisyList, { przepis ->
            val intent = Intent(this, RecipeDetailsActivity::class.java)
            intent.putExtra("przepisId", przepis["id"])
            startActivity(intent) },
            { recipeId -> deleteRecipe(recipeId)
        })

        val dodajPrzepisButton: Button = findViewById(R.id.dodajPrzepisButton)
        dodajPrzepisButton.setOnClickListener {
            val intent = Intent(this, AddRecipeActivity::class.java)
            startActivity(intent)
        }

        val mojMagazynButton: Button = findViewById(R.id.mojMagazynButton)
        mojMagazynButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        val logOutButton: ImageButton = findViewById(R.id.logOutButton)
        logOutButton.setOnClickListener {
            val preferences = getSharedPreferences("checkbox", MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putString("remember", "false")
            editor.apply()

            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LogInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        przepisyRecyclerView.layoutManager = LinearLayoutManager(this)
        przepisyRecyclerView.adapter = przepisyAdapter

        loadRecipes()
    }

    private fun loadRecipes() {
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