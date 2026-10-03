package com.juno.weekendpicks.weather;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class WeatherClientTest {

	@Test
	void usesMostRecentlyPublishedBaseTime() {
		assertThat(WeatherClient.latestBaseTime(LocalDateTime.of(2026, 10, 3, 7, 50)))
				.isEqualTo(LocalDateTime.of(2026, 10, 3, 5, 0));
	}

	@Test
	void waitsForPublishDelayBeforeUsingNewBaseTime() {
		assertThat(WeatherClient.latestBaseTime(LocalDateTime.of(2026, 10, 3, 5, 5)))
				.isEqualTo(LocalDateTime.of(2026, 10, 3, 2, 0));
	}

	@Test
	void fallsBackToPreviousDayBeforeFirstBaseTime() {
		assertThat(WeatherClient.latestBaseTime(LocalDateTime.of(2026, 10, 3, 1, 0)))
				.isEqualTo(LocalDateTime.of(2026, 10, 2, 23, 0));
	}
}
