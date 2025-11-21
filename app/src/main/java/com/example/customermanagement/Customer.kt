package com.example.customermanagement

data class Customer(
    // ID is the Primary Key (6 digits)
    val id: String,
    // Other customer details
    var name: String,
    var email: String,
    var mobile: String
) {
    /**
     * Overrides toString() to provide a formatted string for easy display
     * in the results TextView.
     */
    override fun toString(): String {
        return "ID: $id\nName: $name\nEmail: $email\nMobile: $mobile\n" +
                "----------------------------------------"
    }
}
