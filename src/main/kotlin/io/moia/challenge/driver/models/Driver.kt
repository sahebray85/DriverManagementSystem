package io.moia.challenge.driver.models

import java.time.LocalDateTime
import java.util.UUID

data class Driver(
    val id: UUID = UUID.randomUUID(),
    val firstname: String?,
    val lastname: String?,
    val driverLicenseId: String?,
    val createdDate: LocalDateTime = LocalDateTime.now()
)