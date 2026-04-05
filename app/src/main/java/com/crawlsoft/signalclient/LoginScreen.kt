package com.crawlsoft.signalclient

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import android.provider.Settings


@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val credentialManager = remember {
        CredentialManager.create(context)
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Login Screen", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
                coroutineScope.launch {
                    signInWithGoogle(context, credentialManager, onLoginSuccess)
                }
            }) {
            Text(text = "Sign in with Google")
        }
    }
}

suspend fun signInWithGoogle(
    context: Context,
    credentialManager: CredentialManager,
    onLoginSuccess: () -> Unit
) {
    try {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(
            context = context,
            request = request
        )

        val credential = result.credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
            val googleIdTokenCredential =
                GoogleIdTokenCredential.createFrom(credential.data)

            val idToken = googleIdTokenCredential.idToken

            if (!GetAccessToken(idToken, context))
                throw Exception("GetAccessToken failed")
            else
                onLoginSuccess()
        }
    }
    catch (e: GetCredentialException) {
        e.printStackTrace()
    }
}

suspend fun GetAccessToken(idToken: String, context: Context): Boolean {
    //return true
    return try {
        val deviceId = "Android-" + Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID)
        val response = RetrofitClient.instance.exchangeToken(LoginRequest(idToken, deviceId))

        if (response.isSuccessful && response.body() != null) {
            val tokens = response.body()!!
            saveTokens(context = context,tokens.accessToken, tokens.refreshToken)
            true
        } else {
            // Log error body if needed: response.errorBody()?.string()
            false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

// Could be in a dedicated TokenManager class
fun saveTokens(context: Context, accessToken: String, refreshToken: String) {
    val sharedPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    sharedPrefs.edit().apply {
        putString("access_token", accessToken)
        putString("refresh_token", refreshToken)
        apply()
    }
}