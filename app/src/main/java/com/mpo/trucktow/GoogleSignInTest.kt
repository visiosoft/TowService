package com.mpo.trucktow

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

/**
 * Test class for Google Sign-In integration
 * This class provides utility methods to test Google Sign-In functionality
 */
class GoogleSignInTest(private val context: Context) {
    
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    
    init {
        configureGoogleSignIn()
    }
    
    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        
        mGoogleSignInClient = GoogleSignIn.getClient(context, gso)
    }
    
    fun getGoogleSignInClient(): GoogleSignInClient {
        return mGoogleSignInClient
    }
    
    fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Log.d("GoogleSignInTest", "Sign-in successful")
            Log.d("GoogleSignInTest", "Display name: ${account.displayName}")
            Log.d("GoogleSignInTest", "Email: ${account.email}")
            Log.d("GoogleSignInTest", "Photo URL: ${account.photoUrl}")
        } catch (e: ApiException) {
            Log.w("GoogleSignInTest", "signInResult:failed code=" + e.statusCode)
        }
    }
    
    fun signOut() {
        mGoogleSignInClient.signOut()
            .addOnCompleteListener {
                Log.d("GoogleSignInTest", "Sign-out completed")
            }
    }
} 