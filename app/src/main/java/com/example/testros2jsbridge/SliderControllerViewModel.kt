package com.example.testros2jsbridge

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Holds the state of all sliders and provides increment/decrement actions for controller integration
class SliderControllerViewModel(application: Application) : AndroidViewModel(application) {
    data class SliderState(
        val name: String,
        val topic: String,
        val type: String,
        val min: Float,
        val max: Float,
        val step: Float,
        var value: Float
    )

    private val _sliders = MutableStateFlow<List<SliderState>>(emptyList())
    val sliders: StateFlow<List<SliderState>> = _sliders

    private val _selectedSliderIndex = MutableStateFlow(0)

    fun setSliders(sliderStates: List<SliderState>) {
        // Snap each slider value to the nearest valid step
        fun snapValue(min: Float, step: Float, value: Float): Float {
            if (step <= 0f) return value
            val n = Math.round((value - min) / step)
            val snapped = min + n * step
            return snapped
        }
        val snappedSliders = sliderStates.map { s ->
            val snappedValue = snapValue(s.min, s.step, s.value).coerceIn(s.min, s.max)
            s.copy(value = snappedValue)
        }
        _sliders.value = snappedSliders
    }

    fun selectSlider(index: Int) {
        if (index in _sliders.value.indices) {
            _selectedSliderIndex.value = index
        }
    }

    fun incrementSelectedSlider() {
        val idx = _selectedSliderIndex.value
        val slidersList = _sliders.value.toMutableList()
        if (idx in slidersList.indices) {
            val s = slidersList[idx]
            val newValue = (s.value + s.step).coerceAtMost(s.max)
            slidersList[idx] = s.copy(value = newValue)
            _sliders.value = slidersList
        }
    }

    fun decrementSelectedSlider() {
        val idx = _selectedSliderIndex.value
        val slidersList = _sliders.value.toMutableList()
        if (idx in slidersList.indices) {
            val s = slidersList[idx]
            val newValue = (s.value - s.step).coerceAtLeast(s.min)
            slidersList[idx] = s.copy(value = newValue)
            _sliders.value = slidersList
        }
    }

    fun getSelectedSlider(): SliderState? = _sliders.value.getOrNull(_selectedSliderIndex.value)
}
