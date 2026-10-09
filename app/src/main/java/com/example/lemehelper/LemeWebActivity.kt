package com.example.lemehelper

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject

class LemeWebActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var status: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(16, 16, 16)) }
        val header = TextView(this).apply {
            text = "LEME • بررسی وضعیت جایزه"; textSize = 17f
            setTextColor(Color.rgb(212, 175, 55)); setPadding(16, 12, 16, 12)
        }
        status = TextView(this).apply {
            text = "پس از بارگذاری صفحه، «بررسی وضعیت» را بزن."
            textSize = 13f; setTextColor(Color.WHITE); setPadding(12, 8, 12, 8)
        }
        val check = Button(this).apply { text = "بررسی وضعیت صفحه"; isAllCaps = false; setOnClickListener { inspectPage() } }
        val open = Button(this).apply { text = "بازکردن صفحه اصلی LEME"; isAllCaps = false; setOnClickListener { web.loadUrl("https://coe.leme.hk.cn/") } }
        web = WebView(this).apply {
            settings.javaScriptEnabled = true; settings.domStorageEnabled = true; settings.loadsImagesAutomatically = true
            CookieManager.getInstance().setAcceptCookie(true)
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    status.text = "صفحه بارگذاری شد. برای تحلیل متن، «بررسی وضعیت صفحه» را بزن."
                }
            }
            webChromeClient = WebChromeClient()
            loadUrl("https://coe.leme.hk.cn/")
        }
        root.addView(header); root.addView(status); root.addView(check); root.addView(open)
        root.addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }
    private fun inspectPage() {
        val js = """
            (function() {
              const text = (document.body && document.body.innerText || '').replace(/\\s+/g, ' ').slice(0, 12000);
              const buttons = Array.from(document.querySelectorAll('button, input[type=button], input[type=submit], a'))
                .map(e => ((e.innerText || e.value || e.getAttribute('aria-label') || '').trim()).slice(0,80))
                .filter(Boolean).slice(0,80);
              return JSON.stringify({url: location.href, title: document.title, text: text, buttons: buttons});
            })();
        """.trimIndent()
        web.evaluateJavascript(js) { raw ->
            try {
                val decoded = org.json.JSONTokener(raw ?: "null").nextValue()
                val obj = if (decoded is String) JSONObject(decoded) else decoded as JSONObject
                val text = obj.optString("text")
                val buttons = obj.optJSONArray("buttons")
                val buttonText = mutableListOf<String>()
                if (buttons != null) for (i in 0 until buttons.length()) buttonText.add(buttons.optString(i))
                val loggedIn = text.contains("سکه لِمه") || text.contains("سکه لمه") || text.contains("Leme Coins", ignoreCase = true)
                val alreadyChecked = text.contains("وارد شد") || text.contains("checked in", ignoreCase = true)
                val summary = buildString {
                    append("عنوان: ").append(obj.optString("title")).append("\n")
                    append("نشانه حساب واردشده: ").append(if (loggedIn) "احتمالاً بله" else "نامشخص").append("\n")
                    append("نشانه دریافت قبلی: ").append(if (alreadyChecked) "پیدا شد" else "پیدا نشد").append("\n")
                    append("متن/دکمه‌های مرتبط: ").append(
                        buttonText.filter { it.contains("ورود") || it.contains("Sign", true) || it.contains("check", true) }
                            .take(10).joinToString("، ").ifBlank { "دکمه مرتبطی شناسایی نشد" }
                    ).append("\n\nاین تشخیص تقریبی است؛ هیچ دکمه‌ای خودکار زده نشده.")
                }
                status.text = summary
                Toast.makeText(this, "بررسی صفحه انجام شد", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) { status.text = "خواندن نتیجه صفحه ممکن نشد. صفحه را دوباره بارگذاری و امتحان کن." }
        }
    }
}
