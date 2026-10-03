package com.juno.weekendpicks.weather;

import java.time.LocalDate;

/** Daytime (09~21h) summary of one day's forecast. */
public record DailyWeather(
		LocalDate date,
		int minTemp,
		int maxTemp,
		int maxRainProbability,
		Sky sky,
		Precipitation precipitation
) {

	private static final int RAIN_PROBABILITY_THRESHOLD = 60;
	private static final int HEAT_WAVE_TEMP = 33;
	private static final int FREEZING_TEMP = 0;

	public enum Sky {
		CLEAR("☀️", "맑음"),
		CLOUDY("⛅", "구름많음"),
		OVERCAST("☁️", "흐림");

		private final String emoji;
		private final String label;

		Sky(String emoji, String label) {
			this.emoji = emoji;
			this.label = label;
		}
	}

	public enum Precipitation {
		NONE("", ""),
		RAIN("🌧️", "비"),
		SLEET("🌨️", "비/눈"),
		SNOW("❄️", "눈"),
		SHOWER("🌦️", "소나기");

		private final String emoji;
		private final String label;

		Precipitation(String emoji, String label) {
			this.emoji = emoji;
			this.label = label;
		}
	}

	public boolean isGoodForOutdoor() {
		return precipitation == Precipitation.NONE
				&& maxRainProbability < RAIN_PROBABILITY_THRESHOLD
				&& maxTemp < HEAT_WAVE_TEMP
				&& maxTemp > FREEZING_TEMP;
	}

	public String emoji() {
		return precipitation == Precipitation.NONE ? sky.emoji : precipitation.emoji;
	}

	public String label() {
		return precipitation == Precipitation.NONE ? sky.label : precipitation.label;
	}
}
