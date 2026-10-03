package com.juno.weekendpicks.recommend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.juno.weekendpicks.place.Place;

/** Picks random places from candidates, skipping excluded ids and duplicates. */
public class PlacePicker {

	private final Random random;

	public PlacePicker(Random random) {
		this.random = random;
	}

	public List<Place> pick(List<Place> candidates, Set<String> excludedIds, int count) {
		Map<String, Place> unique = new LinkedHashMap<>();
		for (Place candidate : candidates) {
			if (!excludedIds.contains(candidate.id())) {
				unique.putIfAbsent(candidate.id(), candidate);
			}
		}
		List<Place> shuffled = new ArrayList<>(unique.values());
		Collections.shuffle(shuffled, random);
		return shuffled.subList(0, Math.min(count, shuffled.size()));
	}
}
