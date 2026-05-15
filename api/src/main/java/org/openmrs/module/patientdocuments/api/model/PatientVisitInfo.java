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

public class PatientVisitInfo {

	private final String patientName;

	private final String patientId;

	private final String dateOfBirth;

	private final String gender;

	private final String visitDate;

	private final String visitType;

	private final String visitLocation;

	private final String visitStopDate;

	public PatientVisitInfo(String patientName, String patientId, String dateOfBirth, String gender,
	    String visitDate, String visitType, String visitLocation, String visitStopDate) {
		this.patientName = patientName;
		this.patientId = patientId;
		this.dateOfBirth = dateOfBirth;
		this.gender = gender;
		this.visitDate = visitDate;
		this.visitType = visitType;
		this.visitLocation = visitLocation;
		this.visitStopDate = visitStopDate;
	}

	public String getPatientName() {
		return patientName;
	}

	public String getPatientId() {
		return patientId;
	}

	public String getDateOfBirth() {
		return dateOfBirth;
	}

	public String getGender() {
		return gender;
	}

	public String getVisitDate() {
		return visitDate;
	}

	public String getVisitType() {
		return visitType;
	}

	public String getVisitLocation() {
		return visitLocation;
	}

	public String getVisitStopDate() {
		return visitStopDate;
	}
}
