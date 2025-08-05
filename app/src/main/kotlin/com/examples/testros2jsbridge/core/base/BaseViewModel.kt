package com.examples.testros2jsbridge.core.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel

abstract class BaseViewModel<T> : ViewModel() {
    val data: LiveData<T> = MutableLiveData()

    protected fun updateData(value: T) {
        data.value = value
    }
}