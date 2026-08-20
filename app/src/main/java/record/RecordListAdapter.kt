package com.example.runbuddy.ui.record

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.runbuddy.R
import com.example.runbuddy.data.record.Record
import java.text.SimpleDateFormat
import java.util.*

class RecordListAdapter(
    private val context: Context,
    private var items: List<Record> // 이름 변경: records -> items
) : RecyclerView.Adapter<RecordListAdapter.RecordViewHolder>() {

    // RecordViewHolder: 뷰 ID를 첫 번째 코드에 맞춰 재정의
    inner class RecordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgMap: ImageView = view.findViewById(R.id.imgMapThumb) // 썸네일 이미지뷰
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvDistance: TextView = view.findViewById(R.id.tvDistance)
        val tvAvgPace: TextView = view.findViewById(R.id.tvAvgPace) // 평균 페이스
        val tvDuration: TextView = view.findViewById(R.id.tvDuration) // 시간
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        // 레이아웃 파일을 'record_item'으로 가정하고 통일
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.record_item, parent, false)
        return RecordViewHolder(view)
    }

    override fun getItemCount() = items.size // 이름 변경: records.size -> items.size

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val item = items[position]

        holder.tvTitle.text = item.title
        holder.tvDistance.text = String.format("%.2f km", item.distanceKm)
        // 두 번째 코드의 tvPace 대신, 첫 번째 코드의 tvAvgPace 사용
        holder.tvAvgPace.text = "평균 페이스 ${item.avgPace}"

        // Duration 추가 (두 번째 코드에는 없었으나 첫 번째 코드를 참조하여 추가)
        holder.tvDuration.text = formatDuration(item.durationSec)

        holder.tvDate.text = formatDate(item.date)

        // 🔥 route 로 미니 맵(경로) 썸네일 그리기 (이전 코드에서 누락되어 재추가)
        val thumbBitmap = createRouteThumbnail(item.route)
        if (thumbBitmap != null) {
            holder.imgMap.setImageBitmap(thumbBitmap)
        } else {
            // R.drawable.map_placeholder 리소스가 있다고 가정
            holder.imgMap.setImageResource(R.drawable.map_placeholder)
        }

        // 🔥 상세 페이지 이동
        holder.itemView.setOnClickListener {
            try {
                val intent = Intent(context, RecordDetailActivity::class.java)
                intent.putExtra("record", item)
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --------------------------------------------------------
    // 유틸리티 함수 (첫 번째 코드에서 복원)
    // --------------------------------------------------------

    private fun formatDuration(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return String.format("%02d:%02d", m, s)
    }

    private fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    private fun createRouteThumbnail(
        route: List<Pair<Double, Double>>
    ): Bitmap? {
        if (route.size < 2) return null

        return try {
            val sizePx = dpToPx(90f)
            val padding = dpToPx(6f).toFloat()

            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val bgPaint = Paint().apply {
                color = Color.parseColor("#EEEEEE")
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), bgPaint)

            val pathPaint = Paint().apply {
                color = Color.parseColor("#00C896")
                style = Paint.Style.STROKE
                strokeWidth = dpToPx(3f).toFloat()
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            val lats = route.map { it.first }
            val lngs = route.map { it.second }

            val minLat = lats.minOrNull() ?: return null
            val maxLat = lats.maxOrNull() ?: return null
            val minLng = lngs.minOrNull() ?: return null
            val maxLng = lngs.maxOrNull() ?: return null

            val latRange = (maxLat - minLat).takeIf { it != 0.0 } ?: 1.0
            val lngRange = (maxLng - minLng).takeIf { it != 0.0 } ?: 1.0

            val left = padding
            val top = padding
            val right = sizePx - padding
            val bottom = sizePx - padding

            val path = Path()
            route.forEachIndexed { index, pair ->
                val lat = pair.first
                val lng = pair.second

                val xRatio = ((lng - minLng) / lngRange).toFloat()
                val yRatio = ((lat - minLat) / latRange).toFloat()

                val x = left + (right - left) * xRatio
                val y = bottom - (bottom - top) * yRatio

                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            canvas.drawPath(path, pathPaint)

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
    }
}