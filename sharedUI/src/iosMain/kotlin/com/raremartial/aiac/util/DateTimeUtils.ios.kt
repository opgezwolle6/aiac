package com.raremartial.aiac.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
actual fun currentTime(): Instant = Clock.System.now()

