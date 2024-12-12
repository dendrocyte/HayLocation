package com.dendrocyte.haylocation.module.pin

import com.google.gson.annotations.SerializedName


/**
 * Created by luyiling on 2024/12/10
 * Modified by
 *
 * TODO:
 * Description:
 *
 * @params
 * @params
 */
data class AddressModel(
    @SerializedName("results")
    val results: List<Result>,
    @SerializedName("status")
    val status: String
)

data class Result(
    @SerializedName("address_components")
    val addressComponents: List<AddressComponent>,
    @SerializedName("formatted_address")
    val formattedAddress: String,
    @SerializedName("geometry")
    val geometry: Geometry,
    @SerializedName("place_id")
    val placeId: String,
    @SerializedName("plus_code")
    val plusCode: PlusCode,
    @SerializedName("types")
    val types: List<String>
)

data class AddressComponent(
    @SerializedName("long_name")
    val longName: String,
    @SerializedName("short_name")
    val shortName: String,
    @SerializedName("types")
    val types: List<String>
)

data class Geometry(
    @SerializedName("location")
    val location: Location,
    @SerializedName("location_type")
    val locationType: String,
    @SerializedName("viewport")
    val viewport: Viewport
)

data class Location(
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("lng")
    val lng: Double
)

data class Viewport(
    @SerializedName("northeast")
    val northeast: Location,
    @SerializedName("southwest")
    val southwest: Location
)

data class PlusCode(
    @SerializedName("compound_code")
    val compoundCode: String,
    @SerializedName("global_code")
    val globalCode: String
)