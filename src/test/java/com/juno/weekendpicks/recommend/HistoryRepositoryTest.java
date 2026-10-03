package com.juno.weekendpicks.recommend;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HistoryRepositoryTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 3);

	@TempDir
	Path tempDir;

	@Test
	void returnsEmptyWhenFileDoesNotExist() {
		HistoryRepository repository = new HistoryRepository(tempDir.resolve("data/history.json"), 8);

		assertThat(repository.recentPlaceIds(TODAY)).isEmpty();
	}

	@Test
	void forgetsEntriesOlderThanKeepWeeks() {
		HistoryRepository repository = new HistoryRepository(tempDir.resolve("data/history.json"), 8);
		repository.record(TODAY.minusWeeks(8), List.of("old"));
		repository.record(TODAY.minusWeeks(1), List.of("recent-1", "recent-2"));

		assertThat(repository.recentPlaceIds(TODAY)).containsExactlyInAnyOrder("recent-1", "recent-2");
	}
}
