package com.juno.weekendpicks.performance;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.juno.weekendpicks.config.PicksProperties;
import com.juno.weekendpicks.support.JsonHttp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

/** KOPIS (공연예술통합전산망) performance list client. */
@Component
public class KopisClient {

	public static final String GENRE_MUSICAL = "GGGA";
	public static final String GENRE_POPULAR_MUSIC = "CCCD";
	public static final String AREA_SEOUL = "11";
	public static final String AREA_GYEONGGI = "41";

	private static final Logger log = LoggerFactory.getLogger(KopisClient.class);

	private static final String URL = "http://www.kopis.or.kr/openApi/restful/pblprfr";
	private static final String STATE_UPCOMING = "01";
	// The API returns at most 100 rows per page.
	private static final int ROWS = 100;
	private static final int MAX_PAGES = 3;
	// The KOPIS firewall answers "400 Request Blocked" when calls come too often,
	// so space requests out and back off before retrying a blocked one.
	private static final long REQUEST_INTERVAL_MILLIS = 2_000;
	private static final long BLOCKED_BACKOFF_MILLIS = 20_000;
	private static final int MAX_ATTEMPTS = 3;

	private final JsonHttp jsonHttp;
	private final PicksProperties properties;

	public KopisClient(JsonHttp jsonHttp, PicksProperties properties) {
		this.jsonHttp = jsonHttp;
		this.properties = properties;
	}

	/** Returns not-yet-started performances whose run overlaps the given period. */
	public List<Performance> fetchUpcoming(String genreCode, String areaCode, LocalDate from, LocalDate to) {
		List<Performance> performances = new ArrayList<>();
		for (int page = 1; page <= MAX_PAGES; page++) {
			Map<String, String> params = new LinkedHashMap<>();
			params.put("service", properties.kopisServiceKey().trim());
			params.put("stdate", from.format(DateTimeFormatter.BASIC_ISO_DATE));
			params.put("eddate", to.format(DateTimeFormatter.BASIC_ISO_DATE));
			params.put("cpage", String.valueOf(page));
			params.put("rows", String.valueOf(ROWS));
			params.put("prfstate", STATE_UPCOMING);
			params.put("shcate", genreCode);
			params.put("signgucode", areaCode);

			List<Performance> pageItems = KopisPerformanceParser.parse(getWithRetry(JsonHttp.uri(URL, params)));
			performances.addAll(pageItems);
			if (pageItems.size() < ROWS) {
				break;
			}
		}
		return performances;
	}

	private String getWithRetry(URI uri) {
		for (int attempt = 1; ; attempt++) {
			sleep(REQUEST_INTERVAL_MILLIS);
			try {
				return jsonHttp.getText(uri);
			}
			catch (HttpClientErrorException.BadRequest e) {
				if (attempt == MAX_ATTEMPTS) {
					throw e;
				}
				log.info("KOPIS blocked the request (attempt {}/{}); retrying in {}s",
						attempt, MAX_ATTEMPTS, BLOCKED_BACKOFF_MILLIS / 1000);
				sleep(BLOCKED_BACKOFF_MILLIS);
			}
		}
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting between KOPIS requests", e);
		}
	}
}
