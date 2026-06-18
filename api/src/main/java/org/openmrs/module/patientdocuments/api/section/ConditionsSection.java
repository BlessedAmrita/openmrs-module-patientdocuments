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
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.openmrs.CodedOrFreeText;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.Condition;
import org.openmrs.ConditionVerificationStatus;
import org.openmrs.Visit;
import org.openmrs.api.ConditionService;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientdocuments.api.model.ConditionEntry;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component
@Slf4j
public class ConditionsSection extends TypedSection<List<ConditionEntry>> {

	private static final int DEFAULT_ORDER = 450;

	@Override
	protected int getDefaultOrder() {
		return DEFAULT_ORDER;
	}

	@Override
	public String getSectionKey() {
		return "conditions";
	}

	@Override
	protected List<ConditionEntry> gatherData(Visit visit) {
		List<ConditionEntry> conditions = new ArrayList<>();
		for (Condition condition : Context.getService(ConditionService.class)
				.getActiveConditions(visit.getPatient())) {
			conditions.add(new ConditionEntry(
			    extractName(condition),
			    formatDate(condition.getOnsetDate()),
			    extractVerificationStatus(condition)
			));
		}
		return conditions;
	}

	private String extractName(Condition condition) {
		CodedOrFreeText codedOrFreeText = condition.getCondition();
		if (codedOrFreeText == null) {
			log.warn("Condition {} has null coded-or-free-text wrapper",
				condition.getUuid());
			return "Unknown";
		}
		Concept coded = codedOrFreeText.getCoded();
		if (coded != null) {
			ConceptName conceptName = coded.getName();
			if (conceptName != null && conceptName.getName() != null) {
				return conceptName.getName();
			}
		}
		if (codedOrFreeText.getNonCoded() != null) {
			return codedOrFreeText.getNonCoded();
		}
		log.warn("Condition {} has coded-or-free-text wrapper but "
			+ "neither coded concept name nor non-coded text resolved",
			condition.getUuid());
		return "Unknown";
	}

	private String extractVerificationStatus(Condition condition) {
		ConditionVerificationStatus status = condition.getVerificationStatus();
		if (status == null) {
			return "";
		}
		return status.name();
	}

	@Override
	protected void renderXml(Document doc, Element root, List<ConditionEntry> conditions) {
		Element section = doc.createElement("conditions");
		section.setAttribute("heading", "Conditions");
		section.setAttribute("col-name", "Condition");
		section.setAttribute("col-onset", "Onset Date");
		section.setAttribute("col-verification", "Verification Status");
		root.appendChild(section);

		for (ConditionEntry condition : conditions) {
			Element conditionEl = doc.createElement("condition");
			conditionEl.setAttribute("name", nvl(condition.getName()));
			conditionEl.setAttribute("onset", nvl(condition.getOnsetDate()));
			conditionEl.setAttribute("verification", nvl(condition.getVerificationStatus()));
			section.appendChild(conditionEl);
		}
	}
}
