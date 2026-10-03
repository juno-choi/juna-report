package com.juno.weekendpicks.recommend;

import java.time.Month;

/** Seasonal outing theme; the keyword is used for a Kakao place search. */
public record SeasonalTheme(String emoji, String label, String keyword) {

	public static SeasonalTheme of(Month month) {
		return switch (month) {
			case MARCH -> new SeasonalTheme("🌼", "봄꽃 나들이", "봄꽃 명소");
			case APRIL -> new SeasonalTheme("🌸", "벚꽃 구경", "벚꽃 명소");
			case MAY -> new SeasonalTheme("🌹", "장미와 신록", "장미원");
			case JUNE -> new SeasonalTheme("🌿", "초여름 수목원 산책", "수목원");
			case JULY, AUGUST -> new SeasonalTheme("🏞️", "시원한 계곡", "계곡");
			case SEPTEMBER -> new SeasonalTheme("🌾", "초가을 산책", "생태공원");
			case OCTOBER, NOVEMBER -> new SeasonalTheme("🍁", "단풍 구경", "단풍 명소");
			case DECEMBER, JANUARY, FEBRUARY -> new SeasonalTheme("☃️", "따뜻한 실내 나들이", "식물원");
		};
	}
}
