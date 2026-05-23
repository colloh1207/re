package com.sdd.marketplace.feature.chat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sdd.marketplace.core.ui.theme.SddPink

val reportCategories = listOf(
    "Spam or scam",
    "Offensive or abusive content",
    "Sharing contact info (link/phone/card)",
    "Fake products or misrepresentation",
    "Harassment or bullying",
    "Other"
)

@Composable
fun ReportUserDialog(
    userId: String,
    onReport: (category: String, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report User", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Why are you reporting this user?", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                reportCategories.forEach { cat ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = selectedCategory == cat, onClick = { selectedCategory = cat }).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, colors = RadioButtonDefaults.colors(selectedColor = SddPink))
                        Spacer(Modifier.width(8.dp))
                        Text(cat, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Additional details (optional)") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onReport(selectedCategory, description) },
                enabled = selectedCategory.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Submit Report") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
