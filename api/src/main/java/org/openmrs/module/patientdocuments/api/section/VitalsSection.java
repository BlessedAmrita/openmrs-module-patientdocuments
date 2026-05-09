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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
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
@Order(2)
public class VitalsSection extends AbstractVisitSummarySection {

	private static final Logger log = LoggerFactory.getLogger(VitalsSection.class);

	// CIEL default UUIDs — overrideable via global properties (report.visitSummary.vitals.*)
	private static final String DEFAULT_SYSTOLIC_UUID    = "5085AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String DEFAULT_DIASTOLIC_UUID   = "5086AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String DEFAULT_HEART_RATE_UUID  = "5087AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String DEFAULT_TEMPERATURE_UUID = "5088AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String DEFAULT_WEIGHT_UUID      = "5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String DEFAULT_HEIGHT_UUID      = "5090AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String DEFAULT_SPO2_UUID        = "5092AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

	@Override
	public String getSectionKey() {
		return "vitals";
	}

	@Override
	public Object getSectionData(Visit visit, Patient patient) {
		try {
			List<Map<String, String>> vitals = new ArrayList<Map<String, String>>();

			List<Encounter> encounters = new ArrayList<Encounter>();
			for (Encounter e : visit.getEncounters()) {
				if (!e.getVoided()) {
					encounters.add(e);
				}
			}
			if (encounters.isEmpty()) {
				return vitals;
			}

			// TODO: Confirm config key naming convention with mentor — currently using
			// "report.visitSummary.vitals.systolic" etc. for concept UUID overrides.
			// Enable/disable flags use "documents.section.<key>.enabled".
			// The two namespaces differ intentionally: UUIDs are deploy-time config,
			// enable flags are runtime-mutable. Validate this split is correct.
			String systolicUuid    = resolveConceptUuid("report.visitSummary.vitals.systolic",         DEFAULT_SYSTOLIC_UUID);
			String diastolicUuid   = resolveConceptUuid("report.visitSummary.vitals.diastolic",        DEFAULT_DIASTOLIC_UUID);
			String heartRateUuid   = resolveConceptUuid("report.visitSummary.vitals.heartRate",        DEFAULT_HEART_RATE_UUID);
			String temperatureUuid = resolveConceptUuid("report.visitSummary.vitals.temperature",      DEFAULT_TEMPERATURE_UUID);
			String weightUuid      = resolveConceptUuid("report.visitSummary.vitals.weight",           DEFAULT_WEIGHT_UUID);
			String heightUuid      = resolveConceptUuid("report.visitSummary.vitals.height",           DEFAULT_HEIGHT_UUID);
			String spo2Uuid        = resolveConceptUuid("report.visitSummary.vitals.oxygenSaturation", DEFAULT_SPO2_UUID);

			// Build list of available vital concepts for a single batch query
			List<Concept> vitalConcepts = new ArrayList<Concept>();
			String[] uuids = new String[] {
			    systolicUuid, diastolicUuid, heartRateUuid,
			    temperatureUuid, weightUuid, heightUuid, spo2Uuid
			};
			for (String uuid : uuids) {
				Concept c = Context.getConceptService().getConceptByUuid(uuid);
				if (c != null) {
					vitalConcepts.add(c);
				}
			}

			if (vitalConcepts.isEmpty()) {
				return vitals;
			}

			// Single batch query for all vital obs across all visit encounters
			List<Obs> obsList = Context.getObsService().getObservations(
			    null, encounters, vitalConcepts, null, null, null,
			    Collections.singletonList("obsDatetime desc"),
			    null, null, null, null, false
			);

			// Keep only the most-recent numeric value per concept UUID
			Map<String, Double> obsMap = new HashMap<String, Double>();
			for (Obs obs : obsList) {
				String uuid = obs.getConcept().getUuid();
				if (!obsMap.containsKey(uuid) && obs.getValueNumeric() != null) {
					obsMap.put(uuid, obs.getValueNumeric());
				}
			}

			// Blood Pressure (combined systolic/diastolic)
			if (obsMap.containsKey(systolicUuid) || obsMap.containsKey(diastolicUuid)) {
				String s = obsMap.containsKey(systolicUuid) ? formatNumeric(obsMap.get(systolicUuid)) : "?";
				String d = obsMap.containsKey(diastolicUuid) ? formatNumeric(obsMap.get(diastolicUuid)) : "?";
				vitals.add(vitalEntry("Blood Pressure", s + "/" + d + " mmHg"));
			}

			addVitalIfPresent(vitals, obsMap, heartRateUuid,   "Heart Rate",  "bpm");
			addVitalIfPresent(vitals, obsMap, temperatureUuid, "Temperature", "°C");
			addVitalIfPresent(vitals, obsMap, weightUuid,      "Weight",      "kg");
			addVitalIfPresent(vitals, obsMap, heightUuid,      "Height",      "cm");
			addVitalIfPresent(vitals, obsMap, spo2Uuid,        "SpO2",        "%");

			return vitals;
		} catch (Exception e) {
			log.warn("Could not load vitals for visit; returning empty list", e);
			return new ArrayList<>();
		}
	}

	private String resolveConceptUuid(String globalPropertyKey, String defaultUuid) {
		String val = Context.getAdministrationService().getGlobalProperty(globalPropertyKey, defaultUuid);
		return (val != null && !val.isEmpty()) ? val : defaultUuid;
	}

	private void addVitalIfPresent(List<Map<String, String>> vitals, Map<String, Double> obsMap,
	        String uuid, String label, String unit) {
		if (obsMap.containsKey(uuid)) {
			vitals.add(vitalEntry(label, formatNumeric(obsMap.get(uuid)) + " " + unit));
		}
	}

	private Map<String, String> vitalEntry(String label, String value) {
		Map<String, String> entry = new HashMap<String, String>();
		entry.put("label", label);
		entry.put("value", value);
		return entry;
	}

	private String formatNumeric(Double value) {
		if (value == null) {
			return "";
		}
		if (value == Math.floor(value) && !Double.isInfinite(value)) {
			return String.valueOf(value.intValue());
		}
		return String.format("%.1f", value);
	}
}
