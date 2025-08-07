package com.examples.testros2jsbridge.domain.geometry

/**
 * Describes the fields for each geometry message type for dynamic UI generation.
 */
sealed class GeometryFieldSpec(val label: String) {
    data class FloatField(val tag: String, val hint: String) : GeometryFieldSpec(hint)
    data class IntField(val tag: String, val hint: String) : GeometryFieldSpec(hint)
    data class StringField(val tag: String, val hint: String) : GeometryFieldSpec(hint)
    data class Vector3(val prefix: String = "") : GeometryFieldSpec(if (prefix.isEmpty()) "Vector3" else "$prefix (Vector3)")
    data class Quaternion(val prefix: String = "") : GeometryFieldSpec(if (prefix.isEmpty()) "Quaternion" else "$prefix (Quaternion)")
    object Covariance : GeometryFieldSpec("Covariance (36 floats)")
    object Point32Array : GeometryFieldSpec("Point32 Array (3 points)")
    object PoseArray : GeometryFieldSpec("Pose Array (2 poses)")
}

/**
 * Map of geometry type to its required fields (in order).
 */
val geometryTypeFields: Map<String, List<GeometryFieldSpec>> = mapOf(
    // Acceleration
    "Accel" to listOf(GeometryFieldSpec.Vector3("linear"), GeometryFieldSpec.Vector3("angular")),
    "AccelStamped" to listOf(GeometryFieldSpec.StringField("header_frame_id", "header_frame_id"), GeometryFieldSpec.Vector3("linear"), GeometryFieldSpec.Vector3("angular")),
    "AccelWithCovariance" to listOf(GeometryFieldSpec.Vector3("linear"), GeometryFieldSpec.Vector3("angular"), GeometryFieldSpec.Covariance),
    "AccelWithCovarianceStamped" to listOf(GeometryFieldSpec.StringField("header_frame_id", "header_frame_id"), GeometryFieldSpec.Vector3("linear"), GeometryFieldSpec.Vector3("angular"), GeometryFieldSpec.Covariance),
    // Inertia
    "Inertia" to listOf(
        GeometryFieldSpec.FloatField("m", "m (mass)"),
        GeometryFieldSpec.Vector3("com"),
        GeometryFieldSpec.FloatField("ixx", "ixx"),
        GeometryFieldSpec.FloatField("ixy", "ixy"),
        GeometryFieldSpec.FloatField("ixz", "ixz"),
        GeometryFieldSpec.FloatField("iyy", "iyy"),
        GeometryFieldSpec.FloatField("iyz", "iyz"),
        GeometryFieldSpec.FloatField("izz", "izz")
    ),
    "InertiaStamped" to listOf(
        GeometryFieldSpec.StringField("header_frame_id", "header_frame_id"),
        GeometryFieldSpec.FloatField("m", "m (mass)"),
        GeometryFieldSpec.Vector3("com"),
        GeometryFieldSpec.FloatField("ixx", "ixx"),
        GeometryFieldSpec.FloatField("ixy", "ixy"),
        GeometryFieldSpec.FloatField("ixz", "ixz"),
        GeometryFieldSpec.FloatField("iyy", "iyy"),
        GeometryFieldSpec.FloatField("iyz", "iyz"),
        GeometryFieldSpec.FloatField("izz", "izz")
    ),
    // Point
    "Point" to listOf(GeometryFieldSpec.Vector3()),
    "Point32" to listOf(
        GeometryFieldSpec.FloatField("x", "x (float32)"),
        GeometryFieldSpec.FloatField("y", "y (float32)"),
        GeometryFieldSpec.FloatField("z", "z (float32)")
    ),
    "PointStamped" to listOf(GeometryFieldSpec.StringField("header_frame_id", "header_frame_id"), GeometryFieldSpec.Vector3()),
    // Polygon
    "Polygon" to listOf(GeometryFieldSpec.Point32Array),
    "PolygonStamped" to listOf(GeometryFieldSpec.StringField("header_frame_id", "header_frame_id"), GeometryFieldSpec.Point32Array),
    // Pose
    "Pose" to listOf(GeometryFieldSpec.Vector3("position"), GeometryFieldSpec.Quaternion("orientation")),
    "PoseArray" to listOf(GeometryFieldSpec.StringField("header_frame_id", "header_frame_id"), GeometryFieldSpec.PoseArray),
    "PoseStamped" to listOf(GeometryFieldSpec.StringField("header_frame_id", "header_frame_id"), GeometryFieldSpec.Vector3("position"), GeometryFieldSpec.Quaternion("orientation")),
    "PoseWithCovariance" to listOf(GeometryFieldSpec.Vector3("position"), GeometryFieldSpec.Quaternion("orientation"), GeometryFieldSpec.Covariance),
    "PoseWithCovarianceStamped" to listOf(GeometryFieldSpec.StringField("header_frame_id", "header_frame_id"), GeometryFieldSpec.Vector3("position"), GeometryFieldSpec.Quaternion("orientation"), GeometryFieldSpec.Covariance),
    // Quaternion
    "Quaternion" to listOf(GeometryFieldSpec.Quaternion()),
    "QuaternionStamped" to listOf(GeometryFieldSpec.StringField("header_frame_id", "header_frame_id"), GeometryFieldSpec.Quaternion("quaternion")),
    // Transform
    "Transform" to listOf(GeometryFieldSpec.Vector3("translation"), GeometryFieldSpec.Quaternion("rotation")),
    "TransformStamped" to listOf(GeometryFieldSpec.StringField("header_frame_id", "header_frame_id"), GeometryFieldSpec.Vector3("translation"), GeometryFieldSpec.Quaternion("rotation")),
    // Twist
    "Twist" to listOf(GeometryFieldSpec.Vector3("linear"), GeometryFieldSpec.Vector3("angular")),
    "TwistStamped" to listOf(GeometryFieldSpec.StringField("header_frame_id", "header_frame_id"), GeometryFieldSpec.Vector3("linear"), GeometryFieldSpec.Vector3("angular")),
    "TwistWithCovariance" to listOf(GeometryFieldSpec.Vector3("linear"), GeometryFieldSpec.Vector3("angular"), GeometryFieldSpec.Covariance),
    "TwistWithCovarianceStamped" to listOf(GeometryFieldSpec.StringField("header_frame_id", "header_frame_id"), GeometryFieldSpec.Vector3("linear"), GeometryFieldSpec.Vector3("angular"), GeometryFieldSpec.Covariance),
    // Vector3
    "Vector3" to listOf(GeometryFieldSpec.Vector3()),
    "Vector3Stamped" to listOf(GeometryFieldSpec.StringField("header_frame_id", "header_frame_id"), GeometryFieldSpec.Vector3("vector")),
    // Wrench
    "Wrench" to listOf(GeometryFieldSpec.Vector3("force"), GeometryFieldSpec.Vector3("torque")),
    "WrenchStamped" to listOf(GeometryFieldSpec.StringField("header_frame_id", "header_frame_id"), GeometryFieldSpec.Vector3("force"), GeometryFieldSpec.Vector3("torque"))
)
