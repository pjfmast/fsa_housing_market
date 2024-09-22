package avans.avd

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.random.Random

data class Customer(val name: String, val email: String)
data class Bid(val priceOffered: Int, val customer: Customer, val timeOfBid: Instant)
data class Picture(val description: String, val imagerUrl: String)

interface Locatable {
    fun getLocation(): LatLong
}

data class LatLong(val latitude: Double, val longitude: Double)

sealed class Property(
    val address: String,
    val livingArea: Int,
    var priceAsked: Int? = null,
) : Locatable {
    // because the Bid instances lifetime is dependent on Property: 'composition relation between Property and Bid'
    // the mutable collection _bids is protected because adding a Bid needs checks.
    // A private MutableList with public immutable List can be defined more elegantly in K2
    //      see: https://github.com/Kotlin/KEEP/blob/explicit-backing-fields-re/proposals/explicit-backing-fields.md
    private val _bids: MutableList<Bid> = mutableListOf()
    val allBids: List<Bid> get() = _bids.toList()

    // because a Picture can be added / removed: 'an aggregation relation between Property and Image'
    val images: MutableList<Picture> = mutableListOf()

    override fun getLocation(): LatLong {
        return LatLong(
            51.58494229691791 + Random.nextDouble(-0.1, 0.1),
            4.797559120743779 + Random.nextDouble(-0.1, 0.1)
        )
    }

    override fun toString(): String {
        return "${this.javaClass.simpleName} at $address" +
                " price: ${priceAsked ?: "price information on request."} living area: $livingArea" +
                "\n\testimated monthly costs (mortgage, energy, maintenance): ${getMonthlyPayments()}"
    }

    abstract fun getMonthlyPayments(): Int?

    fun doOffer(customer: Customer, priceOffered: Int) {
        require(priceOffered > 0) {
            println("argument priceOffered should be positive.")
        }

        if (isAccepted(priceOffered)) {
            _bids.add(Bid(priceOffered, customer, Clock.System.now()))
        }
        Thread.sleep(100)
    }

    private fun isAccepted(priceOffered: Int): Boolean =
        priceOffered > (_bids.maxOfOrNull { it.priceOffered } ?: Int.MIN_VALUE)
}

class Garage(
    address: String,
    livingArea: Int,
    priceAsked: Int? = null,
    val hasElectricity: Boolean = false
) : Property(address, livingArea, priceAsked) {
    override fun toString(): String = super.toString() + if (hasElectricity) "\n\t with electricity!" else ""

    // Calculating with nullable numeric types, can be done with like this:
    // (but in general use the solution as shown in Apartment and House with 'let')
    override fun getMonthlyPayments(): Int? = priceAsked?.times(HousingMarket.interest)?.div(12)?.toInt()
}

class Apartment(
    address: String,
    livingArea: Int,
    priceAsked: Int? = null,
    val paymentHOA: Int,
    val floor: Int
) : Property(address, livingArea, priceAsked) {
    override fun toString(): String = super.toString() + "\n\tlocated at ${floor.toOrdinal()} floor"
    override fun getMonthlyPayments(): Int? = priceAsked?.let {
        val mortgageYear = it * HousingMarket.interest
        val costsHoaYear = paymentHOA * 12
        val energyCostsYear = livingArea * this.energyFactor()
        ((mortgageYear + costsHoaYear + energyCostsYear) / 12).toInt()
    }
}

class House(
    address: String,
    livingArea: Int,
    priceAsked: Int? = null,
    val type: HousingType,
    val plotArea: Int
) : Property(address, livingArea, priceAsked) {
    override fun toString(): String = super.toString() +
            "\n\tthis ${type.toString().lowercase()} house is situated at $plotArea m2 plot area"

    override fun getMonthlyPayments(): Int? {
        return priceAsked?.let {
            val mortgageYear = it * HousingMarket.interest
            val maintenanceCostYear = it * 0.01 + plotArea * 5
            val energyCostsYear = livingArea * this.energyFactor()
            ((mortgageYear + maintenanceCostYear + energyCostsYear) / 12).toInt()
        }
    }
}

internal fun Property.energyFactor(): Double =
    when (this) {
        is Garage    -> 0.0
        is Apartment -> 9.0
        is House     -> when (this.type) {
            HousingType.DETACHED, HousingType.BUNGALOW -> 15.0
            HousingType.SEMI_DETACHED                  -> 13.0
            HousingType.TERRACED                       -> 11.0
        }
    }

private fun Int.toOrdinal(): String =
    when (this) {
        1    -> "first"
        2    -> "second"
        3    -> "third"
        else -> "${this}th"
    }

enum class HousingType {
    DETACHED, SEMI_DETACHED, TERRACED, BUNGALOW
}

class HousingMarket {
    private val allHouses = mutableListOf<Property>()
    fun advertise(property: Property) = allHouses.add(property)
    fun advertise(properties: List<Property>) = allHouses.addAll(properties)

    fun search(query: (Property) -> Boolean): List<Property> = allHouses.filter(query)
    fun search(minPrice: Int = 0, maxPrice: Int = Int.MAX_VALUE): List<Property> =
        allHouses.filter { it.priceAsked in minPrice..maxPrice }

    companion object {
        var interest = 0.04
        fun showAdvertisements(selection: List<Property>, includeBids: Boolean = false, description: String = "") {
            println("All advertisements ($description):")
            println("=".repeat(80))
            selection.forEach { property ->
                println("\t$property")
                if (includeBids) println(property.allBids.joinToString(prefix = "\t\t", separator = "\n\t\t"))
                println("-".repeat(80))
            }
            println("#".repeat(80))
            println()

        }
    }
}