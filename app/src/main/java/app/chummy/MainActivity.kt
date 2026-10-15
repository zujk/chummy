package app.chummy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val Paper = Color.White
private val Ink = Color(0xFF1C1C1E)
private val Muted = Color(0xFF8E8E93)
private val Line = Color(0xFFE5E5EA)
private val Pink = Color(0xFFF2609E)
private val Late = Color(0xFFE5484D)
private val Soon = Color(0xFFF08C00)

private val AvatarColors = listOf(
    0xFF8E44D9 to 0xFFB05BD6, 0xFFB8A23A to 0xFFC9B24A, 0xFF2EBF7A to 0xFF4FD1A0,
    0xFFA9C43A to 0xFF7CC243, 0xFFC0485E to 0xFFD45F6F, 0xFF5B4FD6 to 0xFF7A5CE0,
    0xFF2E8FC9 to 0xFF4BB0D6,
)

private val DateFmt = DateTimeFormatter.ofPattern("EEE, MMM d")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = Store(this)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Pink, onPrimary = Paper, background = Paper, surface = Paper, onSurface = Ink,
                )
            ) {
                App(store)
            }
        }
    }
}

@Composable
fun App(store: Store) {
    var friends by remember { mutableStateOf(store.load()) }
    var adding by remember { mutableStateOf(false) }
    var openId by remember { mutableStateOf<Long?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun update(new: List<Friend>) { friends = new; store.save(new) }
    fun replace(f: Friend) = update(friends.map { if (it.id == f.id) f else it })

    val today = today()
    val ranked = friends.sortedByDescending { it.score(today) }

    Scaffold(
        containerColor = Paper,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }, containerColor = Pink, contentColor = Paper, shape = CircleShape) {
                Text("+", fontSize = 32.sp, fontWeight = FontWeight.Light)
            }
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 12.dp)) {
                    Text("Chummy", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Most overdue first.", color = Muted, fontSize = 15.sp)
                }
            }
            items(ranked, key = { it.id }) { f ->
                FriendRow(f, today) { openId = f.id }
                HorizontalDivider(color = Line, modifier = Modifier.padding(start = 88.dp, end = 20.dp))
            }
            if (friends.isEmpty()) {
                item {
                    Text(
                        "No one yet. Tap + to add someone you want to keep in touch with.",
                        color = Muted, modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
    }

    if (adding) {
        AddDialog(
            onDismiss = { adding = false },
            onAdd = { name, cadence ->
                // New friends start out due today.
                update(friends + Friend(System.currentTimeMillis(), name, cadence, today - cadence.days))
                adding = false
            },
        )
    }

    friends.firstOrNull { it.id == openId }?.let { f ->
        FriendDialog(
            f, today,
            onDismiss = { openId = null },
            onLog = {
                replace(f.copy(last = today))
                openId = null
                scope.launch {
                    val r = snackbar.showSnackbar("Caught up with ${f.name}", "Undo", duration = SnackbarDuration.Short)
                    if (r == SnackbarResult.ActionPerformed) {
                        friends.firstOrNull { it.id == f.id }?.let { replace(it.copy(last = f.last)) }
                    }
                }
            },
            onCadence = { replace(f.copy(cadence = it)) },
            onRemove = { update(friends.filter { it.id != f.id }); openId = null },
        )
    }
}

@Composable
fun FriendRow(f: Friend, today: Long, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(f.name)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(f.name, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            CadenceChip(f.cadence)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(LocalDate.ofEpochDay(f.due).format(DateFmt), fontSize = 16.sp, color = Ink)
            Spacer(Modifier.height(4.dp))
            val score = f.score(today)
            Text(
                relative(f.due - today), fontSize = 14.sp,
                color = when { score >= 1 -> Late; score >= 0.75 -> Soon; else -> Muted },
            )
        }
    }
}

private fun relative(days: Long) = when {
    days == 0L -> "today"
    days == 1L -> "tomorrow"
    days > 0 -> "in $days days"
    days == -1L -> "1 day past"
    else -> "${-days} days past"
}

private fun initials(name: String) =
    name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

@Composable
fun Avatar(name: String) {
    val (a, b) = AvatarColors[Math.floorMod(name.hashCode(), AvatarColors.size)]
    Box(
        Modifier.size(54.dp).clip(CircleShape).background(Brush.verticalGradient(listOf(Color(b), Color(a)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials(name), color = Paper, fontSize = 20.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CadenceChip(c: Cadence, selected: Boolean = true, onClick: (() -> Unit)? = null) {
    val color = Color(c.color)
    Text(
        c.label,
        color = if (selected) color else Muted,
        fontSize = 14.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) color.copy(alpha = 0.14f) else Line.copy(alpha = 0.5f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

@Composable
fun CadencePicker(selected: Cadence, onPick: (Cadence) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Cadence.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { c -> CadenceChip(c, c == selected) { onPick(c) } }
            }
        }
    }
}

@Composable
fun AddDialog(onDismiss: () -> Unit, onAdd: (String, Cadence) -> Unit) {
    var name by remember { mutableStateOf("") }
    var cadence by remember { mutableStateOf(Cadence.MONTHLY) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Paper,
        title = { Text("New friend") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Text("Keep in touch", color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                CadencePicker(cadence) { cadence = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name.trim(), cadence) }, enabled = name.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Muted) } },
    )
}

@Composable
fun FriendDialog(
    f: Friend, today: Long,
    onDismiss: () -> Unit, onLog: () -> Unit, onCadence: (Cadence) -> Unit, onRemove: () -> Unit,
) {
    var confirmRemove by remember { mutableStateOf(false) }
    val ago = today - f.last
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Paper,
        title = { Text(f.name) },
        text = {
            Column {
                Text(
                    "Last caught up " + when (ago) {
                        0L -> "today"
                        1L -> "yesterday"
                        else -> "$ago days ago"
                    } + " · ${LocalDate.ofEpochDay(f.last).format(DateFmt)}",
                    color = Muted, fontSize = 14.sp,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onLog,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                ) { Text("Log catchup today", fontSize = 16.sp) }
                Spacer(Modifier.height(20.dp))
                Text("Cadence", color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                CadencePicker(f.cadence, onCadence)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = {
            if (confirmRemove) TextButton(onClick = onRemove) { Text("Really remove?", color = Late) }
            else TextButton(onClick = { confirmRemove = true }) { Text("Remove", color = Muted) }
        },
    )
}
