package com.juno.weekendpicks;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import com.juno.weekendpicks.config.PicksProperties;
import com.juno.weekendpicks.recommend.HistoryRepository;
import com.juno.weekendpicks.recommend.RecommendationService;
import com.juno.weekendpicks.recommend.WeekendPicks;
import com.juno.weekendpicks.slack.SlackMessageBuilder;
import com.juno.weekendpicks.support.JsonHttp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Component
public class WeekendPicksRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(WeekendPicksRunner.class);
	private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

	private final PicksProperties properties;
	private final RecommendationService recommendationService;
	private final SlackMessageBuilder slackMessageBuilder;
	private final HistoryRepository historyRepository;
	private final JsonHttp jsonHttp;

	public WeekendPicksRunner(PicksProperties properties, RecommendationService recommendationService,
			SlackMessageBuilder slackMessageBuilder, HistoryRepository historyRepository, JsonHttp jsonHttp) {
		this.properties = properties;
		this.recommendationService = recommendationService;
		this.slackMessageBuilder = slackMessageBuilder;
		this.historyRepository = historyRepository;
		this.jsonHttp = jsonHttp;
	}

	@Override
	public void run(ApplicationArguments args) {
		LocalDateTime now = LocalDateTime.now(KOREA);
		WeekendPicks picks = recommendationService.recommend(now);
		Map<String, Object> payload = slackMessageBuilder.build(picks);

		if (!properties.hasSlackWebhookUrl()) {
			// Dry run: print the payload and leave the history untouched.
			log.info("SLACK_WEBHOOK_URL is not set; dry run payload:\n{}",
					JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build().writeValueAsString(payload));
			return;
		}
		jsonHttp.postJson(properties.slackWebhookUrl(), payload);
		historyRepository.record(now.toLocalDate(), picks.recommendedIds());
		log.info("Sent {} courses, {} festivals and {} performances to Slack",
				picks.courses().size(), picks.festivals().size(), picks.performances().size());
	}
}
