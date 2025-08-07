package com.examples.testros2jsbridge.domain.geometry

/**
 * List of supported geometry message types for dropdown selection.
 */
val geometryTypes = listOf(
    // Acceleration
    "Accel", "AccelStamped", "AccelWithCovariance", "AccelWithCovarianceStamped",
    // Inertia
    "Inertia", "InertiaStamped",
    // Point
    "Point", "Point32", "PointStamped",
    // Polygon
    "Polygon", "PolygonStamped",
    // Pose
    "Pose", "PoseArray", "PoseStamped", "PoseWithCovariance", "PoseWithCovarianceStamped",
    // Quaternion
    "Quaternion", "QuaternionStamped",
    // Transform
    "Transform", "TransformStamped",
    // Twist
    "Twist", "TwistStamped", "TwistWithCovariance", "TwistWithCovarianceStamped",
    // Vector3
    "Vector3", "Vector3Stamped",
    // Wrench
    "Wrench", "WrenchStamped"
)
