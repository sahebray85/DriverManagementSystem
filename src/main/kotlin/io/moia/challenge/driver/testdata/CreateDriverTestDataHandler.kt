package io.moia.challenge.driver.testdata

import io.moia.challenge.driver.ApplicationContext
import io.moia.challenge.driver.Driver
import io.moia.challenge.driver.DriverRepository
import java.util.UUID

class CreateDriverTestDataHandler(
    private val driverRepository: DriverRepository = ApplicationContext.driverRepository
) {

    fun handle() {
        if (driverRepository.getDrivers().isEmpty()) {
            listOf(
                Driver(
                    firstname = "John",
                    lastname = "Doe",
                    driverLicenseId = UUID.randomUUID().toString()
                ),
                Driver(
                    firstname = "Dean",
                    lastname = "Driver",
                    driverLicenseId = UUID.randomUUID().toString()
                )
            ).forEach {
                println("Creating driver $it")
                driverRepository.createOrUpdate(it)
            }
        } else {
            println("nothing to do - drivers exist")
        }
    }
}