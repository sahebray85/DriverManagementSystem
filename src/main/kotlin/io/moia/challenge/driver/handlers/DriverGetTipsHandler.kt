package io.moia.challenge.driver.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import io.moia.challenge.driver.ApplicationContext
import io.moia.challenge.driver.repository.DriverRepository
import io.moia.challenge.driver.models.DriverTips
import io.moia.challenge.driver.repository.DriverTipsRepository
import software.amazon.awssdk.http.HttpStatusCode
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.UUID

class DriverGetTipsHandler(
    private val driverRepository: DriverRepository = ApplicationContext.driverRepository,
    private val driverTipsRepository: DriverTipsRepository = ApplicationContext.driverTippingRepository
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

        val totalTippingAmount =
            roundOffDecimal(
                driverTipsRepository.getDriverTipsByDriverId(driverId, period).map(DriverTips::amount).sum()
            )
        val msg = "Total Tipping Amount of Driver with Id $driverId is $totalTippingAmount for $period"

        return APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withHeaders(mapOf("content-type" to "application/json"))
            .withBody(msg)
    }

    private fun roundOffDecimal(number: Double): Double? {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING
        return df.format(number).toDouble()
    }
}