package com.exemplo.dynamicwallpaper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Shader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

class DynamicWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = DynamicEngine()

    inner class DynamicEngine : Engine(), SensorEventListener {

        private val handler = Handler(Looper.getMainLooper())
        private var isVisible = false

        private var sensorManager: SensorManager? = null
        private var accelerometer: Sensor? = null

        private var smoothX = 0f
        private var smoothY = 0f

        // Dithering e Anti-alias ativados para evitar color banding
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
        }

        private val drawRunnable = Runnable { drawFrame() }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            // Força renderização em 32-bit (8-bit reais por canal sem perda)
            surfaceHolder?.setFormat(PixelFormat.RGBA_8888)

            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.isVisible = visible
            if (visible) {
                sensorManager?.registerListener(
                    this,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_GAME
                )
                drawFrame()
            } else {
                sensorManager?.unregisterListener(this)
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                val rawX = event.values[0]
                val rawY = event.values[1]

                // Movimento muito mais amortecido e sutil (0.05f)
                smoothX += (rawX - smoothX) * 0.05f
                smoothY += (rawY - smoothY) * 0.05f
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null

            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    val width = canvas.width.toFloat()
                    val height = canvas.height.toFloat()

                    val centerX = width / 2f
                    val centerY = height / 2f

                    // Deslocamento sutil e natural
                    val shiftX = -smoothX * (width * 0.05f)
                    val shiftY = smoothY * (height * 0.05f)

                    // Ângulo contido para evitar saltos visuais na rotação
                    val angle = Math.toRadians((smoothX * 6.0) + 90.0)
                    val radius = Math.max(width, height) * 0.85f

                    val x0 = (centerX + shiftX) - cos(angle).toFloat() * radius
                    val y0 = (centerY + shiftY) - sin(angle).toFloat() * radius
                    val x1 = (centerX + shiftX) + cos(angle).toFloat() * radius
                    val y1 = (centerY + shiftY) + sin(angle).toFloat() * radius

                    val colors = getSelectedPaletteColors()

                    paint.shader = LinearGradient(
                        x0, y0, x1, y1,
                        colors,
                        floatArrayOf(0.0f, 0.5f, 1.0f),
                        Shader.TileMode.CLAMP
                    )

                    canvas.drawRect(0f, 0f, width, height, paint)
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }

            handler.removeCallbacks(drawRunnable)
            if (isVisible) {
                handler.postDelayed(drawRunnable, 16)
            }
        }

        private fun getSelectedPaletteColors(): IntArray {
            val prefs = getSharedPreferences("wallpaper_prefs", Context.MODE_PRIVATE)
            return when (prefs.getString("selected_theme", "dynamic")) {
                "aurora" -> intArrayOf(
                    Color.parseColor("#0F3E46"),
                    Color.parseColor("#1B6B76"),
                    Color.parseColor("#5EEAD4")
                )
                "pastel" -> intArrayOf(
                    Color.parseColor("#7C3AED"),
                    Color.parseColor("#FB7185"),
                    Color.parseColor("#FDE68A")
                )
                "cosmos" -> intArrayOf(
                    Color.parseColor("#090A0F"),
                    Color.parseColor("#1E1B4B"),
                    Color.parseColor("#312E81")
                )
                else -> getDynamicColors()
            }
        }

        private fun getDynamicColors(): IntArray {
            val cal = Calendar.getInstance()
            val totalMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val dayFraction = totalMinutes / 1440f

            return when {
                dayFraction in 0.20f..0.37f -> {
                    val factor = (dayFraction - 0.20f) / 0.17f
                    intArrayOf(
                        blendColors(Color.parseColor("#0F172A"), Color.parseColor("#0284C7"), factor),
                        blendColors(Color.parseColor("#0369A1"), Color.parseColor("#0EA5E9"), factor),
                        blendColors(Color.parseColor("#1E293B"), Color.parseColor("#BAE6FD"), factor)
                    )
                }
                dayFraction in 0.37f..0.68f -> {
                    val factor = (dayFraction - 0.37f) / 0.31f
                    intArrayOf(
                        blendColors(Color.parseColor("#0284C7"), Color.parseColor("#0369A1"), factor),
                        blendColors(Color.parseColor("#0EA5E9"), Color.parseColor("#F97316"), factor),
                        blendColors(Color.parseColor("#BAE6FD"), Color.parseColor("#FDE047"), factor)
                    )
                }
                dayFraction in 0.68f..0.81f -> {
                    val factor = (dayFraction - 0.68f) / 0.13f
                    intArrayOf(
                        blendColors(Color.parseColor("#0369A1"), Color.parseColor("#312E81"), factor),
                        blendColors(Color.parseColor("#F97316"), Color.parseColor("#9D174D"), factor),
                        blendColors(Color.parseColor("#FDE047"), Color.parseColor("#1E1B4B"), factor)
                    )
                }
                else -> intArrayOf(
                    Color.parseColor("#090D16"),
                    Color.parseColor("#172033"),
                    Color.parseColor("#1E1B4B")
                )
            }
        }

        private fun blendColors(color1: Int, color2: Int, factor: Float): Int {
            val f = factor.coerceIn(0f, 1f)
            val a = (Color.alpha(color1) * (1 - f) + Color.alpha(color2) * f).toInt()
            val r = (Color.red(color1) * (1 - f) + Color.red(color2) * f).toInt()
            val g = (Color.green(color1) * (1 - f) + Color.green(color2) * f).toInt()
            val b = (Color.blue(color1) * (1 - f) + Color.blue(color2) * f).toInt()
            return Color.argb(a, r, g, b)
        }

        override fun onDestroy() {
            super.onDestroy()
            sensorManager?.unregisterListener(this)
            handler.removeCallbacks(drawRunnable)
        }
    }
}
