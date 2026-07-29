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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.GlobalProperty;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientdocuments.api.section.VisitSummarySection;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * End-to-end tests of the sample preview against the real registered section beans, plus
 * a section class none of them know about at compile time. Every section emits an element
 * named after its section key, which is what these assertions key off.
 */
public class VisitSummaryPreviewSectionsTest extends BaseModuleContextSensitiveTest {

	private static final String VITALS_ENABLED_GP = "report.visitSummary.section.vitals.enabled";

	private static final String VITALS_ORDER_GP = "report.visitSummary.section.vitals.order";

	@Autowired
	private List<VisitSummarySection> registeredSections;

	/**
	 * Stands in for a section shipped by a downstream module: it is registered nowhere in
	 * this module and implements only the three required SPI methods, so it relies
	 * entirely on the interface's default sample rendering.
	 */
	private static class DownstreamSection implements VisitSummarySection {

		@Override
		public String getSectionKey() {
			return "downstreamModuleSection";
		}

		@Override
		public boolean isEnabled() {
			return true;
		}

		@Override
		public int getOrder() {
			return 650;
		}

		@Override
		public void renderXml(Document doc, Element root, Visit visit) {
			Element el = doc.createElement(getSectionKey());
			el.setAttribute("visitType", visit.getVisitType() != null ? visit.getVisitType().getName() : "");
			root.appendChild(el);
		}
	}

	@BeforeEach
	public void setUp() {
		Context.setLocale(Locale.ENGLISH);
		restoreVitalsDefaults();
	}

	/**
	 * Restoring in @BeforeEach alone protects only this class: the ConfigUtil cache is not
	 * rolled back with the test transaction and surefire reuses one JVM for the module, so
	 * the last-run test's values would otherwise be visible to every class that follows.
	 */
	@AfterEach
	public void tearDown() {
		restoreVitalsDefaults();
	}

	private void restoreVitalsDefaults() {
		Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(VITALS_ENABLED_GP, "true"));
		Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(VITALS_ORDER_GP, "300"));
	}

	private Element renderSample(List<VisitSummarySection> sections) throws Exception {
		VisitSummaryXmlReportRenderer renderer = new VisitSummaryXmlReportRenderer();
		ReflectionTestUtils.setField(renderer, "sections", sections);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		renderer.renderSample(out);

		Document rendered = DocumentBuilderFactory.newInstance().newDocumentBuilder()
		        .parse(new ByteArrayInputStream(out.toByteArray()));
		return rendered.getDocumentElement();
	}

	private List<VisitSummarySection> registeredSectionsPlusDownstream() {
		List<VisitSummarySection> sections = new ArrayList<>(registeredSections);
		sections.add(new DownstreamSection());
		return sections;
	}

	private List<String> elementNames(Element root) {
		List<String> names = new ArrayList<>();
		NodeList children = root.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				names.add(children.item(i).getNodeName());
			}
		}
		return names;
	}

	private Element findElement(Element root, String name) {
		NodeList children = root.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE && name.equals(node.getNodeName())) {
				return (Element) node;
			}
		}
		return null;
	}

	@Test
	public void renderSample_shouldRenderEveryRegisteredSectionIncludingADownstreamOne() throws Exception {
		List<VisitSummarySection> sections = registeredSectionsPlusDownstream();

		Element root = renderSample(sections);
		List<String> names = elementNames(root);

		for (VisitSummarySection section : sections) {
			Assertions.assertTrue(names.contains(section.getSectionKey()),
			    "Section '" + section.getSectionKey() + "' is missing from the preview");
		}
		Assertions.assertFalse(names.contains("section-error"),
		    "No registered section may fail to produce sample content");
	}

	@Test
	public void renderSample_shouldPopulateTheDownstreamSectionFromSampleData() throws Exception {
		Element root = renderSample(registeredSectionsPlusDownstream());

		Element downstream = findElement(root, "downstreamModuleSection");
		Assertions.assertNotNull(downstream);
		Assertions.assertFalse(downstream.getAttribute("visitType").isEmpty(),
		    "The delegated default must hand downstream sections populated sample data");
	}

	@Test
	public void renderSample_shouldOmitADisabledSectionAndHonourConfiguredOrder() throws Exception {
		Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(VITALS_ENABLED_GP, "false"));

		Assertions.assertFalse(elementNames(renderSample(registeredSectionsPlusDownstream())).contains("vitals"),
		    "A disabled section must be absent from the preview");

		// Re-enabled but reordered to the front: the preview must follow the same config.
		Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(VITALS_ENABLED_GP, "true"));
		Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(VITALS_ORDER_GP, "1"));

		List<String> names = elementNames(renderSample(registeredSectionsPlusDownstream()));
		Assertions.assertTrue(names.indexOf("vitals") < names.indexOf("facilityHeader"),
		    "Reordering vitals ahead of the facility header must reorder the preview");
	}

	@Test
	public void renderSample_shouldUseSampleContentThatNeedsNoConceptDictionary() throws Exception {
		Element root = renderSample(new ArrayList<>(registeredSections));

		// Most sections carry their content in attributes, so assert on the serialized
		// document rather than on text nodes alone.
		String serialized = serialize(root);
		Assertions.assertTrue(serialized.contains("120/80 mmHg"), "Sample vitals must be rendered");
		Assertions.assertTrue(serialized.contains("Malaria, uncomplicated"), "Sample diagnoses must be rendered");
		Assertions.assertTrue(serialized.contains("Penicillins"), "Sample allergies must be rendered");
		Assertions.assertTrue(serialized.contains("Amoxicillin 500mg"), "Sample medications must be rendered");
		Assertions.assertTrue(serialized.contains("Complete blood count"), "Sample lab results must be rendered");
		Assertions.assertTrue(serialized.contains("Hypertension"), "Sample conditions must be rendered");
		Assertions.assertTrue(serialized.contains("Sample Patient"), "Sample patient identity must be rendered");
		Assertions.assertTrue(serialized.contains("Sample Health Centre"), "Sample facility must be rendered");
		Assertions.assertTrue(root.getTextContent().contains("fever and headache"),
		    "Sample visit notes must be rendered");
	}

	private String serialize(Element root) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TransformerFactory.newInstance().newTransformer()
		        .transform(new DOMSource(root.getOwnerDocument()), new StreamResult(out));
		return out.toString("UTF-8");
	}
}
