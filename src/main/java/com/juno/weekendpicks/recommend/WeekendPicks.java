package com.juno.weekendpicks.recommend;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import com.juno.weekendpicks.festival.Festival;
import com.juno.weekendpicks.performance.Performance;
import com.juno.weekendpicks.weather.DailyWeather;

public record WeekendPicks(
		List<LocalDate> days,
		List<DailyWeather> weather,
		OutingMode mode,
		SeasonalTheme theme,
		List<Course> courses,
		List<Course> shoppingCourses,
		List<Festival> festivals,
		List<Performance> performances
) {

	public enum OutingMode {
		OUTDOOR, INDOOR
	}

	/** Ids to remember so that the same places and performances are not repeated next week. */
	public List<String> recommendedIds() {
		return Stream.concat(
				Stream.concat(courses.stream(), shoppingCourses.stream()).flatMap(course -> course.placeIds().stream()),
				performances.stream().map(Performance::id)).toList();
	}
}
