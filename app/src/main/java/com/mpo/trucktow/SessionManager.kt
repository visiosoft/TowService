package com.mpo.trucktow

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREF_NAME = "TruckTowSession"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_USER_EMAIL = "userEmail"
        private const val KEY_USER_PASSWORD = "userPassword"
        private const val KEY_AUTO_LOGIN = "autoLogin"
    }
    
    fun saveLoginCredentials(email: String, password: String) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY_USER_EMAIL, email)
        editor.putString(KEY_USER_PASSWORD, password)
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putBoolean(KEY_AUTO_LOGIN, true)
        editor.apply()
    }
    
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    fun isAutoLoginEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_AUTO_LOGIN, false)
    }
    
    fun getSavedEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }
    
    fun getSavedPassword(): String? {
        return sharedPreferences.getString(KEY_USER_PASSWORD, null)
    }
    
    fun logout() {
        val editor = sharedPreferences.edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, false)
        editor.putBoolean(KEY_AUTO_LOGIN, false)
        editor.remove(KEY_USER_EMAIL)
        editor.remove(KEY_USER_PASSWORD)
        editor.apply()
    }
    
    fun clearCredentials() {
        val editor = sharedPreferences.edit()
        editor.remove(KEY_USER_EMAIL)
        editor.remove(KEY_USER_PASSWORD)
        editor.apply()
    }

    fun isGoogleSignIn(): Boolean {
        val password = getSavedPassword()
        return password == "google_sign_in"
    }
} 