package com.juno.weekendpicks.recommend;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.juno.weekendpicks.performance.Performance;
import org.junit.jupiter.api.Test;

class RecommendationServiceTest {

	private static final LocalDate FROM = LocalDate.of(2026, 10, 4);
	private static final LocalDate TO = LocalDate.of(2026, 11, 28);

	@Test
	void selectsSoonestNewPerformancesWithinRange() {
		List<Performance> candidates = List.of(
				performance("late", LocalDate.of(2026, 11, 20)),
				performance("soon", LocalDate.of(2026, 10, 10)),
				performance("soon", LocalDate.of(2026, 10, 10)),          // duplicate across periods
				performance("already-recommended", LocalDate.of(2026, 10, 5)),
				performance("already-running", LocalDate.of(2026, 10, 1)),
				performance("too-far", LocalDate.of(2026, 12, 25)),
				performance("middle", LocalDate.of(2026, 11, 1)));

		List<Performance> selected = RecommendationService.selectUpcoming(
				candidates, Set.of("already-recommended"), FROM, TO, 2);

		assertThat(selected).extracting(Performance::id).containsExactly("soon", "middle");
	}

	private static Performance performance(String id, LocalDate startDate) {
		return new Performance(id, "name-" + id, "뮤지컬", "venue", "서울특별시", startDate, startDate.plusDays(7));
	}
}
