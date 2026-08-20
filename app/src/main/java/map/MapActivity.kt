package com.example.runbuddy.map

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import com.example.runbuddy.R
import com.example.runbuddy.data.running.RunningRepository
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.*

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private var polyline: Polyline? = null
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        Log.d("MapActivity", "🟡 MapActivity onCreate() 시작됨")

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        Log.d("MapActivity", "🟡 getMapAsync() 호출됨")
    }

    override fun onMapReady(map: GoogleMap) {
        Log.d("MapActivity", "🟢 onMapReady() 호출됨")

        googleMap = map

        if (!checkPermission()) {
            Log.e("MapActivity", "❌ 위치 권한 없음 — requestPermission() 호출")
            requestPermission()
            return
        }

        googleMap.isMyLocationEnabled = true
        Log.d("MapActivity", "🟢 GoogleMap 초기화 완료")

        drawAIDirectionsRoute()
        observePath()
    }

    /** ------------------------------------------------------
     *  ① SharedPreferences → 장소명 → 좌표 변환
     * ------------------------------------------------------ */
    private fun drawAIDirectionsRoute() {

        Log.d("MapActivity", "🔥 drawAIDirectionsRoute() 실행됨")

        val prefs = getSharedPreferences("runbuddy_prefs", MODE_PRIVATE)

        // ⭐ 여기부터 수정됨 — route_points 사용 ⭐
        val set = prefs.getStringSet("route_points", null)
        Log.d("MapActivity", "📌 route_points = $set")

        if (set == null || set.isEmpty()) {
            Log.e("MapActivity", "❌ route_points 없음 — 장소 저장 실패")
            return
        }

        val placeList = set.toList()
        Log.d("MapActivity", "📌 placeList = $placeList")
        // ⭐ 여기까지 수정됨 ⭐


        if (placeList.size < 2) {
            Log.e("MapActivity", "❌ 장소 수 부족: ${placeList.size}")
            return
        }

        val geocoder = Geocoder(this, Locale.KOREA)
        val latLngList = mutableListOf<LatLng>()

        for (place in placeList) {
            Log.d("MapActivity", "🌍 Geocoder 요청: $place")
            try {
                val addresses = geocoder.getFromLocationName(place, 1)
                if (!addresses.isNullOrEmpty()) {
                    val a = addresses[0]
                    val latLng = LatLng(a.latitude, a.longitude)
                    latLngList.add(latLng)
                    Log.d("MapActivity", "✔ Geocoded [$place] -> $latLng")
                } else {
                    Log.e("MapActivity", "❌ Geocode 실패: $place")
                }

            } catch (e: Exception) {
                Log.e("MapActivity", "❌ Geocode 에러 [$place]: ${e.message}")
            }
        }

        if (latLngList.size < 2) {
            Log.e("MapActivity", "❌ 좌표 변환 실패: latLngList.size=${latLngList.size}")
            return
        }

        val start = latLngList[0]
        val end = latLngList[1]

        Log.d("MapActivity", "🚀 Directions API 호출 준비: start=$start end=$end")

        requestWalkingRoute(start, end, latLngList)
    }

    /** ------------------------------------------------------
     *  ② Directions API 호출 — 실패하면 직선 fallback
     * ------------------------------------------------------ */
    private fun requestWalkingRoute(start: LatLng, end: LatLng, fallbackPoints: List<LatLng>) {

        // ⭐ 서버용 Directions API Key 사용
        val DIRECTIONS_API_KEY = getString(R.string.Server_Directions_Key)

        Log.d("MapActivity", "🔑 Directions API key = $DIRECTIONS_API_KEY")

        val url =
            "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=${start.latitude},${start.longitude}" +
                    "&destination=${end.latitude},${end.longitude}" +
                    "&mode=transit" +
                    "&key=$DIRECTIONS_API_KEY"



        Log.d("MapActivity", "🌐 Directions API URL = $url")

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MapActivity", "❌ Directions API failure: ${e.message}")
                runOnUiThread { drawSimplePolyline(fallbackPoints) }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: run {
                    Log.e("MapActivity", "❌ Directions API 빈 응답")
                    runOnUiThread { drawSimplePolyline(fallbackPoints) }
                    return
                }

                Log.d("MapActivity", "📥 Directions API raw response = $body")

                val json = JSONObject(body)
                val status = json.optString("status")
                Log.d("MapActivity", "📌 Directions API status = $status")

                if (status != "OK") {
                    Log.e("MapActivity", "❌ Directions API 실패: $status")
                    runOnUiThread { drawSimplePolyline(fallbackPoints) }
                    return
                }

                val routes = json.getJSONArray("routes")
                if (routes.length() == 0) {
                    Log.e("MapActivity", "❌ Directions API: routes 비어 있음")
                    runOnUiThread { drawSimplePolyline(fallbackPoints) }
                    return
                }

                val polyline = routes.getJSONObject(0)
                    .getJSONObject("overview_polyline")
                    .getString("points")

                val decodedPath = PolyUtil.decode(polyline)

                Log.d("MapActivity", "✔ Directions 성공: decodedPath size=${decodedPath.size}")

                runOnUiThread {
                    googleMap.addPolyline(
                        PolylineOptions()
                            .addAll(decodedPath)
                            .width(14f)
                            .color(0xFF1E88E5.toInt())
                    )

                    googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(start, 15f)
                    )
                }
            }
        })
    }

    /** 직선 fallback */
    private fun drawSimplePolyline(points: List<LatLng>) {

        Log.w("MapActivity", "⚠ fallback: 직선 경로 표시")

        if (points.size < 2) return

        googleMap.addPolyline(
            PolylineOptions()
                .addAll(points)
                .width(12f)
                .color(0xFF000000.toInt())
        )

        googleMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(points.first(), 15f)
        )
    }

    /** 실시간 달리기 경로 관찰 */
    private fun observePath() {
        Log.d("MapActivity", "👁 observePath() 시작")

        RunningRepository.pathPoints.observe(
            this,
            Observer { points ->

                Log.d("MapActivity", "👁 pathPoints = ${points.size} 개")

                if (points.isEmpty()) {
                    polyline?.remove()
                    polyline = null
                    return@Observer
                }

                if (points.size < 2) return@Observer

                if (polyline == null) {
                    polyline = googleMap.addPolyline(
                        PolylineOptions()
                            .width(16f)
                            .color(0xFF00C896.toInt())
                            .addAll(points)
                    )
                } else {
                    polyline!!.points = points
                }

                moveCamera(points.last())
            }
        )
    }

    private fun moveCamera(pos: LatLng) {
        Log.d("MapActivity", "📸 moveCamera(): $pos")

        val cam = CameraPosition.Builder()
            .target(pos)
            .zoom(17f)
            .build()

        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cam))
    }

    private fun checkPermission(): Boolean =
        ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission() {
        Log.w("MapActivity", "⚠ requestPermission() 호출됨")
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            100
        )
    }
}
