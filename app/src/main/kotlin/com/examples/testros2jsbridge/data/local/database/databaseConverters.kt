package com.examples.testros2jsbridge.data.local.database

import androidx.room.TypeConverter
import com.examples.testros2jsbridge.domain.model.JoystickMapping
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DatabaseConverters {
    @TypeConverter
    fun fromInputType(value: InputType): String = value.name

    @TypeConverter
    fun toInputType(value: String): InputType = InputType.valueOf(value)

    @TypeConverter
    fun fromRosProtocolType(value: RosProtocolType): String = value.name

    @TypeConverter
    fun toRosProtocolType(value: String): RosProtocolType = RosProtocolType.valueOf(value)

    @TypeConverter
    fun fromButtonAssignments(value: Map<String, String>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toButtonAssignments(value: String): Map<String, String> {
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return Gson().fromJson(value, mapType)
    }

    @TypeConverter
    fun fromJoystickMappings(value: List<JoystickMapping>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toJoystickMappings(value: String): List<JoystickMapping> {
        val listType = object : TypeToken<List<JoystickMapping>>() {}.type
        return Gson().fromJson(value, listType)
    }
}