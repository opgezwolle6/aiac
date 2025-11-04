package com.raremartial.aiac.util

import kotlinx.datetime.Instant

@OptIn(kotlin.time.ExperimentalTime::class)
expect fun currentTime(): Instant

