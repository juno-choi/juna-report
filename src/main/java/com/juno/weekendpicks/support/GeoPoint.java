package com.juno.weekendpicks.support;

public record GeoPoint(double latitude, double longitude) {

	private static final double EARTH_RADIUS_KM = 6371.0;

	/** Great-circle distance (haversine). */
	public double distanceKmTo(GeoPoint other) {
		double dLat = Math.toRadians(other.latitude - latitude);
		double dLon = Math.toRadians(other.longitude - longitude);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(other.latitude))
				* Math.sin(dLon / 2) * Math.sin(dLon / 2);
		return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
	}
}
