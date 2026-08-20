package com.example.runbuddy.map

import com.google.android.gms.maps.model.LatLng

object CourseData {

    // 평택대학교 캠퍼스 내부 코스
    val courseCampus = listOf(
        LatLng(36.992590, 127.088310),
        LatLng(36.992800, 127.089100),
        LatLng(36.993300, 127.088900),
        LatLng(36.993000, 127.088100)
    )

    // 평택호 둘레길 코스
    val courseLake = listOf(
        LatLng(36.990600, 127.084800),
        LatLng(36.990900, 127.085600),
        LatLng(36.991500, 127.085900),
        LatLng(36.991900, 127.085300)
    )

    // 송북동 공원 러닝 코스
    val coursePark = listOf(
        LatLng(36.995900, 127.112300),
        LatLng(36.996300, 127.113200),
        LatLng(36.995700, 127.113900),
        LatLng(36.995100, 127.113200)
    )
}
