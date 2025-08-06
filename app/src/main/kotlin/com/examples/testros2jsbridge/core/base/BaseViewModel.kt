package com.examples.testros2jsbridge.core.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

abstract class BaseViewModel<T> : ViewModel() {
    private val _data = MutableLiveData<T>()
    val data: LiveData<T> = _data

    protected fun updateData(value: T) {
        _data.value = value
    }
}