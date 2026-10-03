package com.juno.weekendpicks.recommend;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** JSON-file-backed history of recommended place ids, used to avoid repeating recent picks. */
public class HistoryRepository {

	public record Entry(String date, List<String> placeIds) {
	}

	private final JsonMapper jsonMapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();
	private final Path file;
	private final int keepWeeks;

	public HistoryRepository(Path file, int keepWeeks) {
		this.file = file;
		this.keepWeeks = keepWeeks;
	}

	public Set<String> recentPlaceIds(LocalDate today) {
		Set<String> ids = new HashSet<>();
		recentEntries(today).forEach(entry -> ids.addAll(entry.placeIds()));
		return ids;
	}

	public void record(LocalDate today, List<String> placeIds) {
		List<Entry> entries = new ArrayList<>(recentEntries(today));
		entries.add(new Entry(today.toString(), placeIds));
		try {
			if (file.getParent() != null) {
				Files.createDirectories(file.getParent());
			}
			Files.write(file, jsonMapper.writeValueAsBytes(entries));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private List<Entry> recentEntries(LocalDate today) {
		LocalDate cutoff = today.minusWeeks(keepWeeks);
		return readAll().stream()
				.filter(entry -> LocalDate.parse(entry.date()).isAfter(cutoff))
				.toList();
	}

	private List<Entry> readAll() {
		if (!Files.exists(file)) {
			return List.of();
		}
		try {
			return jsonMapper.readValue(Files.readAllBytes(file), new TypeReference<List<Entry>>() {
			});
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
