package com.juno.weekendpicks.performance;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** Parses the XML of the KOPIS performance list (pblprfr) API. */
public final class KopisPerformanceParser {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");

	private KopisPerformanceParser() {
	}

	public static List<Performance> parse(String xml) {
		NodeList nodes = parseDocument(xml).getElementsByTagName("db");
		List<Performance> performances = new ArrayList<>();
		for (int i = 0; i < nodes.getLength(); i++) {
			Element element = (Element) nodes.item(i);
			// Errors (e.g. an invalid key) also come back as a <db> element, with <returncode> instead of an id.
			String returnCode = text(element, "returncode");
			if (!returnCode.isEmpty() && !"00".equals(returnCode)) {
				throw new IllegalStateException("KOPIS error %s: %s".formatted(returnCode, text(element, "errmsg")));
			}
			Performance performance = toPerformance(element);
			if (performance != null) {
				performances.add(performance);
			}
		}
		return performances;
	}

	private static Performance toPerformance(Element element) {
		String id = text(element, "mt20id");
		if (id.isEmpty()) {
			return null;
		}
		try {
			return new Performance(
					id,
					text(element, "prfnm"),
					text(element, "genrenm"),
					text(element, "fcltynm"),
					text(element, "area"),
					LocalDate.parse(text(element, "prfpdfrom"), DATE),
					LocalDate.parse(text(element, "prfpdto"), DATE));
		}
		catch (DateTimeParseException e) {
			// Skip entries without a valid period.
			return null;
		}
	}

	private static Document parseDocument(String xml) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
		}
		catch (ParserConfigurationException | SAXException | IOException e) {
			String snippet = xml.substring(0, Math.min(xml.length(), 300));
			throw new IllegalStateException("Invalid XML from KOPIS: " + snippet, e);
		}
	}

	private static String text(Element element, String tagName) {
		NodeList nodes = element.getElementsByTagName(tagName);
		return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
	}
}
