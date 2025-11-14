
---

## (root)
### RosApplication.kt
- **Path:** (root)
- **Description:** Initializes DAOs, repositories, and error handling for the modular codebase. Provides singletons for dependency injection throughout the app.
- **Dependencies:** android.app.Application, dagger.hilt.android.HiltAndroidApp

---

## core/base
### BaseRepository.kt
- **Path:** core/base
- **Description:** Base sealed class for repositories. Placeholder for shared repository logic.
- **Dependencies:** None

### BaseUseCase.kt
- **Path:** core/base
- **Description:** Base sealed class for use cases. Provides a suspendable execute function.
- **Dependencies:** None

### BaseViewModel.kt
- **Path:** core/base
- **Description:** Abstract ViewModel for LiveData management and update logic.
- **Dependencies:** androidx.lifecycle.LiveData, androidx.lifecycle.MutableLiveData, androidx.lifecycle.ViewModel

---

## core/error
### ErrorHandler.kt
- **Path:** core/error
- **Description:** Handles error events and exposes them as a shared flow for UI or logging.
- **Dependencies:** kotlinx.coroutines.flow.MutableSharedFlow, kotlinx.coroutines.flow.asSharedFlow

### ErrorMapper.kt
- **Path:** core/error
- **Description:** Maps exceptions to user-friendly error messages, including custom ROS exceptions.
- **Dependencies:** RosConnectionException, RosMessageException

### RosException.kt
- **Path:** core/error
- **Description:** Defines custom exceptions for ROS connection and message errors.
- **Dependencies:** Exception

---

## core/extension
### FlowExtension.kt
- **Path:** core/extension
- **Description:** Utility for converting lists to Kotlin Flows.
- **Dependencies:** kotlinx.coroutines.flow.Flow, kotlinx.coroutines.flow.asFlow

### JsonExtension.kt
- **Path:** core/extension
- **Description:** Utilities for serializing and deserializing Publisher objects to/from JSON.
- **Dependencies:** com.examples.testros2jsbridge.domain.model.Publisher, kotlinx.serialization.encodeToString, kotlinx.serialization.json.Json

### StringExtension.kt
- **Path:** core/extension
- **Description:** String formatting and case conversion utilities.
- **Dependencies:** None

---

## core/network
### ConnectionManager.kt
- **Path:** core/network
- **Description:** Manages the WebSocket connection to rosbridge, including connect/disconnect, message sending, and listener notification. Singleton interface for all ROS2 network communication.
- **Dependencies:** com.examples.testros2jsbridge.core.util.Logger, com.examples.testros2jsbridge.domain.repository.RosConnectionRepository, com.examples.testros2jsbridge.domain.usecase.connection.ConnectToRosUseCase, com.examples.testros2jsbridge.domain.usecase.connection.DisconnectFromRosUseCase, com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient, com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeWebSocketListener

### NetworkResult.kt
- **Path:** core/network
- **Description:** Sealed class representing the result of a network operation (success or failure).
- **Dependencies:** None

### RetryPolicy.kt
- **Path:** core/network
- **Description:** Object for configuring retry logic for network operations.
- **Dependencies:** None

---

## core/ros
### RosBridgeViewModel.kt
- **Path:** core/ros
- **Description:** Central ViewModel for all ROS2/rosbridge communication (messages, services, actions). Ported from legacy RosViewModel for compatibility.
- **Dependencies:** androidx.lifecycle.ViewModel, androidx.lifecycle.viewModelScope, com.examples.testros2jsbridge.core.util.Logger, com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient, dagger.hilt.android.lifecycle.HiltViewModel, kotlinx.coroutines, kotlinx.serialization, java.util.UUID, javax.inject.Inject

---

## core/util
### Constants.kt
- **Path:** core/util
- **Description:** Object for storing app-wide constant values.
- **Dependencies:** None

### JsonUtils.kt
- **Path:** core/util
- **Description:** Utility for serializing/deserializing objects to/from JSON using kotlinx.serialization.
- **Dependencies:** kotlinx.serialization.encodeToString, kotlinx.serialization.json.Json

### Logger.kt
- **Path:** core/util
- **Description:** Logging utility wrapping Android's Log class for debug/info/warn/error logging.
- **Dependencies:** android.util.Log

