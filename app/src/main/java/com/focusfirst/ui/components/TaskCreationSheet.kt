package com.focusfirst.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusfirst.data.model.TagEntity
import com.focusfirst.ui.theme.TokiTypography
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCreationSheet(
    initialDate: LocalDate,
    isPro: Boolean,
    tags: List<TagEntity>,
    onUpgradeClick: () -> Unit,
    onCreateTag: (String, String) -> Unit,
    onConfirm: (String, String, LocalDate, Int, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember(initialDate) { mutableStateOf(initialDate) }
    var durationMinutes by remember { mutableStateOf(25) }
    var selectedTag by remember { mutableStateOf("Focus") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showNewTag by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }

    val displayTags = remember(tags) {
        if (tags.any { it.name == "Focus" }) tags
        else listOf(TagEntity(name = "Focus", colorHex = "#FFFFFFFF")) + tags
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceContainerHigh,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = colors.onSurface.copy(alpha = 0.42f))
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "New task",
                    style = TokiTypography.pageTitle.copy(fontSize = 26.sp, lineHeight = 32.sp),
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close new task")
                }
            }

            Spacer(Modifier.height(14.dp))
            InputLabel("Task title")
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("What do you want to focus on?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = taskInputColors(),
            )

            Spacer(Modifier.height(12.dp))
            InputLabel("Description")
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Add a note") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = taskInputColors(),
            )

            Spacer(Modifier.height(12.dp))
            InputLabel("Date")
            Spacer(Modifier.height(6.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable { showDatePicker = true }
                    .semantics {
                        role = Role.Button
                        contentDescription = "Date, ${formatTaskDate(selectedDate)}"
                    },
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.9f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(formatTaskDate(selectedDate), style = TokiTypography.bodyTitle, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.height(14.dp))
            InputLabel("Timer duration")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(15, 25, 45, 60).forEach { minutes ->
                    val locked = minutes == 60 && !isPro
                    val isSelected = durationMinutes == minutes
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clickable {
                                if (locked) onUpgradeClick() else durationMinutes = minutes
                            }
                            .semantics {
                                role = Role.Button
                                contentDescription = if (locked) "$minutes minutes, Pro required" else "$minutes minutes"
                                selected = isSelected
                            },
                        shape = CircleShape,
                        color = if (isSelected) colors.primary else Color.Transparent,
                        border = if (isSelected) null else BorderStroke(1.dp, colors.outline),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${minutes}m",
                                style = TokiTypography.action,
                                color = if (isSelected) colors.onPrimary else colors.onSurface,
                            )
                            if (locked) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = colors.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            InputLabel("Tag")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                displayTags.forEach { tag ->
                    TagPill(
                        label = tag.name,
                        selected = selectedTag == tag.name,
                        onClick = { selectedTag = tag.name },
                    )
                }
                TagPill(
                    label = "New tag",
                    selected = false,
                    leadingAdd = true,
                    onClick = {
                        if (!isPro) onUpgradeClick() else showNewTag = true
                    },
                )
            }

            if (showNewTag) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        placeholder = { Text("Tag name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        colors = taskInputColors(),
                    )
                    IconButton(
                        onClick = {
                            val cleanName = newTagName.trim()
                            if (cleanName.isNotEmpty()) {
                                val existing = displayTags.firstOrNull { it.name.equals(cleanName, ignoreCase = true) }
                                if (existing == null) onCreateTag(cleanName, "#FFFFFFFF")
                                selectedTag = existing?.name ?: cleanName
                                newTagName = ""
                                showNewTag = false
                            }
                        },
                        enabled = newTagName.isNotBlank(),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Save tag")
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    onConfirm(title.trim(), description.trim(), selectedDate, durationMinutes, selectedTag)
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    disabledContainerColor = colors.primary.copy(alpha = 0.35f),
                    disabledContentColor = colors.onPrimary.copy(alpha = 0.6f),
                ),
            ) {
                Text("Add task", style = TokiTypography.button.copy(fontSize = 16.sp))
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", style = TokiTypography.action, color = colors.onSurfaceVariant)
            }
        }
    }

    if (showDatePicker) {
        TaskDatePickerDialog(
            initialDate = selectedDate,
            onDateSelected = {
                selectedDate = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
private fun InputLabel(text: String) {
    Text(text = text, style = TokiTypography.action, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun TagPill(
    label: String,
    selected: Boolean,
    leadingAdd: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingAdd) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(label)
            }
        },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.primary,
            selectedLabelColor = colors.onPrimary,
            selectedLeadingIconColor = colors.onPrimary,
            containerColor = Color.Transparent,
            labelColor = colors.onSurfaceVariant,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = colors.outline,
            selectedBorderColor = colors.primary,
        ),
    )
}

@Composable
private fun taskInputColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.onSurface,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
                enabled = pickerState.selectedDateMillis != null,
            ) { Text("Done") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = pickerState)
    }
}

private fun formatTaskDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
