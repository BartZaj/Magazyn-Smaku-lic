package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class ListaPrzepisowActivity : AppCompatActivity() {

    private lateinit var przepisyRecyclerView: RecyclerView
    private lateinit var przepisyAdapter: PrzepisyAdapter
    private lateinit var firebaseRef: DatabaseReference

    // Lista przepisów jako lista map
    private val przepisyList = mutableListOf<Map<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_przepisow)

        przepisyRecyclerView = findViewById(R.id.przepisyRecyclerView)
        przepisyAdapter = PrzepisyAdapter(przepisyList) { przepis ->
            val intent = Intent(this, SzczegolyPrzepisuActivity::class.java)
            intent.putExtra("przepisId", przepis["id"])
            startActivity(intent)
        }

        przepisyRecyclerView.layoutManager = LinearLayoutManager(this)
        przepisyRecyclerView.adapter = przepisyAdapter

        firebaseRef = FirebaseDatabase.getInstance().getReference("przepisy")
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
                przepisyAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Błąd pobierania przepisów: ${error.message}")
                Toast.makeText(this@ListaPrzepisowActivity, "Błąd pobierania przepisów", Toast.LENGTH_SHORT).show()
            }
        })
    }
}