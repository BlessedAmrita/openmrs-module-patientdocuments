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

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.openmrs.module.patientdocuments.reports.VisitSummaryReportManager.DATASET_KEY_VISIT_SUMMARY_FIELDS;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

import org.openmrs.User;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.initializer.api.InitializerService;
import org.openmrs.module.reporting.common.Localized;
import org.openmrs.module.reporting.dataset.DataSet;
import org.openmrs.module.reporting.dataset.DataSetRow;
import org.openmrs.module.reporting.report.ReportData;
import org.openmrs.module.reporting.report.ReportRequest;
import org.openmrs.module.reporting.report.renderer.RenderingException;
import org.openmrs.module.reporting.report.renderer.ReportDesignRenderer;
import org.openmrs.module.reporting.report.renderer.ReportRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * ReportRenderer that transforms visit summary data into an XML document for FOP rendering.
 */
@Component
@Handler
@Localized("patientdocuments.visitSummaryXmlReportRenderer")
public class VisitSummaryXmlReportRenderer extends ReportDesignRenderer {

	private static final Logger log = LoggerFactory.getLogger(VisitSummaryXmlReportRenderer.class);

	private InitializerService initializerService;

	private InitializerService getInitializerService() {
		if (initializerService == null) {
			initializerService = Context.getService(InitializerService.class);
		}
		return initializerService;
	}

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

		// Root element with page dimensions
		Element root = doc.createElement("visitSummary");
		doc.appendChild(root);
		configurePageDimensions(root);

		// Process dataset rows
		if (results.getDataSets().containsKey(DATASET_KEY_VISIT_SUMMARY_FIELDS)) {
			DataSet dataSet = results.getDataSets().get(DATASET_KEY_VISIT_SUMMARY_FIELDS);
			for (DataSetRow row : dataSet) {
				Map visitData = (Map) row.getColumnValue("visitData");
				if (visitData != null) {
					buildXmlFromData(doc, root, visitData);
				}
			}
		}

