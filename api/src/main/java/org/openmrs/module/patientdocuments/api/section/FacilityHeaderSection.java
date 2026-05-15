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

import org.openmrs.Visit;
import org.openmrs.module.patientdocuments.api.model.FacilityInfo;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

// TODO: i18n — section heading currently hardcoded English; wire to MessageSourceService
@Component
@Order(100)
public class FacilityHeaderSection extends TypedSection<FacilityInfo> {

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
	protected FacilityInfo gatherData(Visit visit) {
		// TODO: populate facilityAddress from Location.getAddress1() through getAddress6()
		// TODO: populate facilityPhone from location attributes (LocationAttributeType)
		// Left empty pending address formatting decision
		return new FacilityInfo(
		    visit.getLocation() != null ? visit.getLocation().getName() : "",
		    "",
		    "");
	}

	@Override
	protected void renderXml(Document doc, Element root, FacilityInfo data) {
		Element section = doc.createElement("facilityHeader");
		root.appendChild(section);
		addTextElement(doc, section, "facilityName", data.getFacilityName());
		addTextElement(doc, section, "facilityAddress", data.getFacilityAddress());
		addTextElement(doc, section, "facilityPhone", data.getFacilityPhone());
	}
}
