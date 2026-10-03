package com.juno.weekendpicks.weather;

import com.juno.weekendpicks.support.GeoPoint;

/**
 * Converts latitude/longitude to the KMA forecast grid (Lambert conformal conic, 5km cells).
 * Constants come from the KMA short-term forecast API guide.
 */
public final class GridConverter {

	private static final double EARTH_RADIUS_KM = 6371.00877;
	private static final double GRID_KM = 5.0;
	private static final double STANDARD_LAT_1 = 30.0;
	private static final double STANDARD_LAT_2 = 60.0;
	private static final double ORIGIN_LON = 126.0;
	private static final double ORIGIN_LAT = 38.0;
	private static final double ORIGIN_X = 43;
	private static final double ORIGIN_Y = 136;

	private GridConverter() {
	}

	public record Grid(int nx, int ny) {
	}

	public static Grid toGrid(GeoPoint point) {
		double re = EARTH_RADIUS_KM / GRID_KM;
		double slat1 = Math.toRadians(STANDARD_LAT_1);
		double slat2 = Math.toRadians(STANDARD_LAT_2);
		double olon = Math.toRadians(ORIGIN_LON);
		double olat = Math.toRadians(ORIGIN_LAT);

		double sn = Math.log(Math.cos(slat1) / Math.cos(slat2))
				/ Math.log(Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5));
		double sf = Math.pow(Math.tan(Math.PI * 0.25 + slat1 * 0.5), sn) * Math.cos(slat1) / sn;
		double ro = re * sf / Math.pow(Math.tan(Math.PI * 0.25 + olat * 0.5), sn);

		double ra = re * sf / Math.pow(Math.tan(Math.PI * 0.25 + Math.toRadians(point.latitude()) * 0.5), sn);
		double theta = Math.toRadians(point.longitude()) - olon;
		if (theta > Math.PI) {
			theta -= 2.0 * Math.PI;
		}
		if (theta < -Math.PI) {
			theta += 2.0 * Math.PI;
		}
		theta *= sn;

		int nx = (int) Math.floor(ra * Math.sin(theta) + ORIGIN_X + 0.5);
		int ny = (int) Math.floor(ro - ra * Math.cos(theta) + ORIGIN_Y + 0.5);
		return new Grid(nx, ny);
	}
}
