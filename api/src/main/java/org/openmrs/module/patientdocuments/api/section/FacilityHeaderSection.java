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

import java.util.HashMap;
import java.util.Map;

import org.openmrs.Patient;
import org.openmrs.Visit;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// TODO: i18n — section heading currently hardcoded English; wire to MessageSourceService
@Component
@Order(0)
public class FacilityHeaderSection extends AbstractVisitSummarySection {

	@Override
	public String getSectionKey() {
		return "facilityHeader";
	}

	/** Facility header is always rendered; there is no meaningful PDF without it. */
	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public Object getSectionData(Visit visit, Patient patient) {
		Map<String, String> header = new HashMap<String, String>();
		header.put("facilityName", visit.getLocation() != null ? visit.getLocation().getName() : "");
		header.put("facilityAddress", "");
		header.put("facilityPhone", "");
		return header;
	}
}
