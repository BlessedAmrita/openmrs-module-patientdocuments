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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.GlobalProperty;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientdocuments.api.model.Vital;
import org.openmrs.module.patientdocuments.testconfig.GlobalPropertyRestorer;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
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
 *   Visit 101 — systolic 120 (newer) + diastolic 80 + older systolic 110 + heart rate 72 + SpO2 98
 *   Visit 102 — systolic 120 only   (test: only systolic present)
 *   Visit 103 — weight 70 + height 175
 *   Visit 104 — encounter with no vitals obs
 *   Visit 105 — custom concept (TEST:custom001) obs 118 (concept-mapping override test)
 *   Visit 106 — no encounters
 *   Visit 107 — valueless current obs: systolic (older valued 120) + diastolic 80,
 *               heart rate (older valued 72), weight (no older reading)
 */
public class VitalsSectionTest extends BaseModuleContextSensitiveTest {

	private static final String DATASET =
	    "org/openmrs/module/patientdocuments/include/vitalsSectionTestDataset.xml";

	private static final String ENABLED_GP = "report.visitSummary.section.vitals.enabled";

	private static final String CONCEPTS_GP = "report.visitSummary.vitals.concepts";

	private VitalsSection section;

	private GlobalPropertyRestorer globalProperties;

	@BeforeEach
	public void setUp() throws Exception {
		// Captured before the baseline below overwrites it, so tearDown can put back what
		// this class found rather than what it assumed.
		globalProperties = GlobalPropertyRestorer.capture(ENABLED_GP, CONCEPTS_GP);
		executeDataSet(DATASET);
		// ConfigUtil caches GP values in-memory but does not participate in test
		// transaction rollbacks. Explicitly save the default before each test so the
		// cache reflects the correct baseline regardless of what a prior test wrote.
		Context.getAdministrationService().saveGlobalProperty(
		    new GlobalProperty(CONCEPTS_GP, VitalsSection.DEFAULT_VITAL_CONCEPTS));
		section = new VitalsSection();
	}

