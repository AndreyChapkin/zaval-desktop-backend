package org.home.zaval.zavalbackend.util

import java.util.regex.Pattern

fun countPatternInString(pattern: String, str: String): Int {
    val matcher = Pattern.compile(pattern).matcher(str)
    var counter = 0
    while (matcher.find()) {
        counter++
    }
    return counter
}