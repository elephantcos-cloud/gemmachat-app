package com.elephantcos.gemmachat.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elephantcos.gemmachat.data.entity.Message
import com.elephantcos.gemmachat.ui.theme.*
import com.elephantcos.gemmachat.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: Long,
    onBack: () -> Unit,
    vm: ChatViewModel = viewModel(factory = ChatViewModel.Factory(conversationId))
) {
    val context       = LocalContext.current
    val messages      by vm.messages.collectAsState(initial = emptyList())
    val isGenerating  by vm.isGenerating.collectAsState()
    val streamText    by vm.streamingText.collectAsState()
    val title         by vm.conversationTitle.collectAsState()
    val error         by vm.error.collectAsState()
    val modelLoading  by vm.isModelLoading.collectAsState()

    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope     = rememberCoroutineScope()
    var initialScrolled by remember { mutableStateOf(false) }

    // Initial scroll to bottom when messages first load
    LaunchedEffect(messages.size) {
        if (!initialScrolled && messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
            initialScrolled = true
        }
    }
    // Keep scrolling during streaming
    LaunchedEffect(streamText) {
        if (streamText.isNotEmpty()) {
            val idx = messages.size // streaming item comes after last message
            listState.animateScrollToItem(idx)
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                title = {
                    Column {
                        Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        when {
                            modelLoading -> Text("Loading model…", color = AccentTeal, fontSize = 11.sp)
                            isGenerating -> Text("Generating…", color = AccentPurple, fontSize = 11.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(SurfaceDark)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                if (error != null) {
                    Text(error!!, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message…", color = TextSecondary.copy(0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = AccentPurple,
                            unfocusedBorderColor = DividerColor,
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary,
                            cursorColor          = AccentPurple,
                            focusedContainerColor   = SurfaceVariant,
                            unfocusedContainerColor = SurfaceVariant
                        ),
                        shape = RoundedCornerShape(20.dp),
                        maxLines = 5
                    )
                    val canSend = !isGenerating && !modelLoading && input.isNotBlank()
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSend) Brush.linearGradient(listOf(AccentPurple, AccentTeal))
                                else Brush.linearGradient(listOf(SurfaceVariant, SurfaceVariant))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                val text = input.trim()
                                if (text.isNotBlank()) {
                                    vm.sendMessage(text)
                                    input = ""
                                    scope.launch { listState.animateScrollToItem(messages.size) }
                                }
                            },
                            enabled = canSend
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (canSend) Color.White else TextSecondary.copy(0.3f)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MsgBubble(
                    message = msg,
                    onCopy = { text ->
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("GemmaChat", text))
                    },
                    onShare = { text ->
                        val i = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(i, "Share via"))
                    }
                )
            }
            if (streamText.isNotEmpty()) {
                item { StreamBubble(text = streamText) }
            }
            if (isGenerating && streamText.isEmpty()) {
                item { TypingBubble() }
            }
        }
    }
}

@Composable
private fun MsgBubble(message: Message, onCopy: (String) -> Unit, onShare: (String) -> Unit) {
    val isUser = message.role == "user"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp, topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 5.dp,
                        bottomEnd   = if (isUser) 5.dp  else 20.dp
                    )
                )
                .background(if (isUser) UserBubble else AiBubble)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(message.content, color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
        }
        if (!isUser) {
            Row(modifier = Modifier.padding(start = 4.dp, top = 2.dp)) {
                ActionBtn(Icons.Default.ContentCopy, "Copy")  { onCopy(message.content) }
                ActionBtn(Icons.Default.Share,        "Share") { onShare(message.content) }
            }
        }
    }
}

@Composable
private fun ActionBtn(icon: ImageVector, label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary.copy(0.7f))
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(3.dp))
        Text(label, fontSize = 11.sp)
    }
}

@Composable
private fun StreamBubble(text: String) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 5.dp, bottomEnd = 20.dp))
                .background(AiBubble)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text("$text▍", color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun TypingBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 5.dp, bottomEnd = 20.dp))
            .background(AiBubble)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(3) { i ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccentTeal.copy(alpha = alpha * (0.6f + i * 0.2f)))
                )
            }
        }
    }
}
