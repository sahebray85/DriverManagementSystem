package io.moia.challenge.driver

import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.util.UUID

class DriverRepository(
    private val dynamoDbClient: DynamoDbClient,
    private val tableName: String
) {

    fun createOrUpdate(driver: Driver): Driver {
        dynamoDbClient.putItem {
            it.tableName(tableName)
                .item(
                    mapOf(
                        "id" to AttributeValue.builder().s(driver.id.toString()).build(),
                        "firstname" to AttributeValue.builder().s(driver.firstname).build(),
                        "lastname" to AttributeValue.builder().s(driver.lastname).build(),
                        "driverLicenseId" to AttributeValue.builder().s(driver.driverLicenseId).build(),
                        "createdDate" to AttributeValue.builder().s(driver.createdDate.toString()).build()
                    )
                )
        }
        return getDriver(driver.id)
    }

    fun getDriver(id: UUID): Driver =
        dynamoDbClient.getItem {
            it.tableName(tableName)
            it.key(mapOf("id" to AttributeValue.builder().s(id.toString()).build()))
        }.item().let { toDriver(it) }

    fun getDrivers(): List<Driver> =
        dynamoDbClient.scan {
            it.tableName(tableName)
        }.items().map { toDriver(it) }

    private fun toDriver(it: MutableMap<String, AttributeValue>): Driver {
        return Driver(
            id = UUID.fromString(it["id"]!!.s()),
            firstname = it["firstname"]!!.s(),
            lastname = it["lastname"]!!.s(),
            driverLicenseId = it["driverLicenseId"]!!.s()
        )
    }
}