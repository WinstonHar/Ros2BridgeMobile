package com.examples.testros2jsbridge.core.extension

import kotlinx.coroutines.flow.*
import com.examples.testros2jsbridge.domain.model.Publisher

object FlowExtension {
    fun <T> publisherListToFlow(publisherList: List<T>): Flow<T> {
        return publisherList.asFlow()
    }
}