/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientdocuments.api.document;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.module.patientdocuments.api.section.VisitSummarySection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// TODO: BillingSection — stub activation check wired, data gathering pending billing module integration
// TODO: MedicationsSection, LabResultsSection, ConditionsSection — to be implemented
// TODO: Section ordering — currently @Order annotations; consider config-driven ordering
@Component
public class VisitSummaryDocumentBuilder {

	@Autowired(required = false)
	private List<VisitSummarySection> sections;

	/**
	 * Assembles the full data map for a visit in @Order sequence.
	 * Uses LinkedHashMap to preserve section insertion order for the XML renderer.
	 */
	public Map<String, Object> buildData(Visit visit, Patient patient) {
		Map<String, Object> data = new LinkedHashMap<String, Object>();
		if (sections != null) {
			for (VisitSummarySection section : sections) {
				if (section.isEnabled()) {
					Object sectionData = section.getSectionData(visit, patient);
					if (sectionData != null) {
						data.put(section.getSectionKey(), sectionData);
					}
				}
			}
		}
		return data;
	}
}
