package com.juno.weekendpicks.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import com.juno.weekendpicks.weather.DailyWeather.Precipitation;
import com.juno.weekendpicks.weather.DailyWeather.Sky;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class KmaForecastParserTest {

	private static final LocalDate SATURDAY = LocalDate.of(2026, 10, 3);
	private static final LocalDate SUNDAY = LocalDate.of(2026, 10, 4);

	@Test
	void summarizesDaytimeForecastPerDay() {
		JsonNode root = response(
				item("20261003", "0600", "TMP", "5"),   // before daytime, ignored
				item("20261003", "0900", "TMP", "12"),
				item("20261003", "1500", "TMP", "21"),
				item("20261003", "0900", "POP", "10"),
				item("20261003", "1500", "POP", "20"),
				item("20261003", "0900", "SKY", "1"),
				item("20261003", "1500", "SKY", "1"),
				item("20261003", "0900", "PTY", "0"),
				item("20261004", "1200", "TMP", "15"),
				item("20261004", "1200", "POP", "80"),
				item("20261004", "1200", "SKY", "4"),
				item("20261004", "1200", "PTY", "1"),
				item("20261005", "1200", "TMP", "30")); // not requested, ignored

		List<DailyWeather> weather = KmaForecastParser.parse(root, List.of(SATURDAY, SUNDAY));

		assertThat(weather).containsExactly(
				new DailyWeather(SATURDAY, 12, 21, 20, Sky.CLEAR, Precipitation.NONE),
				new DailyWeather(SUNDAY, 15, 15, 80, Sky.OVERCAST, Precipitation.RAIN));
		assertThat(weather.get(0).isGoodForOutdoor()).isTrue();
		assertThat(weather.get(1).isGoodForOutdoor()).isFalse();
	}

	@Test
	void throwsWhenResultCodeIsNotNormal() {
		JsonNode root = JsonMapper.builder().build().readTree(
				"{\"response\":{\"header\":{\"resultCode\":\"03\",\"resultMsg\":\"NO_DATA\"}}}");

		assertThatThrownBy(() -> KmaForecastParser.parse(root, List.of(SATURDAY)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("NO_DATA");
	}

	private static JsonNode response(String... items) {
		return JsonMapper.builder().build().readTree(
				"{\"response\":{\"header\":{\"resultCode\":\"00\",\"resultMsg\":\"NORMAL_SERVICE\"},"
						+ "\"body\":{\"items\":{\"item\":[" + String.join(",", items) + "]}}}}");
	}

	private static String item(String date, String time, String category, String value) {
		return "{\"fcstDate\":\"%s\",\"fcstTime\":\"%s\",\"category\":\"%s\",\"fcstValue\":\"%s\"}"
				.formatted(date, time, category, value);
	}
}
