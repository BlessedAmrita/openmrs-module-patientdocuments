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

import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientdocuments.api.model.PatientVisitInfo;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;

/**
 * Tests the derived age that accompanies the date of birth in the patient block.
 *
 * Age is derived at the visit start date rather than today, so reprinting an old
 * visit does not silently change the printed age.
 */
public class PatientInfoSectionTest extends BaseModuleContextSensitiveTest {

	private PatientInfoSection section;

	@BeforeEach
	public void setUp() {
		section = new PatientInfoSection();
	}

	private static Date dateOf(int year, int month, int day) {
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set(year, month - 1, day);
		return calendar.getTime();
	}

	private Visit visitFor(Date birthdate, Date visitStart) {
		Patient patient = Context.getPatientService().getPatient(2);
		patient.setBirthdate(birthdate);

		Visit visit = new Visit();
		visit.setPatient(patient);
		visit.setStartDatetime(visitStart);
		return visit;
	}

	@Test
	public void gatherData_shouldDeriveAgeAtVisitStartDate() {
		PatientVisitInfo info = section.gatherData(visitFor(dateOf(1981, 6, 24), dateOf(2026, 6, 24)));

		Assertions.assertEquals("45", info.getAge());
	}

	@Test
	public void gatherData_shouldDeriveAgeAtVisitStartNotToday() {
		// Same patient, a visit ten years earlier: the age must reflect the visit, not now.
		PatientVisitInfo info = section.gatherData(visitFor(dateOf(1981, 6, 24), dateOf(2016, 6, 24)));

		Assertions.assertEquals("35", info.getAge());
	}

	@Test
	public void gatherData_shouldNotYetHaveHadBirthdayInVisitYear() {
		PatientVisitInfo info = section.gatherData(visitFor(dateOf(1981, 6, 24), dateOf(2026, 6, 23)));

		Assertions.assertEquals("44", info.getAge());
	}

	@Test
	public void gatherData_shouldReturnEmptyAgeWhenBirthdateIsNull() {
		PatientVisitInfo info = section.gatherData(visitFor(null, dateOf(2026, 6, 24)));

		Assertions.assertEquals("", info.getAge());
		Assertions.assertEquals("", info.getDateOfBirth());
	}

	@Test
	public void gatherData_shouldReturnEmptyAgeWhenBirthdateIsAfterVisitStart() {
		// A data-entry error: a birthdate after the visit must not print a negative age.
		PatientVisitInfo info = section.gatherData(visitFor(dateOf(2030, 1, 1), dateOf(2026, 6, 24)));

		Assertions.assertEquals("", info.getAge());
	}

	@Test
	public void gatherData_shouldReturnEmptyAgeWhenPatientIsNull() {
		Visit visit = new Visit();
		visit.setStartDatetime(dateOf(2026, 6, 24));

		PatientVisitInfo info = section.gatherData(visit);

		Assertions.assertEquals("", info.getAge());
	}
}
