/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientdocuments.api.section;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Visit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;

/**
 * Unit tests for the TypedSection gather→render orchestration, on both the real path
 * (gatherData from a visit) and the sample path (gatherSampleData for the preview).
 * No Spring context is needed: the only Context calls (the msg() lookup for the error
 * banner and the sample text lookups) fall back safely when no context is open.
 */
public class TypedSectionTest {

	private Document doc;

	private Element root;

	private Visit visit;

	@BeforeEach
	public void setUp() throws Exception {
		doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		root = doc.createElement("root");
		doc.appendChild(root);
		visit = mock(Visit.class);
	}

	// ── concrete test double ──────────────────────────────────────────────────

	private static class TestSection extends TypedSection<List<String>> {

		private final List<String> dataToReturn;

		private final boolean gatherShouldThrow;

		private final boolean renderShouldThrow;

		private Visit gatheredFrom;

		TestSection(List<String> data, boolean gatherThrows, boolean renderThrows) {
			this.dataToReturn = data;
			this.gatherShouldThrow = gatherThrows;
			this.renderShouldThrow = renderThrows;
		}

		@Override
		public String getSectionKey() {
			return "test";
		}

		@Override
		protected List<String> gatherData(Visit visit) {
			gatheredFrom = visit;
			if (gatherShouldThrow) {
				throw new RuntimeException("gatherData failed");
			}
			return dataToReturn;
		}

		@Override
		protected void renderXml(Document doc, Element root, List<String> data) {
			if (renderShouldThrow) {
				throw new RuntimeException("renderXml failed");
			}
			root.appendChild(doc.createElement("test-item"));
		}
	}

	// ── tests ─────────────────────────────────────────────────────────────────

	@Test
	public void renderXml_whenGatherDataReturnsData_producesXmlOutput() {
		TestSection section = new TestSection(Collections.singletonList("item"), false, false);

		section.renderXml(doc, root, visit);

		Assertions.assertEquals(1, root.getChildNodes().getLength());
		Assertions.assertEquals("test-item", root.getChildNodes().item(0).getNodeName());
	}

	@Test
	public void renderXml_whenGatherDataReturnsNull_producesNoOutput() {
		TestSection section = new TestSection(null, false, false);

		section.renderXml(doc, root, visit);

		Assertions.assertEquals(0, root.getChildNodes().getLength());
	}

	@Test
	public void renderXml_whenGatherDataThrows_sectionIsSkippedGracefully() {
		TestSection section = new TestSection(null, true, false);

		Assertions.assertDoesNotThrow(() -> section.renderXml(doc, root, visit));
		// Error element should be added for clinical safety visibility
		Assertions.assertEquals(1, root.getChildNodes().getLength());
		Element errorEl = (Element) root.getChildNodes().item(0);
		Assertions.assertEquals("section-error", errorEl.getNodeName());
		Assertions.assertEquals("test", errorEl.getAttribute("key"));
	}

	@Test
	public void renderXml_whenTypedRenderXmlThrows_sectionIsSkippedGracefully() {
		TestSection section = new TestSection(Collections.singletonList("item"), false, true);

		Assertions.assertDoesNotThrow(() -> section.renderXml(doc, root, visit));
		// Error element should be added for clinical safety visibility
		Assertions.assertEquals(1, root.getChildNodes().getLength());
		Element errorEl = (Element) root.getChildNodes().item(0);
		Assertions.assertEquals("section-error", errorEl.getNodeName());
		Assertions.assertEquals("test", errorEl.getAttribute("key"));
	}

	@Test
	public void renderSampleXml_shouldRenderThroughTheRealTypedRenderXml() {
		TestSection section = new TestSection(Collections.singletonList("item"), false, false);

		section.renderSampleXml(doc, root, new ArrayList<>());

		Assertions.assertEquals(1, root.getChildNodes().getLength());
		Assertions.assertEquals("test-item", root.getChildNodes().item(0).getNodeName());
	}

	@Test
	public void renderSampleXml_shouldGatherFromATransientSampleVisitNeverALoadedOne() {
		TestSection section = new TestSection(Collections.singletonList("item"), false, false);

		section.renderSampleXml(doc, root, new ArrayList<>());

		Assertions.assertNotNull(section.gatheredFrom, "The default must delegate to gatherData");
		Assertions.assertNull(section.gatheredFrom.getVisitId(), "The sample visit must not come from the database");
		Assertions.assertNotNull(section.gatheredFrom.getPatient(), "The sample visit must be populated");
	}

	@Test
	public void renderSampleXml_shouldPropagateFailuresSoTheCallerCanMakeThemVisible() {
		TestSection section = new TestSection(null, true, false);

		Assertions.assertThrows(RuntimeException.class,
		    () -> section.renderSampleXml(doc, root, new ArrayList<>()));
	}
}
