package com.example.runbuddy.ui.record

import android.content.ContentValues
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.runbuddy.R
import com.example.runbuddy.data.record.Record
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.button.MaterialButton
import android.widget.ImageView
import java.text.SimpleDateFormat
import java.util.*

class RecordDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    private lateinit var record: Record
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                Toast.makeText(this, "이미지 선택 완료!", Toast.LENGTH_SHORT).show()
                createRunningCard(uri)
            }
        }

    @Suppress("DEPRECATION") // getSerializableExtra Deprecation 경고 제거
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.record_detail)

        // Serializable extra를 안전하게 받음
        val rec = intent.getSerializableExtra("record") as? Record ?: run {
            finish()
            return
        }
        record = rec

        findViewById<TextView>(R.id.tvTitle).text = record.title
        findViewById<TextView>(R.id.tvDistance).text = String.format("%.2f", record.distanceKm)
        findViewById<TextView>(R.id.tvAvgPace).text = record.avgPace
        findViewById<TextView>(R.id.tvDuration).text = secondsToTime(record.durationSec)

        findViewById<MaterialButton>(R.id.btnUploadImage).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        mapView = findViewById(R.id.mapViewDetail)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    private fun drawTextWithStroke(canvas: Canvas, text: String, x: Float, y: Float, size: Float) {
        val stroke = Paint().apply {
            color = Color.BLACK
            textSize = size
            style = Paint.Style.STROKE
            strokeWidth = size * 0.07f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val fill = Paint().apply {
            color = Color.WHITE
            textSize = size
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        canvas.drawText(text, x, y, stroke)
        canvas.drawText(text, x, y, fill)
    }

    private fun createRunningCard(uri: Uri) {
        val original = MediaStore.Images.Media.getBitmap(contentResolver, uri)

        val result = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(original, 0f, 0f, null)

        val big = original.width * 0.17f
        val small = original.width * 0.045f

        val x1 = original.width * 0.05f
        val x2 = original.width * 0.35f
        val y1 = original.height * 0.22f
        val y2 = original.height * 0.29f
        val y3 = original.height * 0.38f
        val y4 = original.height * 0.455f

        // 날짜
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val dateText = dateFormat.format(Date(record.date))

        // 제목 & 날짜
        drawTextWithStroke(canvas, record.title, x1, original.height * 0.10f, small * 1.2f)
        drawTextWithStroke(canvas, dateText, original.width * 0.68f, original.height * 0.10f, small)

        // 🔥 거리
        drawTextWithStroke(canvas, String.format("%.2f", record.distanceKm), x1, y1, big)
        drawTextWithStroke(canvas, "킬로미터", x1, y2, small)

        // 🔥 평균 페이스 숫자 (조금 확대 → small * 1.4f)
        drawTextWithStroke(canvas, record.avgPace, x1, y3, small * 1.4f)
        drawTextWithStroke(canvas, "평균 페이스", x1, y4, small)

        // 🔥 시간 숫자 (조금 확대 → small * 1.4f)
        drawTextWithStroke(canvas, secondsToTime(record.durationSec), x2, y3, small * 1.4f)
        drawTextWithStroke(canvas, "시간", x2, y4, small)

        showFinalDialog(result)
    }


    // 🔥 커스텀 팝업
    private fun showFinalDialog(bitmap: Bitmap) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_running_card, null)

        val imgCardPreview = dialogView.findViewById<ImageView>(R.id.imgCardPreview)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSave)
        val btnClose = dialogView.findViewById<MaterialButton>(R.id.btnClose)

        imgCardPreview.setImageBitmap(bitmap)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.show()

        btnSave.setOnClickListener {
            saveImageToGallery(bitmap)
            dialog.dismiss()
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap) {
        val filename = "runbuddy_${System.currentTimeMillis()}.jpg"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        if (uri != null) {
            contentResolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
            }

            Toast.makeText(this, "갤러리에 저장되었습니다!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "저장 실패!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        drawRoute(record)
    }

    private fun drawRoute(record: Record) {
        val map = googleMap ?: return

        val points = record.route.map { LatLng(it.first, it.second) }
        if (points.isEmpty()) return

        map.addPolyline(
            PolylineOptions()
                .addAll(points)
                .width(12f)
                .color(0xFF00C896.toInt())
        )

        val bounds = LatLngBounds.Builder().apply {
            points.forEach { include(it) }
        }.build()

        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
    }

    private fun secondsToTime(sec: Int): String =
        String.format("%02d:%02d", sec / 60, sec % 60)

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onStop() { mapView.onStop(); super.onStop() }
    override fun onDestroy() { mapView.onDestroy(); super.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
}