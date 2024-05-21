package io.moia.challenge.driver

import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.time.LocalDateTime
import java.util.UUID

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
        }.item().let { toDriver(it) }

    fun getDriverTipsByDriverId(id: UUID): List<DriverTips> = listOf() // TODO

    private fun toDriver(it: MutableMap<String, AttributeValue>): DriverTips {
        return DriverTips(
            id = UUID.fromString(it["id"]!!.s()),
            driverId = UUID.fromString(it["driverId"]!!.s()),
            amount = it["amount"]!!.n().toDouble(),
            eventTime = LocalDateTime.parse(it["eventTime"]!!.s()),
            createdDate = LocalDateTime.parse(it["createdDate"]!!.s())
        )
    }
}
