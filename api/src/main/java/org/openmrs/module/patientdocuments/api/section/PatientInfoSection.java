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
import java.util.HashMap;
import java.util.Map;

import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonName;
import org.openmrs.Visit;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// TODO: i18n — section heading currently hardcoded English; wire to MessageSourceService
@Component
@Order(1)
public class PatientInfoSection extends AbstractVisitSummarySection {

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
	public Object getSectionData(Visit visit, Patient patient) {
		Map<String, String> info = new HashMap<String, String>();

		PersonName name = patient.getPersonName();
		info.put("patientName", name != null ? name.getFullName() : "");

		Date dob = patient.getBirthdate();
		info.put("dateOfBirth", dob != null ? formatDate(dob) : "");
		info.put("gender", patient.getGender() != null ? patient.getGender() : "");

		PatientIdentifier preferredId = patient.getPatientIdentifier();
		info.put("patientId", preferredId != null ? preferredId.getIdentifier() : "");

		info.put("visitDate", visit.getStartDatetime() != null ? formatDate(visit.getStartDatetime()) : "");
		info.put("visitType", visit.getVisitType() != null ? visit.getVisitType().getName() : "");
		info.put("visitLocation", visit.getLocation() != null ? visit.getLocation().getName() : "");
		info.put("visitStopDate", visit.getStopDatetime() != null ? formatDate(visit.getStopDatetime()) : "");

		return info;
	}

	private String formatDate(Date date) {
		if (date == null) {
			return "";
		}
		return new SimpleDateFormat("yyyy-MM-dd").format(date);
	}
}
