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
import java.util.Date;

import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonName;
import org.openmrs.Visit;
import org.openmrs.module.patientdocuments.api.model.PatientVisitInfo;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component
@Order(200)
public class PatientInfoSection extends TypedSection<PatientVisitInfo> {

	@Override
	public String getSectionKey() {
		return "patientInfo";
	}

	/** Patient identity is always rendered; there is no meaningful PDF without it. */
	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	protected PatientVisitInfo gatherData(Visit visit) {
		Patient patient = visit.getPatient();
		if (patient == null) {
			return buildVisitInfo("", "", "", "", visit);
		}

		PersonName name = patient.getPersonName();
		PatientIdentifier preferredId = patient.getPatientIdentifier();

		return buildVisitInfo(
			name != null ? name.getFullName() : "",
			preferredId != null ? preferredId.getIdentifier() : "",
			patient.getBirthdate() != null ? formatDate(patient.getBirthdate()) : "",
			patient.getGender() != null ? patient.getGender() : "",
			visit
		);
	}

	private PatientVisitInfo buildVisitInfo(String patientName, String patientId,
			String dateOfBirth, String gender, Visit visit) {
		return PatientVisitInfo.builder()
			.patientName(patientName)
			.patientId(patientId)
			.dateOfBirth(dateOfBirth)
			.gender(gender)
			.visitDate(visit.getStartDatetime() != null ? formatDate(visit.getStartDatetime()) : "")
			.visitType(visit.getVisitType() != null ? visit.getVisitType().getName() : "")
			.visitLocation(visit.getLocation() != null ? visit.getLocation().getName() : "")
			.visitStopDate(visit.getStopDatetime() != null ? formatDate(visit.getStopDatetime()) : "")
			.build();
	}

	@Override
	protected void renderXml(Document doc, Element root, PatientVisitInfo data) {
		Element section = doc.createElement("patientInfo");
		section.setAttribute("heading", "Patient Information");
		section.setAttribute("lbl-patient-name", "Patient Name");
		section.setAttribute("lbl-patient-id", "Patient ID");
		section.setAttribute("lbl-dob", "Date of Birth");
		section.setAttribute("lbl-gender", "Gender");
		section.setAttribute("lbl-visit-date", "Visit Date");
		section.setAttribute("lbl-visit-type", "Visit Type");
		section.setAttribute("lbl-location", "Location");
		root.appendChild(section);

		addTextElement(doc, section, "patientName", data.getPatientName());
		addTextElement(doc, section, "patientId", data.getPatientId());
		addTextElement(doc, section, "dateOfBirth", data.getDateOfBirth());
		addTextElement(doc, section, "gender", data.getGender());
		addTextElement(doc, section, "visitDate", data.getVisitDate());
		addTextElement(doc, section, "visitType", data.getVisitType());
		addTextElement(doc, section, "visitLocation", data.getVisitLocation());
		addTextElement(doc, section, "visitStopDate", data.getVisitStopDate());
	}

	private String formatDate(Date date) {
		if (date == null) {
			return "";
		}
		return new SimpleDateFormat("yyyy-MM-dd").format(date);
	}
}
