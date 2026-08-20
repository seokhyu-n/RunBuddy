package com.example.runbuddy.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.runbuddy.R
import com.example.runbuddy.databinding.ActivityHomeBinding
import com.example.runbuddy.map.MapActivity
import com.example.runbuddy.ui.ai.AiChatActivity
import com.example.runbuddy.login.LoginActivity
import com.example.runbuddy.ui.ready.ReadySetGoActivity
import com.example.runbuddy.ui.record.RecordActivity
import com.example.runbuddy.ui.profile.ProfileActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔥 로그인한 사용자 이름 표시
        val userName = intent.getStringExtra("userName") ?: "사용자"
        binding.tvWelcome.text = "${userName}님, 오늘도 달려볼까요?"

        // ============================
        // ⭐ 햄버거 버튼 → 메뉴 열기
        // ============================
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // ============================
        // ⭐ 사이드 메뉴 클릭 처리
        // ============================
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {

                // ▶ 내 정보 보기
                R.id.nav_profile -> {
                    val prefs = getSharedPreferences("runbuddy_prefs", MODE_PRIVATE)
                    val name = prefs.getString("userName", "사용자")
                    val email = prefs.getString("userEmail", "unknown")
                    val uid = prefs.getString("userUid", "-")

                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("userName", name)
                    intent.putExtra("userEmail", email)
                    intent.putExtra("userUid", uid)
                    startActivity(intent)
                }


                // ▶ 로그아웃
                R.id.nav_logout -> {

                    // 🔥 1) 자동로그인 정보 완전 삭제
                    getSharedPreferences("runbuddy_prefs", MODE_PRIVATE)
                        .edit()
                        .clear()
                        .apply()

                    // 🔥 2) 로그인 화면으로 이동 + 뒤로가기 금지
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }

            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // ============================
        // ⭐ 홈 화면 기능 버튼들
        // ============================

        // ✅ '내 활동' 클릭 시 RecordActivity로 이동 (이전 문제 해결)
        binding.layoutMyActivity.setOnClickListener {
            startActivity(Intent(this, RecordActivity::class.java))
        }

        binding.btnStartRunCircle.setOnClickListener {
            startActivity(Intent(this, ReadySetGoActivity::class.java))
        }

        binding.btnAiSmall.setOnClickListener {
            startActivity(Intent(this, AiChatActivity::class.java))
        }

        // ✅ Map 버튼 클릭 시 AI 추천 코스를 인텐트에 담아 MapActivity로 이동
        binding.btnMapSmall.setOnClickListener {
            val prefs = getSharedPreferences("runbuddy_prefs", MODE_PRIVATE)
            val courseName = prefs.getString("ai_course", "none")

            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("courseName", courseName)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        // 화면에 돌아올 때마다 사용자 이름 갱신
        val prefs = getSharedPreferences("runbuddy_prefs", MODE_PRIVATE)
        val updatedName = prefs.getString("userName", "사용자")

        binding.tvWelcome.text = "${updatedName}님, 오늘도 달려볼까요?"
    }
}