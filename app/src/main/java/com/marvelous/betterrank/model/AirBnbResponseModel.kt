package com.marvelous.betterrank.model


import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class AirBnbResponseModel(
    @SerializedName("amenities") var amenities: List<Amenity>? = emptyList(),
//    @SerializedName("arrangement_details") var arrangementDetails: List<ArrangementDetail>? = emptyList(),
//    @SerializedName("category") var category: String? = null,
    @SerializedName("category_rating") var categoryRating: List<CategoryRating>? = emptyList(),
    @SerializedName("description") var description: String? = null,
    @SerializedName("description_by_sections") var descriptionBySections: List<DescriptionBySection>? = emptyList(),
    @SerializedName("description_items") var descriptionItems: List<String>? = emptyList(),
    @SerializedName("details") var details: List<String>? = emptyList(),
//    @SerializedName("final_url") var finalUrl: String? = null,
    @SerializedName("guests") var guests: Int? = null,
    @SerializedName("highlights") var highlights: List<Highlight>? = emptyList(),
//    @SerializedName("host_details") var hostDetails: HostDetails? = HostDetails(),
//    @SerializedName("host_image") var hostImage: String? = null,
//    @SerializedName("host_number_of_reviews") var hostNumberOfReviews: Int? = null,
//    @SerializedName("host_rating") var hostRating: Double? = null,
//    @SerializedName("host_response_rate") var hostResponseRate: Int? = null,
//    @SerializedName("hosts_year") var hostsYear: Int? = null,
//    @SerializedName("house_rules") var houseRules: List<String>? = emptyList(),
    @SerializedName("image") var image: String? = null,
    @SerializedName("images") var images: List<String>? = emptyList(),
//    @SerializedName("is_guest_favorite") var isGuestFavorite: Boolean? = null,
//    @SerializedName("is_supperhost") var isSupperhost: Boolean? = null,
    @SerializedName("lat") var lat: Double? = null,
    @SerializedName("listing_name") var listingName: String? = null,
    @SerializedName("listing_title") var listingTitle: String? = null,
    @SerializedName("location") var location: String? = null,
//    @SerializedName("location_details") var locationDetails: List<LocationDetail>? = emptyList(),
    @SerializedName("long") var long: Double? = null,
    @SerializedName("name") var name: String? = null,
//    @SerializedName("pets_allowed") var petsAllowed: Boolean? = null,
//    @SerializedName("property_id") var propertyId: String? = null,
//    @SerializedName("property_number_of_reviews") var propertyNumberOfReviews: Int? = null,
    @SerializedName("ratings") var ratings: Double? = null,
    @SerializedName("reviews") var reviews: List<String>? = emptyList(),
//    @SerializedName("seller_info") var sellerInfo: SellerInfo? = SellerInfo(),
//    @SerializedName("timestamp") var timestamp: String? = null,
//    @SerializedName("url") var url: String? = null,
    @SerializedName("reviews_details") var reviewsDetails: List<ReviewDetail>? = null
) : Serializable {

    val detailsText: String
        get() = details?.takeIf { it.isNotEmpty() }?.joinToString(" . ") ?: ""

    data class Amenity(
        @SerializedName("group_name") var groupName: String? = null,
        @SerializedName("items") var items: List<Item>? = emptyList()
    ) : Serializable

    data class ArrangementDetail(
        @SerializedName("name") var name: String? = null,
        @SerializedName("value") var value: String? = null
    ) : Serializable

    data class CategoryRating(
        @SerializedName("name") var name: String? = null,
        @SerializedName("value") var value: String? = null
    ) : Serializable

    data class DescriptionBySection(
        @SerializedName("title") var title: String? = null,
        @SerializedName("value") var value: String? = null
    ) : Serializable

    data class Highlight(
        @SerializedName("name") var name: String? = null,
        @SerializedName("value") var value: String? = null
    ) : Serializable

    data class HostDetails(
        @SerializedName("host_id") var hostId: String? = null,
        @SerializedName("image") var image: String? = null,
        @SerializedName("languages") var languages: Any? = null,
        @SerializedName("my_work") var myWork: String? = null,
        @SerializedName("name") var name: String? = null,
        @SerializedName("rating") var rating: Double? = null,
        @SerializedName("response_rate") var responseRate: String? = null,
        @SerializedName("response_time") var responseTime: String? = null,
        @SerializedName("reviews") var reviews: Int? = null,
        @SerializedName("url") var url: String? = null,
        @SerializedName("years_hosting") var yearsHosting: Int? = null
    ) : Serializable

    data class LocationDetail(
        @SerializedName("title") var title: String? = null,
        @SerializedName("value") var value: String? = null
    ) : Serializable

    data class Item(
        @SerializedName("name") var name: String? = null,
        @SerializedName("value") var value: String? = null
    ) : Serializable

    data class SellerInfo(
        @SerializedName("seller_id") var sellerId: String? = null,
        @SerializedName("url") var url: String? = null
    ) : Serializable

    data class ReviewDetail(
        @SerializedName("review_date") var reviewDate: String? = null,
        @SerializedName("rating") var rating: Double? = null,
        @SerializedName("guest_name") var guestName: String? = null,
        @SerializedName("stayed_for") var stayedFor: String? = null,
        @SerializedName("guest_time_on_airbnb") var guestTimeOnAirbnb: String? = null,
        @SerializedName("review") var reviewText: String? = null,
        @SerializedName("host_response") var hostResponse: String? = null
    ) : Serializable

    fun toJson(prettyPrint: Boolean = false): String {
        val gson: Gson = if (prettyPrint) {
            GsonBuilder().setPrettyPrinting().create()
        } else {
            Gson()
        }
        return gson.toJson(this)
    }
    val city: String
        get() {
            val parts = location?.split(",")?.map { it.trim() } ?: return "Unknown City"
            return when {
                parts.size >= 2 -> parts[0].capitalizeWords()  // City is 2nd part
                parts.size == 1 -> parts[0].capitalizeWords()
                else -> "Unknown City"
            }
        }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }

    val cityTravelTips: String
        get() = "$city \nTravel Tips"
}