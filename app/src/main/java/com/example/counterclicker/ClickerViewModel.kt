package com.example.counterclicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClickerViewModel(
    private val repository: CounterRepository
) : ViewModel() {

    val count: StateFlow<Int> =
        repository.countFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    fun increment() {
        viewModelScope.launch {
            repository.increment()
        }
    }

    fun decrement() {
        viewModelScope.launch {
            repository.decrement()
        }
    }

    fun setCount(value: Int) {
        viewModelScope.launch {
            repository.setCount(value)
        }
    }
}

class ClickerViewModelFactory(
    private val repository: CounterRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClickerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClickerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
