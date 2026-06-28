package com.salesinventory.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salesinventory.app.data.Discount
import com.salesinventory.app.data.DiscountType
import com.salesinventory.app.ui.theme.*
import com.salesinventory.app.viewmodel.MainViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscountScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val discounts by viewModel.discounts.collectAsState()
    val currentDiscount by viewModel.currentDiscount.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Promotions & Discounts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Discount")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Blue800,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (currentDiscount != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Amber50)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = Amber700)
                        Spacer(Modifier.width(12.dp))
                        val d = currentDiscount
                        if (d != null) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Active: ${d.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                val desc = if (d.type == DiscountType.PERCENTAGE)
                                    "${d.value}% OFF" else "PHP ${d.value} OFF"
                                Text(desc, color = Amber700, fontSize = 13.sp)
                            }
                        }
                        Button(
                            onClick = { viewModel.setCurrentDiscount(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = Red700),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Remove", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (discounts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Star, contentDescription = "Discounts", modifier = Modifier.size(64.dp), tint = Grey400)
                        Spacer(Modifier.height(16.dp))
                        Text("No discounts configured", style = MaterialTheme.typography.titleMedium)
                        Text("Add discounts for monthly promotions", color = Grey600)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Add Discount")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(discounts, key = { it.id }) { discount ->
                        var showDeleteConfirm by remember { mutableStateOf(false) }
                        DiscountCard(
                            discount = discount,
                            isActive = currentDiscount?.id == discount.id,
                            onActivate = { viewModel.setCurrentDiscount(discount) },
                            onDelete = { showDeleteConfirm = true }
                        )
                        if (showDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm = false },
                                title = { Text("Confirm Delete") },
                                text = { Text("Delete discount \"${discount.name}\"?") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.removeDiscount(discount.id)
                                        showDeleteConfirm = false
                                    }) { Text("Delete", color = Red700) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddDiscountDialog(
            onDismiss = { showAddDialog = false },
            onSave = { discount ->
                viewModel.addDiscount(discount)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun DiscountCard(
    discount: Discount,
    isActive: Boolean,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(if (isActive) 2.dp else 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Amber50 else Color.White
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.LocalOffer,
                contentDescription = null,
                tint = if (isActive) Amber700 else Grey600,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(discount.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                val valStr = if (discount.type == DiscountType.PERCENTAGE)
                    "${discount.value}% OFF" else "PHP ${discount.value} OFF"
                Text(
                    valStr,
                    color = if (isActive) Amber700 else Grey600,
                    fontSize = 13.sp
                )
            }
            if (!isActive) {
                TextButton(onClick = onActivate) {
                    Text("Activate", color = Blue800)
                }
            } else {
                Text("Active", fontWeight = FontWeight.Bold, color = Green700, fontSize = 12.sp)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Red700)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDiscountDialog(
    onDismiss: () -> Unit,
    onSave: (Discount) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var typeIndex by remember { mutableStateOf(0) }
    val types = listOf("Percentage (%)", "Fixed Amount (PHP)")
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Discount", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Discount Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Valentine Promo") },
                    shape = RoundedCornerShape(10.dp)
                )

                Text("Discount Type:", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEachIndexed { index, label ->
                        FilterChip(
                            selected = typeIndex == index,
                            onClick = { typeIndex = index },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(if (typeIndex == 0) "Percentage (%) *" else "Amount (PHP) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text(if (typeIndex == 0) "%" else "PHP ") },
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val valNum = value.toDoubleOrNull() ?: 0.0
                    val isPercent = typeIndex == 0
                    if (name.isNotBlank() && valNum > 0 && (!isPercent || valNum <= 100)) {
                        val discount = Discount(
                            id = UUID.randomUUID().toString().take(8),
                            name = name.trim(),
                            type = if (typeIndex == 0) DiscountType.PERCENTAGE else DiscountType.FIXED_AMOUNT,
                            value = valNum,
                            isActive = true
                        )
                        onSave(discount)
                    }
                },
                enabled = name.isNotBlank() && {
                    val v = value.toDoubleOrNull() ?: 0.0
                    v > 0 && (typeIndex != 0 || v <= 100)
                }(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
