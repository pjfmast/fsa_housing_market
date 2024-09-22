import avans.avd.*
import avans.avd.energyFactor
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll

class HousingMarketTest {

    @Test
    fun `energyFactor of terraced house should be 11 (Double)`() {
        // arrange
        val house = House("", 0, null, HousingType.TERRACED, 0)
        // act
        val eFactor = house.energyFactor()
        // assert
        assertEquals(11.0, eFactor)
    }

    @Test
    fun `getMonthlyPayments of terraced house from 100m2 and area 200m2 should be 17200 divided by 12`() {
        // arrange
        val house = House("", 100, 300000, HousingType.TERRACED, 200)
        // act
        val montlyPayments = house.getMonthlyPayments()
        // assert: (0,04 * 300.000 + 0,01 * 300.000 + 11 * 100 + 5 * 200) / 12 = 17.100 / 12
        assertEquals(17100 / 12, montlyPayments)
    }

    @Test
    fun `doOffer with lower bid should not be accepted`() {
        // arrange
        val house = House("", 0, 300000, HousingType.TERRACED, 0)
        val henk = Customer("Henk", "")
        val anne = Customer("Anne", "")

        // act
        house.doOffer(henk, 500000)
        house.doOffer(anne, 510000)
        house.doOffer(henk, 505000)

        val bids = house.allBids

        // last bid is not accepted so we expect two bids
        assertEquals(2, bids.size)
    }

    @Test
    fun `doOffer with negative bid should throw IllegalArgumentException`() {
        // arrange:
        val house = House("", 0, 300000, HousingType.TERRACED, 0)
        val henk = Customer("Henk", "")


        //act and assert:
        assertThrows(IllegalArgumentException::class.java) {
            house.doOffer(henk, -1)
        }
    }

    @Test
    fun search() {
        val found = funda.search { p -> p is Garage && p.hasElectricity }
        kotlin.test.assertEquals(found.size, 2, "there should be 2 garages with electricity")
    }

    companion object {
        val funda = HousingMarket()

        @JvmStatic
        @BeforeAll()
        fun beforeAll(): Unit {
            val henk = Customer("Henk", "Henk@breda.nl")
            val anne = Customer("Anne", "Anne@avans.nl")

            val house1 = House("Gastakker 12", 130, 380000, HousingType.TERRACED, 210)
            val house2 = House("Singel 123", 120, 650000, HousingType.TERRACED, 180)
            val house3 = House("Hogeschoollaan 1", 40000, null, HousingType.DETACHED, 25000)
            val house4 = House("Bosrijk 10", 110, 510000, HousingType.BUNGALOW, 380)

            val apartment1 = Apartment("Teteringsdijks 110", 65, 260000, 99, 3)
            val apartment2 = Apartment("Tuinzigtlaan 117", 90, 290000, 130, 5)

            val garage1 = Garage("Hofjes 11", 19, 21000, true)
            val garage2 = Garage("Hofjes 13", 19, 19000)
            val garage3 = Garage("Hofjes 13", 19, priceAsked = null, hasElectricity = true)

            val allProperties =
                listOf(house1, house2, house3, house4, apartment1, apartment2, garage1, garage2, garage3)

            funda.advertise(allProperties)

            house4.doOffer(henk, 500000)
            house4.doOffer(anne, 510000)
            house4.doOffer(henk, 505000)
        }
    }
}