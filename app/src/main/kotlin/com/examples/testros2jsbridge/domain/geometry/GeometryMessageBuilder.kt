package com.examples.testros2jsbridge.domain.geometry

object GeometryMessageBuilder {
    /**
     * Builds a geometry_msgs message JSON string from the type and field values.
     * This logic is ported from the legacy Fragment and Compose ViewModel.
     * @param type The geometry message type (e.g., "TwistStamped").
     * @param fields The map of field names to values (all as strings).
     * @return JSON string for the message, with numbers/booleans as native JSON types.
     */
    fun build(type: String, fields: Map<String, String>): String {
        fun f(tag: String, def: String = "0.0") = fields[tag]?.takeIf { it.isNotBlank() } ?: def
        fun i(tag: String, def: String = "0") = fields[tag]?.takeIf { it.isNotBlank() } ?: def
        fun s(tag: String, def: String = "") = fields[tag]?.takeIf { it.isNotBlank() } ?: def

        fun vector3(prefix: String = ""): String =
            "\"x\":${f(if (prefix.isEmpty()) "x" else "${prefix}_x")}," +
            "\"y\":${f(if (prefix.isEmpty()) "y" else "${prefix}_y")}," +
            "\"z\":${f(if (prefix.isEmpty()) "z" else "${prefix}_z")}" 
        fun quaternion(prefix: String = ""): String =
            "\"x\":${f(if (prefix.isEmpty()) "x" else "${prefix}_x")}," +
            "\"y\":${f(if (prefix.isEmpty()) "y" else "${prefix}_y")}," +
            "\"z\":${f(if (prefix.isEmpty()) "z" else "${prefix}_z")}," +
            "\"w\":${f(if (prefix.isEmpty()) "w" else "${prefix}_w")}" 
        fun header(): String =
            "\"frame_id\":\"${s("header_frame_id")}" + "\""
        fun point32Array(label: String = "points"): String =
            (0 until 3).joinToString(",") { i ->
                "{\"x\":${f("${label}_${i}_x")},\"y\":${f("${label}_${i}_y")},\"z\":${f("${label}_${i}_z")}}"
            }
        fun poseArray(label: String = "poses"): String =
            (0 until 2).joinToString(",") { i ->
                "{\"position\":{${vector3("${label}[$i] position")}},\"orientation\":{${quaternion("${label}[$i] orientation")}}}"
            }
        fun covarianceArray(label: String = "covariance"): String =
            (0 until 36).joinToString(",") { f("$label$it") }

        return when (type) {
            "Accel" -> "{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}}"
            "AccelStamped" -> "{\"header\":{${header()}},\"accel\":{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}}}"
            "AccelWithCovariance" -> "{\"accel\":{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}},\"covariance\":[${covarianceArray()}]}"
            "AccelWithCovarianceStamped" -> "{\"header\":{${header()}},\"accel\":{\"accel\":{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}},\"covariance\":[${covarianceArray()}]}}"
            "Inertia" -> "{\"m\":${f("m (mass)")},\"com\":{${vector3("com")}},\"ixx\":${f("ixx")},\"ixy\":${f("ixy")},\"ixz\":${f("ixz")},\"iyy\":${f("iyy")},\"iyz\":${f("iyz")},\"izz\":${f("izz")}}"
            "InertiaStamped" -> "{\"header\":{${header()}},\"inertia\":{\"m\":${f("m (mass)")},\"com\":{${vector3("com")}},\"ixx\":${f("ixx")},\"ixy\":${f("ixy")},\"ixz\":${f("ixz")},\"iyy\":${f("iyy")},\"iyz\":${f("iyz")},\"izz\":${f("izz")}}}"
            "Point" -> "{${vector3()}}"
            "Point32" -> "{\"x\":${f("x (float32)")},\"y\":${f("y (float32)")},\"z\":${f("z (float32)")}}"
            "PointStamped" -> "{\"header\":{${header()}},\"point\":{${vector3()}}}"
            "Polygon" -> "{\"points\":[${point32Array()}]}"
            "PolygonStamped" -> "{\"header\":{${header()}},\"polygon\":{\"points\":[${point32Array()}]}}"
            "Pose" -> "{\"position\":{${vector3("position")}},\"orientation\":{${quaternion("orientation")}}}"
            "PoseArray" -> "{\"header\":{${header()}},\"poses\":[${poseArray()}]}"
            "PoseStamped" -> "{\"header\":{${header()}},\"pose\":{\"position\":{${vector3("position")}},\"orientation\":{${quaternion("orientation")}}}}"
            "PoseWithCovariance" -> "{\"pose\":{\"position\":{${vector3("position")}},\"orientation\":{${quaternion("orientation")}}},\"covariance\":[${covarianceArray()}]}"
            "PoseWithCovarianceStamped" -> "{\"header\":{${header()}},\"pose\":{\"position\":{${vector3("position")}},\"orientation\":{${quaternion("orientation")}}},\"covariance\":[${covarianceArray()}]}"
            "Quaternion" -> "{${quaternion()}}"
            "QuaternionStamped" -> "{\"header\":{${header()}},\"quaternion\":{${quaternion("quaternion")}}}"
            "Transform" -> "{\"translation\":{${vector3("translation")}},\"rotation\":{${quaternion("rotation")}}}"
            "TransformStamped" -> "{\"header\":{${header()}},\"child_frame_id\":\"${s("child_frame_id")}\",\"transform\":{\"translation\":{${vector3("translation")}},\"rotation\":{${quaternion("rotation")}}}}"
            "Twist" -> "{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}}"
            "TwistStamped" -> "{\"header\":{${header()}},\"twist\":{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}}}"
            "TwistWithCovariance" -> "{\"twist\":{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}},\"covariance\":[${covarianceArray()}]}"
            "TwistWithCovarianceStamped" -> "{\"header\":{${header()}},\"twist\":{\"linear\":{${vector3("linear")}},\"angular\":{${vector3("angular")}}},\"covariance\":[${covarianceArray()}]}"
            "Vector3" -> "{${vector3()}}"
            "Vector3Stamped" -> "{\"header\":{${header()}},\"vector\":{${vector3("vector")}}}"
            "Wrench" -> "{\"force\":{${vector3("force")}},\"torque\":{${vector3("torque")}}}"
            "WrenchStamped" -> "{\"header\":{${header()}},\"wrench\":{\"force\":{${vector3("force")}},\"torque\":{${vector3("torque")}}}}"
            else -> "{}"
        }
    }
}
