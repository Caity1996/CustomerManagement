package com.example.customermanagement

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import android.widget.Button
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.example.customermanagement.R

class MainActivity : AppCompatActivity() {

    // 1. Declare the Database Helper instance
    private lateinit var dbHelper: DatabaseHelper

    // 2. Declare all required UI elements
    private lateinit var etCustomerId: TextInputEditText
    private lateinit var etCustomerName: TextInputEditText
    private lateinit var etCustomerEmail: TextInputEditText
    private lateinit var etCustomerMobile: TextInputEditText
    private lateinit var tvResults: TextView
    private lateinit var btnInsert: Button
    private lateinit var btnUpdate: Button
    private lateinit var btnDelete: Button
    private lateinit var btnSearch: Button
    private lateinit var btnShowAll: Button
    private lateinit var btnReset: Button

    // Tag for logging
    private val TAG = "MainActivity"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Set system bar padding on the root view (R.id.main in activity_main.xml)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 3. Initialize the Database Helper
        dbHelper = DatabaseHelper(this)

        // 4. Initialize UI Elements
        etCustomerId = findViewById(R.id.etCustomerId)
        etCustomerName = findViewById(R.id.etCustomerName)
        etCustomerEmail = findViewById(R.id.etCustomerEmail)
        etCustomerMobile = findViewById(R.id.etCustomerMobile)
        tvResults = findViewById(R.id.tvResults)

        btnInsert = findViewById(R.id.btnInsert)
        btnUpdate = findViewById(R.id.btnUpdate)
        btnDelete = findViewById(R.id.btnDelete)
        btnSearch = findViewById(R.id.btnSearch)

        // CORRECTION: Ensure btnShowAll is mapped to its correct ID
        btnShowAll = findViewById(R.id.btnShowAll)
        btnReset = findViewById(R.id.btnReset)

        // 5. Setup Listeners
        setupButtonListeners()

