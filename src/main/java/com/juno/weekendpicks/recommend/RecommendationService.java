package com.juno.weekendpicks.recommend;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.juno.weekendpicks.config.PicksProperties;
import com.juno.weekendpicks.festival.Festival;
import com.juno.weekendpicks.festival.TourApiClient;
import com.juno.weekendpicks.place.KakaoLocalClient;
import com.juno.weekendpicks.place.Place;
import com.juno.weekendpicks.recommend.WeekendPicks.OutingMode;
import com.juno.weekendpicks.support.GeoPoint;
import com.juno.weekendpicks.weather.DailyWeather;
import com.juno.weekendpicks.weather.WeatherClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

	private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

	private static final int CATEGORY_PAGES = 3;
	private static final int MAX_FESTIVALS = 5;
	private static final String INDOOR_KEYWORD = "미술관";
	private static final String RESTAURANT_KEYWORD = "맛집";
	private static final String BAKERY_KEYWORD = "베이커리 카페";

	private final PicksProperties properties;
	private final WeatherClient weatherClient;
	private final KakaoLocalClient kakaoLocalClient;
	private final TourApiClient tourApiClient;
	private final HistoryRepository historyRepository;
	private final PlacePicker placePicker;

	public RecommendationService(PicksProperties properties, WeatherClient weatherClient,
			KakaoLocalClient kakaoLocalClient, TourApiClient tourApiClient,
			HistoryRepository historyRepository, PlacePicker placePicker) {
		this.properties = properties;
		this.weatherClient = weatherClient;
		this.kakaoLocalClient = kakaoLocalClient;
		this.tourApiClient = tourApiClient;
		this.historyRepository = historyRepository;
		this.placePicker = placePicker;
	}

	/** Recommends for today and tomorrow; the job is scheduled on Saturday morning. */
	public WeekendPicks recommend(LocalDateTime now) {
		LocalDate today = now.toLocalDate();
		List<LocalDate> days = List.of(today, today.plusDays(1));

		List<DailyWeather> weather = fetchWeather(now, days);
		OutingMode mode = decideMode(weather);
		SeasonalTheme theme = SeasonalTheme.of(today.getMonth());

		Set<String> usedIds = new HashSet<>(historyRepository.recentPlaceIds(today));
		List<Course> courses = new ArrayList<>();
		for (Place outing : pickOutings(mode, theme, usedIds)) {
			usedIds.add(outing.id());
			Place restaurant = pickNearby(RESTAURANT_KEYWORD, KakaoLocalClient.RESTAURANT, outing.point(), usedIds);
			Place bakery = pickNearby(BAKERY_KEYWORD, KakaoLocalClient.CAFE, outing.point(), usedIds);
			courses.add(new Course(outing, restaurant, bakery));
		}

		return new WeekendPicks(days, weather, mode, theme, courses, fetchFestivals(days));
	}

	/** Outdoor unless the forecast is bad on every day; a missing forecast defaults to outdoor. */
	static OutingMode decideMode(List<DailyWeather> weather) {
		boolean allBad = !weather.isEmpty() && weather.stream().noneMatch(DailyWeather::isGoodForOutdoor);
		return allBad ? OutingMode.INDOOR : OutingMode.OUTDOOR;
	}

	private List<Place> pickOutings(OutingMode mode, SeasonalTheme theme, Set<String> excludedIds) {
		GeoPoint home = properties.home().point();
		int radius = properties.searchRadiusMeters();
		int count = properties.courseCount();

		if (mode == OutingMode.INDOOR) {
			List<Place> candidates = new ArrayList<>(
					kakaoLocalClient.searchCategory(KakaoLocalClient.CULTURAL_FACILITY, home, radius, CATEGORY_PAGES));
			candidates.addAll(kakaoLocalClient.searchKeyword(INDOOR_KEYWORD, null, home, radius, 1));
			return placePicker.pick(candidates, excludedIds, count);
		}

		// At most one seasonal spot so that the list keeps some variety.
		List<Place> outings = new ArrayList<>(
				placePicker.pick(kakaoLocalClient.searchKeyword(theme.keyword(), null, home, radius, 1), excludedIds, 1));
		Set<String> excluded = new HashSet<>(excludedIds);
		outings.forEach(place -> excluded.add(place.id()));
		outings.addAll(placePicker.pick(
				kakaoLocalClient.searchCategory(KakaoLocalClient.TOURIST_ATTRACTION, home, radius, CATEGORY_PAGES),
				excluded, count - outings.size()));
		return outings;
	}

	private Place pickNearby(String keyword, String categoryGroupCode, GeoPoint center, Set<String> usedIds) {
		List<Place> candidates = kakaoLocalClient.searchKeyword(
				keyword, categoryGroupCode, center, properties.courseRadiusMeters(), 1);
		List<Place> picked = placePicker.pick(candidates, usedIds, 1);
		if (picked.isEmpty()) {
			return null;
		}
		usedIds.add(picked.get(0).id());
		return picked.get(0);
	}

	private List<DailyWeather> fetchWeather(LocalDateTime now, List<LocalDate> days) {
		if (!properties.hasDataGoKrServiceKey()) {
			log.warn("DATA_GO_KR_SERVICE_KEY is not set; skipping weather");
			return List.of();
		}
		try {
			return weatherClient.fetch(now, days);
		}
		catch (RuntimeException e) {
			log.warn("Failed to fetch weather; continuing without it", e);
			return List.of();
		}
	}

	private List<Festival> fetchFestivals(List<LocalDate> days) {
		if (!properties.hasDataGoKrServiceKey()) {
			log.warn("DATA_GO_KR_SERVICE_KEY is not set; skipping festivals");
			return List.of();
		}
		GeoPoint home = properties.home().point();
		try {
			return tourApiClient.fetchFestivals(days.get(0), days.get(days.size() - 1)).stream()
					.filter(festival -> home.distanceKmTo(festival.point()) <= properties.festivalRadiusKm())
					.sorted(Comparator.comparingDouble(festival -> home.distanceKmTo(festival.point())))
					.limit(MAX_FESTIVALS)
					.toList();
		}
		catch (RuntimeException e) {
			log.warn("Failed to fetch festivals; continuing without them", e);
			return List.of();
		}
	}
}
