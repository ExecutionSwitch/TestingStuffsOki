package com.example.mathnotes

import android.os.Bundle
import android.graphics.Color
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  window.decorView.setBackgroundColor(Color.rgb(20, 18, 24))
  window.statusBarColor = Color.TRANSPARENT
  window.navigationBarColor = Color.TRANSPARENT
  window.isStatusBarContrastEnforced = false
  window.isNavigationBarContrastEnforced = false
  WindowCompat.getInsetsController(window, window.decorView).apply {
    isAppearanceLightStatusBars = false
    isAppearanceLightNavigationBars = false
  }
  val dao = Room.databaseBuilder(applicationContext, NotesDatabase::class.java, "notes.db").build().notesDao()
  setContent { MaterialTheme(colorScheme = darkColorScheme()) { Box(Modifier.fillMaxSize().background(ComposeColor(0xFF141218)).safeDrawingPadding()) { NotesScreen(dao) } } }
 }
}

@Composable fun NotesScreen(dao: NotesDao) {
 val scope = rememberCoroutineScope()
 val notes by dao.observeAll().collectAsState(initial = emptyList())
 var selected by remember { mutableStateOf<Note?>(null) }
 var search by remember { mutableStateOf("") }
 var deleting by remember { mutableStateOf(false) }
 if (selected == null) {
  Scaffold(topBar = { Column(Modifier.padding(16.dp)) {
   Text("Math Notes", style = MaterialTheme.typography.headlineMedium)
   OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), placeholder = { Text("Search notes") }, singleLine = true)
  } }, floatingActionButton = { FloatingActionButton(onClick = {
   scope.launch { val id = withContext(Dispatchers.IO) { dao.insert(Note()) }; selected = Note(id = id) }
  }) { Text("+") } }) { inset ->
   LazyColumn(Modifier.fillMaxSize().padding(inset), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    items(notes.filter { it.title.contains(search,true) || it.body.contains(search,true) }, key = { it.id }) { note ->
     ElevatedCard(onClick = { selected = note }, modifier = Modifier.fillMaxWidth()) {
      Column(Modifier.padding(16.dp)) {
       Text(note.title.ifBlank { "Untitled" }, fontWeight = FontWeight.Bold)
       Text(note.body.ifBlank { "Empty note" }, maxLines = 4, overflow = TextOverflow.Ellipsis)
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
   LaunchedEffect(title, body.text) {
    delay(300)
    withContext(Dispatchers.IO) { dao.update(Note(note.id, title, body.text, System.currentTimeMillis())) }
   }
   fun goBack() {
    val snapshot = Note(note.id,title,body.text,System.currentTimeMillis())
    scope.launch { withContext(Dispatchers.IO) { dao.update(snapshot) }; selected = null }
   }
   BackHandler { goBack() }
   Scaffold(topBar = { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    TextButton(onClick = { goBack() }) { Text("← Notes") }
    TextButton(onClick = { deleting = true }) { Text("Delete") }
   } }) { inset ->
    Column(Modifier.fillMaxSize().padding(inset).padding(16.dp)) {
     OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), placeholder = { Text("Title") }, singleLine = true)
     Spacer(Modifier.height(16.dp))
     BasicTextField(value = body, onValueChange = { incoming ->
      val old = body
      val cursor = incoming.selection.start
      val equalsTyped = incoming.text.length == old.text.length + 1 && incoming.selection.collapsed &&
       cursor > 0 && incoming.text[cursor-1] == '=' &&
       incoming.text.removeRange(cursor-1,cursor) == old.text
      body = if (equalsTyped) MathEngine.complete(incoming.text,cursor)?.let { (t,c) ->
       TextFieldValue(t,selection=TextRange(c))
      } ?: incoming else incoming
     }, modifier = Modifier.fillMaxSize(), textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
       cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), decorationBox = { inner ->
        Box { if (body.text.isEmpty()) Text("Write a note… 25 + 5 =", color=MaterialTheme.colorScheme.onSurfaceVariant); inner() }
       })
    }
   }
   if (deleting) AlertDialog(onDismissRequest = { deleting=false }, title={Text("Delete note?")},
    confirmButton={TextButton(onClick={ deleting=false; scope.launch { withContext(Dispatchers.IO) { dao.delete(note.id) }; selected=null } }){Text("Delete")}},
    dismissButton={TextButton(onClick={deleting=false}){Text("Cancel")}})
  }
 }
}
