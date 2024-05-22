package io.moia.challenge.driver.handlers

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import io.moia.challenge.driver.models.Driver
import io.moia.challenge.driver.repository.DriverRepository
import io.moia.challenge.driver.repository.DriverTipsRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.AdditionalAnswers.returnsFirstArg
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import software.amazon.awssdk.http.HttpStatusCode
import java.util.*

class DriverGetTipsHandlerTest {

    private val driverRepository: DriverRepository = mock()
    private val driverTipsRepository: DriverTipsRepository = mock()
    private val driverGetTipsHandler = DriverGetTipsHandler(driverRepository, driverTipsRepository)
    private val driverId = UUID.randomUUID()

    @Test
    fun `should create driver tipping`() {
        whenever(driverTipsRepository.createOrUpdate(any())).thenAnswer(returnsFirstArg<Driver>())
        givenDriver()

        val pathParams = mapOf(
            "id" to driverId.toString(),
            "period" to "today"
        )

        val response = driverGetTipsHandler
            .handleRequest(APIGatewayProxyRequestEvent().withPathParameters(pathParams), mock())

        assertEquals(201, response.statusCode)
        assertNotNull(response.body)
    }

    private fun givenDriver(): Driver =
        Driver(
            id = driverId,
            firstname = "John",
            lastname = "Doe",
            driverLicenseId = UUID.randomUUID().toString()
        ).also { whenever(driverRepository.getDriver(it.id)).thenReturn(it) }
}
