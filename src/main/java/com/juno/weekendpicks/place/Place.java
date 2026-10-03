package com.juno.weekendpicks.place;

import com.juno.weekendpicks.support.GeoPoint;

public record Place(
		String id,
		String name,
		String category,
		String address,
		String url,
		GeoPoint point
) {
}
