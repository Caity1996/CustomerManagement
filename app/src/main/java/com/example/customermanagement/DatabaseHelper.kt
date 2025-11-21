package com.example.customermanagement

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "smtbiz"
        private const val DATABASE_VERSION = 1

        // Table and Column Names
        const val TABLE_NAME = "customer"
        const val COL_ID = "Id"
        const val COL_NAME = "Name"
        const val COL_EMAIL = "Email"
        const val COL_MOBILE = "Mobile"

        private const val TAG = "DatabaseHelper"
    }

    /**
     * This method is called when the database is created for the first time.
     */
    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_TABLE_CUSTOMER = """
            CREATE TABLE $TABLE_NAME (
                $COL_ID TEXT PRIMARY KEY, 
                $COL_NAME TEXT, 
                $COL_EMAIL TEXT, 
                $COL_MOBILE TEXT
            )
        """.trimIndent()
        db.execSQL(CREATE_TABLE_CUSTOMER)
        Log.i(TAG, "Table '$TABLE_NAME' created successfully.")

        // Populate the initial 5 records after creation (as per reset requirement)
        insertInitialRecords(db)
    }

    /**
     * This method is called when the database needs to be upgraded (version change).
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop the existing table and recreate it
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
        Log.w(TAG, "Database upgraded from version $oldVersion to $newVersion. Table reset.")
    }

    /**
     * Helper to insert the 5 initial/sample records. Used in onCreate and resetTable.
     */
    private fun insertInitialRecords(db: SQLiteDatabase) {
        val customers = listOf(
            Customer("100001", "John Citizen", "john@smt.com", "0412345678"),
            Customer("100002", "Alice Smith", "alice@smt.com", "0423456789"),
            Customer("100003", "Bob Johnson", "bob@smt.com", "0434567890"),
            Customer("100004", "Clara Lee", "clara@smt.com", "0445678901"),
            Customer("100005", "David Chen", "david@smt.com", "0456789012")
        )

        for (c in customers) {
            val values = ContentValues().apply {
                put(COL_ID, c.id)
                put(COL_NAME, c.name)
                put(COL_EMAIL, c.email)
                put(COL_MOBILE, c.mobile)
            }
            db.insert(TABLE_NAME, null, values)
        }
        Log.i(TAG, "5 sample records inserted.")
    }

    /**
     * Resets the customer table by dropping and recreating it with 5 sample records.
     * This is an excellent example of using a simple transaction.
     */
    fun resetTable(): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            // Drop existing table (DANGER: destroys all existing data)
            db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
            // Recreate table
            onCreate(db)
            db.setTransactionSuccessful()
            Log.i(TAG, "Database reset and populated with 5 records successfully.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting database: ${e.message}")
            false
        } finally {
            db.endTransaction()
            db.close() // Close the database connection
        }
    }

    // --- CRUD Operations ---

    fun insertCustomer(customer: Customer): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COL_ID, customer.id)
            put(COL_NAME, customer.name)
            put(COL_EMAIL, customer.email)
            put(COL_MOBILE, customer.mobile)
        }

        // Inserting Row
        val result = db.insert(TABLE_NAME, null, contentValues)
        db.close() // Closing database connection
        return result // Returns -1 on error, row ID on success
    }

    /**
     * Updates an existing customer record using ID as the key.
     * Demonstrates DB transaction integrity (BEGIN/SET SUCCESSFUL/END).
     */
    fun updateCustomer(customer: Customer): Int {
        val db = this.writableDatabase
        db.beginTransaction() // Start transaction
        val updatedRows = try {
            val contentValues = ContentValues().apply {
                put(COL_NAME, customer.name)
                put(COL_EMAIL, customer.email)
                put(COL_MOBILE, customer.mobile)
            }

            // Updating Row. Where clause targets the row by ID.
            val rowsAffected = db.update(
                TABLE_NAME,
                contentValues,
                "$COL_ID = ?",
                arrayOf(customer.id)
            )

            if (rowsAffected > 0) {
                db.setTransactionSuccessful()
                Log.i(TAG, "Customer ID ${customer.id} updated successfully.")
            } else {
                Log.w(TAG, "Update attempted but no rows affected for ID ${customer.id}.")
            }
            rowsAffected
        } catch (e: Exception) {
            Log.e(TAG, "Transaction failed during update: ${e.message}")
            0
        } finally {
            db.endTransaction() // End transaction
            db.close()
        }
        return updatedRows
    }

    fun deleteCustomer(id: String): Int {
        val db = this.writableDatabase
        // Deleting Row
        val result = db.delete(
            TABLE_NAME,
            "$COL_ID = ?",
            arrayOf(id)
        )
        db.close()
        return result // Returns the number of rows affected
    }

    fun searchCustomerByName(name: String): List<Customer> {
        val customerList = mutableListOf<Customer>()
        val db = this.readableDatabase

        // Use LIKE for partial matching (e.g., searching "John" finds "John Citizen")
        val selectQuery = "SELECT * FROM $TABLE_NAME WHERE $COL_NAME LIKE ?"
        // Add '%' wildcard to allow for partial name matches
        val cursor: Cursor? = db.rawQuery(selectQuery, arrayOf("%$name%"))

        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getString(it.getColumnIndexOrThrow(COL_ID))
                    val customerName = it.getString(it.getColumnIndexOrThrow(COL_NAME))
                    val email = it.getString(it.getColumnIndexOrThrow(COL_EMAIL))
                    val mobile = it.getString(it.getColumnIndexOrThrow(COL_MOBILE))

                    customerList.add(Customer(id, customerName, email, mobile))
                } while (it.moveToNext())
            }
        }
        db.close()
        return customerList
    }

    fun showAllCustomers(): List<Customer> {
        val customerList = mutableListOf<Customer>()
        val selectQuery = "SELECT * FROM $TABLE_NAME"
        val db = this.readableDatabase

        // Using db.rawQuery is simpler for demonstration
        val cursor: Cursor? = db.rawQuery(selectQuery, null)

        // The 'use' block ensures the cursor is closed, preventing memory leaks (Profiler best practice!)
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getString(it.getColumnIndexOrThrow(COL_ID))
                    val name = it.getString(it.getColumnIndexOrThrow(COL_NAME))
                    val email = it.getString(it.getColumnIndexOrThrow(COL_EMAIL))
                    val mobile = it.getString(it.getColumnIndexOrThrow(COL_MOBILE))

                    customerList.add(Customer(id, name, email, mobile))
                } while (it.moveToNext())
            }
        }
        db.close()
        return customerList
    }
}