package com.example.counterclicker

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.withFrameNanos

private val AppBackground = Color(0xFF0F0F10)
private val PrimaryText = Color(0xFFF5F5F5)
private val SecondaryButton = Color(0xFF242427)
private val DangerText = Color(0xFFFF6B6B)

@Composable
fun AttendanceClickerScreen(
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onSetCount: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var editVisible by rememberSaveable { mutableStateOf(false) }
    var editText by rememberSaveable { mutableStateOf("") }
    val performIncrementHaptic = rememberIncrementHaptic()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(20.dp)
            .systemBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = count.toString(),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Current attendance count $count"
                    liveRegion = LiveRegionMode.Polite
                },
            color = PrimaryText,
            fontSize = 96.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false
        )

        Button(
            onClick = {
                performIncrementHaptic()
                onIncrement()
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 260.dp)
                .semantics { contentDescription = "Increment attendance count" },
            shape = MaterialTheme.shapes.extraLarge,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryText,
                contentColor = AppBackground
            )
        ) {
            Text(
                text = "+1",
                fontSize = 72.sp,
                fontWeight = FontWeight.Black
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Button(
                onClick = onDecrement,
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .semantics { contentDescription = "Decrement attendance count" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondaryButton,
                    contentColor = DangerText
                )
            ) {
                Text("−1", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }

            Button(
                onClick = {
                    editText = count.toString()
                    editVisible = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .semantics { contentDescription = "Edit attendance count" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondaryButton,
                    contentColor = PrimaryText
                )
            ) {
                Text("Edit", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }

    if (editVisible) {
        EditCountDialog(
            initialText = editText,
            onDismiss = { editVisible = false },
            onSave = { newCount ->
                onSetCount(newCount)
                editVisible = false
            }
        )
    }
}

@Composable
fun EditCountDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var text by rememberSaveable(initialText) { mutableStateOf(initialText) }
    var showError by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun saveIfValid() {
        val parsed = text.toIntOrNull()
        if (parsed == null || parsed < 0) {
            showError = true
        } else {
            onSave(parsed)
        }
    }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit count") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    showError = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                label = { Text("Count") },
                isError = showError,
                supportingText = {
                    if (showError) {
                        Text("Enter a valid number 0 or higher.")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { saveIfValid() })
            )
        },
        confirmButton = {
            TextButton(onClick = { saveIfValid() }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun rememberIncrementHaptic(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
        }
    }
}
