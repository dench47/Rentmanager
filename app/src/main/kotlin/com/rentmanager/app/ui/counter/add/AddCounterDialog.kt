package com.rentmanager.app.ui.counter.add

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCounterDialog(
    onDismiss: () -> Unit,
    onAdd: (type: String, number: String, value: String, date: String, remind: Boolean) -> Unit,
    viewModel: AddCounterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Title
                Text(
                    text = "Добавить счётчик",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    letterSpacing = (-0.4).sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Counter type dropdown
                Text(
                    text = "Тип счётчика",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0x993C3C43)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0F0F0))
                            .clickable { viewModel.toggleTypeDropdown() }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = uiState.counterType,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    }
                    DropdownMenu(
                        expanded = uiState.isTypeDropdownOpen,
                        onDismissRequest = { viewModel.toggleTypeDropdown() }
                    ) {
                        uiState.types.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = { viewModel.selectType(type) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Counter number
                OutlinedTextField(
                    value = uiState.counterNumber,
                    onValueChange = { viewModel.onNumberChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Номер счётчика") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8F8F8),
                        unfocusedContainerColor = Color(0xFFF8F8F8),
                        focusedBorderColor = Color(0xFF212121),
                        unfocusedBorderColor = Color(0xFFD3D3D3)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Initial value
                OutlinedTextField(
                    value = uiState.initialValue,
                    onValueChange = { viewModel.onValueChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Начальные показания") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8F8F8),
                        unfocusedContainerColor = Color(0xFFF8F8F8),
                        focusedBorderColor = Color(0xFF212121),
                        unfocusedBorderColor = Color(0xFFD3D3D3)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Date picker placeholder
                OutlinedTextField(
                    value = uiState.date,
                    onValueChange = { viewModel.onDateChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Дата") },
                    placeholder = { Text("ДД.ММ.ГГГГ") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8F8F8),
                        unfocusedContainerColor = Color(0xFFF8F8F8),
                        focusedBorderColor = Color(0xFF212121),
                        unfocusedBorderColor = Color(0xFFD3D3D3)
                    )
                )

                // Remind checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.remindToSubmit,
                        onCheckedChange = { viewModel.toggleRemind() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF212121),
                            uncheckedColor = Color(0xFFD3D3D3)
                        )
                    )
                    Text(
                        text = "Прислать напоминание о подаче показаний",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFFF0F0F0))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Отмена",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                    }

                    // Add button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFF212121))
                            .clickable {
                                onAdd(
                                    uiState.counterType,
                                    uiState.counterNumber,
                                    uiState.initialValue,
                                    uiState.date,
                                    uiState.remindToSubmit
                                )
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Добавить",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}