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
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.Visit;
import org.openmrs.module.patientdocuments.api.section.TypedSection;
import org.openmrs.module.patientdocuments.api.section.VisitSummarySection;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Tests for the sample preview XML emitted by
 * {@link VisitSummaryXmlReportRenderer#renderSample(java.io.OutputStream)}: it must apply
 * the same enabled-and-ordered filter as the real path, must never load a visit, and must
 * make every failure visible rather than dropping the section.
 * <p>
 * Context-sensitive because the shared document shell reads its page dimensions from
 * global properties, exactly as the real render path does.
 */
public class VisitSummaryXmlReportRendererSampleTest extends BaseModuleContextSensitiveTest {

	/**
	 * Stands in for a section contributed by a downstream module: it implements the SPI
	 * directly, knows nothing about this module's sections, and overrides nothing beyond
	 * the three required methods — so it exercises the interface's default sample path.
	 */
	private static class DownstreamStubSection implements VisitSummarySection {

		private final String key;

		private final int order;

		private final boolean enabled;

		private Visit receivedVisit;

		DownstreamStubSection(String key, int order, boolean enabled) {
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
			receivedVisit = visit;
			Element el = doc.createElement(key);
			if (visit.getPatient() != null && visit.getPatient().getPersonName() != null) {
				el.setAttribute("patient", visit.getPatient().getPersonName().getFullName());
			}
			root.appendChild(el);
		}
	}

	/** Section whose sample render blows up, to prove the failure is visible. */
	private static class ThrowingStubSection extends TypedSection<String> {

		@Override
		public String getSectionKey() {
			return "throwing";
		}

		@Override
		public int getOrder() {
			return 700;
		}

		@Override
		public boolean isEnabled() {
			return true;
		}

		@Override
		protected String gatherData(Visit visit) {
			return "real data";
		}

		@Override
		protected String gatherSampleData(List<String> notices) {
			throw new IllegalStateException("sample data unavailable");
		}

		@Override
		protected void renderXml(Document doc, Element root, String data) {
			root.appendChild(doc.createElement("throwing"));
		}
	}

	/** Section that renders nothing at all, to prove silence is reported. */
	private static class SilentStubSection implements VisitSummarySection {

		@Override
		public String getSectionKey() {
			return "silent";
		}

		@Override
		public boolean isEnabled() {
			return true;
		}

		@Override
		public int getOrder() {
			return 800;
		}

		@Override
		public void renderXml(Document doc, Element root, Visit visit) {
			// contributes nothing, on both the real and the sample path
		}

		@Override
		public void renderSampleXml(Document doc, Element root, List<String> notices) {
			// contributes nothing
		}
	}

	private Element renderSample(List<VisitSummarySection> stubs) throws Exception {
		VisitSummaryXmlReportRenderer renderer = new VisitSummaryXmlReportRenderer();
		ReflectionTestUtils.setField(renderer, "sections", stubs);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		renderer.renderSample(out);

		Document rendered = DocumentBuilderFactory.newInstance().newDocumentBuilder()
		        .parse(new ByteArrayInputStream(out.toByteArray()));
		Assertions.assertEquals("visitSummary", rendered.getDocumentElement().getTagName());
		return rendered.getDocumentElement();
	}

	private List<Element> elements(Element root) {
		List<Element> elements = new ArrayList<>();
		NodeList children = root.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				elements.add((Element) children.item(i));
			}
		}
		return elements;
	}

	private List<String> elementNames(Element root) {
		List<String> names = new ArrayList<>();
		for (Element element : elements(root)) {
			names.add(element.getNodeName());
		}
		return names;
	}

	private Element findElement(Element root, String name, String keyAttribute) {
		NodeList children = root.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE && name.equals(node.getNodeName())) {
				Element el = (Element) node;
				if (keyAttribute == null || keyAttribute.equals(el.getAttribute("key"))) {
					return el;
				}
			}
		}
		return null;
	}

	@Test
	public void renderSample_shouldRenderASectionContributedByADownstreamModule() throws Exception {
		DownstreamStubSection downstream = new DownstreamStubSection("downstream", 100, true);

		Element root = renderSample(Arrays.asList((VisitSummarySection) downstream));

		Element rendered = findElement(root, "downstream", null);
		Assertions.assertNotNull(rendered, "A downstream section must appear in the preview unchanged");
		Assertions.assertNotNull(downstream.receivedVisit, "The default must hand the section sample data");
		Assertions.assertNull(downstream.receivedVisit.getVisitId(),
		    "The preview must never load a visit from the database");
		Assertions.assertFalse(rendered.getAttribute("patient").isEmpty(),
		    "Sample data must be populated, not blank");
	}

	@Test
	public void renderSample_shouldOmitDisabledSections() throws Exception {
		List<VisitSummarySection> stubs = Arrays.asList(
		    new DownstreamStubSection("alpha", 10, true),
		    new DownstreamStubSection("beta", 20, false),
		    new DownstreamStubSection("gamma", 30, true));

		List<String> names = elementNames(renderSample(stubs));

		Assertions.assertTrue(names.contains("alpha"));
		Assertions.assertTrue(names.contains("gamma"));
		Assertions.assertFalse(names.contains("beta"), "A disabled section must be absent from the preview");
	}

	@Test
	public void renderSample_shouldEmitSectionsInConfiguredOrderNotRegistrationOrder() throws Exception {
		List<VisitSummarySection> stubs = Arrays.asList(
		    new DownstreamStubSection("alpha", 300, true),
		    new DownstreamStubSection("beta", 200, true),
		    new DownstreamStubSection("gamma", 100, true));

		List<String> names = elementNames(renderSample(stubs));
		names.removeIf(name -> "section-notice".equals(name));

		Assertions.assertEquals(Arrays.asList("gamma", "beta", "alpha"), names);
	}

	@Test
	public void renderSample_shouldRenderASectionErrorWhenSampleRenderingThrows() throws Exception {
		List<VisitSummarySection> stubs = Arrays.asList(
		    new DownstreamStubSection("alpha", 10, true),
		    new ThrowingStubSection());

		Element root = renderSample(stubs);

		Element error = findElement(root, "section-error", "throwing");
		Assertions.assertNotNull(error, "A throwing sample render must surface as a visible section-error");
		Assertions.assertFalse(error.getAttribute("message").isEmpty(), "The error must carry a message");
		Assertions.assertTrue(elementNames(root).contains("alpha"),
		    "One failing section must not take out the others");
	}

	@Test
	public void renderSample_shouldRenderANoticeWhenASectionContributesNothing() throws Exception {
		Element root = renderSample(Arrays.asList((VisitSummarySection) new SilentStubSection()));

		Element notice = findElement(root, "section-notice", "silent");
		Assertions.assertNotNull(notice, "A section that renders nothing must say so rather than vanish");
		Assertions.assertFalse(notice.getAttribute("message").isEmpty(), "The notice must carry a message");
	}

	@Test
	public void renderSample_shouldSayWhyThePreviewIsEmptyWhenNoSectionsAreRegistered() throws Exception {
		VisitSummaryXmlReportRenderer renderer = new VisitSummaryXmlReportRenderer();
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		renderer.renderSample(out);

		Document rendered = DocumentBuilderFactory.newInstance().newDocumentBuilder()
		        .parse(new ByteArrayInputStream(out.toByteArray()));
		Element root = rendered.getDocumentElement();
		// The sample-preview banner, then a notice explaining that nothing is registered.
		Assertions.assertEquals(Arrays.asList("section-notice", "section-notice"), elementNames(root));
		Assertions.assertFalse(elements(root).get(1).getAttribute("message").isEmpty(),
		    "An empty preview must explain that no section is registered or enabled");
	}

	@Test
	public void renderSample_shouldLeadWithTheSamplePreviewNotice() throws Exception {
		Element root = renderSample(Arrays.asList((VisitSummarySection)
		    new DownstreamStubSection("alpha", 10, true)));

		List<String> names = elementNames(root);
		Assertions.assertEquals("section-notice", names.get(0),
		    "The preview must open with the notice marking it as sample data");
	}
}
