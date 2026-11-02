package com.example.mathnotes

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color as UiColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  window.statusBarColor = Color.TRANSPARENT
  window.navigationBarColor = Color.TRANSPARENT
  window.isStatusBarContrastEnforced = false
  window.isNavigationBarContrastEnforced = false
  WindowCompat.getInsetsController(window, window.decorView).apply {
   isAppearanceLightStatusBars = false
   isAppearanceLightNavigationBars = false
  }
  val dao = Room.databaseBuilder(applicationContext, NotesDatabase::class.java, "notes.db")
   .addMigrations(NotesDatabase.MIGRATION_1_2).build().notesDao()
  setContent {
   MaterialTheme(colorScheme = darkColorScheme()) {
    Box(Modifier.fillMaxSize().background(UiColor(0xFF141218)).safeDrawingPadding()) {
     NotesScreen(dao)
    }
   }
  }
 }
}

@Composable
fun NotesScreen(dao: NotesDao) {
 val scope = rememberCoroutineScope()
 val notes by dao.observeAll().collectAsState(initial = emptyList())
 val snackbar = remember { SnackbarHostState() }
 var selected by remember { mutableStateOf<Note?>(null) }
 var search by remember { mutableStateOf("") }
 var deleting by remember { mutableStateOf(false) }
 val context = LocalContext.current
 val clipboard = LocalClipboardManager.current

 if (selected == null) {
  val filtered = remember(notes, search) {
   notes.filter { it.title.contains(search, ignoreCase = true) || it.body.contains(search, ignoreCase = true) }
  }
  Scaffold(
   snackbarHost = { SnackbarHost(snackbar) },
   topBar = {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
     Text("Math Notes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
     Spacer(Modifier.height(8.dp))
     OutlinedTextField(
      value = search, onValueChange = { search = it },
      modifier = Modifier.fillMaxWidth(), singleLine = true,
      placeholder = { Text("Search titles and notes") },
      trailingIcon = { if (search.isNotEmpty()) TextButton(onClick = { search = "" }) { Text("Clear") } },
      shape = RoundedCornerShape(16.dp)
     )
     Text("${filtered.size} note${if(filtered.size == 1) "" else "s"}  ·  Type = to calculate",
      style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(top = 8.dp))
    }
   },
   floatingActionButton = {
    FloatingActionButton(onClick = {
     scope.launch {
      val id = withContext(Dispatchers.IO) { dao.insert(Note()) }
      selected = Note(id = id)
     }
    }) { Text("+", style = MaterialTheme.typography.headlineMedium) }
   }
  ) { insets ->
   if (filtered.isEmpty()) {
    Box(Modifier.fillMaxSize().padding(insets).padding(24.dp)) {
     Text(if (search.isBlank()) "No notes yet. Tap + to create one." else "No matching notes.",
      color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
   } else {
    LazyColumn(Modifier.fillMaxSize().padding(insets),
     contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp),
     verticalArrangement = Arrangement.spacedBy(10.dp)) {
     items(filtered, key = { it.id }) { note ->
      ElevatedCard(onClick = { selected = note }, modifier = Modifier.fillMaxWidth(),
       shape = RoundedCornerShape(18.dp)) {
       Column(Modifier.padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
         Text(note.title.ifBlank { "Untitled" }, fontWeight = FontWeight.SemiBold,
          modifier = Modifier.weight(1f))
         if (note.pinned) Text("📌")
        }
        if (note.body.isNotBlank()) {
         Spacer(Modifier.height(6.dp))
         Text(note.body, maxLines = 5, overflow = TextOverflow.Ellipsis,
          color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
       }
      }
     }
    }
   }
  }
 } else {
  val note = selected!!
  key(note.id) {
   var title by remember { mutableStateOf(note.title) }
   var body by remember { mutableStateOf(TextFieldValue(note.body, selection = TextRange(note.body.length))) }
   var pinned by remember { mutableStateOf(note.pinned) }
   val snapshot = { Note(note.id, title, body.text, System.currentTimeMillis(), pinned) }
   LaunchedEffect(title, body.text, pinned) {
    delay(350)
    withContext(Dispatchers.IO) { dao.update(snapshot()) }
   }
   fun goBack() {
    val current = snapshot()
    scope.launch {
     withContext(Dispatchers.IO) { dao.update(current) }
     selected = null
    }
   }
   BackHandler { goBack() }
   Scaffold(
    topBar = {
     Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween) {
      TextButton(onClick = { goBack() }) { Text("← Notes") }
      TextButton(onClick = { pinned = !pinned }) { Text(if (pinned) "Unpin" else "Pin") }
      TextButton(onClick = {
       clipboard.setText(AnnotatedString(listOf(title, body.text).filter { it.isNotBlank() }.joinToString("\n")))
      }) { Text("Copy") }
      TextButton(onClick = { deleting = true }) { Text("Delete") }
     }
    }
   ) { insets ->
    Column(Modifier.fillMaxSize().padding(insets).padding(horizontal = 16.dp)) {
     OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(),
      placeholder = { Text("Title") }, singleLine = true, shape = RoundedCornerShape(14.dp))
     Spacer(Modifier.height(14.dp))
     BasicTextField(value = body, onValueChange = { incoming ->
      val old = body
      val cursor = incoming.selection.start
      val equalsTyped = incoming.text.length == old.text.length + 1 && incoming.selection.collapsed &&
       cursor > 0 && incoming.text[cursor - 1] == '=' &&
       incoming.text.removeRange(cursor - 1, cursor) == old.text
      body = if (equalsTyped) MathEngine.complete(incoming.text, cursor)?.let { (newText, newCursor) ->
       TextFieldValue(newText, selection = TextRange(newCursor))
      } ?: incoming else incoming
     }, modifier = Modifier.weight(1f).fillMaxWidth(),
      textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
      cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
      decorationBox = { inner ->
       Box {
        if (body.text.isEmpty()) Text("Write a note… 25 + 5 =",
         color = MaterialTheme.colorScheme.onSurfaceVariant)
        inner()
       }
      })
     Row(Modifier.fillMaxWidth().padding(vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween) {
      Text("Autosaved", style = MaterialTheme.typography.labelSmall,
       color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text("${body.text.length} characters", style = MaterialTheme.typography.labelSmall,
       color = MaterialTheme.colorScheme.onSurfaceVariant)
      TextButton(onClick = {
       val text = listOf(title, body.text).filter { it.isNotBlank() }.joinToString("\n")
       val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
       context.startActivity(Intent.createChooser(send, "Share note"))
      }) { Text("Share") }
     }
    }
   }
   if (deleting) {
    AlertDialog(onDismissRequest = { deleting = false },
     title = { Text("Delete this note?") },
     text = { Text("You can undo this from the notes list.") },
     confirmButton = {
      TextButton(onClick = {
       deleting = false
       val deleted = snapshot()
       scope.launch {
        withContext(Dispatchers.IO) { dao.delete(note.id) }
        selected = null
        val response = snackbar.showSnackbar("Note deleted", actionLabel = "Undo",
         duration = SnackbarDuration.Long)
        if (response == SnackbarResult.ActionPerformed) {
         withContext(Dispatchers.IO) { dao.insert(deleted) }
        }
       }
      }) { Text("Delete") }
     },
     dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancel") } })
   }
  }
 }
}
