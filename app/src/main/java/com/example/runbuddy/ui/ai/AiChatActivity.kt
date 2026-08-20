package com.example.runbuddy.ui.ai

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.runbuddy.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiChatActivity : AppCompatActivity() {

    private lateinit var rvChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var adapter: ChatAdapter

    private val messages = mutableListOf<ChatMessage>()

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(40, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .writeTimeout(40, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val SERVER_URL = "https://runbuddy-ai-server.onrender.com/api/ai-chat"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_chat)

        rvChat = findViewById(R.id.rvChat)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        adapter = ChatAdapter(messages)
        rvChat.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvChat.adapter = adapter

        // 🔥 저장된 기록 불러오기
        loadChatHistory()

// 🔥 저장된 기록이 없을 때만 인삿말 추가
        if (messages.isEmpty()) {
            adapter.addMessage(
                ChatMessage(
                    "안녕하세요! 저는 RunBuddy AI 러닝 코치예요.\n" +
                            "원하는 러닝 장소 또는 스타일을 말씀해주세요! 😊",
                    isUser = false
                )
            )
        }


        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            adapter.addMessage(ChatMessage(text, true))
            etMessage.setText("")
            scrollToBottom()

            requestAiReply()
        }
    }

    private fun scrollToBottom() {
        rvChat.scrollToPosition(adapter.itemCount - 1)
    }

    /** AI 서버 요청 */
    private fun requestAiReply() {
        lifecycleScope.launch {
            try {
                val reply = withContext(Dispatchers.IO) {
                    callServer(messages)
                }

                Log.d("AI", "📩 AI reply = $reply")

                adapter.addMessage(ChatMessage(reply, false))
                scrollToBottom()
                saveChatHistory()


                // ⭐ 장소 자동 추출 + 저장
                val places = extractPlaceNames(reply)

                Log.d("AI", "📌 추출된 장소 = $places")

                val prefs = getSharedPreferences("runbuddy_prefs", MODE_PRIVATE)
                prefs.edit()
                    .putStringSet("route_points", places.toSet())
                    .apply()

                Log.d("AI", "💾 SharedPreferences 저장됨: route_points = ${places.toSet()}")

            } catch (e: Exception) {

                val msg = when {
                    e.message?.contains("Unable to resolve host") == true ->
                        "서버에 연결할 수 없습니다.\n잠시 후 다시 시도해주세요."

                    else -> "에러 발생: ${e.message}"
                }

                adapter.addMessage(ChatMessage(msg, false))
                scrollToBottom()
            }
        }
    }

    /** 서버 POST 요청 */
    private fun callServer(history: List<ChatMessage>): String {

        val messagesJson = JSONArray()
        history.forEach { msg ->
            messagesJson.put(
                JSONObject().apply {
                    put("role", if (msg.isUser) "user" else "assistant")
                    put("content", msg.text)
                }
            )
        }

        val root = JSONObject().apply { put("messages", messagesJson) }

        val body = root.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(SERVER_URL)
            .post(body)
            .build()

        client.newCall(request).execute().use { res ->
            if (!res.isSuccessful) throw IllegalStateException("서버 오류: ${res.code}")

            val str = res.body?.string() ?: throw IllegalStateException("빈 응답")
            val json = JSONObject(str)
            return json.getString("reply")
        }
    }

    /**
     * ⭐ 최종 개선된 장소명 추출
     */
    private fun extractPlaceNames(text: String): List<String> {

        val result = mutableListOf<String>()

        // ### 넓은 범위의 출발 관련 패턴
        val startPatterns = listOf(
            Regex("""출발점[:：]?\s*([^\n]+)"""),
            Regex("""출발지점[:：]?\s*([^\n]+)"""),
            Regex("""출발 위치[:：]?\s*([^\n]+)"""),
            Regex("""출발[:：]?\s*([^\n]+)""")
        )

        for (regex in startPatterns) {
            val match = regex.find(text)
            if (match != null) {
                val extracted = match.groupValues[1].trim()
                if (extracted.length >= 2) result.add(extracted)
                break
            }
        }

        // ### 넓은 범위의 도착 관련 패턴
        val endPatterns = listOf(
            Regex("""도착점[:：]?\s*([^\n]+)"""),
            Regex("""도착지점[:：]?\s*([^\n]+)"""),
            Regex("""도착 위치[:：]?\s*([^\n]+)"""),
            Regex("""도착[:：]?\s*([^\n]+)""")
        )

        for (regex in endPatterns) {
            val match = regex.find(text)
            if (match != null) {
                val extracted = match.groupValues[1].trim()
                if (extracted.length >= 2) result.add(extracted)
                break
            }
        }

        // 둘 다 찾았다면 반환
        if (result.size >= 2) return result.distinct()

        // ---------------------------
        // fallback (키워드 기반)
        // ---------------------------
        val placeKeywords = listOf(
            "정문", "학교", "공원", "생태", "배다리", "도서관", "광장", "호수"
        )

        val lines = text.split("\n")

        for (line in lines) {
            val clean = line.replace("*", "").trim()

            if (placeKeywords.any { clean.contains(it) }) {
                val parts = clean.split(" ", "-", ":", "–")
                val best = parts.maxByOrNull { it.length }?.trim()
                if (!best.isNullOrEmpty()) result.add(best)
            }
        }

        return result.distinct()
    }

    private fun saveChatHistory() {
        val prefs = getSharedPreferences("runbuddy_chat", MODE_PRIVATE)

        val jsonArray = JSONArray()
        messages.forEach { msg ->
            val obj = JSONObject().apply {
                put("text", msg.text)
                put("isUser", msg.isUser)
            }
            jsonArray.put(obj)
        }

        prefs.edit()
            .putString("chat_history", jsonArray.toString())
            .apply()
    }

    private fun loadChatHistory() {
        val prefs = getSharedPreferences("runbuddy_chat", MODE_PRIVATE)
        val stored = prefs.getString("chat_history", null) ?: return

        try {
            val jsonArray = JSONArray(stored)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                messages.add(
                    ChatMessage(
                        obj.getString("text"),
                        obj.getBoolean("isUser")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}