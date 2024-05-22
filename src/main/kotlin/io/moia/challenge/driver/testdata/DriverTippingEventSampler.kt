package io.moia.challenge.driver.testdata

import io.moia.challenge.driver.ApplicationContext
import io.moia.challenge.driver.repository.DriverRepository
import software.amazon.awssdk.services.sqs.SqsClient
import java.text.DecimalFormat
import java.time.Instant
import kotlin.random.Random

class DriverTippingEventSampler(
    private val driverRepository: DriverRepository = ApplicationContext.driverRepository,
    private val sqsClient: SqsClient = ApplicationContext.sqsClient,
    private val vehicleInteractionQueueUrl: String = System.getenv("DRIVER_TIPS_QUEUE_URL")
) {

    fun handle() {
        val drivers = driverRepository.getDrivers()
        if (drivers.isNotEmpty()) {
            sqsClient.sendMessage {
                it.queueUrl(vehicleInteractionQueueUrl)
                it.messageBody(
                    """
                        {
                            "driverId": "${randomValue(drivers).id}",
                            "amount": "${DecimalFormat("#.00").format(Random.nextDouble(10.0))}",
                            "eventTime": "${Instant.now()}"
                        }
                    """.trimIndent()
                )
            }
        }
    }

    private fun <T> randomValue(list: List<T>): T =
        list[Random.nextInt(list.size)]

}