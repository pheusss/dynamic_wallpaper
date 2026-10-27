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
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("wallpaper_prefs", Context.MODE_PRIVATE)
        val currentTheme = prefs.getString("selected_theme", "dynamic")

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 96, 64, 64)
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.parseColor("#F8FAFC"))
        }

        val title = TextView(this).apply {
            text = "Estilo do Wallpaper"
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0F172A"))
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Escolha sua paleta favorita ou mantenha sincronizado com o horário:"
            textSize = 14f
            setTextColor(Color.parseColor("#64748B"))
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 48)
        }

        val radioGroup = RadioGroup(this)

        val themes = listOf(
            Pair("dynamic", "Ciclo Dinâmico (Muda com as horas)"),
            Pair("aurora", "Aurora Boreal (Ciano e Menta)"),
            Pair("pastel", "Pôr do Sol Pastel (Pêssego e Lavanda)"),
            Pair("cosmos", "Deep Cosmos (Violeta e Noite Escura)")
        )

        for ((key, label) in themes) {
            val rb = RadioButton(this).apply {
                text = label
                textSize = 16f
                setPadding(24, 24, 24, 24)
                setTextColor(Color.parseColor("#1E293B"))
                isChecked = (key == currentTheme)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        prefs.edit().putString("selected_theme", key).apply()
                    }
                }
            }
            radioGroup.addView(rb)
        }

        val applyButton = Button(this).apply {
            text = "Definir como Papel de Parede"
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#0284C7"))
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

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }

        rootLayout.addView(title)
        rootLayout.addView(subtitle)
        rootLayout.addView(radioGroup)
        rootLayout.addView(spacer)
        rootLayout.addView(applyButton)

        setContentView(rootLayout)
    }
}
