package com.juno.weekendpicks.recommend;

import java.time.LocalDate;
import java.util.List;

import com.juno.weekendpicks.festival.Festival;
import com.juno.weekendpicks.weather.DailyWeather;

public record WeekendPicks(
		List<LocalDate> days,
		List<DailyWeather> weather,
		OutingMode mode,
		SeasonalTheme theme,
		List<Course> courses,
		List<Festival> festivals
) {

	public enum OutingMode {
		OUTDOOR, INDOOR
	}

	public List<String> placeIds() {
		return courses.stream().flatMap(course -> course.placeIds().stream()).toList();
	}
}
