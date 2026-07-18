/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientdocuments.renderer;

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the visit summary FOP stylesheet: the FO body must present
 * sections in XML document order (the renderer emits them sorted by getOrder()),
 * not in a sequence hardcoded in the stylesheet.
 */
public class VisitSummaryStylesheetOrderTest {

	private static final String STYLESHEET = "/visitSummaryFopStylesheet.xsl";

	private String transform(String xml) throws Exception {
		try (InputStream stylesheet = getClass().getResourceAsStream(STYLESHEET)) {
			Assertions.assertNotNull(stylesheet, "classpath stylesheet must be readable");
			Transformer transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(stylesheet));
			StringWriter out = new StringWriter();
			transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(out));
			return out.toString();
		}
	}

	private static String visitSummaryXml(String sections) {
		return "<visitSummary page-height=\"297mm\" page-width=\"210mm\">" + sections + "</visitSummary>";
	}

	private static List<String> headingOrder(String foOutput) {
		Matcher matcher = Pattern.compile("HEAD-([a-zA-Z]+)").matcher(foOutput);
		List<String> order = new ArrayList<>();
		while (matcher.find()) {
			order.add(matcher.group(1));
		}
		return order;
	}

	@Test
	public void stylesheet_shouldRenderSectionsInDocumentOrder() throws Exception {
		// Deliberately reversed relative to the old hardcoded template sequence.
		String output = transform(visitSummaryXml(
		    "<medications heading=\"HEAD-medications\" col-name=\"n\" col-dosing=\"d\" col-duration=\"u\" col-start=\"s\"/>"
		            + "<allergies heading=\"HEAD-allergies\" col-allergen=\"a\" col-severity=\"s\" col-reactions=\"r\"/>"
		            + "<conditions heading=\"HEAD-conditions\" col-name=\"n\" col-onset=\"o\"/>"
		            + "<diagnoses heading=\"HEAD-diagnoses\" col-name=\"n\" col-certainty=\"c\" col-rank=\"r\"/>"
		            + "<vitals heading=\"HEAD-vitals\"/>"));

		Assertions.assertEquals(Arrays.asList("medications", "allergies", "conditions", "diagnoses", "vitals"),
		    headingOrder(output));
	}

	@Test
	public void stylesheet_shouldKeepSectionErrorsNextToTheirSection() throws Exception {
		String output = transform(visitSummaryXml(
		    "<vitals heading=\"HEAD-vitals\"/>"
		            + "<section-error key=\"vitals\" message=\"ERR-vitals\"/>"
		            + "<diagnoses heading=\"HEAD-diagnoses\" col-name=\"n\" col-certainty=\"c\" col-rank=\"r\"/>"));

		int vitals = output.indexOf("HEAD-vitals");
		int error = output.indexOf("ERR-vitals");
		int diagnoses = output.indexOf("HEAD-diagnoses");
		Assertions.assertTrue(vitals >= 0 && error > vitals && diagnoses > error,
		    "section-error must render between its section and the next one");
	}

	@Test
	public void stylesheet_shouldRenderFooterOnlyAsStaticContentNotInBodyFlow() throws Exception {
		String output = transform(visitSummaryXml(
		    "<vitals heading=\"HEAD-vitals\"/>"
		            + "<footer lbl-printed-by=\"FOOT-printed-by\" lbl-system-id=\"s\">"
		            + "<printedBy>u</printedBy><timestamp>t</timestamp><systemId>i</systemId></footer>"));

		int first = output.indexOf("FOOT-printed-by");
		Assertions.assertTrue(first >= 0, "footer must render in the static-content region");
		Assertions.assertEquals(first, output.lastIndexOf("FOOT-printed-by"),
		    "footer must not render a second time in the body flow");
	}

	@Test
	public void stylesheet_shouldIgnoreUnknownSectionsInsteadOfLeakingText() throws Exception {
		String output = transform(visitSummaryXml(
		    "<vitals heading=\"HEAD-vitals\"/>"
		            + "<downstreamCustomSection heading=\"HEAD-custom\">rawtext</downstreamCustomSection>"));

		Assertions.assertEquals(Arrays.asList("vitals"), headingOrder(output));
		Assertions.assertFalse(output.contains("rawtext"),
		    "unknown sections must not leak text content into the FO output");
	}
}
