package io.moia.challenge.driver

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

class DriverCreateHandler(
    private val driverRepository: DriverRepository = ApplicationContext.driverRepository,
    private val objectMapper: ObjectMapper = ApplicationContext.objectMapper
) : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    override fun handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {
        val driverRequest = objectMapper.readValue<Driver>(input.body)
        val driver = driverRepository.createOrUpdate(driverRequest)

        return APIGatewayProxyResponseEvent()
            .withStatusCode(201)
            .withHeaders(mapOf("content-type" to "application/json"))
            .withBody(objectMapper.writeValueAsString(driver))
    }
}