package com.marvelous.betterrank.viewModel

import androidx.lifecycle.ViewModel
import com.marvelous.betterrank.databinding.ActivityInstructionTranslationBinding
import com.marvelous.betterrank.model.HomeGuideModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SendInstructionsViewModel @Inject constructor() : ViewModel() {

    fun buildMessage(
        guide: HomeGuideModel,
        b: ActivityInstructionTranslationBinding
    ): String {

        val sections = mutableListOf<String>()

        if (b.cb1.isChecked) sections += formatSection1(guide)
        if (b.cb2.isChecked) sections += formatSection2(guide)
        if (b.cb3.isChecked) sections += formatSection3(guide)
        if (b.cb4.isChecked) sections += formatSection4(guide)
        if (b.cb5.isChecked) sections += formatSection5(guide)
        if (b.cb6.isChecked) sections += formatSection6(guide)
        if (b.cb7.isChecked) sections += formatSection7(guide)
        if (b.cb8.isChecked) sections += formatSection8(guide)
        if (b.cb9.isChecked) sections += formatSection9(guide)

        return sections.joinToString("\n\n")
    }

    // ---------------------------------------------------------
    // SECTION 1 — Host & Welcome
    // ---------------------------------------------------------
    private fun formatSection1(guide: HomeGuideModel) = """
City: ${guide.city ?: "-"}
Welcome Note: ${guide.welcomeNote ?: "-"}
Host Intro: ${guide.hostIntro ?: "-"}
""".trimIndent()


    // ---------------------------------------------------------
    // SECTION 2 — Contact Info
    // ---------------------------------------------------------
    private fun formatSection2(guide: HomeGuideModel) = """
Contact Channels: ${guide.contactChannels.joinToString(", ").ifBlank { "-" }}
Contact Number: ${guide.contactNumber ?: "-"}
More Contact Info: ${guide.moreContactInfo ?: "-"}
Emergency Contact: ${guide.emergencyContact ?: "-"}
""".trimIndent()


    // ---------------------------------------------------------
    // SECTION 3 — Property Basics
    // ---------------------------------------------------------
    private fun formatSection3(guide: HomeGuideModel) = """
Property Type: ${guide.propertyType ?: "-"}
Occupancy Limit: ${guide.occupancyLimit ?: "-"}
Description: ${guide.briefDescription ?: "-"}
""".trimIndent()


    // ---------------------------------------------------------
    // SECTION 4 — Address & Access
    // ---------------------------------------------------------
    private fun formatSection4(guide: HomeGuideModel) = """
Address: ${guide.propertyAddress ?: "-"}
Local Address: ${guide.propertyAddressLocal ?: "-"}
Google Maps: ${guide.googleMapsLink ?: "-"}
How to Reach (Airport): ${guide.howToReachAirport ?: "-"}
How to Reach (Train): ${guide.howToReachTrain ?: "-"}
How to Reach (Car): ${guide.howToReachCar ?: "-"}
How to Find: ${guide.howToFind ?: "-"}
""".trimIndent()


    // ---------------------------------------------------------
    // SECTION 5 — Check-in & Access
    // ---------------------------------------------------------
    private fun formatSection5(guide: HomeGuideModel) = """
How to Get In: ${guide.howToGetIn ?: "-"}
Entrance Code: ${guide.entranceCode ?: "-"}
Check-in Window: ${guide.checkInWindow ?: "-"}
Additional Access: ${guide.additionalAccess ?: "-"}
Floor: ${guide.floor ?: "-"}
Stairs / Elevator: ${guide.stairsElevator ?: "-"}
Parking: ${guide.parking ?: "-"}
Parking Comments: ${guide.parkingComments ?: "-"}
""".trimIndent()


    // ---------------------------------------------------------
    // SECTION 6 — House Rules
    // ---------------------------------------------------------
    private fun formatSection6(guide: HomeGuideModel) = """
WiFi Password: ${guide.wifiPassword ?: "-"}
House Rules: ${guide.propertyRules ?: "-"}
Smoking Rules: ${guide.smokingRules ?: "-"}
Pets Policy: ${guide.petsPolicy ?: "-"}
Quiet Hours: ${guide.quietHours ?: "-"}
Lockout Policy: ${guide.lockoutPolicy ?: "-"}
""".trimIndent()


    // ---------------------------------------------------------
    // SECTION 7 — Check-out
    // ---------------------------------------------------------
    private fun formatSection7(guide: HomeGuideModel) = """
Check-out Window: ${guide.checkOutWindow ?: "-"}
Departure Checklist: ${guide.departureChecklist ?: "-"}
Where to Leave Keys: ${guide.whereToLeaveKeys ?: "-"}
""".trimIndent()


    // ---------------------------------------------------------
    // SECTION 8 — Amenities & Facilities
    // ---------------------------------------------------------
    private fun formatSection8(guide: HomeGuideModel) = """
Facilities: ${guide.facilities ?: "-"}
Amenities: ${guide.amenities ?: "-"}
First Aid: ${guide.firstAid ?: "-"}
Waste Sorting: ${guide.wasteSorting ?: "-"}
Extra Linens: ${guide.extraLinens ?: "-"}
Towels: ${guide.towels ?: "-"}
Cleaning Tools: ${guide.cleaningTools ?: "-"}
Hair Dryer: ${guide.hairDryer ?: "-"}
AC / Fans: ${guide.acFans ?: "-"}
Washer / Dryer: ${guide.washerDryer ?: "-"}
Kettle & Coffee: ${guide.kettleCoffee ?: "-"}
BBQ / Fireplace: ${guide.bbqFireplace ?: "-"}
Hot Water: ${guide.hotWater ?: "-"}
Fire Escape: ${guide.fireEscape ?: "-"}
Nearest Hospital: ${guide.nearestHospital ?: "-"}
""".trimIndent()


    // ---------------------------------------------------------
    // SECTION 9 — Neighborhood Info
    // ---------------------------------------------------------
    private fun formatSection9(guide: HomeGuideModel) = """
Pharmacy: ${guide.pharmacy ?: "-"}
Groceries: ${guide.groceries ?: "-"}
ATMs: ${guide.atms ?: "-"}
SIM Cards: ${guide.simCards ?: "-"}
Parks: ${guide.parks ?: "-"}
Metro: ${guide.metro ?: "-"}
Food Delivery: ${guide.foodDelivery ?: "-"}
Breakfast: ${guide.breakfast ?: "-"}
Lunch / Dinner: ${guide.lunchDinner ?: "-"}
Late Night Food: ${guide.lateNight ?: "-"}
Top Attractions: ${guide.topAttractions ?: "-"}
""".trimIndent()
}
