/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientdocuments.library;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.Allergy;
import org.openmrs.AllergyReaction;
import org.openmrs.Concept;
import org.openmrs.Diagnosis;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonName;
import org.openmrs.Visit;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.initializer.api.InitializerService;
import org.openmrs.module.patientdocuments.api.VisitSummarySectionProvider;
import org.openmrs.module.reporting.dataset.DataSet;
import org.openmrs.module.reporting.dataset.DataSetColumn;
import org.openmrs.module.reporting.dataset.DataSetRow;
import org.openmrs.module.reporting.dataset.SimpleDataSet;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.evaluator.DataSetEvaluator;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evaluates a {@link VisitSummaryDataSetDefinition} and returns clinical data for the visit
 * as a Map stored directly in the DataSet row (no JSON serialization).
 */
@Component
@Handler(supports = VisitSummaryDataSetDefinition.class, order = 50)
public class VisitSummaryDataSetEvaluator implements DataSetEvaluator {

	// Vitals concept UUIDs (CIEL)
	private static final String SYSTOLIC_UUID  = "5085AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String DIASTOLIC_UUID = "5086AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String HEART_RATE_UUID = "5087AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String TEMPERATURE_UUID = "5088AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String WEIGHT_UUID    = "5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String HEIGHT_UUID    = "5090AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String SPO2_UUID      = "5092AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

	private static final Logger log = LoggerFactory.getLogger(VisitSummaryDataSetEvaluator.class);

	@Autowired(required = false)
	private List<VisitSummarySectionProvider> sectionProviders;

	@Override
	public DataSet evaluate(DataSetDefinition dataSetDefinition, EvaluationContext evalContext) {
		SimpleDataSet dataSet = new SimpleDataSet(dataSetDefinition, evalContext);

		String visitUuid = (String) evalContext.getParameterValue("visitUuid");
		if (visitUuid == null) {
			return dataSet;
		}

		Visit visit = Context.getVisitService().getVisitByUuid(visitUuid);
		if (visit == null) {
			return dataSet;
		}

		Patient patient = visit.getPatient();
		Map<String, Object> visitData = buildVisitData(visit, patient);

		DataSetRow row = new DataSetRow();
		row.addColumnValue(new DataSetColumn("visitData", "Visit Data", Map.class), visitData);
		dataSet.addRow(row);

		return dataSet;
	}

	private Map<String, Object> buildVisitData(Visit visit, Patient patient) {
		Map<String, Object> data = new HashMap<String, Object>();

		data.put("facilityHeader", buildFacilityHeader(visit));
		data.put("patientInfo", buildPatientInfo(visit, patient));
		data.put("vitals", buildVitals(visit));
		data.put("diagnoses", buildDiagnoses(visit, patient));
		data.put("allergies", buildAllergies(patient));

		// Extensible section providers registered by other modules
		if (sectionProviders != null) {
			InitializerService initializerService = Context.getService(InitializerService.class);
			for (VisitSummarySectionProvider provider : sectionProviders) {
				String key = provider.getSectionKey();
				Boolean configVal = initializerService.getBooleanFromKey("report.visitSummary.section." + key);
				boolean enabled = (configVal != null) ? configVal : provider.isEnabledByDefault();
				if (enabled) {
					data.put(key, provider.getSectionData(visit, patient));
				}
			}
		}

		return data;
	}

	private Map<String, String> buildFacilityHeader(Visit visit) {
		Map<String, String> header = new HashMap<String, String>();
		header.put("facilityName", visit.getLocation() != null ? visit.getLocation().getName() : "");
		header.put("facilityAddress", "");
		header.put("facilityPhone", "");
		return header;
	}

	private Map<String, String> buildPatientInfo(Visit visit, Patient patient) {
		Map<String, String> info = new HashMap<String, String>();

		PersonName name = patient.getPersonName();
		info.put("patientName", name != null ? name.getFullName() : "");

		Date dob = patient.getBirthdate();
		info.put("dateOfBirth", dob != null ? formatDate(dob) : "");
		info.put("gender", patient.getGender() != null ? patient.getGender() : "");

		PatientIdentifier preferredId = patient.getPatientIdentifier();
		info.put("patientId", preferredId != null ? preferredId.getIdentifier() : "");

		info.put("visitDate", visit.getStartDatetime() != null ? formatDate(visit.getStartDatetime()) : "");
		info.put("visitType", visit.getVisitType() != null ? visit.getVisitType().getName() : "");
		info.put("visitLocation", visit.getLocation() != null ? visit.getLocation().getName() : "");
		info.put("visitStopDate", visit.getStopDatetime() != null ? formatDate(visit.getStopDatetime()) : "");

		return info;
	}

