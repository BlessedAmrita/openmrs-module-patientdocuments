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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import lombok.extern.slf4j.Slf4j;

import org.openmrs.Visit;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientdocuments.api.section.VisitSummarySampleData;
import org.openmrs.module.patientdocuments.api.section.VisitSummarySection;
import org.openmrs.module.patientdocuments.common.PatientDocumentsConstants;
import org.openmrs.util.ConfigUtil;
import org.openmrs.module.reporting.common.Localized;
import org.openmrs.module.reporting.dataset.DataSet;
import org.openmrs.module.reporting.dataset.DataSetRow;
import org.openmrs.module.reporting.report.ReportData;
import org.openmrs.module.reporting.report.ReportRequest;
import org.openmrs.module.reporting.report.renderer.RenderingException;
import org.openmrs.module.reporting.report.renderer.ReportDesignRenderer;
import org.openmrs.module.reporting.report.renderer.ReportRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * ReportRenderer that iterates enabled sections sorted by each section's
 * configurable getOrder() value, delegating all XML construction to each
 * section's renderXml().
 * <p>
 * renderSample() produces the same document from the same section list, through the
 * same enabled-and-ordered filter, but calls each section's renderSampleXml() instead —
 * so the settings-page preview cannot drift from the real summary's configuration.
 */
@Component
@Handler
@Localized("patientdocuments.visitSummaryXmlReportRenderer")
@Slf4j
public class VisitSummaryXmlReportRenderer extends ReportDesignRenderer {

	private static final String NO_DATA_MESSAGE_KEY = "patientdocuments.visitSummary.common.noDataRecorded";

	static final String PREVIEW_NOTICE_KEY = "patientdocuments.visitSummary.preview.notice";

	static final String PREVIEW_EMPTY_SECTION_KEY = "patientdocuments.visitSummary.preview.emptySection";

	static final String PREVIEW_NO_SECTIONS_KEY = "patientdocuments.visitSummary.preview.noSections";

	@Autowired(required = false)
	private List<VisitSummarySection> sections;

	/**
	 * @see ReportRenderer#getFilename(ReportRequest)
	 */
	@Override
	public String getFilename(ReportRequest request) {
		return getFilenameBase(request) + ".xml";
	}

	/**
	 * @see ReportRenderer#getRenderedContentType(ReportRequest)
	 */
	@Override
	public String getRenderedContentType(ReportRequest request) {
		return "text/xml";
	}

	@Override
	public void render(ReportData results, String argument, OutputStream out) throws IOException, RenderingException {
		Document doc = newDocument();
		Element root = newRoot(doc);

		if (results.getDataSets().containsKey(DATASET_KEY_VISIT_SUMMARY_FIELDS)) {
			DataSet dataSet = results.getDataSets().get(DATASET_KEY_VISIT_SUMMARY_FIELDS);
			for (DataSetRow row : dataSet) {
				Visit visit = (Visit) row.getColumnValue("visit");
				if (visit != null) {
					buildXmlFromVisit(doc, root, visit);
				}
			}
		}

		writeToOutputStream(doc, out);
	}

	/**
	 * Renders the sample preview XML: the same document shell and the same enabled,
	 * ordered section list as render(), with each section asked for sample content.
	 * No Visit is loaded — sections are handed in-memory sample data only.
	 */
	public void renderSample(OutputStream out) throws RenderingException {
		Document doc = newDocument();
		Element root = newRoot(doc);

		// Leads the document so nobody mistakes a preview for a patient record. Reuses the
		// existing section-notice element rather than adding a preview-only visual.
		appendNotice(doc, root, null, VisitSummarySampleData.message(PREVIEW_NOTICE_KEY,
		    "Sample preview — example data only, not a patient record"));

		List<VisitSummarySection> enabled = getEnabledSectionsInOrder();
		if (enabled.isEmpty()) {
			// An empty preview would read as "the summary looks like nothing", which is a very
			// different problem from "no section is switched on". Say which one it is.
			appendNotice(doc, root, null, VisitSummarySampleData.message(PREVIEW_NO_SECTIONS_KEY,
			    "No visit summary sections are registered or enabled"));
		}
		for (VisitSummarySection section : enabled) {
			renderSampleSection(doc, root, section);
		}

		writeToOutputStream(doc, out);
	}

