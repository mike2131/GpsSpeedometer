package com.example.gpsspeedometer

object Base64Int {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    fun encode(value: Long): String {
        var n = if (value < 0) (abs(value) shl 1) or 1 else value shl 1
        if (n == 0L) return "A"
        var res = ""
        while (n > 0) {
            res = ALPHABET[(n % 64).toInt()] + res
            n /= 64
        }
        return res
    }

    fun decode(s: String): Long {
        var res = 0L
        for (c in s) {
            val index = ALPHABET.indexOf(c)
            if (index == -1) continue
            res = res * 64 + index
        }
        return if (res % 2 == 1L) -(res shr 1) else res shr 1
    }

    private fun abs(n: Long): Long = if (n < 0) -n else n
}
