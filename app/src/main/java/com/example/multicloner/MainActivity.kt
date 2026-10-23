package com.example.multicloner

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = Store(applicationContext)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) { HomeScreen(store, Engine.current) }
            }
        }
    }
}

data class InstalledApp(val pkg: String, val label: String)

fun loadInstalledApps(ctx: Context): List<InstalledApp> {
    val pm = ctx.packageManager
    return pm.getInstalledApplications(0)
        .filter { it.packageName != ctx.packageName && pm.getLaunchIntentForPackage(it.packageName) != null }
        .map { InstalledApp(it.packageName, it.loadLabel(pm).toString()) }
        .sortedBy { it.label.lowercase() }
}

@Composable
fun AppIcon(pkg: String, size: Int = 44) {
    val ctx = LocalContext.current
    val bmp by produceState<ImageBitmap?>(null, pkg) {
        value = withContext(Dispatchers.IO) {
            runCatching { ctx.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap() }.getOrNull()
        }
    }
    bmp?.let { Image(it, null, Modifier.size(size.dp)) } ?: Box(Modifier.size(size.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(store: Store, engine: CloneEngine) {
    val ctx = LocalContext.current
    var showAddClone by remember { mutableStateOf(false) }
    var showAddFolder by remember { mutableStateOf(false) }
    var moveTarget by remember { mutableStateOf<Clone?>(null) }
    var renameTarget by remember { mutableStateOf<Folder?>(null) }
    val collapsed = remember { mutableStateMapOf<String, Boolean>() }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Multi Cloner") }) }) { pad ->
        LazyColumn(
            Modifier.padding(pad).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { showAddClone = true }, Modifier.weight(1f)) {
                        Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Add Clone")
                    }
                    OutlinedButton(onClick = { showAddFolder = true }, Modifier.weight(1f)) {
                        Icon(Icons.Default.CreateNewFolder, null); Spacer(Modifier.width(6.dp)); Text("Add Folder")
                    }
                }
            }
            if (!engine.hasFileAccess()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "Clonele au nevoie de acces la fisiere ca sa functioneze corect (poze, descarcari).",
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(onClick = { (ctx as? Activity)?.let { engine.requestFileAccess(it) } }) {
                                Text("Permite acces la fisiere")
                            }
                        }
                    }
                }
            }

            item { StorageCard(store) }

            // Foldere
            for (folder in store.folders) {
                val inFolder = store.clones.filter { it.folderId == folder.id }
                val isCollapsed = collapsed[folder.id] == true
                item(key = "f_" + folder.id) {
                    FolderHeader(
                        folder, inFolder.size, isCollapsed,
                        onToggle = { collapsed[folder.id] = !isCollapsed },
                        onRename = { renameTarget = folder },
                        onDelete = { store.deleteFolder(folder.id) }
                    )
                }
                if (!isCollapsed) {
                    items(inFolder, key = { it.id }) { c ->
                        CloneRow(c, store, engine, onMove = { moveTarget = c }, indent = true)
                    }
                }
            }

            // Fara folder
            val loose = store.clones.filter { it.folderId == null }
            if (store.folders.isNotEmpty() && loose.isNotEmpty()) {
                item { Text("Fara folder", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)) }
            }
            items(loose, key = { it.id }) { c -> CloneRow(c, store, engine, onMove = { moveTarget = c }) }

            if (store.clones.isEmpty()) {
                item {
                    Text(
                        "Nicio clona inca. Apasa Add Clone ca sa adaugi prima aplicatie.",
                        Modifier.padding(vertical = 32.dp), style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showAddClone) {
        AddCloneDialog(
            folders = store.folders,
            onDismiss = { showAddClone = false },
            onPick = { app, folderId ->
                showAddClone = false
                val uid = store.nextUserId(app.pkg)
                scope.launch {
                    busy = "Se creeaza clona ${app.label}..."
                    val error = withContext(Dispatchers.IO) { engine.install(ctx, app.pkg, uid) }
                    if (error == null) {
                        val n = store.clones.count { it.pkg == app.pkg } + 1
                        store.addClone(app.pkg, if (n > 1) "${app.label} $n" else "${app.label} (clona)", uid, folderId)
                        // Fara Google Play Services, aplicatiile clonate spun ca nu se pot conecta la Google.
                        if (engine.supportsGoogle && !engine.isGoogleInstalled(uid)) {
                            busy = "Se instaleaza serviciile Google in clona (poate dura cateva minute)..."
                            val gErr = withContext(Dispatchers.IO) { engine.installGoogle(uid) }
                            if (gErr != null) {
                                Toast.makeText(ctx, "Google: $gErr", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(ctx, "Clonare esuata: $error", Toast.LENGTH_LONG).show()
                    }
                    busy = null
                }
            }
        )
    }
    busy?.let { msg ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Te rog asteapta") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(32.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(msg)
                }
            },
            confirmButton = {}
        )
    }
    if (showAddFolder) {
        NameDialog("Folder nou", "", onDismiss = { showAddFolder = false }) { store.addFolder(it); showAddFolder = false }
    }
    renameTarget?.let { f ->
        NameDialog("Redenumeste folderul", f.name, onDismiss = { renameTarget = null }) {
            store.renameFolder(f.id, it); renameTarget = null
        }
    }
    moveTarget?.let { c ->
        MoveDialog(c, store.folders, onDismiss = { moveTarget = null }) { store.moveClone(c.id, it); moveTarget = null }
    }
}

@Composable
fun FolderHeader(
    folder: Folder, count: Int, collapsed: Boolean,
    onToggle: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (collapsed) Icons.Default.Folder else Icons.Default.FolderOpen, null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(folder.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$count aplicatii", style = MaterialTheme.typography.bodySmall)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, null) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("Redenumeste") }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text("Sterge folderul") }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
fun CloneRow(c: Clone, store: Store, engine: CloneEngine, onMove: () -> Unit, indent: Boolean = false) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    Card(
        Modifier.fillMaxWidth().padding(start = if (indent) 20.dp else 0.dp)
            .clickable { engine.launch(ctx, c.pkg, c.userId) },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(c.pkg)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(c.pkg, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, null) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("Muta in folder") }, onClick = { menu = false; onMove() })
                    if (engine.supportsGoogle) {
                        DropdownMenuItem(text = { Text("Instaleaza serviciile Google") }, onClick = {
                            menu = false
                            scope.launch {
                                Toast.makeText(ctx, "Se instaleaza serviciile Google...", Toast.LENGTH_SHORT).show()
                                val err = withContext(Dispatchers.IO) { engine.installGoogle(c.userId) }
                                Toast.makeText(ctx, err ?: "Servicii Google instalate", Toast.LENGTH_LONG).show()
                            }
                        })
                    }
                    DropdownMenuItem(text = { Text("Sterge clona") }, onClick = {
                        menu = false
                        engine.uninstall(ctx, c.pkg, c.userId)
                        store.removeClone(c.id)
                    })
                }
            }
        }
    }
}

