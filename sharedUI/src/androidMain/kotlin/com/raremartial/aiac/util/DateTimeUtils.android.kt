package com.raremartial.aiac.util

import kotlinx.datetime.Instant
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
actual fun currentTime(): Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())

