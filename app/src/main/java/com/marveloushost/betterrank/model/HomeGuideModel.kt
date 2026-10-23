package com.marveloushost.betterrank.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HomeGuideModel(
    // === Section 1: Host & Welcome ===
    var city: String? = null,
    var welcomeNote: String? = null,
    var hostIntro: String? = null,

    // === Section 2: Contact Info ===
    var contactChannels: MutableList<String> = mutableListOf(),  // WhatsApp, Call, SMS, Other
    var contactNumber: String? = null,
    var moreContactInfo: String? = null,
    var emergencyContact: String? = null,

    // === Section 3: Property Basics ===
    var propertyType: String? = null,  // "Entire House" or "Room"
    var occupancyLimit: String? = null,
    var briefDescription: String? = null,

    // === Section 4: Address & How to Reach ===
    var propertyAddress: String? = null,
    var propertyAddressLocal: String? = null,
    var googleMapsLink: String? = null,
    var howToReachAirport: String? = null,
    var howToReachTrain: String? = null,
    var howToReachCar: String? = null,
    var howToFind: String? = null,

    // === Section 5: Check-in & Access ===
    var howToGetIn: String? = null,
    var entranceCode: String? = null,
    var checkInWindow: String? = null,
    var additionalAccess: String? = null,
    var floor: String? = null,
    var stairsElevator: String? = null,
    var parking: String? = null,       // "On Street", "Lot", "None"
    var parkingComments: String? = null,

    // === Section 6: House Rules ===
    var wifiPassword: String? = null,
    var propertyRules: String? = null,
    var smokingRules: String? = null,
    var petsPolicy: String? = null,
    var quietHours: String? = null,
    var lockoutPolicy: String? = null,

    // === Section 7: Check-out ===
    var checkOutWindow: String? = null,
    var departureChecklist: String? = null,
    var whereToLeaveKeys: String? = null,

    // === Section 8: Amenities & Facilities ===
    var facilities: String? = null,
    var amenities: String? = null,
    var firstAid: String? = null,
    var wasteSorting: String? = null,
    var extraLinens: String? = null,
    var towels: String? = null,
    var cleaningTools: String? = null,
    var hairDryer: String? = null,
    var acFans: String? = null,
    var washerDryer: String? = null,
    var kettleCoffee: String? = null,
    var bbqFireplace: String? = null,
    var hotWater: String? = null,
    var fireEscape: String? = null,
    var nearestHospital: String? = null,

    // === Section 9: Neighborhood Info ===
    var pharmacy: String? = null,
    var groceries: String? = null,
    var atms: String? = null,
    var simCards: String? = null,
    var parks: String? = null,
    var metro: String? = null,
    var foodDelivery: String? = null,
    var breakfast: String? = null,
    var lunchDinner: String? = null,
    var lateNight: String? = null,
    var topAttractions: String? = null
): Parcelable