	/**
	 * Renders one section's sample content, guaranteeing it is visible either way: a
	 * throwing section becomes a section-error, and a section that contributes nothing at
	 * all becomes a section-notice, so no configured section can silently vanish from the
	 * preview. Sections are caught individually so one failure cannot take out the rest.
	 */
	private void renderSampleSection(Document doc, Element root, VisitSummarySection section) {
		List<String> notices = new ArrayList<>();
		int childrenBefore = countElements(root);
		boolean failed = false;
		try {
			section.renderSampleXml(doc, root, notices);
		}
		catch (Exception e) {
			log.error("Section '{}' failed to render sample content", section.getSectionKey(), e);
			failed = true;
		}

		if (failed) {
			Element errorEl = doc.createElement("section-error");
			errorEl.setAttribute("key", section.getSectionKey());
			errorEl.setAttribute("message", VisitSummarySampleData.message(
			    "patientdocuments.visitSummary.common.sectionError", "Unable to load data for this section"));
			root.appendChild(errorEl);
		} else if (countElements(root) == childrenBefore && notices.isEmpty()) {
			notices.add(VisitSummarySampleData.message(PREVIEW_EMPTY_SECTION_KEY,
			    "This section produced no sample content"));
		}

		for (String message : notices) {
			appendNotice(doc, root, section.getSectionKey(), message);
		}
	}

	private void appendNotice(Document doc, Element root, String sectionKey, String message) {
		Element noticeEl = doc.createElement("section-notice");
		if (sectionKey != null) {
			noticeEl.setAttribute("key", sectionKey);
		}
		noticeEl.setAttribute("message", message);
		root.appendChild(noticeEl);
	}

	private int countElements(Element root) {
		int count = 0;
		NodeList children = root.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				count++;
			}
		}
		return count;
	}

	private void buildXmlFromVisit(Document doc, Element root, Visit visit) {
		for (VisitSummarySection section : getEnabledSectionsInOrder()) {
			section.renderXml(doc, root, visit);
		}
	}

	/**
	 * The single sort-and-filter both the real summary and the preview go through, so a
	 * disabled section is absent from both and a reordering shows up in both.
	 */
	private List<VisitSummarySection> getEnabledSectionsInOrder() {
		if (sections == null) {
			return new ArrayList<>();
		}
		List<VisitSummarySection> ordered = new ArrayList<>();
		for (VisitSummarySection section : sections) {
			if (section.isEnabled()) {
				ordered.add(section);
			}
		}
		ordered.sort(Comparator.comparingInt(VisitSummarySection::getOrder));
		return ordered;
	}

	private Document newDocument() throws RenderingException {
		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		}
		catch (ParserConfigurationException e) {
			throw new RenderingException(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Builds the shared document shell. Both render() and renderSample() go through here,
	 * so the preview gets the same page dimensions and empty-state label as the real PDF.
	 */
	private Element newRoot(Document doc) {
		Element root = doc.createElement("visitSummary");
		doc.appendChild(root);
		configurePageDimensions(root);
		configureNoDataLabel(root);
		return root;
	}

	/**
	 * Sets the localized "no data recorded" label as a root attribute. It lives on
	 * the root because the stylesheet's empty-state block is shared by every
	 * section template.
	 */
	private void configureNoDataLabel(Element root) {
		String label;
		try {
			label = Context.getMessageSourceService().getMessage(NO_DATA_MESSAGE_KEY, null,
					PatientDocumentsConstants.NO_DATA_RECORDED_PLACEHOLDER, Context.getLocale());
		}
		catch (Exception e) {
			log.warn("Message lookup failed for key '{}'; using fallback", NO_DATA_MESSAGE_KEY, e);
			label = PatientDocumentsConstants.NO_DATA_RECORDED_PLACEHOLDER;
		}
		root.setAttribute("lbl-no-data", label);
	}

	private void configurePageDimensions(Element root) {
		String pageHeight = ConfigUtil.getProperty("report.visitSummary.size.height", "297mm");
		String pageWidth = ConfigUtil.getProperty("report.visitSummary.size.width", "210mm");
		root.setAttribute("page-height", pageHeight);
		root.setAttribute("page-width", pageWidth);
	}

	private void writeToOutputStream(Document doc, OutputStream out) throws RenderingException {
		Transformer transformer;
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
		}
		catch (TransformerConfigurationException | TransformerFactoryConfigurationError e) {
			throw new RenderingException(e.getLocalizedMessage(), new Throwable(e));
		}

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

		DOMSource source = new DOMSource(doc);
		try {
			transformer.transform(source, new StreamResult(out));
		}
		catch (TransformerException e) {
			throw new RenderingException(e.getLocalizedMessage(), new Throwable(e));
		}
	}
}
