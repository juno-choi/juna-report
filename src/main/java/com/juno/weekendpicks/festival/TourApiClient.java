package com.juno.weekendpicks.festival;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.juno.weekendpicks.config.PicksProperties;
import com.juno.weekendpicks.support.GeoPoint;
import com.juno.weekendpicks.support.JsonHttp;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/** Korea Tourism Organization TourAPI 4.0 (KorService2) festival search client. */
@Component
public class TourApiClient {

	private static final String URL = "https://apis.data.go.kr/B551011/KorService2/searchFestival2";
	private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private final JsonHttp jsonHttp;
	private final PicksProperties properties;

	public TourApiClient(JsonHttp jsonHttp, PicksProperties properties) {
		this.jsonHttp = jsonHttp;
		this.properties = properties;
	}

	/** Returns nationwide festivals running between the given dates; callers filter by distance. */
	public List<Festival> fetchFestivals(LocalDate from, LocalDate to) {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("serviceKey", properties.dataGoKrServiceKey());
		params.put("MobileOS", "ETC");
		params.put("MobileApp", "juna-report");
		params.put("_type", "json");
		params.put("numOfRows", "1000");
		params.put("pageNo", "1");
		params.put("eventStartDate", from.format(DATE));
		params.put("eventEndDate", to.format(DATE));

		JsonNode root = jsonHttp.get(JsonHttp.uri(URL, params), Map.of());
		List<Festival> festivals = new ArrayList<>();
		// "items" is an empty string instead of an object when there is no result.
		for (JsonNode item : root.path("response").path("body").path("items").path("item")) {
			Festival festival = toFestival(item);
			// Do not rely on the API's date-filter semantics; check the overlap ourselves.
			if (festival != null && festival.overlaps(from, to)) {
				festivals.add(festival);
			}
		}
		return festivals;
	}

	private static Festival toFestival(JsonNode item) {
		try {
			return new Festival(
					item.path("title").asString(""),
					item.path("addr1").asString(""),
					LocalDate.parse(item.path("eventstartdate").asString(""), DATE),
					LocalDate.parse(item.path("eventenddate").asString(""), DATE),
					new GeoPoint(
							Double.parseDouble(item.path("mapy").asString("")),
							Double.parseDouble(item.path("mapx").asString("")))
			);
		}
		catch (DateTimeParseException | NumberFormatException e) {
			// Skip entries with missing dates or coordinates.
			return null;
		}
	}
}
