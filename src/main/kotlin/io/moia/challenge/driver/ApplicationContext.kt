package io.moia.challenge.driver

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.moia.challenge.driver.repository.DriverRepository
import io.moia.challenge.driver.repository.DriverTipsRepository
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.sqs.SqsClient
import java.text.SimpleDateFormat

object ApplicationContext {
    // private val region = Region.EU_CENTRAL_1
    private val region = Region.US_EAST_1
    val dynamoDbClient = DynamoDbClient
            .builder()
            .region(region)
            .build()

    val objectMapper = jacksonObjectMapper()
        .setDateFormat(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"))
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .registerModule(JavaTimeModule())

    val driverRepository = DriverRepository(dynamoDbClient, System.getenv("DRIVER_TABLE_NAME") ?: "driver-test")
    val driverTippingRepository = DriverTipsRepository(dynamoDbClient, System.getenv("DRIVER_TIPS_TABLE_NAME") ?: "driver-tipping-test")

    val sqsClient = SqsClient.builder()
        .region(region)
        .build()
}