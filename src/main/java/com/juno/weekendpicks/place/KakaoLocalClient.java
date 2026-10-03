package com.juno.weekendpicks.place;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.juno.weekendpicks.config.PicksProperties;
import com.juno.weekendpicks.support.GeoPoint;
import com.juno.weekendpicks.support.JsonHttp;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/** Kakao Local API client. https://developers.kakao.com/docs/latest/ko/local/dev-guide */
@Component
public class KakaoLocalClient {

	public static final String TOURIST_ATTRACTION = "AT4";
	public static final String CULTURAL_FACILITY = "CT1";
	public static final String RESTAURANT = "FD6";
	public static final String CAFE = "CE7";

	private static final String KEYWORD_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";
	private static final String CATEGORY_URL = "https://dapi.kakao.com/v2/local/search/category.json";
	private static final int PAGE_SIZE = 15;

	private final JsonHttp jsonHttp;
	private final PicksProperties properties;

	public KakaoLocalClient(JsonHttp jsonHttp, PicksProperties properties) {
		this.jsonHttp = jsonHttp;
		this.properties = properties;
	}

	/** @param categoryGroupCode optional filter; pass null to search all categories */
	public List<Place> searchKeyword(String query, String categoryGroupCode, GeoPoint center, int radiusMeters, int maxPages) {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("query", query);
		if (categoryGroupCode != null) {
			params.put("category_group_code", categoryGroupCode);
		}
		return search(KEYWORD_URL, params, center, radiusMeters, maxPages);
	}

	public List<Place> searchCategory(String categoryGroupCode, GeoPoint center, int radiusMeters, int maxPages) {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("category_group_code", categoryGroupCode);
		return search(CATEGORY_URL, params, center, radiusMeters, maxPages);
	}

	private List<Place> search(String url, Map<String, String> baseParams, GeoPoint center, int radiusMeters, int maxPages) {
		List<Place> places = new ArrayList<>();
		for (int page = 1; page <= maxPages; page++) {
			Map<String, String> params = new LinkedHashMap<>(baseParams);
			params.put("x", String.valueOf(center.longitude()));
			params.put("y", String.valueOf(center.latitude()));
			params.put("radius", String.valueOf(radiusMeters));
			params.put("size", String.valueOf(PAGE_SIZE));
			params.put("page", String.valueOf(page));

			JsonNode root = jsonHttp.get(JsonHttp.uri(url, params),
					Map.of("Authorization", "KakaoAK " + properties.kakaoRestApiKey()));
			for (JsonNode document : root.path("documents")) {
				places.add(toPlace(document));
			}
			if (root.path("meta").path("is_end").asBoolean(true)) {
				break;
			}
		}
		return places;
	}

	private static Place toPlace(JsonNode document) {
		String roadAddress = document.path("road_address_name").asString("");
		return new Place(
				document.path("id").asString(""),
				document.path("place_name").asString(""),
				lastSegment(document.path("category_name").asString("")),
				roadAddress.isBlank() ? document.path("address_name").asString("") : roadAddress,
				document.path("place_url").asString(""),
				new GeoPoint(
						Double.parseDouble(document.path("y").asString("0")),
						Double.parseDouble(document.path("x").asString("0")))
		);
	}

	/** "여행 > 관광,명소 > 산" -> "산" */
	private static String lastSegment(String categoryName) {
		int index = categoryName.lastIndexOf('>');
		return index < 0 ? categoryName.trim() : categoryName.substring(index + 1).trim();
	}
}
