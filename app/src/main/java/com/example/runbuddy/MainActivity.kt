package com.example.runbuddy

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
// import androidx.core.view.enableEdgeToEdge // 주석 해제하여 import 제거
import com.example.runbuddy.databinding.ActivityMainBinding
import com.example.runbuddy.login.LoginActivity
import com.example.runbuddy.ui.signup.SignUpActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ❌ 문제 원인: enableEdgeToEdge()
        // 해당 함수를 사용하지 않으려면 호출을 제거하거나 주석 처리하고,
        // androidx.core.view.enableEdgeToEdge import 역시 제거합니다.
        // enableEdgeToEdge() // 제거됨

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 로그인 화면으로 이동
        binding.btnGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // 회원가입 화면으로 이동
        binding.btnGoSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }
}