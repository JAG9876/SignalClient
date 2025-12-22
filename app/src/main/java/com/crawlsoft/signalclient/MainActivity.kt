package com.crawlsoft.signalclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.crawlsoft.signalclient.ui.theme.SignalClientTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContent {
            GreetingPreview()
        }
    }
}

@Composable
fun LoginButton() {
    var buttonClickedCount by remember { mutableStateOf(0) }
    Button( onClick = {
        buttonClickedCount++
        println("Button clicked! Count: $buttonClickedCount")
    }) {
        Text( text = "Click me! Count: $buttonClickedCount")
    }
    Greeting(" your Android")
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello again you stupid $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SignalClientTheme {
        Column() {
            Greeting(" my Android")
            LoginButton()
        }
    }
}
