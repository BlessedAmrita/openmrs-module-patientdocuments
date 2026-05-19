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
import org.openmrs.module.patientdocuments.api.model.Vital;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Integration tests for VitalsSection.
 *
 * Obs are pre-loaded from vitalsSectionTestDataset.xml rather than created programmatically,
 * because ObsValidator in OpenMRS 2.7.0 NPEs on numeric concepts that have no ConceptReferenceRange.
 *
 * Dataset layout:
 *   Visit 101 — systolic 120 (newer) + diastolic 80 + older systolic 110 (tests c, d)
 *   Visit 102 — systolic 120 only   (test f: partial BP format)
 *   Visit 103 — weight 70 + height 175  (test e)
 *   Visit 104 — encounter with no vitals obs (test b)
 *   Visit 105 — custom-UUID concept obs 118 (test g)
 */
public class VitalsSectionTest extends BaseModuleContextSensitiveTest {

	private static final String DATASET =
	    "org/openmrs/module/patientdocuments/include/vitalsSectionTestDataset.xml";

	private VitalsSection section;

	@BeforeEach
	public void setUp() throws Exception {
		executeDataSet(DATASET);
		section = new VitalsSection();
	}

	// ── gatherData tests ──────────────────────────────────────────────────────

	@Test
	public void gatherData_visitWithNoEncounters_returnsEmptyList() {
		// Visit 106 has no encounters
		Visit visit = Context.getVisitService().getVisit(106);

		List<Vital> vitals = section.gatherData(visit);

		Assertions.assertEquals(0, vitals.size());
	}

	@Test
	public void gatherData_visitWithEncountersButNoVitalsObs_returnsEmptyList() {
		// Visit 104 has an encounter but no vitals obs
		Visit visit = Context.getVisitService().getVisit(104);

		List<Vital> vitals = section.gatherData(visit);

		Assertions.assertEquals(0, vitals.size());
	}

	@Test
	public void gatherData_visitWithSystolicAndDiastolicObs_returnsBloodPressureVital() {
		// Visit 101: systolic 120 + diastolic 80
		Visit visit = Context.getVisitService().getVisit(101);

		List<Vital> vitals = section.gatherData(visit);

		Assertions.assertFalse(vitals.isEmpty());
		Vital bp = vitals.get(0);
		Assertions.assertEquals("Blood Pressure", bp.getLabel());
		Assertions.assertEquals("120/80 mmHg",    bp.getValue());
	}

	@Test
	public void gatherData_multipleObsForSameConcept_onlyMostRecentValueKept() {
		// Visit 101 has two systolic obs: 120 @ 12:00 (newer) and 110 @ 11:00 (older).
		// obsDatetime-desc ordering means the 120 obs wins.
		Visit visit = Context.getVisitService().getVisit(101);

		List<Vital> vitals = section.gatherData(visit);

		Vital bp = vitals.get(0);
		Assertions.assertEquals("Blood Pressure", bp.getLabel());
		Assertions.assertEquals("120/80 mmHg",    bp.getValue()); // 110 is discarded
	}

	@Test
	public void gatherData_withWeightAndHeight_recordsBothSeparately() {
		// Visit 103: weight 70 + height 175
		Visit visit = Context.getVisitService().getVisit(103);

		List<Vital> vitals = section.gatherData(visit);

		boolean hasWeight = vitals.stream().anyMatch(v -> "Weight".equals(v.getLabel()) && "70 kg".equals(v.getValue()));
		boolean hasHeight = vitals.stream().anyMatch(v -> "Height".equals(v.getLabel()) && "175 cm".equals(v.getValue()));
		Assertions.assertTrue(hasWeight, "Expected Weight vital '70 kg'");
		Assertions.assertTrue(hasHeight, "Expected Height vital '175 cm'");
	}

