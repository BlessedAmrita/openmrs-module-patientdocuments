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
import org.openmrs.ConceptNumeric;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientdocuments.api.model.Vital;
import org.openmrs.util.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component
public class VitalsSection extends TypedSection<List<Vital>> {

	private static final Logger log = LoggerFactory.getLogger(VitalsSection.class);

	/**
	 * Default comma-separated list of concept mappings (source:code) used when
	 * report.visitSummary.vitals.concepts is not set.
	 * CIEL codes: systolic BP, diastolic BP, heart rate, temperature,
	 *             weight, height, oxygen saturation, respiratory rate.
	 * Package-private for test access in VitalsSectionTest @BeforeEach cache reset.
	 */
	static final String DEFAULT_VITAL_CONCEPTS =
	    "CIEL:5085,CIEL:5086,CIEL:5087,CIEL:5088,CIEL:5089,CIEL:5090,CIEL:5092,CIEL:5242";

	private static final int DEFAULT_ORDER = 300;

	@Override
	public String getSectionKey() {
		return "vitals";
	}

	@Override
	protected int getDefaultOrder() {
		return DEFAULT_ORDER;
	}

	@Override
	protected List<Vital> gatherData(Visit visit) {
		List<Vital> vitals = new ArrayList<>();
		List<Encounter> encounters = getNonVoidedEncounters(visit);
		if (encounters.isEmpty()) {
			return vitals;
		}

		List<Concept> vitalConcepts = resolveVitalConcepts();
		if (vitalConcepts.isEmpty()) {
			return vitals;
		}

		Map<Integer, Double> obsMap = buildObsMap(encounters, vitalConcepts);
		emitVitals(vitals, vitalConcepts, obsMap);
		return vitals;
	}

	private Map<Integer, Double> buildObsMap(List<Encounter> encounters, List<Concept> vitalConcepts) {
		List<Obs> obsList = Context.getObsService().getObservations(
		    null, encounters, vitalConcepts, null, null, null,
		    Collections.singletonList("obsDatetime desc"),
		    null, null, null, null, false
		);
		Map<Integer, Double> obsMap = new HashMap<>();
		for (Obs obs : obsList) {
			Integer conceptId = obs.getConcept().getConceptId();
			if (!obsMap.containsKey(conceptId) && obs.getValueNumeric() != null) {
				obsMap.put(conceptId, obs.getValueNumeric());
			}
		}
		return obsMap;
	}

	private void emitVitals(List<Vital> vitals, List<Concept> vitalConcepts, Map<Integer, Double> obsMap) {
		Double systolicValue = null;
		Double diastolicValue = null;
		for (Concept concept : vitalConcepts) {
			if (isCielCode(concept, "5085")) {
				systolicValue = obsMap.get(concept.getConceptId());
			} else if (isCielCode(concept, "5086")) {
				diastolicValue = obsMap.get(concept.getConceptId());
			}
		}

		boolean hasBpData = systolicValue != null || diastolicValue != null;
		boolean bpEmitted = false;
		for (Concept concept : vitalConcepts) {
			if (isBpComponent(concept)) {
				if (!bpEmitted && hasBpData) {
					vitals.add(buildBpVital(systolicValue, diastolicValue));
					bpEmitted = true;
				}
			} else if (obsMap.containsKey(concept.getConceptId())) {
				vitals.add(buildVital(concept, obsMap));
			}
		}
	}

	private boolean isBpComponent(Concept concept) {
		return isCielCode(concept, "5085") || isCielCode(concept, "5086");
	}

	private Vital buildBpVital(Double systolicValue, Double diastolicValue) {
		String s = systolicValue != null ? formatNumeric(systolicValue) : "?";
		String d = diastolicValue != null ? formatNumeric(diastolicValue) : "?";
		return new Vital("Blood Pressure", s + "/" + d + " mmHg");
	}

	private Vital buildVital(Concept concept, Map<Integer, Double> obsMap) {
		String label = concept.getName() != null ? concept.getName().getName() : "";
		String value = formatNumeric(obsMap.get(concept.getConceptId()));
		String unit = "";
		if (concept instanceof ConceptNumeric) {
			String conceptUnit = ((ConceptNumeric) concept).getUnits();
			if (conceptUnit != null && !conceptUnit.isEmpty()) {
				unit = " " + conceptUnit;
			}
		}
		return new Vital(label, value + unit);
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

	/**
	 * Resolves the ordered list of vital concepts from
	 * report.visitSummary.vitals.concepts (defaults to DEFAULT_VITAL_CONCEPTS).
	 * Each entry must be in "source:code" format. Entries that cannot be parsed
	 * or resolved are logged and skipped rather than crashing the PDF.
	 */
	private List<Concept> resolveVitalConcepts() {
		String raw = ConfigUtil.getProperty("report.visitSummary.vitals.concepts", DEFAULT_VITAL_CONCEPTS);
		List<Concept> concepts = new ArrayList<>();
		for (String entry : raw.split(",")) {
			entry = entry.trim();
			String[] parts = entry.split(":");
			if (parts.length != 2) {
				log.warn("Vitals concept entry '{}' is not in source:code format; skipping", entry);
			} else {
				String source = parts[0].trim();
				String code = parts[1].trim();
				Concept concept = Context.getConceptService().getConceptByMapping(code, source);
				if (concept == null) {
					log.warn("Concept mapping {}:{} could not be resolved; skipping", source, code);
				} else {
					concepts.add(concept);
				}
			}
		}
		return concepts;
	}

	/**
	 * Returns true if the given concept has a SAME-AS mapping to the named CIEL code.
	 * Used to identify systolic (5085) and diastolic (5086) for combined BP display,
	 * regardless of which source:code format the caller placed in the GP list.
	 */
	private boolean isCielCode(Concept concept, String cielCode) {
		return concept.getConceptMappings().stream().anyMatch(mapping ->
		    "CIEL".equals(mapping.getConceptReferenceTerm().getConceptSource().getName())
		    && cielCode.equals(mapping.getConceptReferenceTerm().getCode()));
	}

	private List<Encounter> getNonVoidedEncounters(Visit visit) {
		List<Encounter> encounters = new ArrayList<>();
		for (Encounter e : visit.getEncounters()) {
			if (!Boolean.TRUE.equals(e.getVoided())) {
				encounters.add(e);
			}
		}
		return encounters;
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
