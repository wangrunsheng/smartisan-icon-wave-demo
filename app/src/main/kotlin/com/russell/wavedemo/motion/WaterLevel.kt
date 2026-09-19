package com.russell.wavedemo.motion

/** Measured waterline positions; vertical progress is independent of horizontal wave motion. */
object WaterLevel {
    const val START_SECONDS = 8.0
    const val END_SECONDS = 12.0

    private val times = doubleArrayOf(
        8.0, 9.72, 9.746, 9.795, 9.846, 9.895, 9.945,
        9.993, 11.74, 11.793, 11.907, 11.98, 12.0,
    )
    private val heights = floatArrayOf(
        76.48f, 76.48f, 76.32f, 74.48f, 72.31f, 70.10f, 67.87f,
        67.41f, 67.40f, 66.22f, 61.16f, 59.56f, 59.56f,
    )

    fun at(seconds: Double): Float {
        if (seconds <= times.first()) return heights.first()
        for (index in 1 until times.size) {
            if (seconds <= times[index]) {
                val fraction = (seconds - times[index - 1]) / (times[index] - times[index - 1])
                return heights[index - 1] +
                    (heights[index] - heights[index - 1]) * fraction.toFloat()
            }
        }
        return heights.last()
    }

    fun fromProgress(progress: Int): Float = 156f - 136f * progress.coerceIn(0, 100) / 100f
}