	@Test
	public void gatherData_withOnlySystolicObs_formatsBloodPressureWithQuestionMarkForDiastolic() {
		// Visit 102: only systolic 120
		Visit visit = Context.getVisitService().getVisit(102);

		List<Vital> vitals = section.gatherData(visit);

		Assertions.assertFalse(vitals.isEmpty());
		Vital bp = vitals.get(0);
		Assertions.assertEquals("Blood Pressure", bp.getLabel());
		Assertions.assertEquals("120/? mmHg",     bp.getValue());
	}

	@Test
	public void gatherData_withConceptUuidOverrideViaGlobalProperty_resolvesCustomConcept() {
		// Override systolic UUID to the custom concept loaded in the dataset (UUID cccc0001-…)
		String customUuid = "cccc0001-cccc-cccc-cccc-cccc00000001";
		GlobalProperty gp = new GlobalProperty("report.visitSummary.vitals.systolic", customUuid);
		Context.getAdministrationService().saveGlobalProperty(gp);
		// Visit 105 has an obs for that custom concept with value 118
		Visit visit = Context.getVisitService().getVisit(105);

		List<Vital> vitals = section.gatherData(visit);

		Assertions.assertFalse(vitals.isEmpty(), "Expected at least one vital for custom systolic concept");
		Vital bp = vitals.get(0);
		Assertions.assertEquals("Blood Pressure", bp.getLabel());
		Assertions.assertTrue(bp.getValue().startsWith("118/"), "Expected systolic value 118 in BP");
	}

	// ── renderXml tests ───────────────────────────────────────────────────────

	@Test
	public void renderXml_withNonEmptyVitalList_producesVitalsElementWithVitalChildren() throws Exception {
		Document doc  = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element  root = doc.createElement("root");
		doc.appendChild(root);
		List<Vital> vitals = Arrays.asList(
		    new Vital("Heart Rate",  "72 bpm"),
		    new Vital("Temperature", "36.5 °C")
		);

		section.renderXml(doc, root, vitals);

		Assertions.assertEquals(1, root.getChildNodes().getLength());
		Element vitalsEl = (Element) root.getChildNodes().item(0);
		Assertions.assertEquals("vitals", vitalsEl.getNodeName());
		Assertions.assertEquals(2, vitalsEl.getChildNodes().getLength());

		Element first = (Element) vitalsEl.getChildNodes().item(0);
		Assertions.assertEquals("vital",      first.getNodeName());
		Assertions.assertEquals("Heart Rate", first.getAttribute("label"));
		Assertions.assertEquals("72 bpm",     first.getAttribute("value"));
	}

	@Test
	public void renderXml_withEmptyVitalList_producesVitalsContainerWithNoVitalChildren() throws Exception {
		Document doc  = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element  root = doc.createElement("root");
		doc.appendChild(root);

		section.renderXml(doc, root, Collections.emptyList());

		Assertions.assertEquals(1, root.getChildNodes().getLength());
		Element vitalsEl = (Element) root.getChildNodes().item(0);
		Assertions.assertEquals("vitals", vitalsEl.getNodeName());
		Assertions.assertEquals(0, vitalsEl.getChildNodes().getLength());
	}

	// ── isEnabled tests (Step 3) ──────────────────────────────────────────────

	@Test
	public void isEnabled_whenNoGlobalPropertySet_returnsTrue() {
		Assertions.assertTrue(section.isEnabled());
	}

	@Test
	public void isEnabled_whenGlobalPropertySetToFalse_returnsFalse() {
		GlobalProperty gp = new GlobalProperty("documents.section.vitals.enabled", "false");
		Context.getAdministrationService().saveGlobalProperty(gp);

		Assertions.assertFalse(section.isEnabled());
	}

	@Test
	public void isEnabled_whenGlobalPropertySetToTrue_returnsTrue() {
		GlobalProperty gp = new GlobalProperty("documents.section.vitals.enabled", "true");
		Context.getAdministrationService().saveGlobalProperty(gp);

		Assertions.assertTrue(section.isEnabled());
	}
}
