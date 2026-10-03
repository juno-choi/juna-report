package com.juno.weekendpicks.weather;

import static org.assertj.core.api.Assertions.assertThat;

import com.juno.weekendpicks.support.GeoPoint;
import com.juno.weekendpicks.weather.GridConverter.Grid;
import org.junit.jupiter.api.Test;

class GridConverterTest {

	@Test
	void convertsSeoulCityHallToKmaGrid() {
		assertThat(GridConverter.toGrid(new GeoPoint(37.5665, 126.9780))).isEqualTo(new Grid(60, 127));
	}

	@Test
	void convertsBusanCityHallToKmaGrid() {
		assertThat(GridConverter.toGrid(new GeoPoint(35.1796, 129.0756))).isEqualTo(new Grid(98, 76));
	}
}
