package com.crawlsoft.signalclient

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.crawlsoft.signalclient.ui.theme.SignalClientTheme
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        const val WEB_CLIENT_ID = "935891679520-ur0sfnqsi9kchrtlhtmefdnsqup9771s.apps.googleusercontent.com"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContent {
            LoginScreen(this)
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun LoginScreen(context: Context = this) {
        var loggedInState by remember { mutableIntStateOf(0) }
        val account = MutableString("")

        SignalClientTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button( enabled = loggedInState == 0, onClick = {
                    loggedInState = 1
                    println("Button clicked! loggedIn state: $loggedInState")
                }) {
                    when (loggedInState) {
                        2 -> Text(text = "Logged in")
                        1 -> {
                            Text(text = "Logging in")
                            googleLogin(context, account)
                            //Account(per)
                        }
                        else -> Text(text = "Login")
                    }
                }
                Account(account.value)
            }
        }
    }

    @Composable
    fun Account(emailAddress: String, modifier: Modifier = Modifier) {
        Text(
            text = "Google account: $emailAddress",
            modifier = modifier
        )
    }

    fun googleLogin(context: Context, emailAccount: MutableString) {
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(WEB_CLIENT_ID)
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        val credentialManager = CredentialManager.create(context)

        lifecycleScope.launch {
            flow {
                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )
                //**// Boom! NoCredentialException: No credentials available**

                emit(result.credential)
            }.catch { e ->
                if(e is NoCredentialException) {
                    Log.e("TAG", "loginSuccess NoCredentialException", e)
                }
            }.collect {
                emailAccount.value = loginSuccess(it)
            }
        }
    }

    private fun loginSuccess(credential: androidx.credentials.Credential): String {
        var emailAccount: String
        emailAccount = ""
        when(credential) {
            is PasswordCredential -> {
                val password = credential.password
                val id = credential.id
                Log.e("TAG", "loginSuccess id: $id password: $password")
            }
            is GoogleIdTokenCredential -> {
                val idToken = credential.idToken
                Log.e("TAG", "loginSuccess idToken: $idToken")
            }
            is PublicKeyCredential -> {
                Log.e("TAG", "loginSuccess PublicKeyCredential")
            }
            is CustomCredential -> {

                if(credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

                        // Send a get access request to server

                        // googleIdTokenCredential.id is the account / email address
                        emailAccount = googleIdTokenCredential.id
                        Log.e("TAG", "loginSuccess idToken: ${googleIdTokenCredential.idToken}  ${googleIdTokenCredential.displayName}")
                    } catch (e: GoogleIdTokenParsingException) {
                        e.printStackTrace()
                    }
                }
                else {
                    Log.e("TAG", "Unexcepted CustomCredential type: ${credential.type}")
                }
            }
        }
        return emailAccount
    }
}

class MutableString(var value: String)
