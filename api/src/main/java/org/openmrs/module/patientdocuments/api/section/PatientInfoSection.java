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

import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonName;
import org.openmrs.Visit;
import org.openmrs.module.patientdocuments.api.model.PatientVisitInfo;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component
public class PatientInfoSection extends TypedSection<PatientVisitInfo> {

	private static final int DEFAULT_ORDER = 200;

	private static final String KEY_PREFIX = "patientdocuments.visitSummary.section.patientInfo.";

	@Override
	protected int getDefaultOrder() {
		return DEFAULT_ORDER;
	}

	@Override
	public String getSectionKey() {
		return "patientInfo";
	}

	/** Patient identity is always rendered; there is no meaningful PDF without it. */
	@Override
	public boolean isEnabled() {
		return true;
	}

	/** Always on (see isEnabled), so config UIs must not offer a toggle. */
	@Override
	public boolean isToggleable() {
		return false;
	}

	@Override
	protected PatientVisitInfo gatherData(Visit visit) {
		Patient patient = visit.getPatient();
		if (patient == null) {
			return buildVisitInfo("", "", "", "", "", visit);
		}

		PersonName name = patient.getPersonName();
		PatientIdentifier preferredId = patient.getPatientIdentifier();

		return buildVisitInfo(
			name != null ? name.getFullName() : "",
			preferredId != null ? preferredId.getIdentifier() : "",
			patient.getBirthdate() != null ? formatDate(patient.getBirthdate()) : "",
			deriveAge(patient, visit),
			patient.getGender() != null ? patient.getGender() : "",
			visit
		);
	}

	/**
	 * Derives the patient's age in years at the visit start date (or today when
	 * the visit has no start date). Returns "" when the birthdate is missing or
	 * postdates the visit, so the PDF omits the age instead of printing a
	 * garbage or negative value.
	 */
	private String deriveAge(Patient patient, Visit visit) {
		if (patient.getBirthdate() == null) {
			return "";
		}
		Integer age = patient.getAge(visit.getStartDatetime());
		if (age == null || age < 0) {
			return "";
		}
		return String.valueOf(age);
	}

	private PatientVisitInfo buildVisitInfo(String patientName, String patientId,
			String dateOfBirth, String age, String gender, Visit visit) {
		return PatientVisitInfo.builder()
			.patientName(patientName)
			.patientId(patientId)
			.dateOfBirth(dateOfBirth)
			.age(age)
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
		section.setAttribute("heading", msg(KEY_PREFIX + "heading", "Patient Information"));
		section.setAttribute("lbl-patient-name", msg(KEY_PREFIX + "lbl.patientName", "Patient Name"));
		section.setAttribute("lbl-patient-id", msg(KEY_PREFIX + "lbl.patientId", "Patient ID"));
		section.setAttribute("lbl-dob", msg(KEY_PREFIX + "lbl.dob", "Date of Birth"));
		section.setAttribute("lbl-age", msg(KEY_PREFIX + "lbl.age", "Age"));
		section.setAttribute("lbl-gender", msg(KEY_PREFIX + "lbl.gender", "Gender"));
		section.setAttribute("lbl-visit-date", msg(KEY_PREFIX + "lbl.visitDate", "Visit Date"));
		section.setAttribute("lbl-visit-type", msg(KEY_PREFIX + "lbl.visitType", "Visit Type"));
		section.setAttribute("lbl-location", msg(KEY_PREFIX + "lbl.location", "Location"));
		root.appendChild(section);

		addTextElement(doc, section, "patientName", data.getPatientName());
		addTextElement(doc, section, "patientId", data.getPatientId());
		addTextElement(doc, section, "dateOfBirth", data.getDateOfBirth());
		addTextElement(doc, section, "age", data.getAge());
		addTextElement(doc, section, "gender", data.getGender());
		addTextElement(doc, section, "visitDate", data.getVisitDate());
		addTextElement(doc, section, "visitType", data.getVisitType());
		addTextElement(doc, section, "visitLocation", data.getVisitLocation());
		addTextElement(doc, section, "visitStopDate", data.getVisitStopDate());
	}
}
