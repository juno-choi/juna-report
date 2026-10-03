package com.juno.weekendpicks.slack;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.juno.weekendpicks.config.PicksProperties;
import com.juno.weekendpicks.festival.Festival;
import com.juno.weekendpicks.performance.Performance;
import com.juno.weekendpicks.place.Place;
import com.juno.weekendpicks.recommend.Course;
import com.juno.weekendpicks.recommend.WeekendPicks;
import com.juno.weekendpicks.recommend.WeekendPicks.OutingMode;
import com.juno.weekendpicks.support.GeoPoint;
import com.juno.weekendpicks.support.JsonHttp;
import com.juno.weekendpicks.weather.DailyWeather;
import org.springframework.stereotype.Component;

/** Renders picks as a Slack Block Kit payload. */
@Component
public class SlackMessageBuilder {

	private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("M/d");
	private static final String KAKAO_MAP_SEARCH_URL = "https://map.kakao.com/link/search/";

	private final GeoPoint home;

	public SlackMessageBuilder(PicksProperties properties) {
		this.home = properties.home().point();
	}

	public Map<String, Object> build(WeekendPicks picks) {
		String title = "🧺 오늘의 나들이 추천 (%s~%s)".formatted(
				picks.days().get(0).format(SHORT_DATE),
				picks.days().get(picks.days().size() - 1).format(SHORT_DATE));

		List<Map<String, Object>> blocks = new ArrayList<>();
		blocks.add(Map.of("type", "header", "text", Map.of("type", "plain_text", "text", title, "emoji", true)));
		blocks.add(section(weatherText(picks)));
		blocks.add(Map.of("type", "divider"));

		if (picks.courses().isEmpty()) {
			blocks.add(section("이번 주는 새로 추천할 장소를 찾지 못했어요."));
		}
		for (int i = 0; i < picks.courses().size(); i++) {
			blocks.add(section(courseText(i + 1, picks.courses().get(i))));
		}

		if (!picks.festivals().isEmpty()) {
			blocks.add(Map.of("type", "divider"));
			blocks.add(section(festivalText(picks.festivals())));
		}
		if (!picks.performances().isEmpty()) {
			blocks.add(Map.of("type", "divider"));
			blocks.add(section(performanceText(picks.performances())));
		}
		blocks.add(Map.of("type", "context", "elements", List.of(
				Map.of("type", "mrkdwn", "text", "영업 여부와 휴무일은 지도 링크에서 확인하세요."))));

		// "text" is the notification fallback for clients that do not render blocks.
		return Map.of("text", title, "blocks", blocks);
	}

	private String weatherText(WeekendPicks picks) {
		StringBuilder text = new StringBuilder();
		for (DailyWeather day : picks.weather()) {
			text.append("*%s(%s)* %s %s · %d~%d℃ · 강수확률 %d%%\n".formatted(
					dayOfWeek(day.date()), day.date().format(SHORT_DATE), day.emoji(), day.label(),
					day.minTemp(), day.maxTemp(), day.maxRainProbability()));
		}
		if (picks.weather().isEmpty()) {
			text.append("날씨 정보를 가져오지 못했어요.\n");
		}
		text.append(adviceText(picks));
		return text.toString();
	}

	private String adviceText(WeekendPicks picks) {
		if (picks.mode() == OutingMode.INDOOR) {
			return "☔ 오늘과 내일 모두 야외 활동이 어려워 보여서 *실내 위주*로 골랐어요.";
		}
		String theme = "%s 이번 달 테마: *%s*".formatted(picks.theme().emoji(), picks.theme().label());
		List<DailyWeather> goodDays = picks.weather().stream().filter(DailyWeather::isGoodForOutdoor).toList();
		if (goodDays.size() == 1 && picks.weather().size() > 1) {
			return "👉 나들이는 *%s요일*이 더 좋아요.\n%s".formatted(dayOfWeek(goodDays.get(0).date()), theme);
		}
		return theme;
	}

	private String courseText(int number, Course course) {
		Place outing = course.outing();
		StringBuilder text = new StringBuilder();
		text.append("*코스 %d. %s* (%s · %.0fkm)\n".formatted(
				number, link(outing.url(), outing.name()), escape(outing.category()), home.distanceKmTo(outing.point())));
		text.append(escape(outing.address())).append('\n');
		if (course.restaurant() != null) {
			text.append("🍽 ").append(placeLine(course.restaurant())).append('\n');
		}
		if (course.bakery() != null) {
			text.append("🥐 ").append(placeLine(course.bakery())).append('\n');
		}
		return text.toString();
	}

	private String festivalText(List<Festival> festivals) {
		return "*🎪 지금 열리는 근처 축제*\n" + festivals.stream()
				.map(festival -> "• %s (%s~%s · %.0fkm) %s".formatted(
						link(KAKAO_MAP_SEARCH_URL + JsonHttp.encode(festival.title()).replace("+", "%20"), festival.title()),
						festival.startDate().format(SHORT_DATE), festival.endDate().format(SHORT_DATE),
						home.distanceKmTo(festival.point()), escape(festival.address())))
				.collect(Collectors.joining("\n"));
	}

	private static String performanceText(List<Performance> performances) {
		return "*🎭 곧 시작하는 공연 (서울·경기)*\n" + performances.stream()
				.map(performance -> "• [%s] %s (%s~%s) %s".formatted(
						escape(performance.genre()), link(performance.url(), performance.name()),
						performance.startDate().format(SHORT_DATE), performance.endDate().format(SHORT_DATE),
						escape(performance.venue())))
				.collect(Collectors.joining("\n"));
	}

	private static String placeLine(Place place) {
		return "%s — %s".formatted(link(place.url(), place.name()), escape(place.category()));
	}

	private static Map<String, Object> section(String mrkdwn) {
		return Map.of("type", "section", "text", Map.of("type", "mrkdwn", "text", mrkdwn));
	}

	private static String link(String url, String label) {
		return "<%s|%s>".formatted(url, escape(label).replace("|", "/"));
	}

	private static String escape(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static String dayOfWeek(LocalDate date) {
		return date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);
	}
}
