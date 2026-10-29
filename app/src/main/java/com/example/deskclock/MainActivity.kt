package com.example.deskclock

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.random.Random

class MainActivity : Activity() {

    private lateinit var root: View
    private lateinit var content: View
    private lateinit var hours: TextView
    private lateinit var minutes: TextView
    private lateinit var seconds: TextView
    private lateinit var date: TextView
    private lateinit var weekday: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var lastMinute = -1

    private lateinit var dateFormatter: DateTimeFormatter
    private lateinit var weekdayFormatter: DateTimeFormatter
    private lateinit var amPmFormatter: DateTimeFormatter

    private val ticker = object : Runnable {
        override fun run() {
            updateClock()
            // 对齐到下一个整秒，避免秒数跳变不均匀
            val delay = 1000 - (System.currentTimeMillis() % 1000)
            handler.postAtTime(this, SystemClock.uptimeMillis() + delay)
        }
    }

    // 系统时间、时区、语言变化时立即刷新
    private val timeChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            buildFormatters()
            lastMinute = -1
            updateClock()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        root = findViewById(R.id.root)
        content = findViewById(R.id.content)
        hours = findViewById(R.id.hours)
        minutes = findViewById(R.id.minutes)
        seconds = findViewById(R.id.seconds)
        date = findViewById(R.id.date)
        weekday = findViewById(R.id.weekday)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()

        // 点击屏幕切换夜间暗光模式
        root.setOnClickListener { toggleDim() }
        applyDim()

        buildFormatters()
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        registerReceiver(timeChangedReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_LOCALE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        })
        buildFormatters()
        lastMinute = -1
        handler.removeCallbacks(ticker)
        ticker.run()
        root.post { fitTimeSize() }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
        unregisterReceiver(timeChangedReceiver)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun buildFormatters() {
        val locale = Locale.getDefault()
        dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)
        weekdayFormatter = DateTimeFormatter.ofPattern("EEEE", locale)
        amPmFormatter = DateTimeFormatter.ofPattern("a", locale)
    }

    private fun updateClock() {
        val now = LocalDateTime.now()
        val is24h = DateFormat.is24HourFormat(this)

        val hour = if (is24h) now.hour else (now.hour % 12).let { if (it == 0) 12 else it }
        hours.text = if (is24h) "%02d".format(hour) else hour.toString()
        minutes.text = "%02d".format(now.minute)
        seconds.text = if (is24h) {
            "%02d".format(now.second)
        } else {
            "%02d  %s".format(now.second, now.format(amPmFormatter))
        }

        if (now.minute != lastMinute) {
            lastMinute = now.minute
            date.text = now.format(dateFormatter)
            weekday.text = now.format(weekdayFormatter)
            shiftForBurnIn()
        }
    }

    // 按屏幕可用宽高算出时间数字能用的最大字号
    private fun fitTimeSize() {
        val availW = (content.width - content.paddingLeft - content.paddingRight).toFloat()
        val availH = root.height.toFloat()
        if (availW <= 0 || availH <= 0) return

        val dp = resources.displayMetrics.density
        val probe = 100f
        val paint = android.graphics.Paint(hours.paint).apply { textSize = probe }
        val lineHeight = paint.fontMetrics.let { it.descent - it.ascent } / probe
        val secondsSample = if (DateFormat.is24HourFormat(this)) "00"
        else "00  " + LocalDateTime.now().format(amPmFormatter)

        val size: Float
        val secondsSize: Float
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 一行：HH:MM + 秒（秒字号为时间的 0.25）
            val secondsPaint = android.graphics.Paint(seconds.paint).apply { textSize = probe }
            val unitWidth = (paint.measureText("00:00") +
                0.25f * secondsPaint.measureText(secondsSample)) / probe
            val byWidth = (availW * 0.95f - 16 * dp) / unitWidth
            val byHeight = (availH * 0.88f - 60 * dp) / lineHeight
            size = minOf(byWidth, byHeight)
            secondsSize = size * 0.25f
        } else {
            // 两行：HH / MM，两行之间往回收 0.22 倍字号，下面还有秒、日期、星期
            val byWidth = availW * 0.92f / (paint.measureText("00") / probe)
            val byHeight = (availH * 0.9f - 140 * dp) / (lineHeight * 2.18f - 0.22f)
            size = minOf(byWidth, byHeight)
            secondsSize = size * 0.18f
            (minutes.layoutParams as ViewGroup.MarginLayoutParams).topMargin = (-0.22f * size).toInt()
        }

        hours.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
        minutes.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
        findViewById<TextView>(R.id.colon)?.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
        seconds.setTextSize(TypedValue.COMPLEX_UNIT_PX, secondsSize)
        minutes.requestLayout()
    }

    // AMOLED 屏长时间显示静态画面会烧屏，每分钟把内容随机挪动几个像素
    private fun shiftForBurnIn() {
        val range = resources.displayMetrics.density * 8
        content.translationX = Random.nextFloat() * 2 * range - range
        content.translationY = Random.nextFloat() * 2 * range - range
    }

    private fun toggleDim() {
        val prefs = getPreferences(Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DIM, !prefs.getBoolean(KEY_DIM, false)).apply()
        applyDim()
    }

    private fun applyDim() {
        val dim = getPreferences(Context.MODE_PRIVATE).getBoolean(KEY_DIM, false)
        content.alpha = if (dim) 0.4f else 1f
        window.attributes = window.attributes.apply {
            screenBrightness = if (dim) 0.02f
            else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    companion object {
        private const val KEY_DIM = "dim"
    }
}
