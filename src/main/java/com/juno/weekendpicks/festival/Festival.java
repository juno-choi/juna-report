package com.juno.weekendpicks.festival;

import java.time.LocalDate;

import com.juno.weekendpicks.support.GeoPoint;

public record Festival(
		String title,
		String address,
		LocalDate startDate,
		LocalDate endDate,
		GeoPoint point
) {

	public boolean overlaps(LocalDate from, LocalDate to) {
		return !startDate.isAfter(to) && !endDate.isBefore(from);
	}
}
