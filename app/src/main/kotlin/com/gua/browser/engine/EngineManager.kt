package com.gua.browser.engine

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import org.mozilla.geckoview.GeckoView
import java.util.UUID

/**
 * 引擎管理器 — 多标签管理
 *
 * 维护 Tab 列表，每个 Tab 持有一个 GeckoEngine 实例。
 * 提供标签创建、切换、关闭等操作。
 */
class EngineManager(
    private val container: ViewGroup,
    private val lifecycleOwner: LifecycleOwner
) {

    data class Tab(
        val id: String = UUID.randomUUID().toString(),
        val engine: GeckoEngine,
        val geckoView: GeckoView,
        var title: String = "新标签",
        var url: String = "about:blank",
        var isLoading: Boolean = false
    )

    private val tabs = mutableListOf<Tab>()
    private var activeIndex: Int = -1

    /** 当前活动标签 */
    val activeTab: Tab? get() = tabs.getOrNull(activeIndex)

    /** 标签数量 */
    val tabCount: Int get() = tabs.size

    /** 所有标签（只读） */
    val allTabs: List<Tab> get() = tabs.toList()

    /** 当前标签索引 */
    val currentIndex: Int get() = activeIndex

    // ===== 标签操作 =====

    /**
     * 创建新标签
     */
    fun createTab(url: String = "about:blank"): Tab? {
        if (!GeckoSetup.checkAvailability()) {
            android.util.Log.w("EngineMgr", "GeckoView not available, download runtime first")
            return null
        }
        try {
            val geckoView = GeckoView(container.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val engine = GeckoEngine(geckoView, lifecycleOwner)
            val tab = Tab(
                engine = engine,
                geckoView = geckoView
            )

            tabs.add(tab)
            switchToTab(tabs.size - 1)

            if (url != "about:blank") {
                engine.loadUrl(url)
            }

            return tab
        } catch (e: Exception) {
            android.util.Log.e("EngineMgr", "Failed to create tab", e)
            return null
        }
    }

    /**
     * 切换到指定标签
     */
    fun switchToTab(index: Int) {
        if (index !in tabs.indices) return
        if (index == activeIndex) return

        // 隐藏当前标签
        activeTab?.let { currentTab ->
            container.removeView(currentTab.geckoView)
        }

        // 显示目标标签
        activeIndex = index
        val tab = tabs[index]
        container.addView(tab.geckoView, 0)
    }

    /**
     * 关闭标签
     */
    fun closeTab(index: Int): Boolean {
        if (index !in tabs.indices || tabs.size <= 1) return false

        val tab = tabs.removeAt(index)

        if (index == activeIndex) {
            // 关闭的是当前标签，切换到相邻标签
            val newIndex = if (index >= tabs.size) tabs.size - 1 else index
            switchToTab(newIndex)
        } else if (index < activeIndex) {
            // 关闭的是前面的标签，activeIndex 前移
            activeIndex--
        }

        tab.engine.onDestroy()
        container.removeView(tab.geckoView)

        return true
    }

    /**
     * 关闭当前标签
     */
    fun closeActiveTab(): Boolean = closeTab(activeIndex)

    /**
     * 创建空白新标签
     */
    fun createBlankTab(): Tab? = createTab()

    // ===== 清理 =====

    fun destroyAll() {
        tabs.forEach { it.engine.onDestroy() }
        tabs.clear()
        activeIndex = -1
    }

    fun onResume() {
        activeTab?.engine?.onResume()
    }

    fun onPause() {
        activeTab?.engine?.onPause()
    }
}
