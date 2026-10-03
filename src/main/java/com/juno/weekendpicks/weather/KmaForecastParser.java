package com.juno.weekendpicks.weather;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.juno.weekendpicks.weather.DailyWeather.Precipitation;
import com.juno.weekendpicks.weather.DailyWeather.Sky;
import tools.jackson.databind.JsonNode;

/** Aggregates the hourly items of the KMA short-term forecast (getVilageFcst) into daily summaries. */
public final class KmaForecastParser {

	private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
	private static final int DAYTIME_START_HOUR = 9;
	private static final int DAYTIME_END_HOUR = 21;

	private KmaForecastParser() {
	}

	public static List<DailyWeather> parse(JsonNode root, List<LocalDate> days) {
		JsonNode header = root.path("response").path("header");
		if (!"00".equals(header.path("resultCode").asString(""))) {
			throw new IllegalStateException("KMA forecast error: " + header.path("resultMsg").asString(root.toString()));
		}

		Map<LocalDate, DayAccumulator> byDate = new TreeMap<>();
		for (JsonNode item : root.path("response").path("body").path("items").path("item")) {
			LocalDate date = LocalDate.parse(item.path("fcstDate").asString(""), DATE);
			int hour = Integer.parseInt(item.path("fcstTime").asString("0000").substring(0, 2));
			if (!days.contains(date) || hour < DAYTIME_START_HOUR || hour > DAYTIME_END_HOUR) {
				continue;
			}
			byDate.computeIfAbsent(date, d -> new DayAccumulator())
					.accept(item.path("category").asString(""), item.path("fcstValue").asString(""));
		}

		List<DailyWeather> result = new ArrayList<>();
		byDate.forEach((date, acc) -> {
			if (acc.hasTemperature()) {
				result.add(acc.toDailyWeather(date));
			}
		});
		return result;
	}

	private static final class DayAccumulator {

		private final List<Double> temps = new ArrayList<>();
		private final List<Integer> skyCodes = new ArrayList<>();
		private final int[] precipitationCounts = new int[Precipitation.values().length];
		private int maxRainProbability;

		void accept(String category, String value) {
			switch (category) {
				case "TMP" -> temps.add(Double.parseDouble(value));
				case "POP" -> maxRainProbability = Math.max(maxRainProbability, Integer.parseInt(value));
				case "SKY" -> skyCodes.add(Integer.parseInt(value));
				case "PTY" -> {
					// 0: none, 1: rain, 2: rain/snow, 3: snow, 4: shower
					int code = Integer.parseInt(value);
					if (code > 0 && code < precipitationCounts.length) {
						precipitationCounts[code]++;
					}
				}
				default -> {
				}
			}
		}

		boolean hasTemperature() {
			return !temps.isEmpty();
		}

		DailyWeather toDailyWeather(LocalDate date) {
			int min = (int) Math.round(temps.stream().mapToDouble(Double::doubleValue).min().orElseThrow());
			int max = (int) Math.round(temps.stream().mapToDouble(Double::doubleValue).max().orElseThrow());
			return new DailyWeather(date, min, max, maxRainProbability, sky(), precipitation());
		}

		private Sky sky() {
			// 1: clear, 3: mostly cloudy, 4: overcast
			double average = skyCodes.stream().mapToInt(Integer::intValue).average().orElse(1);
			if (average <= 2) {
				return Sky.CLEAR;
			}
			return average <= 3.5 ? Sky.CLOUDY : Sky.OVERCAST;
		}

		private Precipitation precipitation() {
			int dominant = 0;
			for (int code = 1; code < precipitationCounts.length; code++) {
				if (precipitationCounts[code] > precipitationCounts[dominant]) {
					dominant = code;
				}
			}
			return Precipitation.values()[dominant];
		}
	}
}
