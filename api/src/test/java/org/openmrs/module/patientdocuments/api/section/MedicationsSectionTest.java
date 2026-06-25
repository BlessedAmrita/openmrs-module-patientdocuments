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
import org.openmrs.module.patientdocuments.api.model.MedicationEntry;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.Arrays;
import java.util.List;

/**
 * Integration tests for MedicationsSection.
 *
 * Active drug orders are pre-loaded from medicationsSectionTestDataset.xml rather than
 * created programmatically: drug orders carry many required relationships and building
 * them in code triggers validator NPEs.
 *
 * Medications are patient-level: getActiveOrders(patient, drugOrderType, ...) returns
 * active drug orders for the patient regardless of which visit is passed in.
 *
 * Dataset layout:
 *   Visit 8001 — patient 6, who has 4 active drug orders:
 *     Order 8001 — coded drug Lisinopril, 5 mg Oral Twice daily, 10 days
 *     Order 8002 — non-coded "Paracetamol syrup", 2.5 mg Oral Twice daily, no duration
 *     Order 8003 — concept-only Metformin, free-text dosing instructions
 *     Order 8004 — non-coded "Ibuprofen", 250 mg only (no route/frequency/duration)
 *   Visit 8002 — patient 8, who has no drug orders
 */
public class MedicationsSectionTest extends BaseModuleContextSensitiveTest {

	private static final String DATASET =
	    "org/openmrs/module/patientdocuments/include/medicationsSectionTestDataset.xml";

	private MedicationsSection section;

	@BeforeEach
	public void setUp() throws Exception {
		executeDataSet(DATASET);
		// ConfigUtil caches GP values in-memory but does not participate in test
		// transaction rollbacks. Explicitly save the default before each test so the
		// cache reflects the correct baseline regardless of what a prior test wrote.
		Context.getAdministrationService().saveGlobalProperty(
		    new GlobalProperty("report.visitSummary.section.medications.enabled", "true"));
		section = new MedicationsSection();
	}

	private MedicationEntry findByName(List<MedicationEntry> meds, String name) {
		return meds.stream().filter(m -> name.equals(m.getName())).findFirst().orElse(null);
	}

	// ── gatherData tests ──────────────────────────────────────────────────────

	@Test
	public void gatherData_patientWithActiveOrders_returnsAllMedicationEntries() {
		// Visit 8001's patient (patient 6) has 4 active drug orders
		Visit visit = Context.getVisitService().getVisit(8001);

		List<MedicationEntry> meds = section.gatherData(visit);

		Assertions.assertEquals(4, meds.size());
	}

	@Test
	public void gatherData_codedDrug_usesDrugName() {
		Visit visit = Context.getVisitService().getVisit(8001);

		List<MedicationEntry> meds = section.gatherData(visit);

		MedicationEntry lisinopril = findByName(meds, "Lisinopril");
		Assertions.assertNotNull(lisinopril, "Expected coded drug Lisinopril entry");
		Assertions.assertEquals("2025-03-01", lisinopril.getStartDate());
	}

	@Test
	public void gatherData_nonCodedDrug_usesDrugNonCoded() {
		Visit visit = Context.getVisitService().getVisit(8001);

		List<MedicationEntry> meds = section.gatherData(visit);

		MedicationEntry paracetamol = findByName(meds, "Paracetamol syrup");
		Assertions.assertNotNull(paracetamol, "Expected non-coded drug entry from drugNonCoded");
	}

	@Test
	public void gatherData_conceptOnlyDrug_usesConceptName() {
		Visit visit = Context.getVisitService().getVisit(8001);

		List<MedicationEntry> meds = section.gatherData(visit);

		MedicationEntry metformin = findByName(meds, "Metformin");
		Assertions.assertNotNull(metformin, "Expected concept-only drug name resolved from concept");
	}

	@Test
	public void gatherData_simpleDosing_buildsDoseUnitsRouteFrequency() {
		Visit visit = Context.getVisitService().getVisit(8001);

		List<MedicationEntry> meds = section.gatherData(visit);

		MedicationEntry lisinopril = findByName(meds, "Lisinopril");
		Assertions.assertNotNull(lisinopril);
		Assertions.assertEquals("5 mg, Oral, Twice daily", lisinopril.getDosing());
	}

	@Test
	public void gatherData_integerDose_hasNoTrailingDecimal() {
		// dose 5.0 must format as "5", not "5.0"
		Visit visit = Context.getVisitService().getVisit(8001);

		List<MedicationEntry> meds = section.gatherData(visit);

		MedicationEntry lisinopril = findByName(meds, "Lisinopril");
		Assertions.assertNotNull(lisinopril);
		Assertions.assertTrue(lisinopril.getDosing().startsWith("5 mg"),
		    "Expected dose '5 mg' with no trailing .0, got: " + lisinopril.getDosing());
	}

