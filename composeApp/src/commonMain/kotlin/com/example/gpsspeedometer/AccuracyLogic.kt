package com.example.gpsspeedometer

import kotlin.math.*

object AccuracyLogic {
    private const val COEFFICIENT = 6.35

    fun compress(accuracy: Float): Int {
        val value = accuracy.toDouble()
        val logValue = ln(value * 10.0 + 1.0) * COEFFICIENT
        return logValue.roundToInt().coerceIn(0, 63)
    }

    fun decompress(compressed: Int): Float {
        val logValue = compressed.toDouble() / COEFFICIENT
        val accuracy = (exp(logValue) - 1.0) / 10.0
        return accuracy.toFloat()
    }
}
