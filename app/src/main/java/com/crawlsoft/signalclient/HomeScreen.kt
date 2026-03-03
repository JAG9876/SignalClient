package com.crawlsoft.signalclient

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.*

@Composable
fun HomeScreen() {
    var soundRecorder by remember { mutableStateOf(SoundRecorder())}
    var isRecording by remember { mutableStateOf(false ) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = {
            isRecording = !isRecording
            if (isRecording) {
                soundRecorder.init()
            } else {
                soundRecorder.stopRecording()
            }}) {
            Text(text =  if (isRecording) "Stop recording" else "Start recording")
        }
    }
}