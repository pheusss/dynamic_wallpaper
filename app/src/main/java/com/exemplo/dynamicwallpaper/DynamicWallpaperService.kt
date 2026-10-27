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

class DynamicWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = DynamicEngine()

    inner class DynamicEngine : Engine(), SensorEventListener {

        private val handler = Handler(Looper.getMainLooper())
        private var visible = false

        private var sensorManager: SensorManager? = null
        private var rotationSensor: Sensor? = null
        private var tiltX = 0f
        private var tiltY = 0f

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val drawRunnable = Runnable { drawFrame() }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                sensorManager?.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
                drawFrame()
            } else {
                sensorManager?.unregisterListener(this)
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                val rotationMatrix = FloatArray(9)
                val orientation = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)

                val targetX = orientation[2]
                val targetY = orientation[1]

                tiltX += (targetX - tiltX) * 0.1f
                tiltY += (targetY - tiltY) * 0.1f
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

                    val (startColor, endColor) = getPaletteByTime()

                    val offsetX = tiltX * (width * 0.35f)
                    val offsetY = tiltY * (height * 0.35f)

                    val gradient = LinearGradient(
                        0f + offsetX, 0f + offsetY,
                        width - offsetX, height - offsetY,
                        startColor, endColor,
                        Shader.TileMode.CLAMP
                    )

                    paint.shader = gradient
                    canvas.drawRect(0f, 0f, width, height, paint)
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }

            handler.removeCallbacks(drawRunnable)
            if (visible) {
                handler.postDelayed(drawRunnable, 16)
            }
        }

        private fun getPaletteByTime(): Pair<Int, Int> {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 6..11 -> Pair(Color.parseColor("#48CAE4"), Color.parseColor("#A3E635"))
                in 12..17 -> Pair(Color.parseColor("#F97316"), Color.parseColor("#0EA5E9"))
                in 18..20 -> Pair(Color.parseColor("#FB923C"), Color.parseColor("#4C1D95"))
                else -> Pair(Color.parseColor("#0F172A"), Color.parseColor("#1E3A8A"))
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            sensorManager?.unregisterListener(this)
            handler.removeCallbacks(drawRunnable)
        }
    }
}
