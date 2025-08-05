package com.examples.testros2jsbridge.presentation.mapper

import com.examples.testros2jsbridge.domain.model.Publisher

object PublisherUiMapper {
    fun toUiModel(publisher: Publisher): PublisherUiModel =
        PublisherUiModel(
            id = publisher.id?.value ?: "",
            label = publisher.label ?: publisher.topic.value,
            isEnabled = publisher.isEnabled,
            lastPublished = publisher.lastPublishedTimestamp
        )
}

data class PublisherUiModel(
    val id: String,
    val label: String,
    val isEnabled: Boolean,
    val lastPublished: Long?
)