	@Test
	public void gatherData_fractionalDose_keepsDecimal() {
		// dose 2.5 must format as "2.5"
		Visit visit = Context.getVisitService().getVisit(8001);

		List<MedicationEntry> meds = section.gatherData(visit);

		MedicationEntry paracetamol = findByName(meds, "Paracetamol syrup");
		Assertions.assertNotNull(paracetamol);
		Assertions.assertEquals("2.5 mg, Oral, Twice daily", paracetamol.getDosing());
	}

	@Test
	public void gatherData_simpleDosingWithNullRouteAndFrequency_omitsEmptySegments() {
		// Null route + frequency must not produce trailing commas (the NPE-avoidance path).
		Visit visit = Context.getVisitService().getVisit(8001);

		List<MedicationEntry> meds = section.gatherData(visit);

		MedicationEntry ibuprofen = findByName(meds, "Ibuprofen");
		Assertions.assertNotNull(ibuprofen);
		Assertions.assertEquals("250 mg", ibuprofen.getDosing());
	}

	@Test
	public void gatherData_orderWithDuration_formatsDurationWithUnits() {
		Visit visit = Context.getVisitService().getVisit(8001);

		List<MedicationEntry> meds = section.gatherData(visit);

		MedicationEntry lisinopril = findByName(meds, "Lisinopril");
		Assertions.assertNotNull(lisinopril);
		Assertions.assertEquals("10 days", lisinopril.getDuration());
	}

	@Test
	public void gatherData_orderWithoutDuration_usesEmDash() {
		Visit visit = Context.getVisitService().getVisit(8001);

		List<MedicationEntry> meds = section.gatherData(visit);

		MedicationEntry paracetamol = findByName(meds, "Paracetamol syrup");
		Assertions.assertNotNull(paracetamol);
		Assertions.assertEquals("—", paracetamol.getDuration());
	}

	@Test
	public void gatherData_freeTextDosing_usesDosingInstructionsDirectly() {
		Visit visit = Context.getVisitService().getVisit(8001);

		List<MedicationEntry> meds = section.gatherData(visit);

		MedicationEntry metformin = findByName(meds, "Metformin");
		Assertions.assertNotNull(metformin);
		Assertions.assertEquals("Take one tablet as directed by physician", metformin.getDosing());
	}

	@Test
	public void gatherData_patientWithNoActiveOrders_returnsEmptyList() {
		// Visit 8002's patient (patient 8) has no drug orders
		Visit visit = Context.getVisitService().getVisit(8002);

		List<MedicationEntry> meds = section.gatherData(visit);

		Assertions.assertEquals(0, meds.size());
	}

	// ── renderXml tests ───────────────────────────────────────────────────────

	@Test
	public void renderXml_withMedications_producesCorrectXmlStructure() throws Exception {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = doc.createElement("root");
		doc.appendChild(root);
		List<MedicationEntry> entries = Arrays.asList(
		    new MedicationEntry("Lisinopril", "5 mg, Oral, Twice daily", "10 days", "2025-03-01"),
		    new MedicationEntry("Paracetamol syrup", "2.5 mg, Oral, Twice daily", "—", "2025-03-01")
		);

		section.renderXml(doc, root, entries);

		Assertions.assertEquals(1, root.getChildNodes().getLength());
		Element medsEl = (Element) root.getChildNodes().item(0);
		Assertions.assertEquals("medications", medsEl.getNodeName());
		Assertions.assertEquals(2, medsEl.getChildNodes().getLength());

		Element first = (Element) medsEl.getChildNodes().item(0);
		Assertions.assertEquals("medication", first.getNodeName());
		Assertions.assertEquals("Lisinopril", first.getAttribute("name"));
		Assertions.assertEquals("5 mg, Oral, Twice daily", first.getAttribute("dosing"));
		Assertions.assertEquals("10 days", first.getAttribute("duration"));
		Assertions.assertEquals("2025-03-01", first.getAttribute("start"));
	}

	@Test
	public void renderXml_withEmptyList_producesMedicationsContainerWithNoChildren()
	        throws Exception {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = doc.createElement("root");
		doc.appendChild(root);

		section.renderXml(doc, root, java.util.Collections.emptyList());

		Assertions.assertEquals(1, root.getChildNodes().getLength());
		Element medsEl = (Element) root.getChildNodes().item(0);
		Assertions.assertEquals("medications", medsEl.getNodeName());
		Assertions.assertEquals(0, medsEl.getChildNodes().getLength());
	}

	// ── isEnabled tests ───────────────────────────────────────────────────────

	@Test
	public void isEnabled_whenGlobalPropertySetToFalse_returnsFalse() {
		GlobalProperty gp = new GlobalProperty(
		    "report.visitSummary.section.medications.enabled", "false");
		Context.getAdministrationService().saveGlobalProperty(gp);

		Assertions.assertFalse(section.isEnabled());
	}

	@Test
	public void isEnabled_whenNoGlobalPropertySet_returnsTrue() {
		Assertions.assertTrue(section.isEnabled());
	}
}
