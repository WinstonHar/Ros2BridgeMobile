package com.examples.testros2jsbridge.presentation.mapper

import com.examples.testros2jsbridge.domain.model.Subscriber

object SubscriberUiMapper {
    fun toUiModel(subscriber: Subscriber): SubscriberUiModel =
        SubscriberUiModel(
            id = subscriber.id ?: "",
            label = subscriber.label ?: subscriber.topic.value,
            isActive = subscriber.isActive,
            lastMessage = subscriber.lastMessage
        )
}

data class SubscriberUiModel(
    val id: String,
    val label: String,
    val isActive: Boolean,
    val lastMessage: String?
)