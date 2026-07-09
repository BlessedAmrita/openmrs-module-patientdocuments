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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptNumeric;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.ObsReferenceRange;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientdocuments.api.model.LabResult;
import org.openmrs.module.patientdocuments.api.model.LabResultGroup;
import org.openmrs.module.patientdocuments.api.model.LabResultsData;
import org.openmrs.util.ConfigUtil;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component
@Slf4j
public class LabResultsSection extends TypedSection<LabResultsData> {

	// Concept-class names that mark an obs as a lab result. Matches the O3 Results page
	// (FHIR "laboratory" category -> "Test" class; panels are "LabSet"); vitals are "Finding".
	static final String DEFAULT_LAB_CONCEPT_CLASSES = "Test,LabSet";

	static final String CONCEPT_CLASSES_PROPERTY = "report.visitSummary.labResults.conceptClasses";

	private static final int DEFAULT_ORDER = 475;

	private static final String KEY_PREFIX = "patientdocuments.visitSummary.section.labResults.";

	private static final String HEADING = "heading";

	@Override
	public String getSectionKey() {
		return "labResults";
	}

	@Override
	protected int getDefaultOrder() {
		return DEFAULT_ORDER;
	}

	@Override
	protected LabResultsData gatherData(Visit visit) {
		List<LabResultGroup> groups = new ArrayList<>();
		List<LabResult> standalone = new ArrayList<>();

		List<Encounter> encounters = getNonVoidedEncounters(visit);
		if (encounters.isEmpty()) {
			return new LabResultsData(groups, standalone);
		}

		Set<Integer> labClassIds = resolveLabConceptClassIds();

		List<Obs> obsList = Context.getObsService().getObservations(
		    null, encounters, null, null, null, null,
		    Collections.singletonList("obsDatetime desc"),
		    null, null, null, null, false
		);

		// Panels first; their members are tracked so the standalone pass doesn't re-render them.
		Set<Integer> renderedMemberObsIds = new HashSet<>();
		for (Obs obs : obsList) {
			if (obs.isObsGrouping() && isLabConcept(obs, labClassIds)) {
				LabResultGroup group = buildGroup(obs, renderedMemberObsIds);
				if (group != null) {
					groups.add(group);
				}
			}
		}

		for (Obs obs : obsList) {
			if (obs.isObsGrouping() || renderedMemberObsIds.contains(obs.getObsId())) {
				continue;
			}
			if (isLabConcept(obs, labClassIds)) {
				standalone.add(buildResult(obs));
			}
		}

		return new LabResultsData(groups, standalone);
	}

	private LabResultGroup buildGroup(Obs groupObs, Set<Integer> renderedMemberObsIds) {
		Set<Obs> members = groupObs.getGroupMembers();
		if (members == null || members.isEmpty()) {
			return null;
		}
		List<Obs> visibleMembers = new ArrayList<>();
		for (Obs member : members) {
			if (!Boolean.TRUE.equals(member.getVoided())) {
				visibleMembers.add(member);
			}
		}
		if (visibleMembers.isEmpty()) {
			return null;
		}
		visibleMembers.sort(Comparator.comparing(Obs::getObsId));
		List<LabResult> results = new ArrayList<>();
		for (Obs member : visibleMembers) {
			renderedMemberObsIds.add(member.getObsId());
			results.add(buildResult(member));
		}
		return new LabResultGroup(conceptName(groupObs.getConcept()), results);
	}

	private boolean isLabConcept(Obs obs, Set<Integer> labClassIds) {
		Concept concept = obs.getConcept();
		if (concept == null || concept.getConceptClass() == null) {
			return false;
		}
		return labClassIds.contains(concept.getConceptClass().getConceptClassId());
	}

	private LabResult buildResult(Obs obs) {
		Concept concept = obs.getConcept();
		String name = conceptName(concept);
		ConceptDatatype datatype = concept != null ? concept.getDatatype() : null;

		String value;
		String units = "";
		String range = "";
		if (datatype != null && datatype.isNumeric()) {
			value = formatNumeric(obs.getValueNumeric());
			// re-fetch as ConceptNumeric — obs.getConcept() is a lazy proxy, so instanceof fails
			ConceptNumeric numericConcept = Context.getConceptService().getConceptNumeric(concept.getConceptId());
			if (numericConcept != null) {
				units = StringUtils.defaultString(numericConcept.getUnits());
			}
			range = formatRange(obs, numericConcept, units);
		} else if (datatype != null && datatype.isCoded()) {
			value = codedValue(obs);
		} else {
			value = textValue(obs);
		}

		String flag = formatInterpretation(obs.getInterpretation());
		return new LabResult(name, value, units, range, flag);
	}

