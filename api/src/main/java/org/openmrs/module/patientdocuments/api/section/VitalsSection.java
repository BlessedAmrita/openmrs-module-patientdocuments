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
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientdocuments.api.model.Vital;
import org.openmrs.module.patientdocuments.common.CielConceptConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component
@Order(300)
public class VitalsSection extends TypedSection<List<Vital>> {

	private static final Logger log = LoggerFactory.getLogger(VitalsSection.class);

	@Override
	public String getSectionKey() {
		return "vitals";
	}

	@Override
	protected List<Vital> gatherData(Visit visit) {
		try {
			List<Vital> vitals = new ArrayList<Vital>();

			List<Encounter> encounters = new ArrayList<Encounter>();
			for (Encounter e : visit.getEncounters()) {
				if (!e.getVoided()) {
					encounters.add(e);
				}
			}
			if (encounters.isEmpty()) {
				return vitals;
			}

			String systolicUuid    = resolveConceptUuid("report.visitSummary.vitals.systolic", CielConceptConstants.SYSTOLIC_BP);
			String diastolicUuid   = resolveConceptUuid("report.visitSummary.vitals.diastolic", CielConceptConstants.DIASTOLIC_BP);
			String heartRateUuid   = resolveConceptUuid("report.visitSummary.vitals.heartRate", CielConceptConstants.HEART_RATE);
			String temperatureUuid = resolveConceptUuid("report.visitSummary.vitals.temperature", CielConceptConstants.TEMPERATURE);
			String weightUuid      = resolveConceptUuid("report.visitSummary.vitals.weight", CielConceptConstants.WEIGHT);
			String heightUuid      = resolveConceptUuid("report.visitSummary.vitals.height", CielConceptConstants.HEIGHT);
			String spo2Uuid        = resolveConceptUuid("report.visitSummary.vitals.oxygenSaturation", CielConceptConstants.OXYGEN_SAT);

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
				vitals.add(new Vital("Blood Pressure", s + "/" + d + " mmHg"));
			}

			addVitalIfPresent(vitals, obsMap, heartRateUuid,   "Heart Rate",  "bpm");
			addVitalIfPresent(vitals, obsMap, temperatureUuid, "Temperature", "°C");
			addVitalIfPresent(vitals, obsMap, weightUuid,      "Weight",      "kg");
			addVitalIfPresent(vitals, obsMap, heightUuid,      "Height",      "cm");
			addVitalIfPresent(vitals, obsMap, spo2Uuid,        "SpO2",        "%");

			return vitals;
		}
		catch (Exception e) {
			log.warn("Could not load vitals for visit; returning empty list", e);
			return new ArrayList<Vital>();
		}
	}

	@Override
	protected void renderXml(Document doc, Element root, List<Vital> vitals) {
		Element section = doc.createElement("vitals");
		section.setAttribute("heading", "Vital Signs");
		root.appendChild(section);

		for (Vital vital : vitals) {
			Element vitalEl = doc.createElement("vital");
			vitalEl.setAttribute("label", nvl(vital.getLabel()));
			vitalEl.setAttribute("value", nvl(vital.getValue()));
			section.appendChild(vitalEl);
		}
	}

	private String resolveConceptUuid(String globalPropertyKey, String defaultUuid) {
		String val = Context.getAdministrationService().getGlobalProperty(globalPropertyKey, defaultUuid);
		return (val != null && !val.isEmpty()) ? val : defaultUuid;
	}

	private void addVitalIfPresent(List<Vital> vitals, Map<String, Double> obsMap,
	        String uuid, String label, String unit) {
		if (obsMap.containsKey(uuid)) {
			vitals.add(new Vital(label, formatNumeric(obsMap.get(uuid)) + " " + unit));
		}
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
