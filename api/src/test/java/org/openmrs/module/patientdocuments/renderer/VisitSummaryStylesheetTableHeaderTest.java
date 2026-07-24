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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Regression test for the repeating table headers in the visit summary stylesheet.
 * <p>
 * Every data table's column-header row lives in {@code fo:table-header}, which
 * XSL-FO repeats on each page a table spans. A header row left in
 * {@code fo:table-body} would render only once, so page 2 of a broken table would
 * open with unlabeled columns — a patient-safety problem on a document that
 * travels with the patient (a "Critically Low" flag in an unlabeled column).
 * <p>
 * Each case renders one section with enough rows to force a genuine page break,
 * then asserts the column-header labels appear on every page of the PDF. Vitals,
 * visit notes and patient info are intentionally not covered: they have no
 * column-header row to repeat.
 */
public class VisitSummaryStylesheetTableHeaderTest {

	private static final String STYLESHEET = "/visitSummaryFopStylesheet.xsl";

	private static final String FOP_CONFIG = "conf/fop.xconf.xml";

	private static final String FONT_BASE = "fonts/";

	/** Rows per section: comfortably more than one A4 page so the table must break. */
	private static final int ROWS = 90;

	@Test
	public void diagnoses_headerRepeatsOnEveryPage() throws Exception {
		StringBuilder rows = new StringBuilder();
		for (int i = 0; i < ROWS; i++) {
			rows.append("<diagnosis name=\"Diagnosis ").append(i)
			        .append("\" certainty=\"Confirmed\" rank=\"1\"/>");
		}
		String section = "<diagnoses heading=\"HEAD\" col-name=\"COL-name\""
		        + " col-certainty=\"COL-certainty\" col-rank=\"COL-rank\">" + rows + "</diagnoses>";

		assertHeaderOnEveryPage(section, "COL-name", "COL-certainty", "COL-rank");
	}

	@Test
	public void conditions_headerRepeatsOnEveryPage() throws Exception {
		StringBuilder rows = new StringBuilder();
		for (int i = 0; i < ROWS; i++) {
			rows.append("<condition name=\"Condition ").append(i).append("\" onset=\"2026-01-01\"/>");
		}
		String section = "<conditions heading=\"HEAD\" col-name=\"COL-name\" col-onset=\"COL-onset\">"
		        + rows + "</conditions>";

		assertHeaderOnEveryPage(section, "COL-name", "COL-onset");
	}

	@Test
	public void labResults_headerRepeatsOnEveryPage() throws Exception {
		StringBuilder rows = new StringBuilder();
		for (int i = 0; i < ROWS; i++) {
			rows.append("<lab name=\"Lab ").append(i)
			        .append("\" value=\"1.0\" units=\"mg\" range=\"0-2\" flag=\"Critically Low\"/>");
		}
		String section = "<labResults heading=\"HEAD\" col-test=\"COL-test\" col-result=\"COL-result\""
		        + " col-range=\"COL-range\" col-flag=\"COL-flag\">" + rows + "</labResults>";

		assertHeaderOnEveryPage(section, "COL-test", "COL-result", "COL-range", "COL-flag");
	}

	@Test
	public void allergies_headerRepeatsOnEveryPage() throws Exception {
		StringBuilder rows = new StringBuilder();
		for (int i = 0; i < ROWS; i++) {
			rows.append("<allergy allergen=\"Allergen ").append(i)
			        .append("\" severity=\"Severe\" reactions=\"Rash\"/>");
		}
		String section = "<allergies heading=\"HEAD\" col-allergen=\"COL-allergen\""
		        + " col-severity=\"COL-severity\" col-reactions=\"COL-reactions\">" + rows + "</allergies>";

		assertHeaderOnEveryPage(section, "COL-allergen", "COL-severity", "COL-reactions");
	}

	@Test
	public void medications_headerRepeatsOnEveryPage() throws Exception {
		StringBuilder rows = new StringBuilder();
		for (int i = 0; i < ROWS; i++) {
			rows.append("<medication name=\"Medication ").append(i)
			        .append("\" dosing=\"1 tab\" duration=\"5 d\" start=\"2026-01-01\"/>");
		}
		String section = "<medications heading=\"HEAD\" col-name=\"COL-name\" col-dosing=\"COL-dosing\""
		        + " col-duration=\"COL-duration\" col-start=\"COL-start\">" + rows + "</medications>";

		assertHeaderOnEveryPage(section, "COL-name", "COL-dosing", "COL-duration", "COL-start");
	}

	/**
	 * Renders the section to a PDF, confirms it spans more than one page, and asserts
	 * every column header appears on each page.
	 */
	private void assertHeaderOnEveryPage(String sectionXml, String... headers) throws Exception {
		byte[] pdf = renderToPdf(visitSummaryXml(sectionXml));
		try (PDDocument document = PDDocument.load(pdf)) {
			int pageCount = document.getNumberOfPages();
			Assertions.assertTrue(pageCount > 1,
			    "test data must force a page break, but the table fit on one page");

			for (int page = 1; page <= pageCount; page++) {
				PDFTextStripper stripper = new PDFTextStripper();
				stripper.setStartPage(page);
				stripper.setEndPage(page);
				String text = stripper.getText(document);
				for (String header : headers) {
					Assertions.assertTrue(text.contains(header),
					    "column header '" + header + "' must repeat on page " + page + " of " + pageCount);
				}
			}
		}
	}

	private static String visitSummaryXml(String sections) {
		return "<visitSummary page-height=\"297mm\" page-width=\"210mm\" lbl-no-data=\"LBL-no-data\">"
		        + sections + "</visitSummary>";
	}

	/**
	 * Renders the given visit summary XML through the bundled stylesheet and FOP
	 * configuration, exactly as the production renderer does — the same classpath
	 * stylesheet, FOP config and font base.
	 */
	private byte[] renderToPdf(String xml) throws Exception {
		URL fontBase = getClass().getClassLoader().getResource(FONT_BASE);
		Assertions.assertNotNull(fontBase, "bundled font directory must be on the classpath");

		try (InputStream fopConfig = getClass().getClassLoader().getResourceAsStream(FOP_CONFIG);
		        InputStream stylesheet = getClass().getResourceAsStream(STYLESHEET)) {
			Assertions.assertNotNull(fopConfig, "bundled FOP configuration must be on the classpath");
			Assertions.assertNotNull(stylesheet, "bundled stylesheet must be on the classpath");

			FopFactory fopFactory = FopFactory.newInstance(fontBase.toURI(), fopConfig);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, out);

			Transformer transformer = TransformerFactory.newInstance()
			        .newTransformer(new StreamSource(stylesheet));
			Source source = new StreamSource(new StringReader(xml));
			Result result = new SAXResult(fop.getDefaultHandler());
			transformer.transform(source, result);
			return out.toByteArray();
		}
	}
}