### UuidUtils.kt
- **Path:** core/util
- **Description:** Utility for converting UUID strings to byte array string representations.
- **Dependencies:** java.util.UUID, java.nio.ByteBuffer, Logger

### ValidationUtils.kt
- **Path:** core/util
- **Description:** Utility for validating IP addresses, ports, and ROS topic names.
- **Dependencies:** Kotlin Regex

---

## data/mapper
### AppActionMapper.kt
- **Path:** data/mapper
- **Description:** Maps AppAction domain models to AppActionEntity database entities, handling ROS protocol type and message type formatting.
- **Dependencies:** com.examples.testros2jsbridge.data.local.database.RosProtocolType, com.examples.testros2jsbridge.data.local.database.entities.AppActionEntity, com.examples.testros2jsbridge.domain.model.AppAction

### ControllerConfigMapper.kt
- **Path:** data/mapper
- **Description:** Maps ControllerConfig domain models to ControllerConfigEntity database entities and vice versa, including button assignments and joystick mappings.
- **Dependencies:** com.examples.testros2jsbridge.data.local.database.entities.ControllerConfigEntity, com.examples.testros2jsbridge.domain.model.AppAction, com.examples.testros2jsbridge.domain.model.ControllerConfig, com.examples.testros2jsbridge.domain.model.RosId

---

## data/repository
### AppActionRepositoryImpl.kt
- **Path:** data/repository
- **Description:** Implements AppActionRepository for managing custom app actions, including saving, retrieving, and advertising topics. Integrates with rosbridge and local database.
- **Dependencies:** android.content.Context, com.examples.testros2jsbridge.core.util.Logger, com.examples.testros2jsbridge.data.local.database.RosProtocolType, com.examples.testros2jsbridge.data.local.database.dao.AppActionDao, com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient, kotlinx.coroutines.flow, org.json.JSONObject, javax.inject.Inject, javax.inject.Singleton

### ConfigurationRepositoryImpl.kt
- **Path:** data/repository
- **Description:** Implements ConfigurationRepository for managing app configuration, loading/saving from SharedPreferences and YAML files.
- **Dependencies:** android.content.Context, android.content.SharedPreferences, com.examples.testros2jsbridge.domain.model.*, com.google.gson.Gson, org.yaml.snakeyaml.Yaml, kotlinx.coroutines.flow, javax.inject.Inject, javax.inject.Singleton

### ControllerRepositoryImpl.kt
- **Path:** data/repository
- **Description:** Implements ControllerRepository for managing controllers, presets, and button maps, integrating with local database.
- **Dependencies:** com.examples.testros2jsbridge.data.local.database.dao.ControllerDao, com.examples.testros2jsbridge.domain.model.*, kotlinx.coroutines.flow, javax.inject.Inject

### ProtocolRepositoryImpl.kt
- **Path:** data/repository
- **Description:** Implements ProtocolRepository for managing custom protocol files and app actions. (TODO: Protocol file loading/import not yet implemented.)
- **Dependencies:** android.content.Context, com.examples.testros2jsbridge.data.local.database.dao.AppActionDao, com.examples.testros2jsbridge.data.mapper.*, com.examples.testros2jsbridge.domain.model.*, kotlinx.coroutines.flow, javax.inject.Inject, javax.inject.Singleton

### PublisherRepositoryImpl.kt
- **Path:** data/repository
- **Description:** Implements PublisherRepository for managing publishers, saving and retrieving from local database.
- **Dependencies:** com.examples.testros2jsbridge.data.local.database.dao.PublisherDao, com.examples.testros2jsbridge.domain.model.*, kotlinx.coroutines.flow, javax.inject.Inject, javax.inject.Singleton

### RosConnectionRepositoryImpl.kt
- **Path:** data/repository
- **Description:** Implements RosConnectionRepository for managing ROS connection objects in memory.
- **Dependencies:** com.examples.testros2jsbridge.domain.model.*, kotlinx.coroutines.flow, javax.inject.Inject, javax.inject.Singleton

### RosMessageRepositoryImpl.kt
- **Path:** data/repository
- **Description:** Implements RosMessageRepository for managing ROS messages, including saving, deleting, and publishing messages.
- **Dependencies:** com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosMessageDto, com.examples.testros2jsbridge.domain.model.*, kotlinx.coroutines.flow, javax.inject.Inject, javax.inject.Singleton

