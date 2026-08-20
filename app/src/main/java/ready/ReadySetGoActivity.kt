package com.example.runbuddy.ui.ready

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.runbuddy.R
import com.example.runbuddy.ui.running.RunningActivity

class ReadySetGoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ready_set_go)

        val tvReady = findViewById<TextView>(R.id.tvReady)

        val handler = Handler(Looper.getMainLooper())

        tvReady.text = "Ready"
        handler.postDelayed({ tvReady.text = "Set" }, 1000)
        handler.postDelayed({ tvReady.text = "Go!" }, 2000)

        handler.postDelayed({
            startActivity(Intent(this, RunningActivity::class.java))
            finish()
        }, 3000)
    }
}
