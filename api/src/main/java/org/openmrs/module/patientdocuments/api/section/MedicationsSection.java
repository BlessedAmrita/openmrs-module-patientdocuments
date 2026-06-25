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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.FreeTextDosingInstructions;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientdocuments.api.model.MedicationEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component
public class MedicationsSection extends TypedSection<List<MedicationEntry>> {

	private static final Logger log = LoggerFactory.getLogger(MedicationsSection.class);

	// Sorts after Allergies (500) and before Footer (900), following the +50
	// spacing convention (Conditions = 450). NOTE: the printed position of this
	// section is fixed by the XSLT main template (it renders after allergies);
	// DEFAULT_ORDER governs the section-error grouping/ordering convention, not
	// the visual order in the PDF.
	private static final int DEFAULT_ORDER = 550;

	private static final String KEY_PREFIX = "patientdocuments.visitSummary.section.medications.";

	@Override
	protected int getDefaultOrder() {
		return DEFAULT_ORDER;
	}

	@Override
	public String getSectionKey() {
		return "medications";
	}

	@Override
	protected List<MedicationEntry> gatherData(Visit visit) {
		List<MedicationEntry> medications = new ArrayList<>();
		Patient patient = visit.getPatient();

		OrderType drugOrderType = Context.getOrderService()
		        .getOrderTypeByUuid(OrderType.DRUG_ORDER_TYPE_UUID);

		// careSetting = null → include both inpatient and outpatient orders.
		// asOfDate = null → active as of now (print time). Verified null-safe in
		// OrderServiceImpl (it substitutes new Date()). This mirrors the "now"
		// semantics of the patient-level conditions/allergies sections.
		List<Order> orders = Context.getOrderService().getActiveOrders(patient, drugOrderType, null, null);

		for (Order order : orders) {
			if (!(order instanceof DrugOrder)) {
				continue;
			}
			DrugOrder drugOrder = (DrugOrder) order;
			medications.add(new MedicationEntry(
			    buildDrugName(drugOrder),
			    buildDosing(drugOrder),
			    buildDuration(drugOrder),
			    formatDate(drugOrder.getDateActivated())
			));
		}

		return medications;
	}

	private String buildDrugName(DrugOrder order) {
		if (order.isNonCodedDrug()) {
			return order.getDrugNonCoded();
		}
		if (order.getDrug() != null) {
			String name = order.getDrug().getName();
			String strength = order.getDrug().getStrength();
			if (strength != null && !strength.trim().isEmpty()
			        && (name == null || !name.contains(strength.trim()))) {
				return (name != null ? name : "") + " " + strength.trim();
			}
			return name != null ? name : "";
		}
		if (order.getConcept() != null) {
			return conceptName(order.getConcept());
		}
		log.warn("Drug order {} has no drug, non-coded drug, or concept; using 'Unknown'", order.getOrderId());
		return "Unknown";
	}

	private String buildDosing(DrugOrder order) {
		if (FreeTextDosingInstructions.class.equals(order.getDosingType())) {
			return nvl(order.getDosingInstructions());
		}
		// SimpleDosingInstructions (the default) — build the string ourselves so a
		// null dose/units/route/frequency can't NPE (as it would in
		// SimpleDosingInstructions.getDosingInstructionsAsString).
		List<String> parts = new ArrayList<>();
		if (order.getDose() != null) {
			String dose = formatDose(order.getDose());
			if (order.getDoseUnits() != null) {
				dose = dose + " " + conceptName(order.getDoseUnits());
			}
			parts.add(dose.trim());
		}
		if (order.getRoute() != null) {
			parts.add(conceptName(order.getRoute()));
		}
		if (order.getFrequency() != null && order.getFrequency().getName() != null) {
			parts.add(order.getFrequency().getName());
		}
		return String.join(", ", parts);
	}

	private String buildDuration(DrugOrder order) {
		if (order.getDuration() == null) {
			return "—";
		}
		String units = order.getDurationUnits() != null ? conceptName(order.getDurationUnits()) : "";
		return units.isEmpty() ? String.valueOf(order.getDuration())
		        : order.getDuration() + " " + units;
	}

	private String formatDose(Double dose) {
		if (dose % 1 == 0) {
			return String.valueOf(dose.longValue());
		}
		return String.valueOf(dose);
	}

	private String conceptName(Concept concept) {
		if (concept != null && concept.getName() != null) {
			return concept.getName().getName();
		}
		return "";
	}

	private String formatDate(Date date) {
		if (date == null) {
			return "";
		}
		return new SimpleDateFormat("yyyy-MM-dd").format(date);
	}

	@Override
	protected void renderXml(Document doc, Element root, List<MedicationEntry> medications) {
		Element section = doc.createElement("medications");
		section.setAttribute("heading", msg(KEY_PREFIX + "heading", "Active Medications"));
		section.setAttribute("col-name", msg(KEY_PREFIX + "col.name", "Medication"));
		section.setAttribute("col-dosing", msg(KEY_PREFIX + "col.dosing", "Dosing"));
		section.setAttribute("col-duration", msg(KEY_PREFIX + "col.duration", "Duration"));
		section.setAttribute("col-start", msg(KEY_PREFIX + "col.start", "Start Date"));
		root.appendChild(section);

		for (MedicationEntry medication : medications) {
			Element medEl = doc.createElement("medication");
			medEl.setAttribute("name", nvl(medication.getName()));
			medEl.setAttribute("dosing", nvl(medication.getDosing()));
			medEl.setAttribute("duration", nvl(medication.getDuration()));
			medEl.setAttribute("start", nvl(medication.getStartDate()));
			section.appendChild(medEl);
		}
	}
}