### RosServiceRepositoryImpl.kt
- **Path:** data/repository
- **Description:** Implements RosServiceRepository for managing ROS services, including service requests, responses, and busy lock management. (Some methods are TODO.)
- **Dependencies:** com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosServiceDto, com.examples.testros2jsbridge.domain.model.*, kotlinx.coroutines.flow, javax.inject.Inject, javax.inject.Singleton

### RosTopicRepositoryImpl.kt
- **Path:** data/repository
- **Description:** Implements RosTopicRepository for managing topic subscriptions and fetching available topics from ROS bridge.
- **Dependencies:** com.examples.testros2jsbridge.data.remote.rosbridge.dto.RosTopicDto, com.examples.testros2jsbridge.domain.model.RosTopic, kotlinx.coroutines.flow, javax.inject.Inject, javax.inject.Singleton

### SubscriberRepositoryImpl.kt
- **Path:** data/repository
- **Description:** Implements SubscriberRepository for managing subscribers, saving, deleting, and subscribing/unsubscribing to topics.
- **Dependencies:** com.examples.testros2jsbridge.domain.model.Subscriber, kotlinx.coroutines.flow, javax.inject.Inject, javax.inject.Singleton

---

## di
### AppModule.kt
- **Path:** di
- **Description:** Provides application-wide dependencies and event dispatchers for rosbridge events using Dagger Hilt.
- **Dependencies:** android.content.Context, dagger.hilt, com.examples.testros2jsbridge.core.error.ErrorHandler, com.examples.testros2jsbridge.data.repository.*, com.examples.testros2jsbridge.domain.repository.*

### DatabaseModule.kt
- **Path:** di
- **Description:** Provides Room database and DAO dependencies for the app using Dagger Hilt.
- **Dependencies:** android.content.Context, androidx.room.Room, com.examples.testros2jsbridge.data.local.database.*, dagger.hilt.*

### NetworkModule.kt
- **Path:** di
- **Description:** Provides network-related dependencies, including RosbridgeClient and ConnectionManager, using Dagger Hilt.
- **Dependencies:** com.examples.testros2jsbridge.data.remote.rosbridge.RosbridgeClient, dagger.hilt.*

### RepositoryModule.kt
- **Path:** di
- **Description:** Centralizes dependency wiring for repositories, enabling easier testing and abstraction.
- **Dependencies:** com.examples.testros2jsbridge.data.repository.*, com.examples.testros2jsbridge.domain.repository.*, dagger.hilt.*

### ViewModelModule.kt
- **Path:** di
- **Description:** Configures ViewModel injection for Hilt; no manual providers needed.
- **Dependencies:** dagger.hilt.*

---

## data/local/database/dao
### appActionDAO.kt
- **Path:** data/local/database/dao
- **Description:** Room DAO for managing AppActionEntity objects, including insert, update, delete, and query operations.
- **Dependencies:** androidx.room.*, com.examples.testros2jsbridge.data.local.database.entities.AppActionEntity, kotlinx.coroutines.flow.Flow

### ConnectionDao.kt
- **Path:** data/local/database/dao
- **Description:** Room DAO for managing ConnectionEntity objects, including CRUD operations and queries for active connections.
- **Dependencies:** androidx.room.*, com.examples.testros2jsbridge.data.local.database.entities.ConnectionEntity, kotlinx.coroutines.flow.Flow

### ControllerConfigDao.kt
- **Path:** data/local/database/dao
- **Description:** Room DAO for managing ControllerConfigEntity objects, including insert and query operations.
- **Dependencies:** androidx.room.*, com.examples.testros2jsbridge.data.local.database.entities.ControllerConfigEntity

### controllerDAO.kt
- **Path:** data/local/database/dao
- **Description:** Room DAO for managing controllers, presets, and button maps, including insertions and complex queries with relations.
- **Dependencies:** androidx.room.*, com.examples.testros2jsbridge.data.local.database.*, com.examples.testros2jsbridge.data.local.database.entities.*, com.examples.testros2jsbridge.data.local.database.relations.*, kotlinx.coroutines.flow.Flow

### GeometryMessageDao.kt
- **Path:** data/local/database/dao
- **Description:** Room DAO for managing GeometryMessageEntity objects, including CRUD operations and queries for latest messages by topic.
- **Dependencies:** androidx.room.*, com.examples.testros2jsbridge.data.local.database.entities.GeometryMessageEntity, kotlinx.coroutines.flow.Flow

