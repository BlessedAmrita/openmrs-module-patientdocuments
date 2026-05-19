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

import org.openmrs.Location;
import org.openmrs.Visit;
import org.openmrs.module.patientdocuments.api.model.FacilityInfo;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
		if (visit.getLocation() == null) {
			return new FacilityInfo("", "", "");
		}
		Location loc = visit.getLocation();
		String name = loc.getName() != null ? loc.getName() : "";

		StringBuilder addr = new StringBuilder();
		for (String part : new String[] {
				loc.getAddress1(), loc.getAddress2(),
				loc.getCityVillage(), loc.getStateProvince(),
				loc.getCountry(), loc.getPostalCode()}) {
			if (part != null && !part.isEmpty()) {
				if (addr.length() > 0) addr.append(", ");
				addr.append(part);
			}
		}

		// Phone from location attribute (if the attribute type exists)
		String phone = "";

		return new FacilityInfo(name, addr.toString(), phone);
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
