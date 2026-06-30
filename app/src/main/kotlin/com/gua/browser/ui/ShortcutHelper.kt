package com.gua.browser.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat

/**
 * 添加到桌面（创建快捷方式）
 */
object ShortcutHelper {

    fun createShortcut(context: Context, title: String, url: String) {
        val id = "web_${url.hashCode()}_${System.currentTimeMillis()}"

        val intent = Intent(context, com.gua.browser.MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = android.net.Uri.parse(url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val icon = createDefaultIcon(title)

        val shortcut = ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(title.take(10))
            .setLongLabel(url)
            .setIcon(icon)
            .setIntent(intent)
            .build()

        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }

    private fun createDefaultIcon(title: String): androidx.core.graphics.drawable.IconCompat {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 背景圆
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1565C0.toInt()
        }
        canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), 24f, 24f, paint)

        // 首字母
        paint.apply {
            color = android.graphics.Color.WHITE
            textSize = 56f
            textAlign = Paint.Align.CENTER
        }
        val letter = title.firstOrNull()?.uppercase() ?: "W"
        canvas.drawText(letter, size / 2f, size / 2f + 20f, paint)

        return androidx.core.graphics.drawable.IconCompat.createWithBitmap(bitmap)
    }
}
