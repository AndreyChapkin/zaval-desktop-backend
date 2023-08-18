package org.home.zaval.zavalbackend.util

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.TimeZone

val LocalDateTime.asUtc: OffsetDateTime
    get() {
        // Calculate UTC Instant based on system offset
        val utcInstant = this.toInstant(OffsetDateTime.now().offset)
        return OffsetDateTime.ofInstant(utcInstant, ZoneId.of("+00:00"))
    }

val LocalDateTime.asUtcPlusThree: OffsetDateTime
    get() {
        // Calculate UTC Instant based on system offset
        val utcOffsetDateTime = this.asUtc
        // Switch to new time offset
        return utcOffsetDateTime.withOffsetSameInstant(ZoneOffset.of("+03:00"))
    }

val Timestamp.asUtc: OffsetDateTime
    get() {
        // Calculate UTC Instant based on system offset
        val utcInstant = this.toInstant()
        return OffsetDateTime.ofInstant(utcInstant, ZoneId.of("+00:00"))
    }

val Timestamp.asUtcPlusSystemOffset: OffsetDateTime
    get() {
        // Calculate UTC Instant based on system offset
        val utcOffsetDateTime = this.asUtc
        // Switch to new time offset
        return utcOffsetDateTime.withOffsetSameInstant(ZonedDateTime.now().offset)
    }

val OffsetDateTime.asUtc: OffsetDateTime
    get() {
        // Calculate UTC Instant based on system offset
        val utcInstant = this.toInstant()
        return OffsetDateTime.ofInstant(utcInstant, ZoneId.of("+00:00"))
    }

val OffsetDateTime.asUtcPlusThree: OffsetDateTime
    get() {
        // Calculate UTC Instant based on system offset
        val utcOffsetDateTime = this.asUtc
        // Switch to new time offset
        return utcOffsetDateTime.withOffsetSameInstant(ZoneOffset.of("+03:00"))
    }

val OffsetDateTime.asUtcPlusSystemOffset: OffsetDateTime
    get() {
        // Calculate UTC Instant based on system offset
        val utcOffsetDateTime = this.asUtc
        // Switch to new time offset
        return utcOffsetDateTime.withOffsetSameInstant(ZonedDateTime.now().offset)
    }

/**
 * Formatting result example 2019-12-20T17:58:06.847+00:00
 * @see <a href="https://stackoverflow.com/questions/59429419/offsetdatetime-print-offset-instead-of-z">Formatter with offset</a>
 */
fun iso8601DateTimeFormatterWithOffset(): DateTimeFormatter = DateTimeFormatterBuilder()
    .append(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")) // use the  formatter for ISO-8601 pattern
    .appendOffset("+HH:MM", "+00:00") // set 'noOffsetText' to desired '+00:00'
    .toFormatter()

fun OffsetDateTime.asStringFormattedWithISO8601withOffset(): String = this.format(iso8601DateTimeFormatterWithOffset())

fun String.asOffsetDateTimeFromISO8601WithOffset(): OffsetDateTime =
    OffsetDateTime.parse(this, DateTimeFormatter.ISO_OFFSET_DATE_TIME)