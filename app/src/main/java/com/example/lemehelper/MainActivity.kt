package com.example.lemehelper

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private data class Account(val email: String, val password: String)
    private val accounts = mutableListOf<Account>()
    private val logs = mutableListOf<String>()
    private lateinit var accountList: LinearLayout
    private lateinit var logView: TextView
    private lateinit var delayInput: EditText
    private lateinit var timeInput: EditText
    private lateinit var autoSwitch: Switch
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(this, "leme_secure_store", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadAccounts()
        buildUi()
        addLog("نسخه ۰٫۳: حساب‌ها روی دستگاه به‌صورت رمزگذاری‌شده ذخیره می‌شوند.")
        addLog("توجه: ورود خودکار و دریافت جایزه هنوز به سایت متصل نشده است.")
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xFF101010.toInt())
        }
        scroll.addView(root)
        setContentView(scroll)
        root.addView(label("مدیر جایزه LEME", 28, 0xFFD4AF37.toInt()))
        root.addView(label("نسخه ۰٫۳ • مدیریت حساب‌ها و باز کردن سایت", 14, 0xFFE0E0E0.toInt()))
        root.addView(label("🔐 ایمیل و رمزهای ذخیره‌شده با Android Keystore رمزگذاری می‌شوند. برای امنیت، از رمز اختصاصی و غیرحساس استفاده کن.", 13, 0xFFDDDDDD.toInt()))
        val openSite = button("🌐 باز کردن سایت رسمی LEME")
        root.addView(openSite)
        openSite.setOnClickListener { startActivity(Intent(this, LemeWebActivity::class.java)) }
        val add = button("＋ افزودن حساب")
        root.addView(add)
        add.setOnClickListener { showAddAccountDialog() }
        root.addView(label("حساب‌ها", 20, 0xFFD4AF37.toInt()))
        accountList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(accountList)
        root.addView(label("تنظیمات", 20, 0xFFD4AF37.toInt()))
        autoSwitch = Switch(this).apply {
            text = "اجرای خودکار روزانه (فعلاً غیرفعال)"
            setTextColor(0xFFFFFFFF.toInt())
            isChecked = false
            isEnabled = false
        }
        root.addView(autoSwitch)
        timeInput = EditText(this).apply {
            hint = "ساعت دلخواه، مثلاً 21:30"
            setHintTextColor(0xFFAAAAAA.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setSingleLine()
            setText(prefs.getString("run_time", "21:30"))
        }
        root.addView(timeInput, matchWidth())
        val saveTime = button("ذخیره ساعت دلخواه")
        root.addView(saveTime)
        saveTime.setOnClickListener {
            val time = timeInput.text.toString().trim()
            if (time.matches(Regex("^([01]\\d|2[0-3]):[0-5]\\d$"))) {
                prefs.edit().putString("run_time", time).apply()
                addLog("ساعت ترجیحی $time ذخیره شد؛ زمان‌بندی خودکار هنوز فعال نیست.")
            } else addLog("فرمت ساعت درست نیست؛ نمونه صحیح 21:30 است.")
        }
        delayInput = EditText(this).apply {
            hint = "تأخیر بین حساب‌ها به ثانیه"
            setHintTextColor(0xFFAAAAAA.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("delay_seconds", 5).toString())
        }
        root.addView(delayInput, matchWidth())
        val saveDelay = button("ذخیره تأخیر")
        root.addView(saveDelay)
        saveDelay.setOnClickListener {
            val delay = delayInput.text.toString().toIntOrNull()
            if (delay != null && delay in 0..300) {
                prefs.edit().putInt("delay_seconds", delay).apply()
                addLog("تأخیر $delay ثانیه ذخیره شد.")
            } else addLog("تأخیر باید عددی بین ۰ تا ۳۰۰ باشد.")
        }
        val run = button("▶ اجرای آزمایشی فهرست حساب‌ها")
        root.addView(run)
        run.setOnClickListener { runAllManually() }
        root.addView(label("گزارش", 20, 0xFFD4AF37.toInt()))
        logView = label("", 13, 0xFFE0E0E0.toInt())
        logView.setPadding(12, 12, 12, 12)
        root.addView(logView)
        refreshAccounts()
    }

    private fun showAddAccountDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 8, 32, 0) }
        val email = EditText(this).apply {
            hint = "ایمیل"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val password = EditText(this).apply {
            hint = "رمز عبور"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        box.addView(email); box.addView(password)
        AlertDialog.Builder(this).setTitle("افزودن حساب").setView(box).setNegativeButton("لغو", null)
            .setPositiveButton("ذخیره") { _, _ ->
                val e = email.text.toString().trim()
                val p = password.text.toString()
                if (e.isBlank() || p.isBlank()) addLog("ایمیل و رمز عبور را کامل وارد کن.")
                else if (accounts.any { it.email.equals(e, ignoreCase = true) }) addLog("این ایمیل از قبل اضافه شده است.")
                else {
                    accounts.add(Account(e, p)); saveAccounts()
                    addLog("حساب $e به‌صورت رمزگذاری‌شده ذخیره شد."); refreshAccounts()
                }
            }.show()
    }

    private fun runAllManually() {
        if (accounts.isEmpty()) { addLog("اول یک حساب اضافه کن."); return }
        val delaySeconds = delayInput.text.toString().toLongOrNull()?.coerceIn(0, 300) ?: 5L
        prefs.edit().putInt("delay_seconds", delaySeconds.toInt()).apply()
        val snapshot = accounts.toList()
        addLog("بررسی آزمایشی ${snapshot.size} حساب شروع شد؛ فاصله $delaySeconds ثانیه.")
        thread {
            snapshot.forEachIndexed { index, account ->
                runOnUiThread { addLog("${account.email}: فقط در فهرست بررسی شد؛ هنوز ورود یا دریافت جایزه انجام نمی‌شود.") }
                if (index < snapshot.lastIndex && delaySeconds > 0) Thread.sleep(delaySeconds * 1000)
            }
            runOnUiThread { addLog("پایان آزمایش. اتصال واقعی به فرایند ورود/جایزه هنوز پیاده‌سازی نشده است.") }
        }
    }

    private fun loadAccounts() {
        try {
            val json = JSONArray(prefs.getString("accounts_json", "[]"))
            for (i in 0 until json.length()) {
                val item = json.getJSONObject(i)
                val email = item.optString("email"); val password = item.optString("password")
                if (email.isNotBlank() && password.isNotBlank()) accounts.add(Account(email, password))
            }
        } catch (_: Exception) { addLog("خواندن فهرست حساب‌ها ناموفق بود؛ ممکن است داده ذخیره‌شده خراب شده باشد.") }
    }
    private fun saveAccounts() {
        val json = JSONArray()
        accounts.forEach { account -> json.put(JSONObject().put("email", account.email).put("password", account.password)) }
        prefs.edit().putString("accounts_json", json.toString()).apply()
    }
    private fun refreshAccounts() {
        accountList.removeAllViews()
        if (accounts.isEmpty()) { accountList.addView(label("هنوز حسابی اضافه نشده است.", 14, 0xFFAAAAAA.toInt())); return }
        accounts.toList().forEachIndexed { index, account ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val text = label("${index + 1}. ${account.email}", 14, 0xFFFFFFFF.toInt())
            row.addView(text, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val remove = button("حذف"); row.addView(remove)
            remove.setOnClickListener {
                accounts.removeAll { it.email.equals(account.email, ignoreCase = true) }
                saveAccounts(); addLog("حساب ${account.email} حذف شد."); refreshAccounts()
            }
            accountList.addView(row)
        }
    }
    private fun addLog(message: String) {
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logs.add("[$stamp] $message")
        while (logs.size > 80) logs.removeAt(0)
        if (::logView.isInitialized) logView.text = logs.asReversed().joinToString("\n")
    }
    private fun label(text: String, size: Int, color: Int) = TextView(this).apply {
        this.text = text; textSize = size.toFloat(); setTextColor(color); setPadding(0, 10, 0, 10)
    }
    private fun button(text: String) = Button(this).apply {
        this.text = text; setTextColor(0xFF101010.toInt()); setBackgroundColor(0xFFD4AF37.toInt()); isAllCaps = false
    }
    private fun matchWidth() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
}
