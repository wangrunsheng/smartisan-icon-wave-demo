package com.russell.wavedemo.motion

/** Monotonic frame clock. Reset the frame anchor after pauses to prevent a resume-time jump. */
class PlaybackClock(var seconds: Double = WaterLevel.START_SECONDS) {
    private var previousNanos: Long? = null

    fun resetFrameAnchor() {
        previousNanos = null
    }

    fun advance(nowNanos: Long, speed: Double) {
        val previous = previousNanos
        previousNanos = nowNanos
        if (previous == null || nowNanos <= previous) return
        seconds += (nowNanos - previous) / 1_000_000_000.0 * speed
        // Replay the same measured four-second excerpt, including its water-level reset.
        val duration = WaterLevel.END_SECONDS - WaterLevel.START_SECONDS
        seconds = WaterLevel.START_SECONDS + (seconds - WaterLevel.START_SECONDS) % duration
    }
}
