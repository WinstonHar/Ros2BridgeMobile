package com.example.testros2jsbridge

import android.app.Application
import androidx.lifecycle.ViewModelStore

class MyApp : Application() {
    // Application-wide ViewModelStore for sharing ViewModels across activities
    val appViewModelStore: ViewModelStore by lazy { ViewModelStore() }
}