### PublisherDao.kt
- **Path:** data/local/database/dao
- **Description:** Room DAO for managing PublisherEntity objects, including CRUD operations and queries for publishers.
- **Dependencies:** androidx.room.*, com.examples.testros2jsbridge.data.local.database.entities.PublisherEntity, kotlinx.coroutines.flow.Flow

### SubscriberDao.kt
- **Path:** data/local/database/dao
- **Description:** Room DAO for managing SubscriberEntity objects, including CRUD operations and queries for subscribers.
- **Dependencies:** androidx.room.*, com.examples.testros2jsbridge.data.local.database.entities.SubscriberEntity, kotlinx.coroutines.flow.Flow

---

## data/local/database/entities
### appActionEntity.kt
- **Path:** data/local/database/entities
- **Description:** Room entity for storing app actions, including topic, message type, protocol type, and related metadata.
- **Dependencies:** androidx.room.Entity, androidx.room.PrimaryKey, com.examples.testros2jsbridge.data.local.database.RosProtocolType

### buttonMapEntity.kt
- **Path:** data/local/database/entities
- **Description:** Room entity for mapping controller buttons to actions, with joystick settings and foreign key to AppActionEntity.
- **Dependencies:** androidx.room.Entity, androidx.room.ForeignKey, androidx.room.PrimaryKey

### ButtonPresetsEntity.kt
- **Path:** data/local/database/entities
- **Description:** Room entity for button presets, with foreign key to ControllerEntity.
- **Dependencies:** androidx.room.Entity, androidx.room.ForeignKey, androidx.room.PrimaryKey

### ConnectionEntity.kt
- **Path:** data/local/database/entities
- **Description:** Room entity for storing connection information, including host, port, and connection status.
- **Dependencies:** androidx.room.Entity, androidx.room.PrimaryKey

### ControllerButtonFixedMapJunction.kt
- **Path:** data/local/database/entities
- **Description:** Room junction entity for many-to-many relationship between controllers and fixed button maps.
- **Dependencies:** androidx.room.Entity, androidx.room.ForeignKey

### ControllerButtonPresetJunction.kt
- **Path:** data/local/database/entities
- **Description:** Room junction entity for many-to-many relationship between controllers and button presets.
- **Dependencies:** androidx.room.Entity, androidx.room.ForeignKey

### ControllerConfigEntity.kt
- **Path:** data/local/database/entities
- **Description:** Room entity for controller configuration, including addressing mode, sensitivity, assignments, and joystick mappings.
- **Dependencies:** androidx.room.Entity, androidx.room.PrimaryKey, com.examples.testros2jsbridge.domain.model.JoystickMapping

### controllerEntity.kt
- **Path:** data/local/database/entities
- **Description:** Room entity for controllers, storing controller ID and name.
- **Dependencies:** androidx.room.Entity, androidx.room.PrimaryKey

### GeometryMessageEntity.kt
- **Path:** data/local/database/entities
- **Description:** Room entity for geometry messages, including topic, coordinates, and timestamp.
- **Dependencies:** androidx.room.Entity, androidx.room.PrimaryKey

### PresetButtonMapJunction.kt
- **Path:** data/local/database/entities
- **Description:** Room junction entity for many-to-many relationship between button presets and button maps.
- **Dependencies:** androidx.room.Entity, androidx.room.ForeignKey

---

## data/local/database/relations
### ControllerWithButtonMaps.kt
- **Path:** data/local/database/relations
- **Description:** Room relation for controllers and their associated button maps using a junction table.
- **Dependencies:** androidx.room.Embedded, androidx.room.Relation, androidx.room.Junction, com.examples.testros2jsbridge.data.local.database.entities.*

### PresetWithButtonMaps.kt
- **Path:** data/local/database/relations
- **Description:** Room relation for button presets and their associated button maps using a junction table.
- **Dependencies:** androidx.room.Embedded, androidx.room.Relation, androidx.room.Junction, com.examples.testros2jsbridge.data.local.database.entities.*

---

