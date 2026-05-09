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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.Diagnosis;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// TODO: i18n — section heading currently hardcoded English; wire to MessageSourceService
// TODO: Add unit test for isEnabled() with mock global properties
@Component
@Order(3)
public class DiagnosesSection extends AbstractVisitSummarySection {

	private static final Logger log = LoggerFactory.getLogger(DiagnosesSection.class);

	@Override
	public String getSectionKey() {
		return "diagnoses";
	}

	@Override
	public Object getSectionData(Visit visit, Patient patient) {
		List<Map<String, String>> diagnoses = new ArrayList<Map<String, String>>();

		try {
			// getDiagnoses(Patient, Date) returns diagnoses since the given date;
			// using visit start as a proxy to scope results to this visit
			List<Diagnosis> diagnosisList = Context.getDiagnosisService()
			        .getDiagnoses(patient, visit.getStartDatetime());
			if (diagnosisList == null) {
				return diagnoses;
			}
			for (Diagnosis diagnosis : diagnosisList) {
				Map<String, String> entry = new HashMap<String, String>();

				String name = "";
				if (diagnosis.getDiagnosis() != null) {
					if (diagnosis.getDiagnosis().getCoded() != null
					        && diagnosis.getDiagnosis().getCoded().getName() != null) {
						name = diagnosis.getDiagnosis().getCoded().getName().getName();
					} else if (diagnosis.getDiagnosis().getNonCoded() != null) {
						name = diagnosis.getDiagnosis().getNonCoded();
					}
				}
				entry.put("name", name);
				entry.put("certainty", diagnosis.getCertainty() != null ? diagnosis.getCertainty().name() : "");
				entry.put("rank", diagnosis.getRank() != null ? String.valueOf(diagnosis.getRank()) : "");
				diagnoses.add(entry);
			}
		}
		catch (Exception e) {
			log.warn("Could not load diagnoses for visit; returning empty list", e);
		}

		return diagnoses;
	}
}
