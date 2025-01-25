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
    private var partieList = mutableListOf<Pair<String, Pair<Int, String>>>() // Lista partii (ID, (waga, dataWaznosci))
    private var totalWeight = 0

    private var uid: String = ""
    private var idProduktu: String = ""
    private var nazwaProduktu: String = ""
    private var idKategorii: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_produkt_details)

        productNameTextView = findViewById(R.id.productNameTextView)
        totalWeightTextView = findViewById(R.id.totalWeightTextView)
        addBatchButton = findViewById(R.id.addBatchButton)
        batchesRecyclerView = findViewById(R.id.batchesRecyclerView)

        uid = intent.getStringExtra("uid") ?: ""
        idKategorii = intent.getStringExtra("idKategorii") ?: ""
        idProduktu = intent.getStringExtra("idProduktu") ?: ""
        nazwaProduktu = intent.getStringExtra("nazwaProduktu") ?: ""

        if (uid.isEmpty() || idProduktu.isEmpty() || idKategorii.isEmpty() || nazwaProduktu.isEmpty()) {
            Toast.makeText(this, "Nie znaleziono danych produktu", Toast.LENGTH_SHORT).show()
            finish()
        }

        productNameTextView.text = "Produkt: $nazwaProduktu"

        databaseRef =
            FirebaseDatabase.getInstance().getReference("users/$uid/kategorie/$idKategorii/produkty/$idProduktu/partie")

        partieAdapter = PartieAdapter(partieList, { batchId -> deleteBatch(batchId) }, { batchId, currentWeight -> showEditBatchDialog(batchId, currentWeight) })
        batchesRecyclerView.layoutManager = LinearLayoutManager(this)
        batchesRecyclerView.adapter = partieAdapter

        loadBatches()

        addBatchButton.setOnClickListener { showAddBatchDialog() }
    }

    private fun loadBatches() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                partieList.clear()
                totalWeight = 0

                for (batchSnapshot in snapshot.children) {
                    val batchId = batchSnapshot.key ?: continue
                    val weight = batchSnapshot.child("waga").getValue(Int::class.java) ?: 0
                    val expiryDate = batchSnapshot.child("dataWaznosci").getValue(String::class.java) ?: "Brak daty"
                    partieList.add(batchId to Pair(weight, expiryDate))
                    totalWeight += weight
                }

                updateTotalWeightInDatabase(totalWeight)

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

        val dialogLayout = layoutInflater.inflate(R.layout.dialog_add_batch, null)
        val weightInput = dialogLayout.findViewById<android.widget.EditText>(R.id.weightEditText)
        val datePicker = dialogLayout.findViewById<android.widget.DatePicker>(R.id.datePicker)

        dialogBuilder.setView(dialogLayout)

        dialogBuilder.setPositiveButton("Dodaj") { _, _ ->
            val weight = weightInput.text.toString().toIntOrNull()
            val selectedDate = "${datePicker.dayOfMonth}-${datePicker.month + 1}-${datePicker.year}"

            if (weight != null && weight > 0) {
                addNewBatch(weight, selectedDate)
            } else {
                Toast.makeText(this, "Podaj poprawną wagę", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBuilder.setNegativeButton("Anuluj") { dialog, _ -> dialog.dismiss() }

        dialogBuilder.show()
    }

    private fun addNewBatch(weight: Int, expiryDate: String) {
        val newBatchId = databaseRef.push().key
        if (newBatchId != null) {
            val batchData = mapOf(
                "waga" to weight,
                "dataWaznosci" to expiryDate
            )
            databaseRef.child(newBatchId).setValue(batchData)

            updateTotalWeightInDatabase(totalWeight + weight)
        }
    }

    private fun deleteBatch(batchId: String) {
        val batchWeight = partieList.find { it.first == batchId }?.second?.first ?: return
        databaseRef.child(batchId).removeValue()

        updateTotalWeightInDatabase(totalWeight - batchWeight)
    }

    private fun showEditBatchDialog(batchId: String, currentWeight: Int) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Edytuj partię")

        val dialogLayout = layoutInflater.inflate(R.layout.dialog_edit_batch, null)
        val weightInput = dialogLayout.findViewById<android.widget.EditText>(R.id.weightEditText)

        dialogBuilder.setView(dialogLayout)

        dialogBuilder.setPositiveButton("Zaktualizuj") { _, _ ->
            val weightToSubtract = weightInput.text.toString().toIntOrNull()
            if (weightToSubtract != null && weightToSubtract > 0) {
                updateBatchWeight(batchId, currentWeight, weightToSubtract)
            } else {
                Toast.makeText(this, "Podaj poprawną wagę do odjęcia", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBuilder.setNegativeButton("Anuluj") { dialog, _ -> dialog.dismiss() }

        dialogBuilder.show()
    }

    private fun updateBatchWeight(batchId: String, currentWeight: Int, weightToSubtract: Int) {
        val newWeight = currentWeight - weightToSubtract
        if (newWeight >= 0) {
            databaseRef.child(batchId).child("waga").setValue(newWeight)
            updateTotalWeightInDatabase(totalWeight - weightToSubtract)
        } else {
            Toast.makeText(this, "Waga nie może być mniejsza niż 0", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTotalWeightInDatabase(newTotalWeight: Int) {
        databaseRef.parent?.child("ogolnaIlosc")?.setValue(newTotalWeight)
    }
}