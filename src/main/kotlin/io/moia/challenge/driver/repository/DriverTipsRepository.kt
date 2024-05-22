package io.moia.challenge.driver.repository

import io.moia.challenge.driver.models.DriverTips
import io.moia.challenge.driver.enums.Period
import io.moia.challenge.driver.exceptions.InvalidTippingPeriodException
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class DriverTipsRepository(
    private val dynamoDbClient: DynamoDbClient,
    private val tableName: String
) {

    fun createOrUpdate(driverTips: DriverTips): DriverTips {
        dynamoDbClient.putItem {
            it.tableName(tableName)
                .item(
                    mapOf(
                        "id" to AttributeValue.builder().s(driverTips.id.toString()).build(),
                        "driverId" to AttributeValue.builder().s(driverTips.driverId.toString()).build(),
                        "amount" to AttributeValue.builder().n(driverTips.amount.toString()).build(),
                        "eventTime" to AttributeValue.builder().s(driverTips.eventTime.toString()).build(),
                        "createdDate" to AttributeValue.builder().s(driverTips.createdDate.toString()).build()
                    )
                )
        }
        return getDriverTip(driverTips.id)
    }

    fun getDriverTip(id: UUID): DriverTips =
        dynamoDbClient.getItem {
            it.tableName(tableName)
            it.key(mapOf("id" to AttributeValue.builder().s(id.toString()).build()))
        }.item().let { toDriverTips(it) }

    fun getDriverTipsByDriverId(driverId: String?, period: String?): List<DriverTips> {
        val eventTime = getEventStartDate(period)
        val attrValues = mutableMapOf<String, AttributeValue>()
        attrValues[":eventTime"] = AttributeValue.builder().s(eventTime).build()
        attrValues[":driverId"] = AttributeValue.builder().s(driverId).build()
        return dynamoDbClient.query {
            it.tableName(tableName)
            it.keyConditionExpression("driverId = :driverId and eventTime >= :eventTime")
            it.expressionAttributeValues(attrValues)
        }.items().map { toDriverTips(it) }
    }

    private fun getEventStartDate(period: String?): String {
        val periodEnum = period?.let { Period.valueOf(it.uppercase()) }
        if (periodEnum == Period.TODAY) {
            return LocalDate.now().toString()
        } else if (periodEnum == Period.WEEK) {
            return LocalDate.now().minusWeeks(1).toString()
        }
        throw period?.let { InvalidTippingPeriodException(it) }!!
    }

    private fun toDriverTips(it: MutableMap<String, AttributeValue>): DriverTips {
        return DriverTips(
            id = UUID.fromString(it["id"]!!.s()),
            driverId = UUID.fromString(it["driverId"]!!.s()),
            amount = it["amount"]!!.n().toDouble(),
            eventTime = LocalDateTime.parse(it["eventTime"]!!.s()),
            createdDate = LocalDateTime.parse(it["createdDate"]!!.s())
        )
    }
}
