package com.juno.weekendpicks.recommend;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.juno.weekendpicks.place.Place;

/**
 * One outing place with a nearby restaurant and bakery cafe.
 * restaurant / bakery are null when nothing suitable was found nearby.
 */
public record Course(Place outing, Place restaurant, Place bakery) {

	public List<String> placeIds() {
		return Stream.of(outing, restaurant, bakery)
				.filter(Objects::nonNull)
				.map(Place::id)
				.toList();
	}
}
