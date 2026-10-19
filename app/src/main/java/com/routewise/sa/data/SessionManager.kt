package com.routewise.sa.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("routewise_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_PROVINCE = "user_province"
        private const val KEY_USER_CITY = "user_city"
    }

    // --- SAVE LOGIN SESSION ---
    fun saveUser(
        uid: String,
        email: String,
        name: String,
        phone: String = "",
        province: String = "Gauteng",
        city: String = "Johannesburg"
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, uid)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_PHONE, phone)
            putString(KEY_USER_PROVINCE, province)
            putString(KEY_USER_CITY, city)
            apply()
        }
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun getUserId(): String = prefs.getString(KEY_USER_ID, "") ?: ""
    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""
    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""
    fun getUserPhone(): String = prefs.getString(KEY_USER_PHONE, "") ?: ""
    fun getUserProvince(): String = prefs.getString(KEY_USER_PROVINCE, "Gauteng") ?: ""
    fun getUserCity(): String = prefs.getString(KEY_USER_CITY, "Johannesburg") ?: ""

    // --- CLEAR LOGOUT SESSION ---
    fun logout() {
        prefs.edit().clear().apply()
    }
}
