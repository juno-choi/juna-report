package com.juno.weekendpicks.config;

import java.nio.file.Path;
import java.util.Random;

import com.juno.weekendpicks.recommend.HistoryRepository;
import com.juno.weekendpicks.recommend.PlacePicker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PicksConfig {

	@Bean
	public HistoryRepository historyRepository(PicksProperties properties) {
		return new HistoryRepository(Path.of(properties.history().file()), properties.history().keepWeeks());
	}

	@Bean
	public PlacePicker placePicker() {
		return new PlacePicker(new Random());
	}
}
