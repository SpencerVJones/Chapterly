package com.example.chapterly.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope

private const val SWIPE_REPEAT_COUNT = 4
private const val SWIPE_STEPS = 16

internal fun MacrobenchmarkScope.scrollBookGrid() {
    val centerX = device.displayWidth / 2
    val startY = (device.displayHeight * 3) / 4
    val endY = device.displayHeight / 4

    repeat(SWIPE_REPEAT_COUNT) {
        device.swipe(centerX, startY, centerX, endY, SWIPE_STEPS)
        device.waitForIdle()
    }
}
