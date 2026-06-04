package com.elephantcos.gemmachat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elephantcos.gemmachat.data.entity.Conversation
import com.elephantcos.gemmachat.ui.theme.*
import com.elephantcos.gemmachat.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenChat: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val conversations by viewModel.conversations.collectAsState(initial = emptyList())
    var deleteTarget by remember { mutableStateOf<Conversation?>(null) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(Brush.linearGradient(listOf(AccentPurple, AccentTeal))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("G", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Text("GemmaChat", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.createConversation { id -> onOpenChat(id) } },
                containerColor = AccentPurple,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Chat", tint = Color.White)
            }
        }
    ) { padding ->
        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = TextSecondary.copy(0.4f), modifier = Modifier.size(52.dp))
                    Text("No chats yet", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text("Tap  +  to start a new conversation", color = TextSecondary.copy(0.6f), fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(conversations, key = { it.id }) { conv ->
                    ConvItem(
                        conversation = conv,
                        onClick = { onOpenChat(conv.id) },
                        onDelete = { deleteTarget = conv }
                    )
                }
            }
        }
    }

    deleteTarget?.let { conv ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = SurfaceDark,
            title = { Text("Delete Chat?", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text = { Text("\"${conv.title}\" will be permanently deleted.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteConversation(conv); deleteTarget = null }) {
                    Text("Delete", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel", color = AccentPurple) }
            }
        )
    }
}

@Composable
private fun ConvItem(conversation: Conversation, onClick: () -> Unit, onDelete: () -> Unit) {
    val fmt = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(AccentPurple.copy(0.25f), AccentTeal.copy(0.2f)))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = conversation.title.firstOrNull()?.uppercaseChar()?.toString() ?: "C",
                    color = AccentPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(conversation.title, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Text(fmt.format(Date(conversation.updatedAt)), color = TextSecondary, fontSize = 12.sp)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary.copy(0.5f))
            }
        }
    }
}