	private String codedValue(Obs obs) {
		if (obs.getValueCoded() != null) {
			String coded = conceptName(obs.getValueCoded());
			if (!coded.isEmpty()) {
				return coded;
			}
		}
		return StringUtils.defaultString(obs.getValueAsString(Context.getLocale()));
	}

	private String textValue(Obs obs) {
		if (obs.getValueText() != null) {
			return obs.getValueText();
		}
		return StringUtils.defaultString(obs.getValueAsString(Context.getLocale()));
	}

	private String conceptName(Concept concept) {
		if (concept != null && concept.getName() != null && concept.getName().getName() != null) {
			return concept.getName().getName();
		}
		return "";
	}

	// Prefers the per-obs snapshot (2.7+), falls back to ConceptNumeric bounds; "" when neither is set.
	private String formatRange(Obs obs, ConceptNumeric numericConcept, String units) {
		Double low = null;
		Double high = null;
		ObsReferenceRange snapshot = obs.getReferenceRange();
		if (snapshot != null && (snapshot.getLowNormal() != null || snapshot.getHiNormal() != null)) {
			low = snapshot.getLowNormal();
			high = snapshot.getHiNormal();
		} else if (numericConcept != null) {
			low = numericConcept.getLowNormal();
			high = numericConcept.getHiNormal();
		}
		if (low == null && high == null) {
			return "";
		}
		String core;
		if (low != null && high != null) {
			core = formatNumeric(low) + " – " + formatNumeric(high);
		} else if (low != null) {
			core = "≥ " + formatNumeric(low);
		} else {
			core = "≤ " + formatNumeric(high);
		}
		return units.isEmpty() ? core : core + " " + units;
	}

	// Renders the stored interpretation as a flag; the flag is read, never computed. NORMAL/null -> no flag.
	private String formatInterpretation(Obs.Interpretation interpretation) {
		if (interpretation == null || interpretation == Obs.Interpretation.NORMAL) {
			return "";
		}
		String name = interpretation.name();
		return msg(KEY_PREFIX + "flag." + name.toLowerCase(), formatEnumName(name));
	}

	// Throws if none of the configured class names resolve (so a wholly invalid config surfaces as an error banner).
	private Set<Integer> resolveLabConceptClassIds() {
		String raw = ConfigUtil.getProperty(CONCEPT_CLASSES_PROPERTY, DEFAULT_LAB_CONCEPT_CLASSES);
		Set<Integer> classIds = new LinkedHashSet<>();
		for (String entry : raw.split(",")) {
			String name = entry.trim();
			if (name.isEmpty()) {
				continue;
			}
			ConceptClass conceptClass = Context.getConceptService().getConceptClassByName(name);
			if (conceptClass == null) {
				log.warn("Lab-result concept class '{}' could not be resolved; skipping", name);
			} else {
				classIds.add(conceptClass.getConceptClassId());
			}
		}
		if (classIds.isEmpty()) {
			throw new IllegalStateException(
			    "None of the configured lab-result concept classes could be resolved; check " + CONCEPT_CLASSES_PROPERTY);
		}
		return classIds;
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

	// Keeps the stored precision and strips trailing zeros; toPlainString avoids scientific notation.
	private String formatNumeric(Double value) {
		if (value == null) {
			return "";
		}
		return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
	}

	@Override
	protected void renderXml(Document doc, Element root, LabResultsData data) {
		Element section = doc.createElement("labResults");
		section.setAttribute(HEADING, msg(KEY_PREFIX + HEADING, "Lab Results"));
		section.setAttribute("col-test", msg(KEY_PREFIX + "col.test", "Test"));
		section.setAttribute("col-result", msg(KEY_PREFIX + "col.result", "Result"));
		section.setAttribute("col-range", msg(KEY_PREFIX + "col.range", "Reference Range"));
		section.setAttribute("col-flag", msg(KEY_PREFIX + "col.flag", "Flag"));
		root.appendChild(section);

		for (LabResultGroup group : data.getGroups()) {
			Element groupEl = doc.createElement("lab-group");
			groupEl.setAttribute(HEADING, StringUtils.defaultString(group.getHeading()));
			for (LabResult result : group.getResults()) {
				groupEl.appendChild(buildLabElement(doc, result));
			}
			section.appendChild(groupEl);
		}
		for (LabResult result : data.getStandalone()) {
			section.appendChild(buildLabElement(doc, result));
		}
	}

	private Element buildLabElement(Document doc, LabResult result) {
		Element labEl = doc.createElement("lab");
		labEl.setAttribute("name", StringUtils.defaultString(result.getName()));
		labEl.setAttribute("value", StringUtils.defaultString(result.getValue()));
		labEl.setAttribute("units", StringUtils.defaultString(result.getUnits()));
		labEl.setAttribute("range", StringUtils.defaultString(result.getRange()));
		labEl.setAttribute("flag", StringUtils.defaultString(result.getFlag()));
		return labEl;
	}
}