	private List<Map<String, String>> buildVitals(Visit visit) {
		List<Map<String, String>> vitals = new ArrayList<Map<String, String>>();

		List<Encounter> encounters = new ArrayList<Encounter>(visit.getEncounters());
		if (encounters.isEmpty()) {
			return vitals;
		}

		// Build list of available vital concepts for a single batch query
		List<Concept> vitalConcepts = new ArrayList<Concept>();
		String[] uuids = new String[]{
			SYSTOLIC_UUID, DIASTOLIC_UUID, HEART_RATE_UUID,
			TEMPERATURE_UUID, WEIGHT_UUID, HEIGHT_UUID, SPO2_UUID
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
			null, encounters, vitalConcepts, null, null, null, null, null, null, null, null, false
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
		if (obsMap.containsKey(SYSTOLIC_UUID) || obsMap.containsKey(DIASTOLIC_UUID)) {
			String s = obsMap.containsKey(SYSTOLIC_UUID) ? formatNumeric(obsMap.get(SYSTOLIC_UUID)) : "?";
			String d = obsMap.containsKey(DIASTOLIC_UUID) ? formatNumeric(obsMap.get(DIASTOLIC_UUID)) : "?";
			vitals.add(vitalEntry("Blood Pressure", s + "/" + d + " mmHg"));
		}

		addVitalIfPresent(vitals, obsMap, HEART_RATE_UUID, "Heart Rate", "bpm");
		addVitalIfPresent(vitals, obsMap, TEMPERATURE_UUID, "Temperature", "\u00b0C");
		addVitalIfPresent(vitals, obsMap, WEIGHT_UUID, "Weight", "kg");
		addVitalIfPresent(vitals, obsMap, HEIGHT_UUID, "Height", "cm");
		addVitalIfPresent(vitals, obsMap, SPO2_UUID, "SpO2", "%");

		return vitals;
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

	private List<Map<String, String>> buildDiagnoses(Visit visit, Patient patient) {
		List<Map<String, String>> diagnoses = new ArrayList<Map<String, String>>();

		try {
			// getDiagnoses(Patient, Date) returns diagnoses since the given date;
			// using visit start as a proxy to scope results to this visit
			List<Diagnosis> diagnosisList = Context.getDiagnosisService()
			        .getDiagnoses(patient, visit.getStartDatetime());
			if (diagnosisList == null) {
				return diagnoses;
			}
			for (Diagnosis diagnosis : diagnosisList) {
				Map<String, String> entry = new HashMap<String, String>();

				String name = "";
				if (diagnosis.getDiagnosis() != null) {
					if (diagnosis.getDiagnosis().getCoded() != null
					        && diagnosis.getDiagnosis().getCoded().getName() != null) {
						name = diagnosis.getDiagnosis().getCoded().getName().getName();
					} else if (diagnosis.getDiagnosis().getNonCoded() != null) {
						name = diagnosis.getDiagnosis().getNonCoded();
					}
				}
				entry.put("name", name);
				entry.put("certainty", diagnosis.getCertainty() != null ? diagnosis.getCertainty().name() : "");
				entry.put("rank", diagnosis.getRank() != null ? String.valueOf(diagnosis.getRank()) : "");
				diagnoses.add(entry);
			}
		}
		catch (Exception e) {
			log.warn("Could not load diagnoses for visit; returning empty list", e);
		}

		return diagnoses;
	}

	private List<Map<String, String>> buildAllergies(Patient patient) {
		List<Map<String, String>> allergies = new ArrayList<Map<String, String>>();

		try {
			for (Allergy allergy : Context.getPatientService().getAllergies(patient)) {
				Map<String, String> entry = new HashMap<String, String>();

				String allergenName = "";
				if (allergy.getAllergen() != null) {
					if (allergy.getAllergen().getCodedAllergen() != null
					        && allergy.getAllergen().getCodedAllergen().getName() != null) {
						allergenName = allergy.getAllergen().getCodedAllergen().getName().getName();
					} else if (allergy.getAllergen().getNonCodedAllergen() != null) {
						allergenName = allergy.getAllergen().getNonCodedAllergen();
					}
				}
				entry.put("allergen", allergenName);

				String severity = "";
				if (allergy.getSeverity() != null && allergy.getSeverity().getName() != null) {
					severity = allergy.getSeverity().getName().getName();
				}
				entry.put("severity", severity);

				StringBuilder reactions = new StringBuilder();
				if (allergy.getReactions() != null) {
					for (AllergyReaction reaction : allergy.getReactions()) {
						if (reactions.length() > 0) {
							reactions.append(", ");
						}
						if (reaction.getReaction() != null && reaction.getReaction().getName() != null) {
							reactions.append(reaction.getReaction().getName().getName());
						} else if (reaction.getReactionNonCoded() != null) {
							reactions.append(reaction.getReactionNonCoded());
						}
					}
				}
				entry.put("reactions", reactions.toString());
				allergies.add(entry);
			}
		}
		catch (Exception e) {
			log.warn("Could not load allergies for patient; returning empty list", e);
		}

		return allergies;
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

	private String formatDate(Date date) {
		if (date == null) {
			return "";
		}
		return new SimpleDateFormat("yyyy-MM-dd").format(date);
	}
}