## data/remote/rosbridge
### RosbridgeClient.kt
- **Path:** data/remote/rosbridge
- **Description:** Manages the WebSocket connection to rosbridge, message queueing, and event listeners. Uses OkHttp for networking.
- **Dependencies:** com.examples.testros2jsbridge.core.util.Logger, okhttp3.*, javax.inject.*

### RosbridgeWebSocketListener.kt
- **Path:** data/remote/rosbridge
- **Description:** WebSocket listener that forwards events to provided callbacks for rosbridge communication.
- **Dependencies:** okhttp3.WebSocketListener, okhttp3.Response, okhttp3.WebSocket

---

## data/remote/rosbridge/dto
### RosActionDto.kt
- **Path:** data/remote/rosbridge/dto
- **Description:** Data Transfer Object for ROS actions, including goal, result, and feedback field values for network communication.
- **Dependencies:** None

### RosMessageDto.kt
- **Path:** data/remote/rosbridge/dto
- **Description:** Data Transfer Object for ROS messages, including topic, type, content, and metadata for network communication.
- **Dependencies:** com.examples.testros2jsbridge.domain.model.RosId

### RosServiceDto.kt
- **Path:** data/remote/rosbridge/dto
- **Description:** Data Transfer Object for ROS services, including request and response fields for network communication.
- **Dependencies:** None

### RosTopicDto.kt
- **Path:** data/remote/rosbridge/dto
- **Description:** Data Transfer Object for ROS topics, including name and type for network communication.
- **Dependencies:** None

---

## presentation/mapper
### ControllerUiMapper.kt
- **Path:** presentation/mapper
- **Description:** Maps ControllerConfig domain models to ControllerUiState for UI, and vice versa.
- **Dependencies:** com.examples.testros2jsbridge.domain.model.ControllerConfig, com.examples.testros2jsbridge.presentation.state.ControllerUiState

### MessageUiMapper.kt
- **Path:** presentation/mapper
- **Description:** Maps RosMessage domain models to GeometryUiState for UI, formats message content and timestamps, and extracts key fields from JSON.
- **Dependencies:** com.examples.testros2jsbridge.domain.model.RosMessage, com.examples.testros2jsbridge.presentation.state.GeometryUiState, kotlinx.serialization.json.Json

### PublisherUiMapper.kt
- **Path:** presentation/mapper
- **Description:** Maps Publisher domain models to PublisherUiModel for UI display.
- **Dependencies:** com.examples.testros2jsbridge.domain.model.Publisher

### SubscriberUiMapper.kt
- **Path:** presentation/mapper
- **Description:** Maps Subscriber domain models to SubscriberUiModel for UI display.
- **Dependencies:** com.examples.testros2jsbridge.domain.model.Subscriber

---

## presentation/state
### ConnectionUiState.kt
- **Path:** presentation/state
- **Description:** UI state for managing ROS connection, including connection status, error dialogs, and input fields.
- **Dependencies:** com.examples.testros2jsbridge.domain.model.RosConnection

### ControllerUiState.kt
- **Path:** presentation/state
- **Description:** UI state for controller configuration, wrapping ControllerConfig and adding UI flags/fields.
- **Dependencies:** com.examples.testros2jsbridge.domain.model.ControllerConfig, com.examples.testros2jsbridge.domain.model.ControllerPreset

### GeometryUiState.kt
- **Path:** presentation/state
- **Description:** UI state for geometry message management and visualization, including message list, selection, and error handling.
- **Dependencies:** com.examples.testros2jsbridge.domain.model.RosMessage

### ProtocolUiState.kt
- **Path:** presentation/state
- **Description:** UI state for protocol management, including available messages, services, actions, and import status.
- **Dependencies:** None

### PublisherUiState.kt
- **Path:** presentation/state
- **Description:** UI state for custom publisher management and message publishing, including input fields and message history.
- **Dependencies:** com.examples.testros2jsbridge.domain.model.Publisher

### SettingUiState.kt
- **Path:** presentation/state
- **Description:** UI state for app settings management, including theme, language, notifications, and error handling.
- **Dependencies:** None

---

## presentation/ui/components
### ControllerButton.kt
- **Path:** presentation/ui/components
- **Description:** Composable for rendering a controller button in the UI, with label and action callbacks.
- **Dependencies:** androidx.compose.material3.*, androidx.compose.runtime.Composable, com.examples.testros2jsbridge.domain.model.AppAction

