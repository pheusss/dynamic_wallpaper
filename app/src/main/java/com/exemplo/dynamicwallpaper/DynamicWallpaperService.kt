package com.exemplo.dynamicwallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
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

        // Suavização do movimento
        private var smoothX = 0f
        private var smoothY = 0f

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val drawRunnable = Runnable { drawFrame() }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
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
                // event.values[0]: inclinação lateral (-9.8 a +9.8)
                // event.values[1]: inclinação vertical (-9.8 a +9.8)
                val rawX = event.values[0]
                val rawY = event.values[1]

                // Filtro de suavização (amortecimento para evitar tremeliques)
                smoothX += (rawX - smoothX) * 0.15f
                smoothY += (rawY - smoothY) * 0.15f
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

                    // Deslocamento do centro visual baseado na inclinação
                    val shiftX = -smoothX * (width * 0.08f)
                    val shiftY = smoothY * (height * 0.08f)

                    // Rotação dinâmica do gradiente (ângulo varia com a rotação do aparelho)
                    val angle = Math.toRadians((smoothX * 12.0) + 90.0)
                    val radius = Math.max(width, height) * 0.9f

                    val x0 = (centerX + shiftX) - cos(angle).toFloat() * radius
                    val y0 = (centerY + shiftY) - sin(angle).toFloat() * radius
                    val x1 = (centerX + shiftX) + cos(angle).toFloat() * radius
                    val y1 = (centerY + shiftY) + sin(angle).toFloat() * radius

                    // Cores calculadas dinamicamente com base no minuto atual
                    val (startColor, midColor, endColor) = getDynamicDayPalette()

                    paint.shader = LinearGradient(
                        x0, y0, x1, y1,
                        intArrayOf(startColor, midColor, endColor),
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
                handler.postDelayed(drawRunnable, 16) // ~60 FPS
            }
        }

        // Calcula a cor exata interpolando minuto a minuto ao longo das 24h
        private fun getDynamicDayPalette(): Triple<Int, Int, Int> {
            val cal = Calendar.getInstance()
            val totalMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val dayFraction = totalMinutes / 1440f // 0.0 a 1.0 ao longo do dia

            return when {
                // Madrugada para Manhã (05:00 às 09:00)
                dayFraction in 0.20f..0.37f -> {
                    val factor = (dayFraction - 0.20f) / 0.17f
                    Triple(
                        blendColors(Color.parseColor("#0F172A"), Color.parseColor("#0284C7"), factor),
                        blendColors(Color.parseColor("#38BDF8"), Color.parseColor("#34D399"), factor),
                        blendColors(Color.parseColor("#1E293B"), Color.parseColor("#FDE047"), factor)
                    )
                }
                // Dia Pleno (09:00 às 16:30)
                dayFraction in 0.37f..0.68f -> {
                    val factor = (dayFraction - 0.37f) / 0.31f
                    Triple(
                        blendColors(Color.parseColor("#0284C7"), Color.parseColor("#0284C7"), factor),
                        blendColors(Color.parseColor("#34D399"), Color.parseColor("#FB923C"), factor),
                        blendColors(Color.parseColor("#FDE047"), Color.parseColor("#F43F5E"), factor)
                    )
                }
                // Pôr do Sol / Entardecer (16:30 às 19:30)
                dayFraction in 0.68f..0.81f -> {
                    val factor = (dayFraction - 0.68f) / 0.13f
                    Triple(
                        blendColors(Color.parseColor("#0284C7"), Color.parseColor("#4C1D95"), factor),
                        blendColors(Color.parseColor("#FB923C"), Color.parseColor("#BE185D"), factor),
                        blendColors(Color.parseColor("#F43F5E"), Color.parseColor("#1E1B4B"), factor)
                    )
                }
                // Noite (19:30 às 05:00)
                else -> {
                    Triple(
                        Color.parseColor("#090D16"),
                        Color.parseColor("#1E1B4B"),
                        Color.parseColor("#022C43")
                    )
                }
            }
        }

        // Mistura linear de cores para criar transições imperceptíveis
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
