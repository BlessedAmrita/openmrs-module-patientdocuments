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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.GlobalProperty;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientdocuments.api.model.AllergyEntry;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.Arrays;
import java.util.List;

/**
 * Integration tests for AllergiesSection.
 *
 * Allergies are pre-loaded from allergiesSectionTestDataset.xml rather than created
 * programmatically to avoid triggering validators that NPE on new entities without
 * fully populated relationships.
 *
 * Allergies are patient-level (not visit-scoped): getAllergies(patient) returns ALL
 * allergies for the patient regardless of which visit is passed in.
 *
 * Dataset layout:
 *   Visit 301 — patient 2, who has 2 allergies:
 *     Allergy 301 — Penicillin (DRUG), severity Severe, reactions Rash + Nausea
 *     Allergy 302 — Peanuts (FOOD), severity Mild, reaction Rash
 *   Visit 302 — patient 7, who has no allergies
 */
public class AllergiesSectionTest extends BaseModuleContextSensitiveTest {

	private static final String DATASET =
	    "org/openmrs/module/patientdocuments/include/allergiesSectionTestDataset.xml";

	private AllergiesSection section;

	@BeforeEach
	public void setUp() throws Exception {
		executeDataSet(DATASET);
		section = new AllergiesSection();
	}

	// ── gatherData tests ──────────────────────────────────────────────────────

	@Test
	public void gatherData_patientWithAllergies_returnsAllAllergyEntries() {
		// Visit 301's patient (patient 2) has 2 allergies
		Visit visit = Context.getVisitService().getVisit(301);

		List<AllergyEntry> allergies = section.gatherData(visit);

		Assertions.assertEquals(2, allergies.size());
		AllergyEntry first = allergies.get(0);
		Assertions.assertEquals("Penicillin", first.getAllergen());
		Assertions.assertEquals("Severe", first.getSeverity());
		Assertions.assertEquals("Rash, Nausea", first.getReactions());
	}

	@Test
	public void gatherData_allergenWithNoResolvableName_returnsUnknown() {
		// Visit 303's patient (patient 8) has one allergy whose coded allergen has no concept name, so the allergen name cannot be resolved
		Visit visit = Context.getVisitService().getVisit(303);

		List<AllergyEntry> allergies = section.gatherData(visit);

		Assertions.assertEquals(1, allergies.size());
		Assertions.assertEquals("Unknown", allergies.get(0).getAllergen());
	}

	@Test
	public void gatherData_patientWithNoAllergies_returnsEmptyList() {
		// Visit 302's patient (patient 7) has no allergy records
		Visit visit = Context.getVisitService().getVisit(302);

		List<AllergyEntry> allergies = section.gatherData(visit);

		Assertions.assertEquals(0, allergies.size());
	}

	@Test
	public void gatherData_allergyWithMultipleReactions_joinsWithComma() {
		// Allergy 301 (Penicillin) has two reactions: Rash and Nausea
		Visit visit = Context.getVisitService().getVisit(301);

		List<AllergyEntry> allergies = section.gatherData(visit);

		AllergyEntry penicillin = allergies.stream()
		    .filter(a -> "Penicillin".equals(a.getAllergen())).findFirst().orElse(null);
		Assertions.assertNotNull(penicillin, "Expected Penicillin allergy entry");
		Assertions.assertEquals("Rash, Nausea", penicillin.getReactions());
	}

	// ── renderXml tests ───────────────────────────────────────────────────────

	@Test
	public void renderXml_withAllergies_producesCorrectXmlStructure() throws Exception {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = doc.createElement("root");
		doc.appendChild(root);
		List<AllergyEntry> entries = Arrays.asList(
		    new AllergyEntry("Penicillin", "Severe", "Rash, Nausea"),
		    new AllergyEntry("Peanuts", "Mild", "Rash")
		);

		section.renderXml(doc, root, entries);

		Assertions.assertEquals(1, root.getChildNodes().getLength());
		Element allergiesEl = (Element) root.getChildNodes().item(0);
		Assertions.assertEquals("allergies", allergiesEl.getNodeName());
		Assertions.assertEquals(2, allergiesEl.getChildNodes().getLength());

		Element first = (Element) allergiesEl.getChildNodes().item(0);
		Assertions.assertEquals("allergy", first.getNodeName());
		Assertions.assertEquals("Penicillin", first.getAttribute("allergen"));
		Assertions.assertEquals("Severe", first.getAttribute("severity"));
		Assertions.assertEquals("Rash, Nausea", first.getAttribute("reactions"));
	}

	// ── isEnabled tests ───────────────────────────────────────────────────────

	@Test
	public void isEnabled_whenGlobalPropertySetToFalse_returnsFalse() {
		GlobalProperty gp = new GlobalProperty(
		    "report.visitSummary.section.allergies.enabled", "false");
		Context.getAdministrationService().saveGlobalProperty(gp);

		Assertions.assertFalse(section.isEnabled());
	}
}
