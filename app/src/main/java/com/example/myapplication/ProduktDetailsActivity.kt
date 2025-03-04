package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
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
    private lateinit var partieAdapter: BatchAdapter
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

        idKategorii = intent.getStringExtra("idKategorii") ?: ""
        idProduktu = intent.getStringExtra("idProduktu") ?: ""
        nazwaProduktu = intent.getStringExtra("nazwaProduktu") ?: ""

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Użytkownik niezalogowany", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

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
            partieAdapter = BatchAdapter(
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
            val intent = Intent(this, RecipeListActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
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
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_batch, null)
        val weightInput = dialogView.findViewById<EditText>(R.id.weightEditText)
        val batchNumberInput = dialogView.findViewById<EditText>(R.id.batchNumberEditText)
        val weightUnitText = dialogView.findViewById<TextView>(R.id.weightUnitTextView)
        val datePicker = dialogView.findViewById<DatePicker>(R.id.datePicker)

        weightUnitText.text = jednostkaProduktu

        val dialog = AlertDialog.Builder(this)
            .setTitle("Dodaj partię")
            .setView(dialogView)
            .setPositiveButton("Dodaj") { _, _ ->
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
            .setNegativeButton("Anuluj", null)
            .create()

        dialog.show()

        val defaultBlue = ContextCompat.getColor(this, R.color.blue)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(defaultBlue)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(defaultBlue)
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
            partieAdapter.notifyDataSetChanged()
        }
    }
    private fun deleteBatch(batchId: String) {
        databaseRef.child(batchId).removeValue()
    }

    private fun showEditBatchDialog(batchId: String, currentWeight: Double) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_batch, null)
        val weightInput = dialogView.findViewById<EditText>(R.id.weightEditText)
        val weightUnitText = dialogView.findViewById<TextView>(R.id.weightUnitEditTextView)

        weightUnitText.text = jednostkaProduktu

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edytuj partię")
            .setView(dialogView)
            .setPositiveButton("Zaktualizuj") { _, _ ->
                val weightToSubtract = weightInput.text.toString().toDoubleOrNull()
                if (weightToSubtract != null && weightToSubtract > 0) {
                    updateBatchWeight(batchId, currentWeight, weightToSubtract)
                } else {
                    Toast.makeText(this, "Podaj poprawną wagę do odjęcia", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Anuluj", null)
            .create()

        dialog.show()

        val defaultBlue = ContextCompat.getColor(this, R.color.blue)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(defaultBlue)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(defaultBlue)
    }

    private fun updateBatchWeight(batchId: String, currentWeight: Double, weightToSubtract: Double) {
        val newWeight = currentWeight - weightToSubtract
        val newWeightFormatted = String.format(Locale.US, "%.2f", newWeight).toDouble()
        if (newWeight >= 0) {
            databaseRef.child(batchId).child("waga").setValue(newWeightFormatted)
        } else {
            Toast.makeText(this, "Waga nie może być mniejsza niż 0", Toast.LENGTH_SHORT).show()
        }
    }


    private fun updateTotalWeightInDatabase(newTotalWeight: Double) {
        databaseRef.parent?.child("ogolnaIlosc")?.setValue(newTotalWeight)
    }
}