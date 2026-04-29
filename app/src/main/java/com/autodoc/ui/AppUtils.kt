package com.autodoc.ui

import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

fun formatDate(millis: Long): String {
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString()
}

fun documentStatusText(daysLeft: Int): String {
    return when {
        daysLeft < 0 -> {
            val days = abs(daysLeft)
            "expirat de $days ${if (days == 1) "zi" else "zile"}"
        }

        daysLeft == 0 -> "expira azi"
        daysLeft == 1 -> "expira maine"
        else -> "expira in $daysLeft zile"
    }
}