### MessageEditor.kt
- **Path:** presentation/ui/components
- **Description:** Composable for editing and saving message content, with support for change and delete callbacks.
- **Dependencies:** androidx.compose.foundation.layout.*, androidx.compose.material3.*, androidx.compose.runtime.*, androidx.compose.ui.Modifier

### MessageHistoryList.kt
- **Path:** presentation/ui/components
- **Description:** Composable for displaying a collapsible list of message history in the UI.
- **Dependencies:** androidx.compose.foundation.*, androidx.compose.material3.*, androidx.compose.runtime.*, androidx.compose.ui.*

### RosConnectionCard.kt
- **Path:** presentation/ui/components
- **Description:** Composable for displaying and managing ROS2 connection settings in the UI.
- **Dependencies:** androidx.compose.foundation.layout.*, androidx.compose.material3.*, androidx.compose.runtime.*, androidx.compose.ui.Modifier

### TopicSelector.kt
- **Path:** presentation/ui/components
- **Description:** Composable for selecting a topic from a list of AppActions in the UI.
- **Dependencies:** androidx.compose.foundation.*, androidx.compose.material3.*, androidx.compose.runtime.*, androidx.compose.ui.Modifier, com.examples.testros2jsbridge.domain.model.AppAction

---

## presentation/ui/navigation

### NavigationArgs.kt
- **Path:** presentation/ui/navigation
- **Description:** Type-safe navigation arguments for passing between destinations, using sealed classes for all major domain types.
- **Dependencies:** java.io.Serializable, com.examples.testros2jsbridge.domain.model.*

---

## presentation/ui/screens/connection
### ConnectionScreen.kt
- **Path:** presentation/ui/screens/connection
- **Description:** Composable screen for managing and displaying ROS2 connection settings, using RosConnectionCard and ViewModel state.
- **Dependencies:** androidx.compose.*, com.examples.testros2jsbridge.presentation.ui.components.RosConnectionCard, com.ramcosta.composedestinations.annotation.Destination, hiltViewModel

### ConnectionViewModel.kt
- **Path:** presentation/ui/screens/connection
- **Description:** ViewModel for connection screen, manages connection state, preferences, and integrates with ConnectionManager.
- **Dependencies:** android.app.Application, androidx.lifecycle.AndroidViewModel, com.examples.testros2jsbridge.core.network.ConnectionManager, com.examples.testros2jsbridge.presentation.state.ConnectionUiState, dagger.hilt.android.lifecycle.HiltViewModel, kotlinx.coroutines.flow, javax.inject.Inject

---

## presentation/ui/screens/controller
### ControllerConfigScreen.kt
- **Path:** presentation/ui/screens/controller
- **Description:** Composable screen for configuring controller settings, including joystick mappings and topic selection.
- **Dependencies:** androidx.compose.*, com.examples.testros2jsbridge.domain.model.JoystickMapping, com.examples.testros2jsbridge.presentation.ui.components.TopicSelector, com.ramcosta.composedestinations.annotation.Destination, hiltViewModel

### ControllerOverviewScreen.kt
- **Path:** presentation/ui/screens/controller
- **Description:** Composable screen for displaying an overview of controller configurations and presets, with support for key events and focus management.
- **Dependencies:** androidx.compose.*, androidx.compose.ui.*, androidx.compose.material3.*, androidx.compose.runtime.*, androidx.compose.foundation.*, androidx.compose.ui.focus.FocusRequester

### ControllerScreen.kt
- **Path:** presentation/ui/screens/controller
- **Description:** Composable screen for managing controller input, assignments, and configuration, with support for key events and dropdowns.
- **Dependencies:** androidx.compose.*, androidx.compose.ui.*, androidx.compose.material3.*, androidx.compose.runtime.*, androidx.compose.foundation.*, androidx.compose.ui.focus.FocusRequester

### ControllerViewModel.kt
- **Path:** presentation/ui/screens/controller
- **Description:** ViewModel for controller screens, manages controller configs, presets, and integrates with domain use cases and repositories.
- **Dependencies:** androidx.lifecycle.ViewModel, dagger.hilt.android.lifecycle.HiltViewModel, kotlinx.coroutines.flow, com.examples.testros2jsbridge.domain.model.*, com.examples.testros2jsbridge.domain.repository.*, com.examples.testros2jsbridge.presentation.state.ControllerUiState, javax.inject.Inject

