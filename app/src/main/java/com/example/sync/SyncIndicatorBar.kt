package com.example.sync

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.CreditGreenBg
import com.example.ui.theme.DebitRed
import com.example.ui.theme.DebitRedBg

@Composable
fun SyncIndicatorBar(
    syncState: SyncEngineState,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // Animated rotation for syncing
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(syncState.isSyncing) {
        if (syncState.isSyncing) {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            rotation.snapTo(0f)
        }
    }

    val (bgColor, contentColor, statusText, statusIcon) = when (syncState.status) {
        ConnectionStatus.ONLINE -> {
            if (syncState.isSyncing) {
                Tuple4(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                    "Syncing with Cloud...",
                    Icons.Default.Sync
                )
            } else if (syncState.pendingUploads > 0) {
                Tuple4(
                    Color(0xFFFFF3E0),
                    Color(0xFFE65100),
                    "${syncState.pendingUploads} Pending Sync Items",
                    Icons.Default.CloudQueue
                )
            } else {
                Tuple4(
                    CreditGreenBg,
                    CreditGreen,
                    "Cloud Synced • Offline Ready",
                    Icons.Default.CloudDone
                )
            }
        }
        ConnectionStatus.SYNCING -> {
            Tuple4(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer,
                "Syncing with Cloud...",
                Icons.Default.Sync
            )
        }
        ConnectionStatus.OFFLINE -> {
            Tuple4(
                DebitRedBg,
                DebitRed,
                "Offline Mode • Local Saved",
                Icons.Default.WifiOff
            )
        }
        ConnectionStatus.WAITING -> {
            Tuple4(
                Color(0xFFFFF8E1),
                Color(0xFFF57F17),
                "Waiting for Network...",
                Icons.Default.CloudQueue
            )
        }
        ConnectionStatus.SYNC_FAILED -> {
            Tuple4(
                DebitRedBg,
                DebitRed,
                "Sync Failed • Tap to Retry",
                Icons.Default.ErrorOutline
            )
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sync_indicator_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = "Sync Status",
                            tint = contentColor,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(if (syncState.isSyncing) rotation.value else 0f)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = contentColor
                        )
                        val lastSyncStr = if (syncState.lastSyncTime == 0L) "Never" else {
                            val diffSec = (System.currentTimeMillis() - syncState.lastSyncTime) / 1000
                            if (diffSec < 10) "Just now" else "${diffSec / 60}m ago"
                        }
                        Text(
                            text = "Last sync: $lastSyncStr • Local Cache Active",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = contentColor.copy(alpha = 0.8f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSyncNow,
                        enabled = !syncState.isSyncing,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("sync_now_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Now",
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand details",
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pending Uploads:", style = MaterialTheme.typography.bodySmall)
                            Text("${syncState.pendingUploads} items", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Realtime Devices Connected:", style = MaterialTheme.typography.bodySmall)
                            Text("${syncState.activeDevices.size} active", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Conflict Resolution:", style = MaterialTheme.typography.bodySmall)
                            Text("Auto (Newest Wins)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = CreditGreen))
                        }

                        if (!syncState.errorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Notice: ${syncState.errorMessage}",
                                style = MaterialTheme.typography.bodySmall.copy(color = DebitRed, fontSize = 11.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
