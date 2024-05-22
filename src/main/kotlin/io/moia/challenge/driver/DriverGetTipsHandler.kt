package io.moia.challenge.driver

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import software.amazon.awssdk.http.HttpStatusCode
import java.util.UUID

class DriverGetTipsHandler(
    private val driverRepository: DriverRepository = ApplicationContext.driverRepository,
    private val driverTipsRepository: DriverTipsRepository = ApplicationContext.driverTippingRepository,
    private val objectMapper: ObjectMapper = ApplicationContext.objectMapper
) : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    override fun handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {
        val driverId = input.pathParameters["id"]
        val period = input.pathParameters["period"]
        println("DriverGetTipsHandler: driverId $driverId for period $period")
        val driver = driverRepository.getDriver(UUID.fromString(driverId))
            ?: return APIGatewayProxyResponseEvent()
                .withStatusCode(HttpStatusCode.BAD_REQUEST)
                .withHeaders(mapOf("content-type" to "application/json"))
                .withBody("Incorrect Driver Id $driverId")
        println("DriverGetTipsHandler: $driver for period $period")

        return APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withHeaders(mapOf("content-type" to "application/json"))
            .withBody(objectMapper.writeValueAsString(driver))
    }
}