		writeToOutputStream(doc, out);
	}

	private void configurePageDimensions(Element root) {
		InitializerService svc = getInitializerService();
		String pageHeight = svc.getValueFromKey("report.visitSummary.size.height");
		String pageWidth = svc.getValueFromKey("report.visitSummary.size.width");
		root.setAttribute("page-height", isNotBlank(pageHeight) ? pageHeight : "297mm");
		root.setAttribute("page-width", isNotBlank(pageWidth) ? pageWidth : "210mm");
	}

	@SuppressWarnings("unchecked")
	private void buildXmlFromData(Document doc, Element root, Map visitData) {
		buildFacilityHeaderElement(doc, root, (Map<String, String>) visitData.get("facilityHeader"));
		buildPatientInfoElement(doc, root, (Map<String, String>) visitData.get("patientInfo"));
		buildVitalsElement(doc, root, (List<Map<String, String>>) visitData.get("vitals"));
		buildDiagnosesElement(doc, root, (List<Map<String, String>>) visitData.get("diagnoses"));
		buildAllergiesElement(doc, root, (List<Map<String, String>>) visitData.get("allergies"));
		buildFooterElement(doc, root);
	}

	private void buildFacilityHeaderElement(Document doc, Element root, Map<String, String> data) {
		Element section = doc.createElement("facilityHeader");
		root.appendChild(section);

		if (data != null) {
			addTextElement(doc, section, "facilityName", data.get("facilityName"));
			addTextElement(doc, section, "facilityAddress", data.get("facilityAddress"));
			addTextElement(doc, section, "facilityPhone", data.get("facilityPhone"));
		}
	}

	private void buildPatientInfoElement(Document doc, Element root, Map<String, String> data) {
		Element section = doc.createElement("patientInfo");
		section.setAttribute("heading", "Patient Information");
		// Field-label attributes — English defaults; replace with i18n-resolved text here when ready
		section.setAttribute("lbl-patient-name", "Patient Name");
		section.setAttribute("lbl-patient-id", "Patient ID");
		section.setAttribute("lbl-dob", "Date of Birth");
		section.setAttribute("lbl-gender", "Gender");
		section.setAttribute("lbl-visit-date", "Visit Date");
		section.setAttribute("lbl-visit-type", "Visit Type");
		section.setAttribute("lbl-location", "Location");
		root.appendChild(section);

		if (data != null) {
			addTextElement(doc, section, "patientName", data.get("patientName"));
			addTextElement(doc, section, "patientId", data.get("patientId"));
			addTextElement(doc, section, "dateOfBirth", data.get("dateOfBirth"));
			addTextElement(doc, section, "gender", data.get("gender"));
			addTextElement(doc, section, "visitDate", data.get("visitDate"));
			addTextElement(doc, section, "visitType", data.get("visitType"));
			addTextElement(doc, section, "visitLocation", data.get("visitLocation"));
			addTextElement(doc, section, "visitStopDate", data.get("visitStopDate"));
		}
	}

	private void buildVitalsElement(Document doc, Element root, List<Map<String, String>> vitals) {
		Element section = doc.createElement("vitals");
		section.setAttribute("heading", "Vital Signs");
		root.appendChild(section);

		if (vitals != null) {
			for (Map<String, String> vital : vitals) {
				Element vitalEl = doc.createElement("vital");
				vitalEl.setAttribute("label", nvl(vital.get("label")));
				vitalEl.setAttribute("value", nvl(vital.get("value")));
				section.appendChild(vitalEl);
			}
		}
	}

	private void buildDiagnosesElement(Document doc, Element root, List<Map<String, String>> diagnoses) {
		Element section = doc.createElement("diagnoses");
		section.setAttribute("heading", "Diagnoses");
		// Column-header attributes — English defaults; replace with i18n-resolved text here when ready
		section.setAttribute("col-name", "Diagnosis");
		section.setAttribute("col-certainty", "Certainty");
		section.setAttribute("col-rank", "Rank");
		root.appendChild(section);

		if (diagnoses != null) {
			for (Map<String, String> diag : diagnoses) {
				Element diagEl = doc.createElement("diagnosis");
				diagEl.setAttribute("name", nvl(diag.get("name")));
				diagEl.setAttribute("certainty", nvl(diag.get("certainty")));
				diagEl.setAttribute("rank", nvl(diag.get("rank")));
				section.appendChild(diagEl);
			}
		}
	}

	private void buildAllergiesElement(Document doc, Element root, List<Map<String, String>> allergies) {
		Element section = doc.createElement("allergies");
		section.setAttribute("heading", "Allergies");
		// Column-header attributes — English defaults; replace with i18n-resolved text here when ready
		section.setAttribute("col-allergen", "Allergen");
		section.setAttribute("col-severity", "Severity");
		section.setAttribute("col-reactions", "Reactions");
		root.appendChild(section);

		if (allergies != null) {
			for (Map<String, String> allergy : allergies) {
				Element allergyEl = doc.createElement("allergy");
				allergyEl.setAttribute("allergen", nvl(allergy.get("allergen")));
				allergyEl.setAttribute("severity", nvl(allergy.get("severity")));
				allergyEl.setAttribute("reactions", nvl(allergy.get("reactions")));
				section.appendChild(allergyEl);
			}
		}
	}

	private void buildFooterElement(Document doc, Element root) {
		Element footer = doc.createElement("footer");
		// Label attributes — English defaults; replace with i18n-resolved text here when ready
		footer.setAttribute("lbl-printed-by", "Printed by:");
		footer.setAttribute("lbl-system-id", "System ID:");
		root.appendChild(footer);

		User currentUser = Context.getAuthenticatedUser();
		String printedBy = "";
		if (currentUser != null) {
			if (currentUser.getPersonName() != null) {
				printedBy = currentUser.getPersonName().getFullName();
			} else {
				printedBy = currentUser.getUsername();
			}
		}
		addTextElement(doc, footer, "printedBy", printedBy);

		String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		addTextElement(doc, footer, "timestamp", timestamp);

		String systemId = currentUser != null ? nvl(currentUser.getSystemId()) : "";
		addTextElement(doc, footer, "systemId", systemId);
	}

	private void addTextElement(Document doc, Element parent, String tag, String value) {
		Element el = doc.createElement(tag);
		el.setTextContent(nvl(value));
		parent.appendChild(el);
	}

	private String nvl(String value) {
		return value != null ? value : "";
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
