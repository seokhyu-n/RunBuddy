package com.example.runbuddy.data.running

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import kotlin.math.*

object RunningRepository {

    // -------------------------------
    // LiveData
    // -------------------------------
    private val _pathPoints = MutableLiveData<MutableList<LatLng>>(mutableListOf())
    val pathPoints: LiveData<MutableList<LatLng>> = _pathPoints

    private val _currentPoint = MutableLiveData<LatLng?>()
    val currentPoint: LiveData<LatLng?> = _currentPoint

    private val _distance = MutableLiveData(0.0)   // 총 거리 (km)
    val distance: LiveData<Double> = _distance

    private val _pace = MutableLiveData("--'--\"") // 현재 페이스
    val pace: LiveData<String> = _pace

    private val _avgPace = MutableLiveData("--'--\"") // 평균 페이스
    val avgPace: LiveData<String> = _avgPace

    private val paceList = mutableListOf<Double>() // 정상 페이스값만 기록

    // ✅✅ [추가] 칼로리 / 고도
    private val _calories = MutableLiveData(0) // kcal
    val calories: LiveData<Int> = _calories

    private val _altitude = MutableLiveData(0) // m
    val altitude: LiveData<Int> = _altitude


    // -------------------------------
    // 내부 상태
    // -------------------------------
    private var lastLat = 0.0
    private var lastLon = 0.0
    private var isFirstLocation = true

    private var startTime = 0L
    private var totalDistance = 0.0

    // 최근 10개의 GPS 위치 → 실시간 페이스 계산용
    private val recentPoints = ArrayDeque<LatLng>()

    // ✅✅ [추가] 고도/칼로리 계산용 내부 상태
    private var maxAltitude = 0          // 고도는 "현재고도"로 표시해도 되지만, 튐이 심하면 최대값/평균값 쓰기도 함
    private var userWeightKg = 70.0      // 기본값(원하면 사용자 설정으로 바꿀 수 있음)
    private var metValue = 8.0           // 조깅 기준 MET(간단 추정치)


    // ------------------------------------------------------------
    // 초기화
    // ------------------------------------------------------------
    fun reset() {
        _pathPoints.value = mutableListOf()
        _distance.value = 0.0
        _pace.value = "--'--\""
        _avgPace.value = "--'--\""

        paceList.clear()
        isFirstLocation = true
        totalDistance = 0.0
        recentPoints.clear()

        startTime = System.currentTimeMillis()

        // ✅✅ [추가] 초기화
        _calories.value = 0
        _altitude.value = 0
        maxAltitude = 0
    }

    // ✅✅ [추가] 사용자 체중 설정(선택)
    fun setUserWeightKg(kg: Double) {
        if (kg.isNaN() || kg.isInfinite()) return
        if (kg < 30.0 || kg > 200.0) return
        userWeightKg = kg
    }

    // ✅✅ [추가] altitude 업데이트(미터)
    // - Location.altitude를 그대로 넣어주면 됨
    // - 튀는 값 방지로 약간 클램프
    fun updateAltitude(altitudeMeters: Double) {
        if (altitudeMeters.isNaN() || altitudeMeters.isInfinite()) return

        // 고도가 너무 튀면 무시(예: -200m 이하, 5000m 이상은 러닝 환경에서 비정상)
        if (altitudeMeters < -200 || altitudeMeters > 5000) return

        val altInt = altitudeMeters.roundToInt()

        // 필요하면 "현재고도" 대신 "최고고도"로 표시 가능
        if (altInt > maxAltitude) maxAltitude = altInt

        // 여기서는 "현재고도"를 표시
        _altitude.postValue(altInt)
    }

    // ✅✅ [추가] 시간(초) 기반 칼로리 누적 업데이트
    // - Chronometer의 elapsedSeconds를 넣어주면 됨
    // - kcal/min = MET * 3.5 * kg / 200
    fun updateCaloriesByElapsedSeconds(elapsedSeconds: Long) {
        if (elapsedSeconds < 0) return
        val minutes = elapsedSeconds / 60.0
        val kcal = (metValue * 3.5 * userWeightKg / 200.0) * minutes
        _calories.postValue(kcal.toInt())
    }


    // ------------------------------------------------------------
    // GPS 업데이트
    // ------------------------------------------------------------
    fun updateLocation(lat: Double, lon: Double) {

        val point = LatLng(lat, lon)
        _currentPoint.postValue(point)

        // 첫 위치는 거리 계산 안 함
        if (isFirstLocation) {
            lastLat = lat
            lastLon = lon
            isFirstLocation = false

            val list = _pathPoints.value ?: mutableListOf()
            list.add(point)
            _pathPoints.postValue(list)
            return
        }

        // 새 거리 (단위: km)
        val d = haversine(lastLat, lastLon, lat, lon)

        // GPS 튐 방지 --------------------------------------------
        if (d > 0.03) return    // 30m 이상 → 튐
        if (d < 0.001) return   // 1m 이하 → 움직임 X

        // 거리 증가
        totalDistance += d
        _distance.postValue(totalDistance)

        // 경로 점 추가
        val list = _pathPoints.value ?: mutableListOf()
        list.add(point)
        _pathPoints.postValue(list)

        // 최근 위치 10개 저장 → 즉시 페이스 계산용
        recentPoints.add(point)
        if (recentPoints.size > 10) recentPoints.removeFirst()

        // 페이스 계산
        calculatePace()

        lastLat = lat
        lastLon = lon
    }


    // ------------------------------------------------------------
    // 고급 페이스 계산 (최근 10초 기준)
    // ------------------------------------------------------------
    private fun calculatePace() {

        // 최근 점이 너무 적으면 계산 불가
        if (recentPoints.size < 2) {
            _pace.postValue("--'--\"")
            _avgPace.postValue("--'--\"")
            return
        }

        // 최근 10개 거리 합산
        var segmentDistance = 0.0  // km
        for (i in 0 until recentPoints.size - 1) {
            val a = recentPoints[i]
            val b = recentPoints[i + 1]
            segmentDistance += haversine(a.latitude, a.longitude, b.latitude, b.longitude)
        }

        // 최근 구간이 3m 이하라면 페이스 만들지 않음
        if (segmentDistance < 0.003) { // 3m
            _pace.postValue("--'--\"")
            return
        }

        // 10초 기준 페이스 (min/km)
        val paceFloat = (10.0 / 60.0) / segmentDistance

        // 비정상 값 제거
        if (paceFloat.isNaN() || paceFloat.isInfinite()) return
        if (paceFloat < 3.0) return      // 너무 빠름 (3분/km 미만)
        if (paceFloat > 20.0) return     // 너무 느림 (20분/km 이상)

        // 정상 값이면 기록
        paceList.add(paceFloat)

        // 현재 페이스
        _pace.postValue(formatPace(paceFloat))

        // 평균 페이스
        _avgPace.postValue(formatPace(paceList.average()))
    }


    // ------------------------------------------------------------
    // Haversine 거리 계산 (km 단위)
    // ------------------------------------------------------------
    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)

        return 2 * R * asin(sqrt(a))
    }

    private fun formatPace(p: Double): String {
        val min = p.toInt()
        val sec = ((p - min) * 60).toInt()
        return String.format("%02d'%02d\"", min, sec)
    }
}