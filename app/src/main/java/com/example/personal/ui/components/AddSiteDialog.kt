package com.example.personal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.personal.models.Categories
import com.example.personal.models.MovieSite
import com.example.personal.ui.theme.DarkBackground
import com.example.personal.ui.theme.DarkCard
import com.example.personal.ui.theme.GlassBorder
import com.example.personal.ui.theme.PrimaryBlue
import com.example.personal.ui.theme.TextPrimary
import com.example.personal.ui.theme.TextSecondary
import com.example.personal.ui.theme.TextTertiary
import com.example.personal.ui.theme.getCategoryColor
import java.util.UUID

val emojiOptions = listOf("🌐", "🎬", "🎥", "🍿", "📺", "🎮", "🎵", "📱", "💻", "⭐", "🔥", "💎")

@Composable
fun AddSiteDialog(
    onDismiss: () -> Unit,
    onAddSite: (MovieSite) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Categories.HOLLYWOOD) }
    var selectedEmoji by remember { mutableStateOf("🌐") }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            color = DarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Custom Site",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Emoji picker
                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    emojiOptions.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedEmoji == emoji) PrimaryBlue.copy(alpha = 0.2f) else DarkCard)
                                .border(
                                    width = if (selectedEmoji == emoji) 2.dp else 1.dp,
                                    color = if (selectedEmoji == emoji) PrimaryBlue else GlassBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 22.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Name input
                InputField(
                    label = "Site Name",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "e.g., My Movie Site"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // URL input
                InputField(
                    label = "URL",
                    value = url,
                    onValueChange = { url = it },
                    placeholder = "https://example.com"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Description input
                InputField(
                    label = "Description (optional)",
                    value = description,
                    onValueChange = { description = it },
                    placeholder = "Short description"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Category selector
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Categories.list.filter { it != Categories.ALL }.forEach { category ->
                        val categoryColor = getCategoryColor(category)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selectedCategory == category) 
                                        categoryColor.copy(alpha = 0.2f) 
                                    else DarkCard
                                )
                                .border(
                                    width = if (selectedCategory == category) 2.dp else 1.dp,
                                    color = if (selectedCategory == category) categoryColor else GlassBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selectedCategory == category) categoryColor else TextSecondary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Cancel",
                            color = TextSecondary
                        )
                    }
                    
                    Button(
                        onClick = {
                            if (name.isNotBlank() && url.isNotBlank()) {
                                val cleanUrl = if (!url.startsWith("http")) "https://$url" else url
                                val site = MovieSite(
                                    id = "custom_${UUID.randomUUID().toString().take(8)}",
                                    name = name.trim(),
                                    url = cleanUrl.trim(),
                                    category = selectedCategory,
                                    description = description.ifBlank { "Custom site" },
                                    iconEmoji = selectedEmoji
                                )
                                onAddSite(site)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = name.isNotBlank() && url.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            disabledContainerColor = PrimaryBlue.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Site")
                    }
                }
            }
        }
    }
}

@Composable
private fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCard)
                .border(
                    width = 1.dp,
                    color = GlassBorder,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                cursorBrush = SolidColor(PrimaryBlue),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextTertiary
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}
