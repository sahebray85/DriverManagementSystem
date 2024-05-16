package io.moia.challenge.driver

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.then
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import java.util.UUID
import java.util.function.Consumer

class DriverRepositoryTest {

    private val dynamoDbClient: DynamoDbClient = mock()
    private val driverRepository = DriverRepository(dynamoDbClient, "not-important-here")

    @Test
    fun `should get driver`() {
        val driver = givenPersistentDriver()

        val actualDriver = driverRepository.getDriver(driver.id)

        assertEquals(driver.id, actualDriver.id)
        assertEquals(driver.firstname, actualDriver.firstname)
        assertEquals(driver.lastname, actualDriver.lastname)
    }

    @Test
    fun `should create driver`() {
        val driver = givenPersistentDriver()

        val createdDriver = driverRepository.createOrUpdate(driver)

        assertEquals(driver.id, createdDriver.id)
        then(dynamoDbClient).should().putItem(any<Consumer<PutItemRequest.Builder>>())
    }

    private fun givenDriver(): Driver {
        return Driver(
            firstname = "John",
            lastname = "Doe",
            driverLicenseId = UUID.randomUUID().toString()
        )
    }

    private fun givenPersistentDriver(driver: Driver = givenDriver()): Driver {
        whenever(dynamoDbClient.getItem(any<Consumer<GetItemRequest.Builder>>()))
            .thenReturn(
                GetItemResponse.builder()
                    .item(
                        mapOf(
                            "id" to AttributeValue.builder().s(driver.id.toString()).build(),
                            "firstname" to AttributeValue.builder().s(driver.firstname).build(),
                            "lastname" to AttributeValue.builder().s(driver.lastname).build(),
                            "driverLicenseId" to AttributeValue.builder().s(driver.driverLicenseId).build()
                        )
                    ).build()
            )
        return driver
    }
}