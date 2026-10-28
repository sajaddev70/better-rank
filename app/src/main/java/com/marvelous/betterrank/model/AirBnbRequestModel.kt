package com.marvelous.betterrank.model


import com.google.gson.annotations.SerializedName

data class AirBnbRequestModel(
    @SerializedName("custom_output_fields")
    var customOutputFields: List<String> = listOf("name",
        "image",
        "description",
//        "category",
//        "discount",
        "reviews",
        "ratings",
//        "seller_info",
        "location",
        "lat",
        "long",
        "guests",
//        "pets_allowed",
        "category_rating",
//        "house_rules",
        "details",
        "highlights",
//        "arrangement_details",
        "amenities",
        "images",
//        "url",
//        "final_url",
        "listing_title",
//        "property_id",
        "listing_name",
//        "location_details",
        "description_by_sections",
//        "is_supperhost",
//        "host_number_of_reviews",
//        "host_rating",
//        "hosts_year",
//        "host_response_rate",
//        "is_guest_favorite",
//        "property_number_of_reviews",
//        "host_image",
//        "host_details",
//        "timestamp",
//        "price",
        "description_items",
        "reviews_details"),
    @SerializedName("input")
    var input: List<Input>
){
    data class Input(
        @SerializedName("country")
        var country: String="CA",
        @SerializedName("url")
        var url: String
    )
}

