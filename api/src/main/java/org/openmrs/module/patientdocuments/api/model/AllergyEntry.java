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

public class AllergyEntry {

	private final String allergen;

	private final String severity;

	private final String reactions;

	public AllergyEntry(String allergen, String severity, String reactions) {
		this.allergen = allergen;
		this.severity = severity;
		this.reactions = reactions;
	}

	public String getAllergen() {
		return allergen;
	}

	public String getSeverity() {
		return severity;
	}

	public String getReactions() {
		return reactions;
	}
}