---

## presentation/ui/screens/publisher

### EditPublisherDialog.kt
- **Path:** presentation/ui/screens/publisher
- **Description:** Composable dialog for editing publisher details, including topic, type, message, and label.
- **Dependencies:** androidx.compose.*, com.examples.testros2jsbridge.domain.model.Publisher

### ProtocolViewModel.kt
- **Path:** presentation/ui/screens/publisher
- **Description:** ViewModel for managing custom protocol actions and fields, integrates with AppActionRepository and RosBridgeViewModel.
- **Dependencies:** androidx.lifecycle.ViewModel, dagger.hilt.android.lifecycle.HiltViewModel, kotlinx.coroutines.flow, com.examples.testros2jsbridge.domain.model.*, com.examples.testros2jsbridge.presentation.state.ProtocolUiState, javax.inject.Inject

### PublisherScreen.kt
- **Path:** presentation/ui/screens/publisher
- **Description:** Composable screen for managing publishers, editing, deleting, and displaying publisher list and details.
- **Dependencies:** androidx.compose.*, androidx.compose.material3.*, androidx.compose.runtime.*, com.examples.testros2jsbridge.domain.model.Publisher

---

## presentation/ui/screens/settings
### SettingScreen.kt
- **Path:** presentation/ui/screens/settings
- **Description:** Composable screen for managing app settings, including theme, language, notifications, and reconnect options.
- **Dependencies:** androidx.compose.*, com.ramcosta.composedestinations.annotation.Destination, hiltViewModel

### SettingsViewModel.kt
- **Path:** presentation/ui/screens/settings
- **Description:** ViewModel for settings screen, manages app configuration state and integrates with ConfigurationRepository.
- **Dependencies:** androidx.lifecycle.ViewModel, dagger.hilt.android.lifecycle.HiltViewModel, kotlinx.coroutines.flow, com.examples.testros2jsbridge.domain.model.AppConfiguration, com.examples.testros2jsbridge.domain.repository.ConfigurationRepository, com.examples.testros2jsbridge.presentation.state.SettingUiState, javax.inject.Inject

---

## presentation/ui/screens/subscriber
### SubscriberScreen.kt
- **Path:** presentation/ui/screens/subscriber
- **Description:** Composable screen for managing ROS2 topic subscriptions, displaying message history and topic details.
- **Dependencies:** androidx.compose.*, com.examples.testros2jsbridge.presentation.ui.components.CollapsibleMessageHistoryList, com.ramcosta.composedestinations.annotation.Destination, hiltViewModel

### SubscriberViewModel.kt
- **Path:** presentation/ui/screens/subscriber
- **Description:** ViewModel for managing ROS2 topic subscriptions, topic discovery, and message updates. Integrates with DAOs, repositories, and ConnectionManager.
- **Dependencies:** androidx.lifecycle.ViewModel, dagger.hilt.android.lifecycle.HiltViewModel, kotlinx.coroutines.flow, com.examples.testros2jsbridge.domain.model.*, com.examples.testros2jsbridge.domain.repository.*, com.examples.testros2jsbridge.presentation.state.SubscriberUiState, javax.inject.Inject

### TopicListScreen.kt
- **Path:** presentation/ui/screens/subscriber
- **Description:** Composable screen for displaying available ROS2 topics and subscribing to them.
- **Dependencies:** androidx.compose.*, com.ramcosta.composedestinations.annotation.Destination, hiltViewModel

---

## presentation/ui/theme
### Color.kt
- **Path:** presentation/ui/theme
- **Description:** Defines color schemes for light and dark themes in the app using Compose Material3.
- **Dependencies:** androidx.compose.material3.*, androidx.compose.ui.graphics.Color

### Themes.kt
- **Path:** presentation/ui/theme
- **Description:** Provides the main Compose theme for the app, supporting dark and light mode.
- **Dependencies:** androidx.compose.foundation.isSystemInDarkTheme, androidx.compose.runtime.Composable

### Type.kt
- **Path:** presentation/ui/theme
- **Description:** Defines typography styles for the app using Compose Material3 Typography.
- **Dependencies:** androidx.compose.material3.Typography, androidx.compose.ui.text.*, androidx.compose.ui.unit.sp

---

