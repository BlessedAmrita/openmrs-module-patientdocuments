/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientdocuments.api.model;

public class FacilityInfo {

	private final String facilityName;

	private final String facilityAddress;

	private final String facilityPhone;

	public FacilityInfo(String facilityName, String facilityAddress, String facilityPhone) {
		this.facilityName = facilityName;
		this.facilityAddress = facilityAddress;
		this.facilityPhone = facilityPhone;
	}

	public String getFacilityName() {
		return facilityName;
	}

	public String getFacilityAddress() {
		return facilityAddress;
	}

	public String getFacilityPhone() {
		return facilityPhone;
	}
}
