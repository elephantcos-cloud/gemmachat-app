package com.elephantcos.gemmachat.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.elephantcos.gemmachat.ui.theme.*
import com.elephantcos.gemmachat.util.PathResolver
import java.io.File

@Composable
fun SetupScreen(onModelSelected: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var resolvedPath by remember { mutableStateOf<String?>(null) }
    var manualPath by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showManual by remember { mutableStateOf(false) }

    fun hasStoragePermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            Environment.isExternalStorageManager()
        else true

    var hasPermission by remember { mutableStateOf(hasStoragePermission()) }

    // Re-check permission when app resumes (user returns from Settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = hasStoragePermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasPermission = hasStoragePermission()
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {}
        val path = PathResolver.resolve(context, uri)
        if (path != null) {
            resolvedPath = path
            error = null
            showManual = false
        } else {
            error = "Path auto-resolve failed. Enter manually below."
            showManual = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(AccentPurple, AccentTeal))),
                contentAlignment = Alignment.Center
            ) {
                Text("G", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Text("GemmaChat", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

            Text(
                text = "Offline AI — runs entirely on your phone\nSelect your Gemma .task model file",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp
            )

            // Permission card (Android 11+ only)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1A1A))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                            Text("Storage Permission Required", color = ErrorRed, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Text(
                            "To read the model file from Downloads, grant All Files Access permission.",
                            color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp
                        )
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                intent.data = Uri.parse("package:${context.packageName}")
                                permissionLauncher.launch(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) {
                            Text("Grant Permission", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // File picker card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Model File", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)

                    if (resolvedPath != null) {
                        Text(resolvedPath!!.substringAfterLast("/"), color = AccentTeal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(resolvedPath!!, color = TextSecondary, fontSize = 11.sp)
                    } else {
                        Text("No file selected", color = TextSecondary.copy(0.5f), fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { fileLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPurple)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Browse .task file")
                    }

                    if (showManual) {
                        Divider(color = DividerColor)
                        Text("Enter path manually:", color = TextSecondary, fontSize = 12.sp)
                        OutlinedTextField(
                            value = manualPath,
                            onValueChange = { manualPath = it },
                            placeholder = { Text("/storage/emulated/0/Download/file.task", color = TextSecondary.copy(0.4f), fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentPurple,
                                unfocusedBorderColor = DividerColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = AccentPurple
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        TextButton(onClick = {
                            val f = File(manualPath.trim())
                            if (f.exists()) { resolvedPath = manualPath.trim(); error = null }
                            else error = "File not found at that path."
                        }) { Text("Use this path", color = AccentTeal) }
                    }

                    if (error != null) {
                        Text(error!!, color = ErrorRed, fontSize = 12.sp)
                    }
                }
            }

            Button(
                onClick = {
                    if (resolvedPath != null) {
                        context.getSharedPreferences("gemmachat_prefs", 0)
                            .edit().putString("model_path", resolvedPath).apply()
                        onModelSelected()
                    }
                },
                enabled = resolvedPath != null && hasPermission,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPurple,
                    disabledContainerColor = SurfaceVariant
                )
            ) {
                Text("Start Chatting →", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}