@Composable
fun AddCloneDialog(folders: List<Folder>, onDismiss: () -> Unit, onPick: (InstalledApp, String?) -> Unit) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var folderId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { apps = withContext(Dispatchers.IO) { loadInstalledApps(ctx) } }
    val shown = apps.filter { it.label.contains(query, true) || it.pkg.contains(query, true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alege aplicatia de clonat") },
        text = {
            Column {
                OutlinedTextField(query, { query = it }, singleLine = true, label = { Text("Cauta") }, modifier = Modifier.fillMaxWidth())
                if (folders.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Adauga in:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterChip(folderId == null, { folderId = null }, { Text("Fara folder") }) }
                        items(folders) { f -> FilterChip(folderId == f.id, { folderId = f.id }, { Text(f.name) }) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(shown, key = { it.pkg }) { a ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(a, folderId) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(a.pkg, 36); Spacer(Modifier.width(12.dp))
                            Text(a.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onDismiss) { Text("Anuleaza") } }
    )
}

@Composable
fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onOk: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Nume") }) },
        confirmButton = { TextButton({ if (name.isNotBlank()) onOk(name) }) { Text("OK") } },
        dismissButton = { TextButton(onDismiss) { Text("Anuleaza") } }
    )
}

@Composable
fun MoveDialog(c: Clone, folders: List<Folder>, onDismiss: () -> Unit, onPick: (String?) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Muta \"${c.label}\"") },
        text = {
            Column {
                if (folders.isEmpty()) Text("Nu ai foldere. Creeaza unul cu Add Folder.")
                TextButton({ onPick(null) }) { Text("Fara folder") }
                folders.forEach { f -> TextButton({ onPick(f.id) }) { Text(f.name) } }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onDismiss) { Text("Anuleaza") } }
    )
}

@Composable
fun StorageCard(store: Store) {
    val options = listOf(
        3 to "Stocarea telefonului, comuna (recomandat)",
        2 to "Stocarea telefonului, fara separare deloc",
        1 to "Virtuala, comuna pentru toate clonele",
        0 to "Virtuala, separata pentru fiecare clona"
    )
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Text("Stocarea clonelor", style = MaterialTheme.typography.titleSmall)
            Text(
                "Fiecare clona are propriul utilizator (cont si date separate). Modul recomandat foloseste stocarea " +
                    "reala a telefonului (poze, descarcari, fisiere) comuna pentru toate clonele. Modurile virtuale " +
                    "nu vad pozele tale. Schimbarea se aplica dupa ce inchizi si redeschizi clonele.",
                style = MaterialTheme.typography.bodySmall
            )
            options.forEach { (mode, label) ->
                Row(
                    Modifier.fillMaxWidth().clickable { store.updateStorageMode(mode) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = store.storageMode == mode, onClick = { store.updateStorageMode(mode) })
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
