package com.examples.testros2jsbridge

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * RosApplication initializes DAOs, repositories, and error handling for the modular codebase.
 * Provides singletons for dependency injection throughout the app.
 */
@HiltAndroidApp
class RosApplication : Application()