package com.chatbuddy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.chatbuddy.service.ChatBuddyService
import com.chatbuddy.ui.theme.ChatBuddyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkPermissions()
        
        setContent {
            ChatBuddyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
    
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var showSettings by remember { mutableStateOf(false) }
    var isServiceRunning by remember { mutableStateOf(ChatBuddyService.isRunning) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat Buddy") },
                actions = {
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(android.R.drawable.ic_menu_preferences, "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Chat Buddy Character",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    if (ChatBuddyService.isRunning) {
                        ChatBuddyService.stop(context)
                    } else {
                        ChatBuddyService.start(context)
                    }
                    isRunning = ChatBuddyService.isRunning
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isServiceRunning) "Stop Service" else "Start Service")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Customize your character's appearance and reactions to notifications.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Features:",
                style = MaterialTheme.typography.titleMedium
            )
            
            Column {
                Text("• Customizable appearance (hair, skin, body, clothes)")
                Text("• Character actions (yawning, knocking, playing)")
                Text("• Expressions (happy, sad, angry, laughing)")
                Text("• Notification reactions (customizable responses)")
                Text("• Optional - reactions to messages, battery, etc.")
            }
        }
    }
}

var isRunning = false
