package com.examples.testros2jsbridge.presentation.ui.navigation

import androidx.navigation.NavController

object BackNavigationHandler {
    fun handleBack(navController: NavController, onEmptyBackStack: (() -> Unit)? = null) {
        if (!navController.popBackStack()) {
            // If the back stack is empty, invoke the provided callback or close the app
            onEmptyBackStack?.invoke()
        }
    }
}
