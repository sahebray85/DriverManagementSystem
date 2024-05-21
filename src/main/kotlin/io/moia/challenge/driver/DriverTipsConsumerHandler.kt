package io.moia.challenge.driver

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.ObjectMapper

class DriverTipsConsumerHandler(
    private val driverRepository: DriverTipsRepository = ApplicationContext.driverTippingRepository,
    private val objectMapper: ObjectMapper = ApplicationContext.objectMapper
) : RequestHandler<SQSEvent, Unit> {

    override fun handleRequest(sqsEvent: SQSEvent, context: Context) {
        println("Example lambda with SQS Event")
        sqsEvent.records.forEach {
            println(it.body)
        }
    }
}