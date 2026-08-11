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

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
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

import org.apache.commons.lang3.StringUtils;
import org.openmrs.Visit;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientdocuments.api.section.VisitSummarySection;
import org.openmrs.module.patientdocuments.common.PatientDocumentsConstants;
import org.openmrs.util.ConfigUtil;
import org.openmrs.module.reporting.common.Localized;
import org.openmrs.module.reporting.dataset.DataSet;
import org.openmrs.module.reporting.dataset.DataSetRow;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.report.ReportData;
import org.openmrs.module.reporting.report.ReportRequest;
import org.openmrs.module.reporting.report.renderer.RenderingException;
import org.openmrs.module.reporting.report.renderer.ReportDesignRenderer;
import org.openmrs.module.reporting.report.renderer.ReportRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * ReportRenderer that iterates enabled sections sorted by each section's
 * configurable getOrder() value, delegating all XML construction to each
 * section's renderXml().
 */
@Slf4j
@Component
@Handler
@Localized("patientdocuments.visitSummaryXmlReportRenderer")
public class VisitSummaryXmlReportRenderer extends ReportDesignRenderer {

	private static final String NO_DATA_MESSAGE_KEY = "patientdocuments.visitSummary.common.noDataRecorded";

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
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = docFactory.newDocumentBuilder();
		}
		catch (ParserConfigurationException e) {
			throw new RenderingException(e.getLocalizedMessage(), e);
		}

		Document doc = docBuilder.newDocument();

		Element root = doc.createElement("visitSummary");
		doc.appendChild(root);
		configurePageDimensions(root, results.getContext());
		configureNoDataLabel(root);

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

	private void buildXmlFromVisit(Document doc, Element root, Visit visit) {
		if (sections != null) {
			List<VisitSummarySection> ordered = new ArrayList<>(sections);
			ordered.sort(Comparator.comparingInt(VisitSummarySection::getOrder));
			for (VisitSummarySection section : ordered) {
				if (section.isEnabled()) {
					section.renderXml(doc, root, visit);
				}
			}
		}
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

	/**
	 * Publishes the page frame as root attributes for the stylesheet to interpolate. It reads
	 * six of the seven; {@code content-width-mm} it does not, and that one ships as the raw
	 * measurement behind the banded {@code layout-profile} so a consumer that needs the number
	 * rather than the band does not have to re-derive it.
	 * <p>
	 * Measured in Java because XSLT 1.0 cannot do unit arithmetic. Emitting the normalised
	 * value rather than the raw configuration also keeps an unparseable page size from
	 * reaching FOP as a page dimension.
	 * <p>
	 * Each dimension takes the request override if this render was asked for one, and the
	 * global property otherwise, so a request naming only one dimension keeps the configured
	 * default for the other. The context is null on the report-design path, where there is no
	 * request to override anything.
	 */
	private void configurePageDimensions(Element root, EvaluationContext context) {
		ConfiguredDimension width = ConfiguredDimension.resolve(context,
				PatientDocumentsConstants.VISIT_SUMMARY_PAGE_WIDTH_PARAMETER,
				PatientDocumentsConstants.VISIT_SUMMARY_PAGE_WIDTH_PROPERTY);
		ConfiguredDimension height = ConfiguredDimension.resolve(context,
				PatientDocumentsConstants.VISIT_SUMMARY_PAGE_HEIGHT_PARAMETER,
				PatientDocumentsConstants.VISIT_SUMMARY_PAGE_HEIGHT_PROPERTY);

		VisitSummaryPageLayout layout = VisitSummaryPageLayout.from(width.value, height.value, width.source,
				height.source);

		root.setAttribute("page-height", layout.getPageHeightAttribute());
		root.setAttribute("page-width", layout.getPageWidthAttribute());
		root.setAttribute("side-margin", layout.getSideMarginAttribute());
		root.setAttribute("logo-column-width", layout.getLogoColumnAttribute());
		root.setAttribute("logo-graphic-width", layout.getLogoGraphicAttribute());
		root.setAttribute("content-width-mm", VisitSummaryPageLayout.formatMm(layout.getContentWidthMm()));
		root.setAttribute("layout-profile", layout.getLayoutProfile());
	}

	/**
	 * A raw page dimension paired with the knob it came from. It only picks which of the two
	 * knobs to read; the value stays raw so that {@link VisitSummaryPageLayout#from} remains
	 * the only place a page dimension is parsed or validated.
	 */
	private static final class ConfiguredDimension {

		private final String value;

		private final String source;

		private ConfiguredDimension(String value, String source) {
			this.value = value;
			this.source = source;
		}

		static ConfiguredDimension resolve(EvaluationContext context, String parameterName, String globalPropertyKey) {
			Object requested = context == null ? null : context.getParameterValue(parameterName);
			if (requested != null && StringUtils.isNotBlank(requested.toString())) {
				return new ConfiguredDimension(requested.toString(),
						VisitSummaryPageLayout.requestParameterSource(parameterName));
			}
			return new ConfiguredDimension(ConfigUtil.getProperty(globalPropertyKey),
					VisitSummaryPageLayout.globalPropertySource(globalPropertyKey));
		}
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
