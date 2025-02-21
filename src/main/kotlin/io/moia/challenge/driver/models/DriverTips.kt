package io.moia.challenge.driver.models

import java.time.LocalDateTime
import java.util.*

data class DriverTips(
    val id: UUID = UUID.randomUUID(),
    val amount: Double,
    val driverId: UUID,
    val eventTime: LocalDateTime,
    val createdDate: LocalDateTime = LocalDateTime.now()
)
