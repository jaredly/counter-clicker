package com.example.counterclicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {

    private val viewModel: ClickerViewModel by viewModels {
        ClickerViewModelFactory(
            CounterRepository(applicationContext.clickerDataStore)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val count by viewModel.count.collectAsState()

            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0F0F10),
                    surface = Color(0xFF1B1B1F),
                    primary = Color(0xFFF5F5F5),
                    onPrimary = Color(0xFF0F0F10),
                    onBackground = Color(0xFFF5F5F5),
                    onSurface = Color(0xFFF5F5F5)
                )
            ) {
                AttendanceClickerScreen(
                    count = count,
                    onIncrement = viewModel::increment,
                    onDecrement = viewModel::decrement,
                    onSetCount = viewModel::setCount
                )
            }
        }
    }
}
