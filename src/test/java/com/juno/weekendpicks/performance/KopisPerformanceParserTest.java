package com.juno.weekendpicks.performance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class KopisPerformanceParserTest {

	@Test
	void parsesPerformanceList() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8"?>
				<dbs>
					<db>
						<mt20id>PF100001</mt20id>
						<prfnm>Sample Musical</prfnm>
						<prfpdfrom>2026.10.20</prfpdfrom>
						<prfpdto>2026.12.31</prfpdto>
						<fcltynm>Sample Art Center</fcltynm>
						<area>서울특별시</area>
						<genrenm>뮤지컬</genrenm>
						<prfstate>공연예정</prfstate>
					</db>
					<db>
						<mt20id>PF100002</mt20id>
						<prfnm>Broken Period</prfnm>
						<prfpdfrom></prfpdfrom>
						<prfpdto></prfpdto>
					</db>
				</dbs>
				""";

		List<Performance> performances = KopisPerformanceParser.parse(xml.strip());

		assertThat(performances).containsExactly(new Performance("PF100001", "Sample Musical", "뮤지컬",
				"Sample Art Center", "서울특별시", LocalDate.of(2026, 10, 20), LocalDate.of(2026, 12, 31)));
	}

	@Test
	void returnsEmptyListWhenNoResult() {
		assertThat(KopisPerformanceParser.parse("<dbs></dbs>")).isEmpty();
	}

	@Test
	void throwsOnErrorResponse() {
		String xml = "<dbs><db><returncode>02</returncode><errmsg>SERVICE KEY IS NOT REGISTERED</errmsg></db></dbs>";

		assertThatThrownBy(() -> KopisPerformanceParser.parse(xml))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("NOT REGISTERED");
	}
}
