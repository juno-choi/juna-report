package com.juno.weekendpicks.support;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class JsonHttp {

	private final RestClient restClient = RestClient.create();
	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	/**
	 * Builds a URI with every query value percent-encoded by hand.
	 * data.go.kr service keys contain '+' and '=', which URI builders leave as-is
	 * and the gateway then rejects as an unregistered key.
	 */
	public static URI uri(String baseUrl, Map<String, String> params) {
		String query = params.entrySet().stream()
				.map(e -> e.getKey() + "=" + encode(e.getValue()))
				.collect(Collectors.joining("&"));
		return URI.create(baseUrl + "?" + query);
	}

	public static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	public JsonNode get(URI uri, Map<String, String> headers) {
		byte[] body = restClient.get()
				.uri(uri)
				.headers(h -> headers.forEach(h::set))
				.retrieve()
				.body(byte[].class);
		// Decode explicitly: some public APIs omit the charset in Content-Type.
		String text = body == null ? "" : new String(body, StandardCharsets.UTF_8);
		try {
			return jsonMapper.readTree(text);
		}
		catch (JacksonException e) {
			// data.go.kr answers with an XML error document even when JSON is requested.
			String snippet = text.substring(0, Math.min(text.length(), 300));
			throw new IllegalStateException("Non-JSON response from " + uri.getHost() + uri.getPath() + ": " + snippet, e);
		}
	}

	/** For APIs that answer in XML or plain text. */
	public String getText(URI uri) {
		byte[] body = restClient.get().uri(uri).retrieve().body(byte[].class);
		return body == null ? "" : new String(body, StandardCharsets.UTF_8);
	}

	public void postJson(String url, Object payload) {
		restClient.post()
				.uri(URI.create(url))
				.contentType(MediaType.APPLICATION_JSON)
				.body(jsonMapper.writeValueAsBytes(payload))
				.retrieve()
				.toBodilessEntity();
	}
}
