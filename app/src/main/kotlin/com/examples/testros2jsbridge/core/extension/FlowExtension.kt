package com.examples.testros2jsbridge.core.extension

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

object FlowExtension {
    fun <T> publisherListToFlow(publisherList: List<T>): Flow<T> {
        return publisherList.asFlow()
    }
}