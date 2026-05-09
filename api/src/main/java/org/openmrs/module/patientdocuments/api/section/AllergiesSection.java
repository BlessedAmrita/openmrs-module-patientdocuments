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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.Allergy;
import org.openmrs.AllergyReaction;
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
@Order(4)
public class AllergiesSection extends AbstractVisitSummarySection {

	private static final Logger log = LoggerFactory.getLogger(AllergiesSection.class);

	@Override
	public String getSectionKey() {
		return "allergies";
	}

	@Override
	public Object getSectionData(Visit visit, Patient patient) {
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
}
