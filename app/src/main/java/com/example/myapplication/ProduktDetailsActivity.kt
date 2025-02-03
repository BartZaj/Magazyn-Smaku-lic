package com.example.myapplication

import android.content.Intent
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProduktDetailsActivity : AppCompatActivity() {

    private lateinit var productNameTextView: TextView
    private lateinit var totalWeightTextView: TextView
    private lateinit var addBatchButton: Button
    private lateinit var batchesRecyclerView: RecyclerView

    private lateinit var databaseRef: DatabaseReference
    private lateinit var partieAdapter: PartieAdapter
    private var partieList = mutableListOf<Pair<String, Triple<Double, String, String>>>() // Waga jako Float
    private var totalWeight = 0.0

    private var uid: String = ""
    private var idProduktu: String = ""
    private var nazwaProduktu: String = ""
    private var idKategorii: String = ""
    private var jednostkaProduktu: String = ""

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

        val productRef = FirebaseDatabase.getInstance().getReference("users/$uid/kategorie/$idKategorii/produkty/$idProduktu")
        productRef.child("unit").get().addOnSuccessListener { snapshot ->
            jednostkaProduktu = snapshot.getValue(String::class.java) ?: ""
            partieAdapter = PartieAdapter(
                partieList,
                { batchId -> deleteBatch(batchId) },
                { batchId, currentWeight -> showEditBatchDialog(batchId, currentWeight) },
                jednostkaProduktu
            )
            batchesRecyclerView.layoutManager = LinearLayoutManager(this)
            batchesRecyclerView.adapter = partieAdapter
        }

        loadBatches()

        addBatchButton.setOnClickListener { showAddBatchDialog() }

        val zobaczPrzepisyButton: Button = findViewById(R.id.zobaczPrzepisyButton)
        zobaczPrzepisyButton.setOnClickListener {
            val intent = Intent(this, ListaPrzepisowActivity::class.java)
            startActivity(intent)

            // Używamy FLAG_ACTIVITY_CLEAR_TOP, aby usunąć inne aktywności w tle
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

            // Zakończenie bieżącej aktywności (aby nie wrócić do niej)
            finish()
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

    }

    private fun loadBatches() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                partieList.clear()
                totalWeight = 0.0

                val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val today = Calendar.getInstance()

                for (batchSnapshot in snapshot.children) {
                    val batchId = batchSnapshot.key ?: continue
                    val weight = batchSnapshot.child("waga").getValue(Double::class.java) ?: 0.0
                    val expiryDate = batchSnapshot.child("dataWaznosci").getValue(String::class.java) ?: "Brak daty"
                    val batchNumber = batchSnapshot.child("numerPartii").getValue(String::class.java) ?: "Brak numeru"

                    try {
                        val expiryDateCalendar = Calendar.getInstance().apply {
                            time = dateFormat.parse(expiryDate) ?: Date()
                        }
                        val daysDifference = ((expiryDateCalendar.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()

                        if (daysDifference >= 0) {
                            totalWeight += weight
                        }

                        partieList.add(batchId to Triple(weight, expiryDate, batchNumber))

                    } catch (e: Exception) {
                        Log.e("DateParsing", "Błąd parsowania daty: $expiryDate", e)
                        partieList.add(batchId to Triple(weight, "Nieprawidłowa data", "Brak numeru"))
                    }
                }

                val roundedWeight = String.format(Locale.US, "%.2f", totalWeight).toDouble()

                updateTotalWeightInDatabase(roundedWeight)

                totalWeightTextView.text = "Ogólna ilość: $roundedWeight $jednostkaProduktu"
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
        val batchNumberInput = dialogLayout.findViewById<android.widget.EditText>(R.id.batchNumberEditText)
        val weightUnitText = dialogLayout.findViewById<android.widget.TextView>(R.id.weightUnitTextView)
        val datePicker = dialogLayout.findViewById<android.widget.DatePicker>(R.id.datePicker)

        weightUnitText.text = jednostkaProduktu

        dialogBuilder.setView(dialogLayout)

        dialogBuilder.setPositiveButton("Dodaj") { _, _ ->
            val weight = weightInput.text.toString().toDoubleOrNull()
            val batchNumber = batchNumberInput.text.toString()
            val selectedDay = datePicker.dayOfMonth
            val selectedMonth = datePicker.month + 1
            val selectedYear = datePicker.year
            val selectedDate = String.format("%02d-%02d-%d", selectedDay, selectedMonth, selectedYear)

            if (weight != null && weight > 0) {
                addNewBatch(batchNumber, weight, selectedDate)
            } else {
                Toast.makeText(this, "Podaj poprawną wagę", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBuilder.setNegativeButton("Anuluj") { dialog, _ -> dialog.dismiss() }

        dialogBuilder.show()
    }

    private fun addNewBatch(batchNumber: String, weight: Double, expiryDate: String) {
        val newBatchId = databaseRef.push().key
        if (newBatchId != null) {
            val batchData = mapOf(
                "waga" to weight,
                "dataWaznosci" to expiryDate,
                "numerPartii" to batchNumber
            )
            databaseRef.child(newBatchId).setValue(batchData)

            updateTotalWeightInDatabase(totalWeight + weight)

            partieList.add(newBatchId to Triple(weight, expiryDate, batchNumber))
            partieAdapter.notifyDataSetChanged()
        }
    }
    private fun deleteBatch(batchId: String) {
        val batchWeight = partieList.find { it.first == batchId }?.second?.first ?: return
        databaseRef.child(batchId).removeValue()

        updateTotalWeightInDatabase(totalWeight - batchWeight)
    }

    private fun showEditBatchDialog(batchId: String, currentWeight: Double) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Edytuj partię")

        val dialogLayout = layoutInflater.inflate(R.layout.dialog_edit_batch, null)
        val weightInput = dialogLayout.findViewById<android.widget.EditText>(R.id.weightEditText)

        dialogBuilder.setView(dialogLayout)

        dialogBuilder.setPositiveButton("Zaktualizuj") { _, _ ->
            val weightToSubtract = weightInput.text.toString().toDoubleOrNull()
            if (weightToSubtract != null && weightToSubtract > 0) {
                updateBatchWeight(batchId, currentWeight, weightToSubtract)
            } else {
                Toast.makeText(this, "Podaj poprawną wagę do odjęcia", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBuilder.setNegativeButton("Anuluj") { dialog, _ -> dialog.dismiss() }

        dialogBuilder.show()
    }

    private fun updateBatchWeight(batchId: String, currentWeight: Double, weightToSubtract: Double) {
        val newWeight = currentWeight - weightToSubtract
        val newWeightFormatted = String.format("%.2f", newWeight).replace(',', '.').toDouble()
        if (newWeight >= 0) {
            databaseRef.child(batchId).child("waga").setValue(newWeightFormatted)
            updateTotalWeightInDatabase(totalWeight - weightToSubtract)
        } else {
            Toast.makeText(this, "Waga nie może być mniejsza niż 0", Toast.LENGTH_SHORT).show()
        }
    }


    private fun updateTotalWeightInDatabase(newTotalWeight: Double) {
        databaseRef.parent?.child("ogolnaIlosc")?.setValue(newTotalWeight)
    }
}