        // Show all customers on startup to check initial state (T01 verification)
        showAllCustomers()
    }

    /**
     * Helper function to get customer data from input fields.
     */
    private fun getCustomerFromInput(): Customer? {
        val id = etCustomerId.text.toString().trim()
        val name = etCustomerName.text.toString().trim()
        val email = etCustomerEmail.text.toString().trim()
        val mobile = etCustomerMobile.text.toString().trim()

        // Primary Key validation
        if (id.isEmpty() || id.length != 6) {
            Snackbar.make(tvResults, "ID must be exactly 6 digits (Primary Key).", Snackbar.LENGTH_LONG).show()
            return null
        }

        // Basic check for other fields during insert/update
        if (name.isEmpty() || email.isEmpty() || mobile.isEmpty()) {
            Snackbar.make(tvResults, "All customer fields must be filled for Insert/Update.", Snackbar.LENGTH_LONG).show()
            return null
        }

        return Customer(id, name, email, mobile)
    }

    /**
     * Helper function to display all customer records in the results TextView.
     * This method is targeted by the CPU Profiler requirement (T08/T09).
     */
    private fun showAllCustomers() {
        Log.i(TAG, "Starting showAllCustomers operation.")
        val customers = dbHelper.showAllCustomers()

        if (customers.isEmpty()) {
            tvResults.text = "No customers found in the database."
        } else {
            val resultText = StringBuilder("--- ALL CUSTOMER RECORDS (${customers.size}) ---\n")
            // Efficiently build the string for display
            customers.forEach {
                resultText.append(it.toString()).append("\n")
            }
            tvResults.text = resultText.toString()
        }
        Log.i(TAG, "Completed showAllCustomers. Displaying ${customers.size} records.")
    }

    /**
     * Setup the click handlers for all buttons.
     */
    private fun setupButtonListeners() {

        // --- INSERT OPERATION (T02, T07 Debugger Demonstration) ---
        btnInsert.setOnClickListener {
            val customer = getCustomerFromInput()
            if (customer != null) {
                // T07 Debugger: Set a breakpoint on the next line to inspect the 'customer' object
                // and the ContentValues inside the dbHelper method before DB commit.
                val rowId = dbHelper.insertCustomer(customer)
                if (rowId > 0) {
                    Snackbar.make(it, "Customer ID ${customer.id} Inserted Successfully!", Snackbar.LENGTH_LONG).show()
                } else {
                    Snackbar.make(it, "Error: Could not insert customer. ID might already exist.", Snackbar.LENGTH_LONG).show()
                }
                clearInputFields(false)
                showAllCustomers() // Refresh display
            }
        }

        // --- UPDATE OPERATION (T05 Transaction Test) ---
        btnUpdate.setOnClickListener {
            val customer = getCustomerFromInput()
            if (customer != null) {
                // updateCustomer uses a database transaction (T05)
                val rowsAffected = dbHelper.updateCustomer(customer)
                if (rowsAffected > 0) {
                    Snackbar.make(it, "Customer ID ${customer.id} Updated Successfully (Rows: $rowsAffected)!", Snackbar.LENGTH_LONG).show()
                } else {
                    Snackbar.make(it, "Error: Update failed. Customer ID not found.", Snackbar.LENGTH_LONG).show()
                }
                clearInputFields(true)
                showAllCustomers() // Refresh display
            }
        }

        // --- DELETE OPERATION (T06) ---
        btnDelete.setOnClickListener {
            val idToDelete = etCustomerId.text.toString().trim()
            if (idToDelete.isNotEmpty()) {
                val rowsAffected = dbHelper.deleteCustomer(idToDelete)
                if (rowsAffected > 0) {
                    Snackbar.make(it, "Customer ID $idToDelete Deleted Successfully!", Snackbar.LENGTH_LONG).show()
                } else {
                    Snackbar.make(it, "Error: Delete failed. Customer ID not found.", Snackbar.LENGTH_LONG).show()
                }
                clearInputFields(true)
                showAllCustomers() // Refresh display
            } else {
                Snackbar.make(it, "Please enter the ID of the customer to delete.", Snackbar.LENGTH_LONG).show()
            }
        }

        // --- SEARCH OPERATION (T03, T04 Friendly Message) ---
        btnSearch.setOnClickListener {
            val nameToSearch = etCustomerName.text.toString().trim()
            if (nameToSearch.isNotEmpty()) {
                val results = dbHelper.searchCustomerByName(nameToSearch)
                if (results.isNotEmpty()) {
                    val resultText = StringBuilder("--- SEARCH RESULTS for '$nameToSearch' (${results.size}) ---\n")
                    results.forEach { resultText.append(it.toString()).append("\n") }
                    tvResults.text = resultText.toString()
                    Snackbar.make(it, "${results.size} customer(s) found.", Snackbar.LENGTH_LONG).show()
                } else {
                    // Friendly message implementation (T04)
                    tvResults.text = "No customers matched '$nameToSearch'. Please try a different name."
                    Snackbar.make(it, "Customer '$nameToSearch' not found.", Snackbar.LENGTH_LONG).show()
                }
            } else {
                Snackbar.make(it, "Please enter a Name to search for.", Snackbar.LENGTH_LONG).show()
            }
        }

        // --- SHOW ALL OPERATION (T09 Profiler Target) ---
        btnShowAll.setOnClickListener {
            showAllCustomers()
            Snackbar.make(it, "All records retrieved and displayed.", Snackbar.LENGTH_SHORT).show()
        }

        // --- RESET OPERATION (T01) ---
        btnReset.setOnClickListener {
            val success = dbHelper.resetTable()
            if (success) {
                Snackbar.make(it, "Database table reset and 5 sample records inserted.", Snackbar.LENGTH_LONG).show()
                clearInputFields(true) // Clear all fields after reset
                showAllCustomers() // Refresh display
            } else {
                Snackbar.make(it, "ERROR: Failed to reset the database.", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Clears the input fields after a successful operation.
     * @param clearId Clears the ID field if true. (Should be false for Delete/Search/Update by ID prep)
     */
    private fun clearInputFields(clearId: Boolean) {
        if (clearId) etCustomerId.setText("")
        etCustomerName.setText("")
        etCustomerEmail.setText("")
        etCustomerMobile.setText("")
    }
}