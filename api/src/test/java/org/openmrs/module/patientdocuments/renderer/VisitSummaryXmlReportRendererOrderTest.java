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

import static org.openmrs.module.patientdocuments.reports.VisitSummaryReportManager.DATASET_KEY_VISIT_SUMMARY_FIELDS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.Visit;
import org.openmrs.module.patientdocuments.api.section.VisitSummarySection;
import org.openmrs.module.reporting.dataset.DataSetColumn;
import org.openmrs.module.reporting.dataset.DataSetRow;
import org.openmrs.module.reporting.dataset.SimpleDataSet;
import org.openmrs.module.reporting.report.ReportData;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Regression tests for the rendered section sequence: the pre-FOP XML emitted by
 * {@link VisitSummaryXmlReportRenderer} must list sections sorted by getOrder(),
 * regardless of registration order, and must omit disabled sections.
 */
public class VisitSummaryXmlReportRendererOrderTest extends BaseModuleContextSensitiveTest {

	/** Stub section writing an element named after its key; order/enabled are fixed. */
	private static class StubSection implements VisitSummarySection {

		private final String key;

		private final int order;

		private final boolean enabled;

		StubSection(String key, int order, boolean enabled) {
			this.key = key;
			this.order = order;
			this.enabled = enabled;
		}

		@Override
		public String getSectionKey() {
			return key;
		}

		@Override
		public boolean isEnabled() {
			return enabled;
		}

		@Override
		public int getOrder() {
			return order;
		}

		@Override
		public void renderXml(Document doc, Element root, Visit visit) {
			root.appendChild(doc.createElement(key));
		}
	}

	private List<String> renderAndListSectionElements(List<VisitSummarySection> stubs) throws Exception {
		VisitSummaryXmlReportRenderer renderer = new VisitSummaryXmlReportRenderer();
		ReflectionTestUtils.setField(renderer, "sections", stubs);

		DataSetRow row = new DataSetRow();
		row.addColumnValue(new DataSetColumn("visit", "Visit", Visit.class), new Visit());
		SimpleDataSet dataSet = new SimpleDataSet(null, null);
		dataSet.addRow(row);
		ReportData reportData = new ReportData();
		reportData.getDataSets().put(DATASET_KEY_VISIT_SUMMARY_FIELDS, dataSet);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		renderer.render(reportData, null, out);

		Document rendered = DocumentBuilderFactory.newInstance().newDocumentBuilder()
		        .parse(new ByteArrayInputStream(out.toByteArray()));
		Assertions.assertEquals("visitSummary", rendered.getDocumentElement().getTagName());

		List<String> elementNames = new ArrayList<>();
		NodeList children = rendered.getDocumentElement().getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				elementNames.add(children.item(i).getNodeName());
			}
		}
		return elementNames;
	}

	@Test
	public void render_shouldEmitSectionsSortedByOrderNotRegistrationOrder() throws Exception {
		List<VisitSummarySection> stubs = Arrays.asList(
		    new StubSection("gamma", 30, true),
		    new StubSection("alpha", 10, true),
		    new StubSection("beta", 20, true));

		Assertions.assertEquals(Arrays.asList("alpha", "beta", "gamma"), renderAndListSectionElements(stubs));
	}

	@Test
	public void render_shouldHonorAScrambledConfiguredOrder() throws Exception {
		// Simulates an admin reordering: registration order alpha,beta,gamma but
		// configured order puts gamma first and alpha last.
		List<VisitSummarySection> stubs = Arrays.asList(
		    new StubSection("alpha", 300, true),
		    new StubSection("beta", 200, true),
		    new StubSection("gamma", 100, true));

		Assertions.assertEquals(Arrays.asList("gamma", "beta", "alpha"), renderAndListSectionElements(stubs));
	}

	@Test
	public void render_shouldOmitDisabledSections() throws Exception {
		List<VisitSummarySection> stubs = Arrays.asList(
		    new StubSection("alpha", 10, true),
		    new StubSection("beta", 20, false),
		    new StubSection("gamma", 30, true));

		Assertions.assertEquals(Arrays.asList("alpha", "gamma"), renderAndListSectionElements(stubs));
	}
}
