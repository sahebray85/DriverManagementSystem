package io.moia.challenge.driver

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.UUID

class DriverTipsConsumerHandler(
    private val driverRepository: DriverTipsRepository = ApplicationContext.driverTippingRepository,
    private val objectMapper: ObjectMapper = ApplicationContext.objectMapper
) : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    override fun handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {
        val driver = driverRepository.getDriverTip(UUID.fromString(input.pathParameters["id"]))

        return APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withHeaders(mapOf("content-type" to "application/json"))
            .withBody(objectMapper.writeValueAsString(driver))
    }
}