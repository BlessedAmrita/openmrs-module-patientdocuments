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

import org.openmrs.Allergy;
import org.openmrs.AllergyReaction;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientdocuments.api.model.AllergyEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

// TODO: i18n — section heading currently hardcoded English; wire to MessageSourceService
// TODO: Add unit test for isEnabled() with mock global properties
@Component
@Order(500)
public class AllergiesSection extends TypedSection<List<AllergyEntry>> {

	private static final Logger log = LoggerFactory.getLogger(AllergiesSection.class);

	@Override
	public String getSectionKey() {
		return "allergies";
	}

	@Override
	protected List<AllergyEntry> gatherData(Visit visit) {
		List<AllergyEntry> allergies = new ArrayList<AllergyEntry>();

		try {
			for (Allergy allergy : Context.getPatientService().getAllergies(visit.getPatient())) {
				String allergenName = "";
				if (allergy.getAllergen() != null) {
					if (allergy.getAllergen().getCodedAllergen() != null
					        && allergy.getAllergen().getCodedAllergen().getName() != null) {
						allergenName = allergy.getAllergen().getCodedAllergen().getName().getName();
					} else if (allergy.getAllergen().getNonCodedAllergen() != null) {
						allergenName = allergy.getAllergen().getNonCodedAllergen();
					}
				}

				String severity = "";
				if (allergy.getSeverity() != null && allergy.getSeverity().getName() != null) {
					severity = allergy.getSeverity().getName().getName();
				}

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

				allergies.add(new AllergyEntry(allergenName, severity, reactions.toString()));
			}
		}
		catch (Exception e) {
			log.warn("Could not load allergies for patient; returning empty list", e);
		}

		return allergies;
	}

	@Override
	protected void renderXml(Document doc, Element root, List<AllergyEntry> allergies) {
		Element section = doc.createElement("allergies");
		section.setAttribute("heading", "Allergies");
		section.setAttribute("col-allergen", "Allergen");
		section.setAttribute("col-severity", "Severity");
		section.setAttribute("col-reactions", "Reactions");
		root.appendChild(section);

		for (AllergyEntry allergy : allergies) {
			Element allergyEl = doc.createElement("allergy");
			allergyEl.setAttribute("allergen", nvl(allergy.getAllergen()));
			allergyEl.setAttribute("severity", nvl(allergy.getSeverity()));
			allergyEl.setAttribute("reactions", nvl(allergy.getReactions()));
			section.appendChild(allergyEl);
		}
	}
}
