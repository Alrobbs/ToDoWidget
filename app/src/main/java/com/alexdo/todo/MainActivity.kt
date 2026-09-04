package com.alexdo.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import androidx.compose.foundation.isSystemInDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            ) {
                Surface(modifier = Modifier.fillMaxSize()) { App() }
            }
        }
    }
}

@Composable
private fun App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { TodoDb.get(context).dao() }

    var offset by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

    val date = LocalDate.now().plusDays(offset.toLong())
    val iso = Fmt.iso(date)

    val todos by remember(iso) { dao.watch(iso) }.collectAsState(initial = emptyList())
    val settings by remember { Settings.flow(context) }.collectAsState(initial = WidgetSettings())

    val open = todos.filter { !it.completed }
    val done = todos.filter { it.completed }

    fun refreshWidget() = scope.launch { TodoWidget().updateAll(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .padding(horizontal = 20.dp)
    ) {

        // ---- Header, mirroring the widget ----
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
            IconButton(onClick = { offset-- }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "To do",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    Fmt.label(date),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            IconButton(onClick = { offset++ }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next day")
            }
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }

        Spacer(Modifier.height(6.dp))
        Divider()
        Spacer(Modifier.height(4.dp))

        if (offset != 0) {
            TextButton(onClick = { offset = 0 }) { Text("Back to today") }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {

            items(open, key = { it.id }) { t ->
                TodoRow(
                    todo = t,
                    onToggle = {
                        scope.launch { dao.setDone(t.id, true); refreshWidget() }
                    },
                    onDelete = {
                        scope.launch { dao.delete(t.id); refreshWidget() }
                    }
                )
            }

            if (done.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Completed",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Divider()
                }
                items(done, key = { it.id }) { t ->
                    TodoRow(
                        todo = t,
                        onToggle = {
                            scope.launch { dao.setDone(t.id, false); refreshWidget() }
                        },
                        onDelete = {
                            scope.launch { dao.delete(t.id); refreshWidget() }
                        }
                    )
                }
            }

            if (open.isEmpty() && done.isEmpty()) {
                item {
                    Spacer(Modifier.height(32.dp))
                    Text(
                        "Add something for ${Fmt.label(date).lowercase()}.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ---- Add ----
        val submit = {
            val text = draft.trim()
            if (text.isNotEmpty()) {
                scope.launch {
                    dao.insert(Todo(text = text, date = iso))
                    refreshWidget()
                }
                draft = ""
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("New to-do") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = submit) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            current = settings,
            onDismiss = { showSettings = false },
            onChange = { s ->
                scope.launch {
                    Settings.save(context, s)
                    TodoWidget().updateAll(context)
                }
            }
        )
    }
}

@Composable
private fun TodoRow(todo: Todo, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(onClick = onToggle) {
            Icon(
                if (todo.completed) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = if (todo.completed) "Mark not done" else "Mark done"
            )
        }
        Text(
            text = todo.text,
            modifier = Modifier.weight(1f),
            textDecoration = if (todo.completed) TextDecoration.LineThrough else null,
            color = if (todo.completed) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SettingsDialog(
    current: WidgetSettings,
    onDismiss: () -> Unit,
    onChange: (WidgetSettings) -> Unit
) {
    var s by remember { mutableStateOf(current) }

    LaunchedEffect(s) {
        if (s != current) {
            delay(100)
            onChange(s)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Widget settings") },
        text = {
            Column {
                Text(
                    "Changes apply to the widget automatically.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text("Text size", fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(14 to "Compact", 16 to "Standard", 18 to "Large")
                        .forEach { (value, label) ->
                            FilterChip(
                                selected = s.fontSizeSp == value,
                                onClick = { s = s.copy(fontSizeSp = value) },
                                label = { Text(label) }
                            )
                        }
                }

                Spacer(Modifier.height(8.dp))
                Text("Alignment", fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Align.START to "Left", Align.CENTER to "Center", Align.END to "Right")
                        .forEach { (value, label) ->
                            FilterChip(
                                selected = s.align == value,
                                onClick = { s = s.copy(align = value) },
                                label = { Text(label) }
                            )
                        }
                }

                Spacer(Modifier.height(12.dp))
                Text("Background dim: ${s.backgroundDim}%", fontSize = 13.sp)
                Slider(
                    value = s.backgroundDim.toFloat(),
                    onValueChange = { s = s.copy(backgroundDim = it.toInt()) },
                    valueRange = 0f..70f,
                    steps = 6
                )

                ToggleRow("Dark text", s.darkText) { s = s.copy(darkText = it) }
                ToggleRow("Line under header", s.showDivider) { s = s.copy(showDivider = it) }
                ToggleRow("Ask \"Completed?\"", s.confirmOnComplete) { s = s.copy(confirmOnComplete = it) }
            }
        }
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    ) {
        Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
