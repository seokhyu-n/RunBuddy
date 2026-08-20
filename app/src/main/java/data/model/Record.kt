package com.example.runbuddy.data.record

import java.io.Serializable

/**
 * 러닝 1회의 모든 기록을 저장하는 데이터 모델
 */
data class Record(
    val id: Long = System.currentTimeMillis(),
    val date: Long,
    val title: String,
    val distanceKm: Double,
    val avgPace: String,
    val durationSec: Int,
    val route: List<Pair<Double, Double>>,

    // 확장 정보
    val calories: Int = 0,
    val altitude: Int = 0,
    val avgHeartRate: Int = 0,
    val cadence: Int = 0
) : Serializable
