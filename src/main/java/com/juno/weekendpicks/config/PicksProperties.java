package com.juno.weekendpicks.config;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import com.juno.weekendpicks.support.GeoPoint;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "picks")
public record PicksProperties(
		Home home,
		int searchRadiusMeters,
		int courseRadiusMeters,
		int festivalRadiusKm,
		int courseCount,
		History history,
		String kakaoRestApiKey,
		String dataGoKrServiceKey,
		String slackWebhookUrl
) {

	public PicksProperties {
		// data.go.kr issues both an "Encoding" and a "Decoding" key; accept either.
		// Requests always percent-encode the key, so an already-encoded one must be decoded first.
		if (dataGoKrServiceKey != null) {
			dataGoKrServiceKey = dataGoKrServiceKey.trim();
			if (dataGoKrServiceKey.contains("%")) {
				dataGoKrServiceKey = URLDecoder.decode(dataGoKrServiceKey, StandardCharsets.UTF_8);
			}
		}
	}

	public record Home(String name, double latitude, double longitude) {

		public GeoPoint point() {
			return new GeoPoint(latitude, longitude);
		}
	}

	public record History(String file, int keepWeeks) {
	}

	public boolean hasDataGoKrServiceKey() {
		return dataGoKrServiceKey != null && !dataGoKrServiceKey.isBlank();
	}

	public boolean hasSlackWebhookUrl() {
		return slackWebhookUrl != null && !slackWebhookUrl.isBlank();
	}
}
