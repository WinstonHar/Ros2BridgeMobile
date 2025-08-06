package com.examples.testros2jsbridge.domain.usecase.messaging

import com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosMessageDto
import com.examples.testros2jsbridge.domain.model.RosId
import com.examples.testros2jsbridge.domain.repository.RosMessageRepository
import javax.inject.Inject

class GetMessageHistoryUseCase @Inject constructor(private val rosMessageRepository: RosMessageRepository) {

    suspend fun getMessages(topicId: RosId): List<RosMessageDto> {
        return rosMessageRepository.getMessagesByTopic(topicId)
    }
}