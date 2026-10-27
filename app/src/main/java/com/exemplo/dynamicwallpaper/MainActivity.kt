package com.exemplo.dynamicwallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("wallpaper_prefs", Context.MODE_PRIVATE)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0F172A"))
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 64)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        scroll.addView(root)

        val title = TextView(this).apply {
            text = "Ajustes do Wallpaper"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        root.addView(title)

        // Seletor de Temas
        val group = RadioGroup(this).apply { setPadding(0, 32, 0, 32) }
        val currentTheme = prefs.getString("selected_theme", "dynamic")
        val themes = listOf(
            Pair("dynamic", "Ciclo Dinâmico (Horário)"),
            Pair("aurora", "Aurora Boreal (Verde e Menta)"),
            Pair("pastel", "Pôr do Sol Pastel (Laranja e Rosa)"),
            Pair("cosmos", "Deep Cosmos (Violeta e Escuro)")
        )

        for ((key, label) in themes) {
            val rb = RadioButton(this).apply {
                text = label
                textSize = 15f
                setTextColor(Color.parseColor("#E2E8F0"))
                isChecked = (key == currentTheme)
                setOnCheckedChangeListener { _, checked ->
                    if (checked) prefs.edit().putString("selected_theme", key).apply()
                }
            }
            group.addView(rb)
        }
        root.addView(group)

        // 1. Slider: Suavização (Lerp) - quanto menor, mais amortecido
        addSlider(
            root,
            label = "Suavização do Movimento",
            initial = prefs.getInt("pref_lerp", 5), // 1 a 20 (0.01 a 0.20)
            max = 20,
            unit = "%"
        ) { value ->
            prefs.edit().putInt("pref_lerp", if (value < 1) 1 else value).apply()
        }

        // 2. Slider: Sensibilidade de Rotação (Ângulo)
        addSlider(
            root,
            label = "Giro do Degradê (Ângulo)",
            initial = prefs.getInt("pref_angle_sens", 6), // 0 a 20
            max = 20,
            unit = "x"
        ) { value ->
            prefs.edit().putInt("pref_angle_sens", value).apply()
        }

        // 3. Slider: Deslocamento do Centro (Shift)
        addSlider(
            root,
            label = "Deslocamento Físico (Paralaxe)",
            initial = prefs.getInt("pref_shift_sens", 5), // 0 a 20
            max = 20,
            unit = "%"
        ) { value ->
            prefs.edit().putInt("pref_shift_sens", value).apply()
        }

        // Botão para redefinir ou abrir o seletor nativo
        val btnApply = Button(this).apply {
            text = "Definir / Abrir Prévia Nativa"
            setBackgroundColor(Color.parseColor("#0284C7"))
            setTextColor(Color.WHITE)
            setPadding(32, 24, 32, 24)
            setOnClickListener {
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(this@MainActivity, DynamicWallpaperService::class.java)
                    )
                }
                startActivity(intent)
            }
        }
        root.addView(btnApply)

        setContentView(scroll)
    }

    private fun addSlider(
        parent: LinearLayout,
        label: String,
        initial: Int,
        max: Int,
        unit: String,
        onChange: (Int) -> Unit
    ) {
        val labelView = TextView(this).apply {
            text = "$label: $initial$unit"
            textSize = 14f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 16, 0, 8)
        }
        val seekBar = SeekBar(this).apply {
            this.max = max
            progress = initial
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    labelView.text = "$label: $p$unit"
                    onChange(p)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        parent.addView(labelView)
        parent.addView(seekBar)
    }
}
