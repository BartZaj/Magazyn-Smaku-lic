package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class ProduktDetailsActivity : AppCompatActivity() {

    private lateinit var productNameTextView: TextView
    private lateinit var totalWeightTextView: TextView
    private lateinit var addBatchButton: Button
    private lateinit var batchesRecyclerView: RecyclerView

    private lateinit var databaseRef: DatabaseReference
    private lateinit var partieAdapter: PartieAdapter
    private var partieList = mutableListOf<Pair<String, Int>>() // Lista partii (ID, waga)
    private var totalWeight = 0

    private var idProduktu: String = ""
    private var nazwaProduktu: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_produkt_details)

        productNameTextView = findViewById(R.id.productNameTextView)
        totalWeightTextView = findViewById(R.id.totalWeightTextView)
        addBatchButton = findViewById(R.id.addBatchButton)
        batchesRecyclerView = findViewById(R.id.batchesRecyclerView)

        // Pobieranie danych z Intent
        idProduktu = intent.getStringExtra("idProduktu") ?: ""
        nazwaProduktu = intent.getStringExtra("nazwaProduktu") ?: ""

        if (idProduktu.isEmpty() || nazwaProduktu.isEmpty()) {
            Toast.makeText(this, "Nie znaleziono produktu", Toast.LENGTH_SHORT).show()
            finish()
        }

        productNameTextView.text = "Produkt: $nazwaProduktu"

        databaseRef = FirebaseDatabase.getInstance().getReference("produkty/$idProduktu")

        // Inicjalizacja RecyclerView
        partieAdapter = PartieAdapter(partieList) { batchId ->
            deleteBatch(batchId)
        }
        batchesRecyclerView.layoutManager = LinearLayoutManager(this)
        batchesRecyclerView.adapter = partieAdapter

        loadBatches()

        addBatchButton.setOnClickListener {
            showAddBatchDialog()
        }
    }

    private fun loadBatches() {
        databaseRef.child("partie").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                partieList.clear()
                totalWeight = 0

                for (batchSnapshot in snapshot.children) {
                    val batchId = batchSnapshot.key ?: continue
                    val weight = batchSnapshot.child("waga").getValue(Int::class.java) ?: 0
                    partieList.add(batchId to weight)
                    totalWeight += weight
                }

                totalWeightTextView.text = "Ogólna ilość: $totalWeight g"
                partieAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Błąd podczas pobierania partii: ${error.message}")
            }
        })
    }

    private fun showAddBatchDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Dodaj partię")
        val input = android.widget.EditText(this)
        input.hint = "Waga (w gramach)"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        dialogBuilder.setView(input)

        dialogBuilder.setPositiveButton("Dodaj") { _, _ ->
            val weight = input.text.toString().toIntOrNull()
            if (weight != null && weight > 0) {
                addNewBatch(weight)
            } else {
                Toast.makeText(this, "Podaj poprawną wagę", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBuilder.setNegativeButton("Anuluj") { dialog, _ -> dialog.dismiss() }

        dialogBuilder.show()
    }

    private fun addNewBatch(weight: Int) {
        val newBatchId = databaseRef.child("partie").push().key
        if (newBatchId != null) {
            val batchData = mapOf("waga" to weight)
            databaseRef.child("partie").child(newBatchId).setValue(batchData)
            databaseRef.child("ogolnaIlosc").setValue(totalWeight + weight)
        }
    }

    private fun deleteBatch(batchId: String) {
        val batchWeight = partieList.find { it.first == batchId }?.second ?: return
        databaseRef.child("partie").child(batchId).removeValue()
        databaseRef.child("ogolnaIlosc").setValue(totalWeight - batchWeight)
    }
}