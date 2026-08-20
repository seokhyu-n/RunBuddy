package com.example.runbuddy.ui.record

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.runbuddy.R
import com.example.runbuddy.data.record.Record
import com.example.runbuddy.data.record.RecordRepository
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Calendar

class RecordActivity : AppCompatActivity() {

    private var currentTab = "week" // week / month / all
    private var recordList = listOf<Record>()

    private lateinit var recyclerView: RecyclerView
    private lateinit var recordAdapter: RecordListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // XML 레이아웃을 로드하는 부분 (여기가 오류의 원인이었으나, XML은 현재 수정된 상태입니다.)
        setContentView(R.layout.activity_record)

        // ⭐ 저장된 기록 불러오기 (이 부분에서 RecordRepository가 null을 반환하지 않는지 확인 필요)
        recordList = RecordRepository.getAllRecords(this)

        // ⭐ RecyclerView 연결
        setupRecyclerView()

        // ⭐ 기본 UI 설정
        setupTabs()
        updateTabHighlight("week")

        // ⭐ 초기 this week 거리
        updateDistanceForWeek()
    }

    // ------------------------------------------------------------
    // ⭐ RecyclerView 설정
    // ------------------------------------------------------------
    private fun setupRecyclerView() {
        // ID가 'recyclerRecords' 인지 다시 한번 확인하세요.
        recyclerView = findViewById(R.id.recyclerRecords)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 최신 순 정렬
        val sortedList = recordList.sortedByDescending { it.date }

        recordAdapter = RecordListAdapter(this, sortedList)
        recyclerView.adapter = recordAdapter
    }

    // ------------------------------------------------------------
    // 탭 클릭 리스너들
    // ------------------------------------------------------------
    private fun setupTabs() {
        // ID가 올바른지 다시 한번 확인하세요.
        val layoutWeekSelector = findViewById<LinearLayout>(R.id.layoutWeekSelector)
        val layoutMonthSelector = findViewById<LinearLayout>(R.id.layoutMonthSelector)
        val layoutAllSelector = findViewById<LinearLayout>(R.id.layoutAllSelector)

        layoutWeekSelector.setOnClickListener { showWeekSelectSheet() }
        layoutMonthSelector.setOnClickListener { showMonthSelectSheet() }

        layoutAllSelector.setOnClickListener {
            currentTab = "all"
            updateTabHighlight("all")
            updateDistanceForAll()
        }
    }

    // ------------------------------------------------------------
    // ⭐ 거리 업데이트
    // ------------------------------------------------------------
    private fun updateDistanceForWeek() {
        val tvWeekSelected = findViewById<TextView>(R.id.tvWeekSelected)
        val tvTotalDistance = findViewById<TextView>(R.id.tvTotalDistance)

        tvWeekSelected.text = "이번 주"

        // NOTE: 현재는 전체 기록 합계를 계산하고 있습니다. (주별 필터링 로직은 별도 구현 필요)
        val total = recordList.sumOf { it.distanceKm }
        tvTotalDistance.text = String.format("%.2f km", total)
    }

    private fun updateDistanceForLastWeek() {
        val tvWeekSelected = findViewById<TextView>(R.id.tvWeekSelected)
        val tvTotalDistance = findViewById<TextView>(R.id.tvTotalDistance)

        tvWeekSelected.text = "지난 주"

        val total = 0.0 // 임시 값
        tvTotalDistance.text = String.format("%.2f km", total)
    }

    private fun updateDistanceForAll() {
        val tvWeekSelected = findViewById<TextView>(R.id.tvWeekSelected)
        val tvTotalDistance = findViewById<TextView>(R.id.tvTotalDistance)

        tvWeekSelected.text = "총 거리"

        val total = recordList.sumOf { it.distanceKm }
        tvTotalDistance.text = String.format("%.2f km", total)
    }

    // ------------------------------------------------------------
    // ⭐ 탭 색상 업데이트
    // ------------------------------------------------------------
    private fun updateTabHighlight(selected: String) {
        val mint = Color.parseColor("#00C896")
        val white = Color.parseColor("#FFFFFF")
        val black = Color.parseColor("#000000")

        val layoutWeek = findViewById<LinearLayout>(R.id.layoutWeekSelector)
        val layoutMonth = findViewById<LinearLayout>(R.id.layoutMonthSelector)
        val layoutAll = findViewById<LinearLayout>(R.id.layoutAllSelector)

        val tvWeek = findViewById<TextView>(R.id.tvTabWeek)
        val tvMonth = findViewById<TextView>(R.id.tvTabMonth)
        val tvAll = findViewById<TextView>(R.id.tvTabAll)

        if (selected == "week") {
            layoutWeek.setBackgroundColor(mint)
            tvWeek.setTextColor(white)
        } else {
            layoutWeek.setBackgroundColor(white)
            tvWeek.setTextColor(black)
        }

        if (selected == "month") {
            layoutMonth.setBackgroundColor(mint)
            tvMonth.setTextColor(white)
        } else {
            layoutMonth.setBackgroundColor(white)
            tvMonth.setTextColor(black)
        }

        if (selected == "all") {
            layoutAll.setBackgroundColor(mint)
            tvAll.setTextColor(white)
        } else {
            layoutAll.setBackgroundColor(white)
            tvAll.setTextColor(black)
        }
    }

    // ------------------------------------------------------------
    // ⭐ 주 선택 BottomSheet
    // ------------------------------------------------------------
    private fun showWeekSelectSheet() {
        val dialog = BottomSheetDialog(this)
        // R.layout.bottomsheet_week 파일이 올바른지 확인
        val view = layoutInflater.inflate(R.layout.bottomsheet_week, null)
        dialog.setContentView(view)

        val tvLast = view.findViewById<TextView>(R.id.tvLastWeek)
        val tvThis = view.findViewById<TextView>(R.id.tvThisWeek)
        val btnSelect = view.findViewById<Button>(R.id.btnSelectWeek)

        var selected = "이번 주"
        tvThis.setTextColor(Color.BLACK)

        tvLast.setOnClickListener {
            selected = "지난 주"
            tvLast.setTextColor(Color.BLACK)
            tvThis.setTextColor(Color.GRAY)
        }

        tvThis.setOnClickListener {
            selected = "이번 주"
            tvThis.setTextColor(Color.BLACK)
            tvLast.setTextColor(Color.GRAY)
        }

        btnSelect.setOnClickListener {
            currentTab = "week"
            updateTabHighlight("week")

            if (selected == "이번 주") updateDistanceForWeek()
            else updateDistanceForLastWeek()

            dialog.dismiss()
        }

        dialog.show()
    }

    // ------------------------------------------------------------
    // ⭐ 월 선택 BottomSheet
    // ------------------------------------------------------------
    private fun showMonthSelectSheet() {
        val dialog = BottomSheetDialog(this)
        // R.layout.bottomsheet_month 파일이 올바른지 확인
        val view = layoutInflater.inflate(R.layout.bottomsheet_month, null)
        dialog.setContentView(view)

        // 연도는 2025년으로 고정
        val currentYear = 2025
        val tvYear = view.findViewById<TextView>(R.id.tvSelectYear)
        val list = view.findViewById<LinearLayout>(R.id.layoutMonthList)
        val btnSelect = view.findViewById<Button>(R.id.btnSelectMonth)

        tvYear.text = "${currentYear}년"

        // 현재 시스템의 월을 기본 선택값으로 설정
        val currentCalendar = Calendar.getInstance()
        var selectedMonth = currentCalendar.get(Calendar.MONTH) + 1
        list.removeAllViews()

        for (i in 1..12) {
            val tv = TextView(this).apply {
                text = "${i}월"
                textSize = 20f
                setPadding(40, 30, 40, 30)
                setTextColor(if (i == selectedMonth) Color.BLACK else Color.GRAY)
            }

            tv.setOnClickListener {
                selectedMonth = i

                for (j in 0 until list.childCount) {
                    (list.getChildAt(j) as TextView).setTextColor(Color.GRAY)
                }
                tv.setTextColor(Color.BLACK)
            }

            list.addView(tv)
        }

        btnSelect.setOnClickListener {
            currentTab = "month"
            updateTabHighlight("month")

            // 상단 텍스트 업데이트
            findViewById<TextView>(R.id.tvWeekSelected).text =
                "${currentYear}년 ${selectedMonth}월"

            // 선택한 연/월에 해당하는 기록만 합계
            val cal = Calendar.getInstance()
            val total = recordList.filter { record ->
                cal.timeInMillis = record.date
                val year = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH) + 1
                year == currentYear && month == selectedMonth
            }.sumOf { it.distanceKm }

            findViewById<TextView>(R.id.tvTotalDistance).text =
                String.format("%.2f km", total)

            dialog.dismiss()
        }

        dialog.show()
    }
}