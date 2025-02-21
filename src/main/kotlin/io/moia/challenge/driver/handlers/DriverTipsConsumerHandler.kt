package io.moia.challenge.driver.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.moia.challenge.driver.ApplicationContext
import io.moia.challenge.driver.models.DriverTips
import io.moia.challenge.driver.repository.DriverTipsRepository

class DriverTipsConsumerHandler(
    private val driverTipsRepository: DriverTipsRepository = ApplicationContext.driverTippingRepository,
    private val objectMapper: ObjectMapper = ApplicationContext.objectMapper
) : RequestHandler<SQSEvent, Unit> {

    override fun handleRequest(sqsEvent: SQSEvent, context: Context) {
        println("DriverTipsConsumerHandler: SQS Event => $sqsEvent")
        sqsEvent.records.forEach {
            println(it.body)
            val driverTipRequest = objectMapper.readValue<DriverTips>(it.body)
            driverTipsRepository.createOrUpdate(driverTipRequest)
        }
    }
}
