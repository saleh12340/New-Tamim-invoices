package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.R
import com.example.data.NoteItem
import com.example.data.Suggestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(viewModel: OmniViewModel, onOpenHistory: () -> Unit) {
    val currentNote by viewModel.currentNote.collectAsStateWithLifecycle()
    val currentItems by viewModel.currentItems.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    
    var itemName by remember { mutableStateOf(TextFieldValue("")) }
    var quantity by remember { mutableStateOf(TextFieldValue("1")) }
    var price by remember { mutableStateOf(TextFieldValue("0")) }
    var totalInput by remember { mutableStateOf(TextFieldValue("0")) }
    var customerName by remember { mutableStateOf(TextFieldValue("")) }
    var invoiceNumber by remember { mutableStateOf(TextFieldValue("")) }
    var showSuggestions by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    fun updatePriceFromTotal(total: String, qty: String) {
        val t = total.toDoubleOrNull() ?: 0.0
        val q = qty.toDoubleOrNull() ?: 1.0
        if (q != 0.0) {
            val p = t / q
            price = TextFieldValue(if (p % 1.0 == 0.0) p.toInt().toString() else "%.2f".format(java.util.Locale.US, p))
        }
    }

    fun updateTotalFromPrice(p: String, qty: String) {
        val priceVal = p.toDoubleOrNull() ?: 0.0
        val q = qty.toDoubleOrNull() ?: 1.0
        val t = priceVal * q
        totalInput = TextFieldValue(if (t % 1.0 == 0.0) t.toInt().toString() else "%.2f".format(java.util.Locale.US, t))
    }
    
    // Update local state when note changes
    LaunchedEffect(currentNote?.id) {
        customerName = TextFieldValue(currentNote?.customerName ?: "")
        invoiceNumber = TextFieldValue(currentNote?.invoiceNumber ?: "")
    }

    val context = LocalContext.current
    val appNameLabel = stringResource(R.string.app_name)
    val customerNameLabel = stringResource(R.string.customer_name)
    val itemNameLabel = stringResource(R.string.item_name)
    val quantityLabel = stringResource(R.string.quantity)
    val priceLabel = stringResource(R.string.price)
    val totalLabel = stringResource(R.string.total)
    val invoiceNumberLabel = stringResource(R.string.invoice_number)

    fun printNote() {
        val html = buildString {
            append("<html><body style='font-family: Arial; direction: rtl; padding: 20px;'>")
            append("<h2 style='text-align: center; margin-bottom: 5px;'>$appNameLabel</h2>")
            if (currentNote?.invoiceNumber?.isNotBlank() == true) {
                append("<p style='text-align: right; margin-top: 0;'><b>$invoiceNumberLabel:</b> ${currentNote?.invoiceNumber}</p>")
            }
            append("<p style='text-align: right; margin-top: 0;'><b>$customerNameLabel:</b> ${currentNote?.customerName ?: ""}</p>")
            append("<p style='text-align: right; margin-top: 0;'><b>التاريخ:</b> ${java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(currentNote?.timestamp ?: System.currentTimeMillis()))}</p>")
            append("<table style='width: 100%; border-collapse: collapse; margin-top: 10px;'>")
            append("<tr style='background-color: #f2f2f2;'>")
            append("<th style='border: 1px solid black; padding: 8px;'>$itemNameLabel</th>")
            append("<th style='border: 1px solid black; padding: 8px;'>$quantityLabel</th>")
            append("<th style='border: 1px solid black; padding: 8px;'>$priceLabel</th>")
            append("<th style='border: 1px solid black; padding: 8px;'>$totalLabel</th>")
            append("</tr>")
            
            var grandTotal = 0.0
            currentItems.forEach { item ->
                val total = item.quantity * item.price
                grandTotal += total
                append("<tr>")
                append("<td style='border: 1px solid black; padding: 8px; text-align: right;'>${item.name}</td>")
                append("<td style='border: 1px solid black; padding: 8px; text-align: center;'>${if (item.quantity % 1.0 == 0.0) item.quantity.toInt() else item.quantity}</td>")
                append("<td style='border: 1px solid black; padding: 8px; text-align: center;'>${item.price}</td>")
                append("<td style='border: 1px solid black; padding: 8px; text-align: center;'>$total</td>")
                append("</tr>")
            }
            
            append("<tr style='font-weight: bold; background-color: #f2f2f2;'>")
            append("<td colspan='3' style='border: 1px solid black; padding: 8px; text-align: left;'>$totalLabel</td>")
            append("<td style='border: 1px solid black; padding: 8px; text-align: center;'>$grandTotal</td>")
            append("</tr>")
            append("</table></body></html>")
        }
        
        val webView = android.webkit.WebView(context)
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView, url: String) {
                val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                val printAdapter = view.createPrintDocumentAdapter("Invoice")
                printManager.print("Invoice", printAdapter, null)
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    Scaffold(
        topBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.primaryContainer).statusBarsPadding().padding(top = 12.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ActionChip(stringResource(R.string.clear_page), Color(0xFFFF9800)) { viewModel.clearCurrentNote() }
                    ActionChip(stringResource(R.string.new_note), Color(0xFF2196F3)) { viewModel.createNewNote() }
                    ActionChip(stringResource(R.string.history), Color(0xFF9C27B0)) { onOpenHistory() }
                    ActionChip(stringResource(R.string.smart_print), Color(0xFF4CAF50)) { printNote() }
                    ActionChip(stringResource(R.string.delete), Color(0xFFF44336)) { 
                        showDeleteConfirm = true
                    }
                }
                
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.updateNoteSettings((currentNote?.fontSize ?: 14) - 1, currentNote?.scrollEnabled ?: true) }) {
                            Icon(Icons.Default.Remove, null)
                        }
                        Text("${stringResource(R.string.font_size)} ${currentNote?.fontSize ?: 14}")
                        IconButton(onClick = { viewModel.updateNoteSettings((currentNote?.fontSize ?: 14) + 1, currentNote?.scrollEnabled ?: true) }) {
                            Icon(Icons.Default.Add, null)
                        }
                    }
                    
                    val grandTotal = currentItems.sumOf { it.quantity * it.price }
                    Text(
                        "${stringResource(R.string.total)}: $grandTotal",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }
    ) { pad ->
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text(stringResource(R.string.delete)) },
                text = { Text(stringResource(R.string.confirm_delete)) },
                confirmButton = {
                    TextButton(onClick = {
                        currentNote?.let { viewModel.deleteNote(it) }
                        showDeleteConfirm = false
                    }) { Text(stringResource(R.string.confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
        Column(Modifier.padding(pad).fillMaxSize()) {
            // Input Area
            Card(
                Modifier.fillMaxWidth().padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = invoiceNumber,
                            onValueChange = { 
                                invoiceNumber = it
                                viewModel.updateInvoiceNumber(it.text)
                            },
                            label = { Text(stringResource(R.string.invoice_number)) },
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { 
                                customerName = it
                                viewModel.updateCustomerName(it.text)
                            },
                            label = { Text(stringResource(R.string.customer_name)) },
                            modifier = Modifier.weight(1.5f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { 
                                quantity = it
                                updateTotalFromPrice(price.text, it.text)
                            },
                            label = { Text(stringResource(R.string.quantity)) },
                            modifier = Modifier.weight(0.2f).onFocusChanged { 
                                if (it.isFocused) {
                                    scope.launch {
                                        delay(100)
                                        quantity = quantity.copy(selection = TextRange(0, quantity.text.length))
                                    }
                                }
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = price,
                            onValueChange = { 
                                price = it
                                updateTotalFromPrice(it.text, quantity.text)
                            },
                            label = { Text(stringResource(R.string.price)) },
                            modifier = Modifier.weight(0.2f).onFocusChanged { 
                                if (it.isFocused) {
                                    scope.launch {
                                        delay(100)
                                        price = price.copy(selection = TextRange(0, price.text.length))
                                    }
                                }
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = totalInput,
                            onValueChange = { 
                                totalInput = it
                                updatePriceFromTotal(it.text, quantity.text)
                            },
                            label = { Text(stringResource(R.string.total)) },
                            modifier = Modifier.weight(0.2f).onFocusChanged { 
                                if (it.isFocused) {
                                    scope.launch {
                                        delay(100)
                                        totalInput = totalInput.copy(selection = TextRange(0, totalInput.text.length))
                                    }
                                }
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        Box(Modifier.weight(0.4f)) {
                            OutlinedTextField(
                                value = itemName,
                                onValueChange = { 
                                    itemName = it
                                    showSuggestions = it.text.isNotEmpty()
                                },
                                label = { Text(stringResource(R.string.item_name)) },
                                modifier = Modifier.fillMaxWidth().onFocusChanged {
                                    if (it.isFocused) {
                                        scope.launch {
                                            delay(100)
                                            itemName = itemName.copy(selection = TextRange(0, itemName.text.length))
                                        }
                                    }
                                },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                            if (showSuggestions && suggestions.any { it.word.contains(itemName.text, ignoreCase = true) }) {
                                Card(Modifier.fillMaxWidth().padding(top = 60.dp), elevation = CardDefaults.cardElevation(8.dp)) {
                                    Column {
                                        suggestions.filter { it.word.contains(itemName.text, ignoreCase = true) }.take(5).forEach { sug ->
                                            Text(sug.word, Modifier.fillMaxWidth().clickable {
                                                itemName = TextFieldValue(sug.word, TextRange(sug.word.length))
                                                showSuggestions = false
                                            }.padding(12.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (itemName.text.isNotBlank()) {
                                viewModel.addItem(
                                    itemName.text, 
                                    quantity.text.toDoubleOrNull() ?: 1.0, 
                                    price.text.toDoubleOrNull() ?: 0.0,
                                    "left"
                                )
                                itemName = TextFieldValue("")
                                quantity = TextFieldValue("1")
                                price = TextFieldValue("0")
                                totalInput = TextFieldValue("0")
                                showSuggestions = false
                            }
                        },
                        Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.save_item))
                    }
                }
            }
            
            // List Area
            val fontSize = (currentNote?.fontSize ?: 14).sp
            val items = currentItems

            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                itemsIndexed(items) { index, item ->
                    NoteItemRow(
                        item, 
                        fontSize, 
                        Modifier.fillMaxWidth(),
                        onUpdate = { viewModel.updateItem(it) },
                        onDelete = { viewModel.deleteItem(it) }
                    )
                    HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun NoteItemRow(
    item: NoteItem, 
    fontSize: androidx.compose.ui.unit.TextUnit, 
    modifier: Modifier, 
    onUpdate: (NoteItem) -> Unit,
    onDelete: (NoteItem) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var editName by remember { mutableStateOf(TextFieldValue(item.name)) }
    var editQty by remember { mutableStateOf(TextFieldValue(if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString())) }
    var editPrice by remember { mutableStateOf(TextFieldValue(item.price.toString())) }
    var editTotal by remember { mutableStateOf(TextFieldValue((item.quantity * item.price).toString())) }

    fun updateEditPriceFromTotal(total: String, qty: String) {
        val t = total.toDoubleOrNull() ?: 0.0
        val q = qty.toDoubleOrNull() ?: 1.0
        if (q != 0.0) {
            val p = t / q
            editPrice = TextFieldValue(if (p % 1.0 == 0.0) p.toInt().toString() else "%.2f".format(java.util.Locale.US, p))
        }
    }

    fun updateEditTotalFromPrice(p: String, qty: String) {
        val priceVal = p.toDoubleOrNull() ?: 0.0
        val q = qty.toDoubleOrNull() ?: 1.0
        val t = priceVal * q
        editTotal = TextFieldValue(if (t % 1.0 == 0.0) t.toInt().toString() else "%.2f".format(java.util.Locale.US, t))
    }

    if (isEditing) {
        AlertDialog(
            onDismissRequest = { isEditing = false },
            title = { Text(stringResource(R.string.edit_item)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(stringResource(R.string.item_name)) },
                        modifier = Modifier.onFocusChanged {
                            if (it.isFocused) {
                                scope.launch {
                                    delay(100)
                                    editName = editName.copy(selection = TextRange(0, editName.text.length))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editQty,
                            onValueChange = { 
                                editQty = it
                                updateEditTotalFromPrice(editPrice.text, it.text)
                            },
                            label = { Text(stringResource(R.string.quantity)) },
                            modifier = Modifier.weight(1f).onFocusChanged {
                                if (it.isFocused) {
                                    scope.launch {
                                        delay(100)
                                        editQty = editQty.copy(selection = TextRange(0, editQty.text.length))
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                        )
                        OutlinedTextField(
                            value = editPrice,
                            onValueChange = { 
                                editPrice = it
                                updateEditTotalFromPrice(it.text, editQty.text)
                            },
                            label = { Text(stringResource(R.string.price)) },
                            modifier = Modifier.weight(1f).onFocusChanged {
                                if (it.isFocused) {
                                    scope.launch {
                                        delay(100)
                                        editPrice = editPrice.copy(selection = TextRange(0, editPrice.text.length))
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                        )
                        OutlinedTextField(
                            value = editTotal,
                            onValueChange = { 
                                editTotal = it
                                updateEditPriceFromTotal(it.text, editQty.text)
                            },
                            label = { Text(stringResource(R.string.total)) },
                            modifier = Modifier.weight(1f).onFocusChanged {
                                if (it.isFocused) {
                                    scope.launch {
                                        delay(100)
                                        editTotal = editTotal.copy(selection = TextRange(0, editTotal.text.length))
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdate(item.copy(
                        name = editName.text, 
                        quantity = editQty.text.toDoubleOrNull() ?: 1.0,
                        price = editPrice.text.toDoubleOrNull() ?: 0.0
                    ))
                    isEditing = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { isEditing = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Row(
        modifier.padding(vertical = 4.dp)
            .clickable { isEditing = true },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = "${item.quantity} x ${item.price} = ${item.quantity * item.price}",
                    fontSize = (fontSize.value * 0.8).sp,
                    color = Color.Gray
                )
            }
        }
        IconButton(onClick = { onDelete(item) }) {
            Icon(Icons.Default.Delete, null, tint = Color.Red)
        }
    }
}

@Composable
fun ActionChip(text: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = color,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.height(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
