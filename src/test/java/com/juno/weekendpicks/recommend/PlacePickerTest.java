package com.juno.weekendpicks.recommend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Random;
import java.util.Set;

import com.juno.weekendpicks.place.Place;
import com.juno.weekendpicks.support.GeoPoint;
import org.junit.jupiter.api.Test;

class PlacePickerTest {

	private final PlacePicker placePicker = new PlacePicker(new Random(42));

	@Test
	void skipsExcludedAndDuplicatePlaces() {
		List<Place> candidates = List.of(place("1"), place("2"), place("2"), place("3"));

		List<Place> picked = placePicker.pick(candidates, Set.of("1"), 5);

		assertThat(picked).extracting(Place::id).containsExactlyInAnyOrder("2", "3");
	}

	@Test
	void limitsToRequestedCount() {
		List<Place> candidates = List.of(place("1"), place("2"), place("3"));

		assertThat(placePicker.pick(candidates, Set.of(), 2)).hasSize(2);
	}

	private static Place place(String id) {
		return new Place(id, "place-" + id, "category", "address", "https://place.map.kakao.com/" + id, new GeoPoint(37.4, 127.1));
	}
}
