package com.juno.weekendpicks.weather;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.juno.weekendpicks.config.PicksProperties;
import com.juno.weekendpicks.support.JsonHttp;
import com.juno.weekendpicks.weather.GridConverter.Grid;
import org.springframework.stereotype.Component;

/** KMA short-term forecast (기상청 단기예보) client. */
@Component
public class WeatherClient {

	private static final String URL = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst";
	// Forecasts are issued every 3 hours from 02:00 and become available about 10 minutes later.
	private static final int FIRST_BASE_HOUR = 2;
	private static final int BASE_INTERVAL_HOURS = 3;
	private static final int PUBLISH_DELAY_MINUTES = 10;

	private final JsonHttp jsonHttp;
	private final PicksProperties properties;

	public WeatherClient(JsonHttp jsonHttp, PicksProperties properties) {
		this.jsonHttp = jsonHttp;
		this.properties = properties;
	}

	public List<DailyWeather> fetch(LocalDateTime now, List<LocalDate> days) {
		LocalDateTime base = latestBaseTime(now);
		Grid grid = GridConverter.toGrid(properties.home().point());

		Map<String, String> params = new LinkedHashMap<>();
		params.put("serviceKey", properties.dataGoKrServiceKey());
		params.put("pageNo", "1");
		params.put("numOfRows", "1000");
		params.put("dataType", "JSON");
		params.put("base_date", base.format(DateTimeFormatter.BASIC_ISO_DATE));
		params.put("base_time", base.format(DateTimeFormatter.ofPattern("HHmm")));
		params.put("nx", String.valueOf(grid.nx()));
		params.put("ny", String.valueOf(grid.ny()));

		return KmaForecastParser.parse(jsonHttp.get(JsonHttp.uri(URL, params), Map.of()), days);
	}

	static LocalDateTime latestBaseTime(LocalDateTime now) {
		LocalDateTime available = now.minusMinutes(PUBLISH_DELAY_MINUTES);
		LocalDateTime firstOfDay = available.toLocalDate().atTime(FIRST_BASE_HOUR, 0);
		if (available.isBefore(firstOfDay)) {
			return firstOfDay.minusHours(BASE_INTERVAL_HOURS);
		}
		int slots = (available.getHour() - FIRST_BASE_HOUR) / BASE_INTERVAL_HOURS;
		return firstOfDay.plusHours((long) slots * BASE_INTERVAL_HOURS);
	}
}
