package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.Note
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.runtime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: OmniViewModel, onBack: () -> Unit) {
    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()
    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { pad ->
        if (noteToDelete != null) {
            AlertDialog(
                onDismissRequest = { noteToDelete = null },
                title = { Text(stringResource(R.string.delete)) },
                text = { Text(stringResource(R.string.confirm_delete)) },
                confirmButton = {
                    TextButton(onClick = {
                        noteToDelete?.let { viewModel.deleteNote(it) }
                        noteToDelete = null
                    }) { Text(stringResource(R.string.confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { noteToDelete = null }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(8.dp)) {
            items(allNotes, key = { it.id }) { note ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                        viewModel.selectNote(note.id)
                        onBack()
                    }
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (note.customerName.isNotBlank()) note.customerName else note.title,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (note.invoiceNumber.isNotBlank()) {
                                Text(
                                    "${stringResource(R.string.invoice_number)}: ${note.invoiceNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Text(
                                SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(note.timestamp)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(onClick = { noteToDelete = note }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