	/** Undoes both the baseline above and whatever the test itself wrote. */
	@AfterEach
	public void tearDown() {
		globalProperties.restore();
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
	public void gatherData_withSystolicAndDiastolicObs_returnsCombinedBloodPressure() {
		// Visit 101: systolic 120 + diastolic 80 — must appear as one combined entry
		Visit visit = Context.getVisitService().getVisit(101);

		List<Vital> vitals = section.gatherData(visit);

		Vital bp = vitals.stream()
		    .filter(v -> "Blood Pressure".equals(v.getLabel())).findFirst().orElse(null);
		Assertions.assertNotNull(bp, "Expected combined Blood Pressure vital");
		Assertions.assertEquals("120/80 mmHg", bp.getValue());
	}

	@Test
	public void gatherData_multipleObsForSameConcept_onlyMostRecentValueKept() {
		// Visit 101 has two systolic obs: 120 @ 12:00 (newer) and 110 @ 11:00 (older).
		// obsDatetime-desc ordering means the 120 obs wins; the combined BP reflects it.
		Visit visit = Context.getVisitService().getVisit(101);

		List<Vital> vitals = section.gatherData(visit);

		Vital bp = vitals.stream()
		    .filter(v -> "Blood Pressure".equals(v.getLabel())).findFirst().orElse(null);
		Assertions.assertNotNull(bp);
		Assertions.assertEquals("120/80 mmHg", bp.getValue()); // older systolic 110 discarded
	}

	@Test
	public void gatherData_withWeightAndHeight_recordsBothSeparately() {
		// Visit 103: weight 70 kg + height 175 cm — units come from ConceptNumeric rows
		Visit visit = Context.getVisitService().getVisit(103);

		List<Vital> vitals = section.gatherData(visit);

		boolean hasWeight = vitals.stream()
		    .anyMatch(v -> "Weight (kg)".equals(v.getLabel()) && "70 kg".equals(v.getValue()));
		boolean hasHeight = vitals.stream()
		    .anyMatch(v -> "Height (cm)".equals(v.getLabel()) && "175 cm".equals(v.getValue()));
		Assertions.assertTrue(hasWeight, "Expected Weight (kg) vital with value '70 kg'");
		Assertions.assertTrue(hasHeight, "Expected Height (cm) vital with value '175 cm'");
	}

	@Test
	public void gatherData_currentObsHasNoValue_rendersPlaceholderInsteadOfDropping() {
		// Visit 107: the only weight obs is valueless — the row must still appear, marked
		Visit visit = Context.getVisitService().getVisit(107);

		List<Vital> vitals = section.gatherData(visit);

		Vital weight = vitals.stream()
		    .filter(v -> "Weight (kg)".equals(v.getLabel())).findFirst().orElse(null);
		Assertions.assertNotNull(weight, "A valueless vital must render a marked row, not disappear");
		Assertions.assertEquals("—", weight.getValue());
	}

	@Test
	public void gatherData_currentObsHasNoValue_doesNotFallBackToOlderValue() {
		// Visit 107: heart rate @12:00 is valueless, @11:00 has 72 — the stale 72 must not
		// print as if it were the current reading
		Visit visit = Context.getVisitService().getVisit(107);

		List<Vital> vitals = section.gatherData(visit);

		Vital heartRate = vitals.stream()
		    .filter(v -> "Heart Rate".equals(v.getLabel())).findFirst().orElse(null);
		Assertions.assertNotNull(heartRate, "Expected a Heart Rate vital");
		Assertions.assertEquals("—", heartRate.getValue());
	}

	@Test
	public void gatherData_currentBpComponentHasNoValue_rendersQuestionMarkNotStaleValue() {
		// Visit 107: systolic @12:00 is valueless, @11:00 has 120, diastolic is 80 —
		// the combined BP must show "?" for systolic, not the stale 120
		Visit visit = Context.getVisitService().getVisit(107);

		List<Vital> vitals = section.gatherData(visit);

		Vital bp = vitals.stream()
		    .filter(v -> "Blood Pressure".equals(v.getLabel())).findFirst().orElse(null);
		Assertions.assertNotNull(bp, "Expected combined Blood Pressure vital");
		Assertions.assertEquals("?/80 mmHg", bp.getValue());
	}

	@Test
	public void gatherData_withOnlySystolicObs_returnsBPWithQuestionMarkForDiastolic() {
		// Visit 102: only systolic 120 — diastolic is absent so the combined entry uses "?"
		Visit visit = Context.getVisitService().getVisit(102);

		List<Vital> vitals = section.gatherData(visit);

		Assertions.assertFalse(vitals.isEmpty());
		boolean hasBP = vitals.stream()
		    .anyMatch(v -> "Blood Pressure".equals(v.getLabel()) && "120/? mmHg".equals(v.getValue()));
		Assertions.assertTrue(hasBP, "Expected Blood Pressure vital '120/? mmHg'");
	}

	// ── concept-map resolution tests ─────────────────────────────────────────

	@Test
	public void gatherData_withValidConceptMapping_resolvesConcept() {
		// Set concepts GP to only CIEL:5085 (systolic). Visit 101 has systolic obs = 120.
		// With only systolic present, BP is emitted as "120/? mmHg" (no diastolic concept).
		GlobalProperty gp = new GlobalProperty(CONCEPTS_GP, "CIEL:5085");
		Context.getAdministrationService().saveGlobalProperty(gp);
		Visit visit = Context.getVisitService().getVisit(101);

		List<Vital> vitals = section.gatherData(visit);

		Assertions.assertEquals(1, vitals.size());
		Assertions.assertEquals("Blood Pressure", vitals.get(0).getLabel());
		Assertions.assertEquals("120/? mmHg", vitals.get(0).getValue());
	}

	@Test
	public void gatherData_someConceptEntriesUnresolvable_surfacesNoticeAndKeepsGood() {
		// "BADFORMAT" has no colon separator, "NONEXISTENT:99999" doesn't resolve;
		// "CIEL:5085" is valid → BP emitted as "120/? mmHg" (systolic-only).
		GlobalProperty gp = new GlobalProperty(CONCEPTS_GP, "BADFORMAT,NONEXISTENT:99999,CIEL:5085");
		Context.getAdministrationService().saveGlobalProperty(gp);
		Visit visit = Context.getVisitService().getVisit(101);
		List<String> notices = new ArrayList<>();

		List<Vital> vitals = section.gatherData(visit, notices);

		Assertions.assertEquals(1, vitals.size());
		Assertions.assertEquals("Blood Pressure", vitals.get(0).getLabel());
		Assertions.assertEquals("120/? mmHg", vitals.get(0).getValue());
		Assertions.assertEquals(1, notices.size(), "Expected one notice covering all unresolved entries");
		Assertions.assertTrue(notices.get(0).contains("BADFORMAT"),
		    "Notice should name the malformed entry");
		Assertions.assertTrue(notices.get(0).contains("NONEXISTENT:99999"),
		    "Notice should name the unresolvable mapping");
	}

	@Test
	public void renderXml_someConceptEntriesUnresolvable_emitsNoticeAlongsideData() throws Exception {
		// The notice must be a sibling of the data element — data renders AND the gap is visible.
		GlobalProperty gp = new GlobalProperty(CONCEPTS_GP, "BADFORMAT,CIEL:5085");
		Context.getAdministrationService().saveGlobalProperty(gp);
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = doc.createElement("root");
		doc.appendChild(root);
		Visit visit = Context.getVisitService().getVisit(101);

		section.renderXml(doc, root, visit);

		Assertions.assertEquals(2, root.getChildNodes().getLength());
		Element dataEl = (Element) root.getChildNodes().item(0);
		Assertions.assertEquals("vitals", dataEl.getNodeName());
		Assertions.assertTrue(dataEl.getElementsByTagName("vital").getLength() > 0,
		    "Resolved vitals should still render");
		Element noticeEl = (Element) root.getChildNodes().item(1);
		Assertions.assertEquals("section-notice", noticeEl.getNodeName());
		Assertions.assertEquals("vitals", noticeEl.getAttribute("key"));
		Assertions.assertTrue(noticeEl.getAttribute("message").contains("BADFORMAT"),
		    "Notice message should name the unresolved entry");
	}

	@Test
	public void gatherData_allConceptEntriesUnresolvable_throws() {
		// All entries configured but none resolve — must throw so the error banner fires
		GlobalProperty gp = new GlobalProperty(CONCEPTS_GP, "NONEXISTENT:99999");
		Context.getAdministrationService().saveGlobalProperty(gp);
		Visit visit = Context.getVisitService().getVisit(101);

		Assertions.assertThrows(IllegalStateException.class, () -> section.gatherData(visit),
		    "Expected IllegalStateException when all configured concepts are unresolvable");
	}

	@Test
	public void gatherData_withCustomConceptsGP_overridesDefaults() {
		// Override the concepts GP to use only the TEST-source custom concept
		GlobalProperty gp = new GlobalProperty(CONCEPTS_GP, "TEST:custom001");
		Context.getAdministrationService().saveGlobalProperty(gp);
		// Visit 105 has an obs for the custom concept with value 118
		Visit visit = Context.getVisitService().getVisit(105);

		List<Vital> vitals = section.gatherData(visit);

		Assertions.assertFalse(vitals.isEmpty(), "Expected a vital for the custom concept");
		Assertions.assertEquals("Custom Systolic", vitals.get(0).getLabel());
		Assertions.assertEquals("118", vitals.get(0).getValue());
	}

	@Test
	public void gatherData_withConceptNumericConcepts_includesUnitsInValue() {
		// CIEL:5087 is Heart Rate (concept_id=5005, concept_numeric units="bpm").
		// Visit 101 has a heart rate obs with value 72.
		// The unit must be appended from ConceptNumeric.getUnits() → "72 bpm".
		GlobalProperty gp = new GlobalProperty(CONCEPTS_GP, "CIEL:5087");
		Context.getAdministrationService().saveGlobalProperty(gp);
		Visit visit = Context.getVisitService().getVisit(101);

		List<Vital> vitals = section.gatherData(visit);

		Assertions.assertEquals(1, vitals.size());
		Assertions.assertEquals("Heart Rate", vitals.get(0).getLabel());
		Assertions.assertEquals("72 bpm", vitals.get(0).getValue());
	}

	// ── renderXml tests ───────────────────────────────────────────────────────

	@Test
	public void renderXml_withNonEmptyVitalList_producesVitalsElementWithVitalChildren()
	        throws Exception {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = doc.createElement("root");
		doc.appendChild(root);
		List<Vital> vitals = Arrays.asList(
		    new Vital("Heart Rate", "72"),
		    new Vital("Temperature", "36.5")
		);

		section.renderXml(doc, root, vitals);

		Assertions.assertEquals(1, root.getChildNodes().getLength());
		Element vitalsEl = (Element) root.getChildNodes().item(0);
		Assertions.assertEquals("vitals", vitalsEl.getNodeName());
		Assertions.assertEquals(2, vitalsEl.getChildNodes().getLength());

		Element first = (Element) vitalsEl.getChildNodes().item(0);
		Assertions.assertEquals("vital", first.getNodeName());
		Assertions.assertEquals("Heart Rate", first.getAttribute("label"));
		Assertions.assertEquals("72", first.getAttribute("value"));
	}

	@Test
	public void renderXml_withEmptyVitalList_producesVitalsContainerWithNoVitalChildren()
	        throws Exception {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = doc.createElement("root");
		doc.appendChild(root);

		section.renderXml(doc, root, Collections.emptyList());

		Assertions.assertEquals(1, root.getChildNodes().getLength());
		Element vitalsEl = (Element) root.getChildNodes().item(0);
		Assertions.assertEquals("vitals", vitalsEl.getNodeName());
		Assertions.assertEquals(0, vitalsEl.getChildNodes().getLength());
	}

	// ── isEnabled tests ───────────────────────────────────────────────────────

	@Test
	public void isEnabled_whenNoGlobalPropertySet_returnsTrue() {
		Assertions.assertTrue(section.isEnabled());
	}

	@Test
	public void isEnabled_whenGlobalPropertySetToFalse_returnsFalse() {
		GlobalProperty gp = new GlobalProperty(ENABLED_GP, "false");
		Context.getAdministrationService().saveGlobalProperty(gp);

		Assertions.assertFalse(section.isEnabled());
	}

	@Test
	public void isEnabled_whenGlobalPropertySetToTrue_returnsTrue() {
		GlobalProperty gp = new GlobalProperty(ENABLED_GP, "true");
		Context.getAdministrationService().saveGlobalProperty(gp);

		Assertions.assertTrue(section.isEnabled());
	}
}
