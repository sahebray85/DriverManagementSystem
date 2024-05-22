package io.moia.challenge.driver.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import io.moia.challenge.driver.ApplicationContext
import io.moia.challenge.driver.repository.DriverRepository
import software.amazon.awssdk.http.HttpStatusCode
import java.util.UUID

class DriverGetHandler(
    private val driverRepository: DriverRepository = ApplicationContext.driverRepository,
    private val objectMapper: ObjectMapper = ApplicationContext.objectMapper
) : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    override fun handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {
        val driverId = input.pathParameters["id"]
        val driver = driverRepository.getDriver(UUID.fromString(driverId))
            ?: return APIGatewayProxyResponseEvent()
                .withStatusCode(HttpStatusCode.BAD_REQUEST)
                .withHeaders(mapOf("content-type" to "application/json"))
                .withBody("Incorrect Driver Id $driverId")

        return APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withHeaders(mapOf("content-type" to "application/json"))
            .withBody(objectMapper.writeValueAsString(driver))
    }
}