package com.juno.weekendpicks.performance;

import java.time.LocalDate;

public record Performance(
		String id,
		String name,
		String genre,
		String venue,
		String area,
		LocalDate startDate,
		LocalDate endDate
) {

	private static final String DETAIL_URL = "https://www.kopis.or.kr/por/db/pblprfr/pblprfrView.do?menuId=MNU_00020&mt20Id=";

	public String url() {
		return DETAIL_URL + id;
	}
}
