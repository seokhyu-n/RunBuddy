package com.example.runbuddy.ui.running

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.Chronometer
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import com.example.runbuddy.R
import com.example.runbuddy.data.running.RunningRepository
import com.example.runbuddy.data.record.Record
import com.example.runbuddy.data.record.RecordRepository
import com.example.runbuddy.map.MapActivity
import com.example.runbuddy.service.RunningService
import java.util.Locale

class RunningActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var paceText: TextView
    private lateinit var avgPaceText: TextView
    private lateinit var chronometer: Chronometer
    private lateinit var distanceText: TextView
    private lateinit var pauseButton: ImageButton
    private lateinit var resumeButton: ImageButton
    private lateinit var stopButton: ImageButton
    private lateinit var mapButton: Button
    private lateinit var finishMessage: TextView

    // ✅ 추가: 칼로리/고도 텍스트뷰
    private lateinit var caloriesText: TextView
    private lateinit var altitudeText: TextView

    private var isRunning = true
    private var pausedTime = 0L

    private var isStopPressed = false
    private var stopPressStartTime = 0L

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_running)

        tts = TextToSpeech(this, this)

        // RunningRepository 초기화
        RunningRepository.reset()

        paceText = findViewById(R.id.tvPace)
        avgPaceText = findViewById(R.id.tvAvgPace)
        chronometer = findViewById(R.id.chronometer)
        distanceText = findViewById(R.id.tvDistance)
        pauseButton = findViewById(R.id.btnPause)
        resumeButton = findViewById(R.id.btnResume)
        stopButton = findViewById(R.id.btnStop)
        mapButton = findViewById(R.id.btnMap)
        finishMessage = findViewById(R.id.tvFinishMessage)
        finishMessage.visibility = View.GONE

        // ✅ 추가: XML에 추가한 칼로리/고도 TextView 연결
        caloriesText = findViewById(R.id.tvCalories)
        altitudeText = findViewById(R.id.tvAltitude)

        requestLocationPermission()

        val serviceIntent = Intent(this, RunningService::class.java)
        serviceIntent.action = "ACTION_START"
        androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent)

        startRunning()
        setupObservers()
        setupButtons()

        // ✅ 추가: Chronometer tick마다 칼로리 갱신
        chronometer.setOnChronometerTickListener {
            val elapsedSeconds = (SystemClock.elapsedRealtime() - chronometer.base) / 1000
            RunningRepository.updateCaloriesByElapsedSeconds(elapsedSeconds)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.KOREAN
            tts?.speak("러닝이 시작되었습니다.", TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun setupObservers() {
        RunningRepository.distance.observe(this, Observer { km ->
            distanceText.text = String.format("%.2f", km)
        })
        RunningRepository.pace.observe(this, Observer { pace ->
            paceText.text = pace
        })
        RunningRepository.avgPace.observe(this, Observer { avg ->
            avgPaceText.text = avg
        })

        // ✅ 추가: 칼로리 / 고도 observe
        RunningRepository.calories.observe(this, Observer { kcal ->
            caloriesText.text = kcal.toString()
        })
        RunningRepository.altitude.observe(this, Observer { altM ->
            altitudeText.text = "${altM}m"
        })
    }

    private fun setupButtons() {
        pauseButton.setOnClickListener { pauseRunning() }
        resumeButton.setOnClickListener { resumeRunning() }
        mapButton.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
        setupStopButton()
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                99
            )
        }
    }

    private fun startRunning() {
        isRunning = true
        chronometer.base = SystemClock.elapsedRealtime()
        chronometer.start()
    }

    private fun pauseRunning() {
        if (!isRunning) return
        isRunning = false
        chronometer.stop()
        pausedTime = SystemClock.elapsedRealtime() - chronometer.base

        val intent = Intent(this, RunningService::class.java)
        intent.action = "ACTION_STOP"
        startService(intent)

        pauseButton.visibility = View.GONE
        resumeButton.visibility = View.VISIBLE
    }

    private fun resumeRunning() {
        if (isRunning) return
        isRunning = true

        chronometer.base = SystemClock.elapsedRealtime() - pausedTime
        chronometer.start()

        val intent = Intent(this, RunningService::class.java)
        intent.action = "ACTION_START"
        startService(intent)

        resumeButton.visibility = View.GONE
        pauseButton.visibility = View.VISIBLE
    }

    // 종료 버튼 길게 누르기
    private fun setupStopButton() {
        stopButton.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isStopPressed = true
                    stopPressStartTime = SystemClock.elapsedRealtime()

                    v.animate().scaleX(1.15f).scaleY(1.15f).setDuration(120).start()
                    checkStopHold()
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isStopPressed = false
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }
            }
            true
        }
    }

    private fun checkStopHold() {
        Thread {
            while (isStopPressed) {
                val hold = SystemClock.elapsedRealtime() - stopPressStartTime
                if (hold >= 3000) {
                    runOnUiThread { finishRunning() }
                    break
                }
                Thread.sleep(80)
            }
        }.start()
    }

    private fun finishRunning() {
        isRunning = false
        chronometer.stop()

        stopButton.animate().scaleX(1f).scaleY(1f).setDuration(120).start()

        val intent = Intent(this, RunningService::class.java)
        intent.action = "ACTION_STOP"
        startService(intent)

        tts?.speak("러닝이 종료되었습니다.", TextToSpeech.QUEUE_FLUSH, null, null)

        val totalTimeMs = SystemClock.elapsedRealtime() - chronometer.base
        val km = RunningRepository.distance.value ?: 0.0
        val avg = RunningRepository.avgPace.value ?: "--'--\""
        val pathList = RunningRepository.pathPoints.value ?: mutableListOf()

        if (pathList.size < 2) {
            finishMessage.text = "GPS 경로가 저장되지 않았습니다."
            finishMessage.visibility = View.VISIBLE
        }

        val title = createRunningTitle()

        val record = Record(
            date = System.currentTimeMillis(),
            title = title,
            distanceKm = km,
            avgPace = avg,
            durationSec = (totalTimeMs / 1000).toInt(),
            route = pathList.map { lat -> lat.latitude to lat.longitude }
        )

        RecordRepository.saveRecord(this, record)
        RunningRepository.reset()

        finishMessage.visibility = View.VISIBLE
        finishMessage.text = "운동이 저장되었습니다!"

        Handler(Looper.getMainLooper()).postDelayed({
            finishMessage.visibility = View.GONE
            finish()
        }, 3000)
    }

    private fun createRunningTitle(): String {
        val weekdays = arrayOf("일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일")
        val now = java.util.Calendar.getInstance()

        val day = weekdays[now.get(java.util.Calendar.DAY_OF_WEEK) - 1]
        val hour = now.get(java.util.Calendar.HOUR_OF_DAY)

        val part = when (hour) {
            in 0..6 -> "새벽"
            in 7..11 -> "오전"
            in 12..19 -> "오후"
            else -> "밤"
        }

        return "$day $part 러닝"
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}