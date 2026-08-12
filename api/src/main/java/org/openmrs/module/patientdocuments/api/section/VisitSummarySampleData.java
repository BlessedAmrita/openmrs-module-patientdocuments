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
import java.util.LinkedHashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonName;
import org.openmrs.Visit;
import org.openmrs.VisitType;
import org.openmrs.api.context.Context;

/**
 * Shared sample content for the visit summary preview.
 * <p>
 * Everything here is built in memory: {@link #newSampleVisit()} constructs a transient
 * object graph with {@code new}, so nothing on the preview path is read from, or written
 * to, the database — no Visit, Patient, Location or Encounter is ever loaded.
 * <p>
 * All display text is resolved through message keys under {@link #KEY_PREFIX} and is
 * plain text only: no concept, drug or order metadata is referenced, so a deployment with
 * a different (or empty) concept dictionary gets exactly the same preview.
 */
@Slf4j
public final class VisitSummarySampleData {

	/** Message key prefix for every piece of sample display text. */
	public static final String KEY_PREFIX = "patientdocuments.visitSummary.preview.sample.";

	private VisitSummarySampleData() {
	}

	/**
	 * Builds the transient Visit handed to sections that have no custom sample content.
	 * <p>
	 * The graph carries only what the always-on identity sections read — patient name,
	 * identifier, birthdate, gender, visit type, location and dates — plus one encounter,
	 * so a section iterating {@code visit.getEncounters()} sees a non-null collection
	 * rather than an NPE. None of it is persisted or attached to a Hibernate session.
	 */
	public static Visit newSampleVisit() {
		Location location = newSampleLocation();

		Visit visit = new Visit();
		visit.setPatient(newSamplePatient(location));
		visit.setVisitType(new VisitType(msg("visitType", "Outpatient Visit"), null));
		visit.setLocation(location);
		visit.setStartDatetime(hoursAgo(3));
		visit.setStopDatetime(hoursAgo(1));
		visit.setEncounters(new LinkedHashSet<>());
		visit.addEncounter(newSampleEncounter(visit, location));
		return visit;
	}

	private static Location newSampleLocation() {
		Location location = new Location();
		location.setName(msg("facilityName", "Sample Health Centre"));
		location.setAddress1(msg("facilityAddress1", "12 Example Road"));
		location.setCityVillage(msg("facilityCity", "Sample Town"));
		location.setCountry(msg("facilityCountry", "Sample Country"));
		return location;
	}

	/**
	 * Names and identifiers are held in LinkedHashSets rather than added through
	 * addName()/addIdentifier(), whose sorted-set backing compares fields this transient
	 * graph deliberately leaves unset. Both entries are explicitly preferred and
	 * non-voided so getPersonName()/getPatientIdentifier() select them.
	 */
	private static Patient newSamplePatient(Location location) {
		Patient patient = new Patient();

		PersonName name = new PersonName(msg("patientGivenName", "Sample"), null,
				msg("patientFamilyName", "Patient"));
		name.setPreferred(true);
		name.setVoided(false);
		Set<PersonName> names = new LinkedHashSet<>();
		names.add(name);
		patient.setNames(names);

		PatientIdentifier identifier = new PatientIdentifier();
		identifier.setIdentifier(msg("patientIdentifier", "SAMPLE-0001"));
		identifier.setLocation(location);
		identifier.setPreferred(true);
		identifier.setVoided(false);
		Set<PatientIdentifier> identifiers = new LinkedHashSet<>();
		identifiers.add(identifier);
		patient.setIdentifiers(identifiers);

		patient.setBirthdate(yearsAgo(34));
		patient.setGender(msg("patientGender", "F"));
		return patient;
	}

	private static Encounter newSampleEncounter(Visit visit, Location location) {
		Encounter encounter = new Encounter();
		encounter.setVisit(visit);
		encounter.setPatient(visit.getPatient());
		encounter.setEncounterType(new EncounterType(msg("encounterType", "Consultation"), null));
		encounter.setEncounterDatetime(hoursAgo(2));
		encounter.setLocation(location);
		encounter.setVoided(false);
		return encounter;
	}

	/**
	 * Looks up sample display text by its key suffix under {@link #KEY_PREFIX} for the
	 * current locale, returning the fallback (not the raw key) when the key is missing.
	 */
	public static String msg(String keySuffix, String fallback) {
		return message(KEY_PREFIX + keySuffix, fallback);
	}

	/**
	 * Full-key variant of {@link #msg(String, String)}, for callers outside the sample
	 * key namespace such as the preview banner and the renderer's failure notices.
	 */
	public static String message(String key, String fallback) {
		try {
			return Context.getMessageSourceService().getMessage(key, null, fallback, Context.getLocale());
		}
		catch (Exception e) {
			log.warn("Message lookup failed for key '{}'; using fallback", key, e);
			return fallback;
		}
	}

	/** Sample dates are always computed relative to now so the preview never looks stale. */
	public static Date daysAgo(int days) {
		return shift(Calendar.DAY_OF_MONTH, -days);
	}

	public static Date hoursAgo(int hours) {
		return shift(Calendar.HOUR_OF_DAY, -hours);
	}

	public static Date yearsAgo(int years) {
		return shift(Calendar.YEAR, -years);
	}

	private static Date shift(int calendarField, int amount) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(calendarField, amount);
		return calendar.getTime();
	}
}
