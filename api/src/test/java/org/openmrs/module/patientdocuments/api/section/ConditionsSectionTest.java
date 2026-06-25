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
import org.openmrs.module.patientdocuments.api.model.ConditionEntry;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.Arrays;
import java.util.List;

/**
 * Integration tests for ConditionsSection.
 *
 * Conditions are pre-loaded from conditionsSectionTestDataset.xml rather than created
 * programmatically to avoid triggering validators that NPE on new entities without
 * fully populated relationships.
 *
 * Conditions are patient-level (not visit-scoped): getActiveConditions(patient) returns
 * ALL active conditions for the patient regardless of which visit is passed in.
 *
 * Dataset layout:
 *   Visit 401 — patient 2, who has 3 conditions:
 *     Condition 401 — Type 2 Diabetes Mellitus (coded), ACTIVE, CONFIRMED, onset 2024-06-15
 *     Condition 402 — "Seasonal Allergic Rhinitis" (non-coded), ACTIVE, PROVISIONAL, no onset
 *     Condition 403 — Chronic Sinusitis (coded), INACTIVE, CONFIRMED (must be excluded)
 *   Visit 402 — patient 7, who has no conditions
 */
public class ConditionsSectionTest extends BaseModuleContextSensitiveTest {

	private static final String DATASET =
	    "org/openmrs/module/patientdocuments/include/conditionsSectionTestDataset.xml";

	private ConditionsSection section;

	@BeforeEach
	public void setUp() throws Exception {
		executeDataSet(DATASET);
		section = new ConditionsSection();
	}

	// ── gatherData tests ──────────────────────────────────────────────────────

	@Test
	public void gatherData_patientWithActiveConditions_returnsOnlyActiveConditionEntries() {
		// Visit 401's patient (patient 2) has 2 ACTIVE conditions + 1 INACTIVE condition
		Visit visit = Context.getVisitService().getVisit(401);

		List<ConditionEntry> conditions = section.gatherData(visit);

		Assertions.assertEquals(2, conditions.size());

		boolean hasDiabetes = conditions.stream()
		    .anyMatch(c -> "Type 2 Diabetes Mellitus".equals(c.getName()));
		Assertions.assertTrue(hasDiabetes, "Expected Type 2 Diabetes Mellitus in active conditions");

		boolean hasRhinitis = conditions.stream()
		    .anyMatch(c -> "Seasonal Allergic Rhinitis".equals(c.getName()));
		Assertions.assertTrue(hasRhinitis, "Expected Seasonal Allergic Rhinitis in active conditions");

		boolean hasInactive = conditions.stream()
		    .anyMatch(c -> "Chronic Sinusitis".equals(c.getName()));
		Assertions.assertFalse(hasInactive, "INACTIVE condition must not appear in active conditions");
	}

	@Test
	public void gatherData_codedConditionWithOnsetDate_returnsFormattedDate() {
		Visit visit = Context.getVisitService().getVisit(401);

		List<ConditionEntry> conditions = section.gatherData(visit);

		ConditionEntry diabetes = conditions.stream()
		    .filter(c -> "Type 2 Diabetes Mellitus".equals(c.getName())).findFirst().orElse(null);
		Assertions.assertNotNull(diabetes, "Expected Type 2 Diabetes Mellitus condition entry");
		Assertions.assertEquals("2024-06-15", diabetes.getOnsetDate());
	}

	@Test
	public void gatherData_nonCodedConditionWithNoOnsetDate_returnsFreeTextNameAndEmptyDate() {
		Visit visit = Context.getVisitService().getVisit(401);

		List<ConditionEntry> conditions = section.gatherData(visit);

		ConditionEntry rhinitis = conditions.stream()
		    .filter(c -> "Seasonal Allergic Rhinitis".equals(c.getName())).findFirst().orElse(null);
		Assertions.assertNotNull(rhinitis, "Expected Seasonal Allergic Rhinitis condition entry");
		Assertions.assertEquals("", rhinitis.getOnsetDate());
	}

	@Test
	public void gatherData_patientWithNoConditions_returnsEmptyList() {
		// Visit 402's patient (patient 7) has no condition records
		Visit visit = Context.getVisitService().getVisit(402);

		List<ConditionEntry> conditions = section.gatherData(visit);

		Assertions.assertEquals(0, conditions.size());
	}

	// ── renderXml tests ───────────────────────────────────────────────────────

	@Test
	public void renderXml_withConditions_producesCorrectXmlStructure() throws Exception {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = doc.createElement("root");
		doc.appendChild(root);
		List<ConditionEntry> entries = Arrays.asList(
		    new ConditionEntry("Type 2 Diabetes Mellitus", "2024-06-15"),
		    new ConditionEntry("Seasonal Allergic Rhinitis", "")
		);

		section.renderXml(doc, root, entries);

		Assertions.assertEquals(1, root.getChildNodes().getLength());
		Element conditionsEl = (Element) root.getChildNodes().item(0);
		Assertions.assertEquals("conditions", conditionsEl.getNodeName());
		Assertions.assertEquals(2, conditionsEl.getChildNodes().getLength());

		Element first = (Element) conditionsEl.getChildNodes().item(0);
		Assertions.assertEquals("condition", first.getNodeName());
		Assertions.assertEquals("Type 2 Diabetes Mellitus", first.getAttribute("name"));
		Assertions.assertEquals("2024-06-15", first.getAttribute("onset"));
	}

	@Test
	public void renderXml_withEmptyList_producesSectionElementWithNoChildren() throws Exception {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = doc.createElement("root");
		doc.appendChild(root);

		section.renderXml(doc, root, Arrays.asList());

		Assertions.assertEquals(1, root.getChildNodes().getLength());
		Element conditionsEl = (Element) root.getChildNodes().item(0);
		Assertions.assertEquals("conditions", conditionsEl.getNodeName());
		Assertions.assertEquals(0, conditionsEl.getChildNodes().getLength());
	}

	// ── isEnabled tests ───────────────────────────────────────────────────────

	@Test
	public void isEnabled_whenGlobalPropertySetToFalse_returnsFalse() {
		GlobalProperty gp = new GlobalProperty(
		    "report.visitSummary.section.conditions.enabled", "false");
		Context.getAdministrationService().saveGlobalProperty(gp);

		Assertions.assertFalse(section.isEnabled());
	}
}
