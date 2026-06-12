package com.example.articles.nominatim

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NominatimResponse(
    @SerialName("place_id") val placeId: Long? = null,
    @SerialName("licence") val licence: String? = null,
    @SerialName("osm_type") val osmType: String? = null,
    @SerialName("osm_id") val osmId: Long? = null,
    @SerialName("boundingbox") val boundingbox: List<String>? = null,
    @SerialName("lat") val lat: String? = null,
    @SerialName("lon") val lon: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("importance") val importance: Double? = null,
    @SerialName("icon") val icon: String? = null,
    @SerialName("address") val address: AddressResponse? = null,
    @SerialName("extratags") val extraTags: ExtraTags? = null,
)

@Serializable
data class AddressResponse(
    @SerialName("house_number") val houseNumber: String? = null,
    @SerialName("road") val road: String? = null,
    @SerialName("suburb") val suburb: String? = null,
    @SerialName("city_district") val cityDistrict: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("county") val county: String? = null,
    @SerialName("state_district") val stateDistrict: String? = null,
    @SerialName("state") val state: String? = null,
    @SerialName("ISO3166-2-lvl4") val isoSubdivision: String? = null,
    @SerialName("postcode") val postcode: String? = null,
    @SerialName("country") val country: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
)

@Serializable
data class ExtraTags(
    @SerialName("capital") val capital: String? = null,
    @SerialName("website") val website: String? = null,
    @SerialName("wikidata") val wikidata: String? = null,
    @SerialName("wikipedia") val wikipedia: String? = null,
    @SerialName("population") val population: String? = null,
)