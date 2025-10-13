package com.examples.testros2jsbridge.data.local.database

import androidx.room.TypeConverter

class DatabaseConverters {
    @TypeConverter
    fun fromInputType(value: InputType): String = value.name

    @TypeConverter
    fun toInputType(value: String): InputType = InputType.valueOf(value)

    @TypeConverter
    fun fromRosProtocolType(value: RosProtocolType): String = value.name

    @TypeConverter
    fun toRosProtocolType(value: String): RosProtocolType = RosProtocolType.valueOf(value)
}