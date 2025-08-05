package com.examples.testros2jsbridge

import android.app.Application
import com.examples.testros2jsbridge.data.local.database.dao.SubscriberDao
import com.examples.testros2jsbridge.data.local.database.dao.PublisherDao
import com.examples.testros2jsbridge.data.local.database.dao.ControllerDao
import com.examples.testros2jsbridge.data.repository.*
import com.examples.testros2jsbridge.domain.repository.*
import com.examples.testros2jsbridge.presentation.ui.screens.subscriber.ErrorHandler

/**
 * RosApplication initializes DAOs, repositories, and error handling for the modular codebase.
 * Provides singletons for dependency injection throughout the app.
 */
class RosApplication : Application() {
    lateinit var subscriberDao: SubscriberDao
    lateinit var publisherDao: PublisherDao
    lateinit var controllerDao: ControllerDao

    lateinit var configurationRepository: ConfigurationRepository
    lateinit var controllerRepository: ControllerRepository
    lateinit var publisherRepository: PublisherRepository
    lateinit var rosConnectionRepository: RosConnectionRepository
    lateinit var rosMessageRepository: RosMessageRepository
    lateinit var rosServiceRepository: RosServiceRepository
    lateinit var rosActionRepository: RosActionRepository
    lateinit var rosTopicRepository: RosTopicRepository

    lateinit var errorHandler: ErrorHandler

    // RosbridgeClient singleton
    lateinit var rosbridgeClient: RosbridgeClient

    override fun onCreate() {
        super.onCreate()
        // Initialize DAOs
        subscriberDao = SubscriberDao()
        publisherDao = PublisherDao()
        controllerDao = ControllerDao()

        // Instantiate RosbridgeClient singleton
        rosbridgeClient = RosbridgeClient()

        // Initialize repositories
        configurationRepository = ConfigurationRepositoryImpl(
            context = this,
            prefs = getSharedPreferences("ros2_prefs", MODE_PRIVATE)
        )
        controllerRepository = ControllerRepositoryImpl(
            context = this,
            prefs = getSharedPreferences("ros2_prefs", MODE_PRIVATE)
        )
        publisherRepository = PublisherRepositoryImpl(publisherDao)
        rosConnectionRepository = RosConnectionRepositoryImpl(rosbridgeClient)
        rosMessageRepository = RosMessageRepositoryImpl(rosbridgeClient)
        rosServiceRepository = RosServiceRepositoryImpl(rosbridgeClient)
        rosActionRepository = RosActionRepositoryImpl(rosbridgeClient)
        rosTopicRepository = RosTopicRepositoryImpl(rosbridgeClient)

        // Error handler
        errorHandler = ErrorHandler()
    }
}