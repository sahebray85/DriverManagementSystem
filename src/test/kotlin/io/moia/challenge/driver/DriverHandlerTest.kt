package io.moia.challenge.driver

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.AdditionalAnswers.returnsFirstArg
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.UUID

class DriverHandlerTest {

    private val driverRepository: DriverRepository = mock()
    private val driverGetHandler = DriverGetHandler(driverRepository)
    private val driverGetHDriverCreateHandlerandler = DriverCreateHandler(driverRepository)

    @Test
    fun `should get driver`() {
        val driver = givenDriver()

        val response = driverGetHandler.handleRequest(APIGatewayProxyRequestEvent().withPathParamters(mapOf("id" to driver.id.toString())), mock())

        assertEquals(response.statusCode, 200)
        assertEquals(JsonPath.read<String>(response.body, "id"), driver.id.toString())
    }

    @Test
    fun `should create driver`() {
        whenever(driverRepository.createOrUpdate(any())).thenAnswer(returnsFirstArg<Driver>())
        val body = """{
            "firstname": "John",
            "lastname": "Doe",
            "driverLicenseId": "${UUID.randomUUID()}"
            }"""

        val response = driverGetHDriverCreateHandlerandler
            .handleRequest(APIGatewayProxyRequestEvent().withBody(body), mock())

        assertEquals(response.statusCode, 201)
        assertNotNull(JsonPath.read<String>(response.body, "id"))
        assertEquals(JsonPath.read<String>(response.body, "firstname"), "John")
    }

    private fun givenDriver(): Driver =
        Driver(
            firstname = "John",
            lastname = "Doe",
            driverLicenseId = UUID.randomUUID().toString()
        ).also { whenever(driverRepository.getDriver(it.id)).thenReturn(it) }
}