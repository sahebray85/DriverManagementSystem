package io.moia.challenge.driver.repository

import io.moia.challenge.driver.enums.Period
import io.moia.challenge.driver.exceptions.InvalidTippingPeriodException
import io.moia.challenge.driver.models.DriverTips
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.function.Consumer

class DriverTipsRepositoryTest {

    private val dynamoDbClient: DynamoDbClient = mock()
    private val driverTippingRepository = DriverTipsRepository(dynamoDbClient, "not-important-here")

    @Test
    fun `should get driver tipping`() {
        val driverTippingExpected = givenPersistentDriverTipping()

        val actualDriverTips = driverTippingRepository.getDriverTip(driverTippingExpected.id)

        assertEquals(driverTippingExpected.id, actualDriverTips.id)
        assertEquals(driverTippingExpected.amount, actualDriverTips.amount)
        assertEquals(driverTippingExpected.driverId, actualDriverTips.driverId)
        assertEquals(driverTippingExpected.eventTime, actualDriverTips.eventTime)
        assertEquals(driverTippingExpected.createdDate, actualDriverTips.createdDate)
    }

    @Test
    fun `should create driver tipping`() {
        val driverTipping = givenPersistentDriverTipping()

        val createdDriverTipping = driverTippingRepository.createOrUpdate(driverTipping)

        assertEquals(driverTipping.id, createdDriverTipping.id)
        then(dynamoDbClient).should().putItem(any<Consumer<PutItemRequest.Builder>>())
    }

    @Test()
    fun `test getEventStartDate`() {
        assertEquals(LocalDate.now().toString(), driverTippingRepository.getEventStartDate("today"))
    }

    @Test()
    fun `test getEventStartDate with invalid enum`() {
        assertThrows(InvalidTippingPeriodException::class.java) {
            driverTippingRepository.getEventStartDate("invalid")
        }
    }

    private fun givenDriverTips(): DriverTips {
        return DriverTips(
            id = UUID.randomUUID(),
            amount = 3.22,
            driverId = UUID.randomUUID(),
            eventTime = LocalDateTime.now(),
            createdDate = LocalDateTime.now()
        )
    }

    private fun givenPersistentDriverTipping(driverTips: DriverTips = givenDriverTips()): DriverTips {
        whenever(dynamoDbClient.getItem(any<Consumer<GetItemRequest.Builder>>())).thenReturn(
            GetItemResponse.builder().item(
                mapOf(
                    "id" to AttributeValue.builder().s(driverTips.id.toString()).build(),
                    "amount" to AttributeValue.builder().n(driverTips.amount.toString()).build(),
                    "driverId" to AttributeValue.builder().s(driverTips.driverId.toString()).build(),
                    "eventTime" to AttributeValue.builder().s(driverTips.eventTime.toString()).build(),
                    "createdDate" to AttributeValue.builder().s(driverTips.createdDate.toString()).build()
                )
            ).build()
        )
        return driverTips
    }
}
