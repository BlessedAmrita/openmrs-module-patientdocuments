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
import org.openmrs.DosingInstructions;
import org.openmrs.DrugOrder;
import org.openmrs.GlobalProperty;
import org.openmrs.OrderType;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientdocuments.api.model.MedicationEntry;
import org.openmrs.module.patientdocuments.testconfig.GlobalPropertyRestorer;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.springframework.validation.Errors;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
 *   Visit 8001 — patient 6, who has 7 active drug orders:
 *     Order 8001 — coded drug Lisinopril, 5 mg Oral Twice daily, 10 days
 *     Order 8002 — non-coded "Paracetamol syrup", 2.5 mg Oral Twice daily, no duration
 *     Order 8003 — concept-only Metformin, free-text dosing instructions
 *     Order 8004 — non-coded "Ibuprofen", 250 mg only (no route/frequency/duration)
 *     Order 8005 — concept-only (concept 8008 has no name), no drug/non-coded text → "Unknown"
 *     Order 8006 — coded drug Aspirin, strength 81mg → "Aspirin 81mg"
 *     Order 8007 — coded drug "Amoxicillin 500mg", strength 500mg → kept as-is (de-dup)
 *   Visit 8002 — patient 8, who has no drug orders
 */
public class MedicationsSectionTest extends BaseModuleContextSensitiveTest {

	private static final String DATASET =
	    "org/openmrs/module/patientdocuments/include/medicationsSectionTestDataset.xml";

	private static final String ENABLED_GP = "report.visitSummary.section.medications.enabled";

	private MedicationsSection section;

	private GlobalPropertyRestorer globalProperties;

	@BeforeEach
	public void setUp() throws Exception {
		// Captured before the baseline below overwrites it, so tearDown can put back what
		// this class found rather than what it assumed.
		globalProperties = GlobalPropertyRestorer.capture(ENABLED_GP);
		executeDataSet(DATASET);
		// ConfigUtil caches GP values in-memory but does not participate in test
		// transaction rollbacks. Explicitly save the default before each test so the
		// cache reflects the correct baseline regardless of what a prior test wrote.
		Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(ENABLED_GP, "true"));
		section = new MedicationsSection();
	}

	/** Undoes both the baseline above and whatever the test itself wrote. */
	@AfterEach
	public void tearDown() {
		globalProperties.restore();
	}

	private MedicationEntry findByName(List<MedicationEntry> meds, String name) {
		return meds.stream().filter(m -> name.equals(m.getName())).findFirst().orElse(null);
	}

	// ── gatherData tests ──────────────────────────────────────────────────────

	@Test
	public void gatherData_patientWithActiveOrders_returnsAllMedicationEntries() {
		// Visit 8001's patient (patient 6) has 7 active drug orders
		Visit visit = Context.getVisitService().getVisit(8001);

		List<MedicationEntry> meds = section.gatherData(visit);

		Assertions.assertEquals(7, meds.size());
	}

	@Test
	public void gatherData_drugWithNoResolvableName_returnsUnknown() {
		// Order 8005 references concept 8008, which has no name, and has no drug or
		// non-coded text, so buildDrugName falls through every step to "Unknown".
		Visit visit = Context.getVisitService().getVisit(8001);

		List<MedicationEntry> meds = section.gatherData(visit);

		MedicationEntry unknown = findByName(meds, "Unknown");
		Assertions.assertNotNull(unknown, "Expected a medication entry with name 'Unknown'");
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
	public void gatherData_codedDrugWithStrength_appendsStrengthToName() {
		// Drug 8009 is "Aspirin" with strength "81mg" → name renders as "Aspirin 81mg".
		Visit visit = Context.getVisitService().getVisit(8001);

		List<MedicationEntry> meds = section.gatherData(visit);

		MedicationEntry aspirin = findByName(meds, "Aspirin 81mg");
		Assertions.assertNotNull(aspirin, "Expected coded drug name to include its strength: 'Aspirin 81mg'");
	}

	@Test
	public void gatherData_codedDrugNameAlreadyContainsStrength_isNotDuplicated() {
		// Drug 8010's name already contains its strength ("Amoxicillin 500mg", strength
		// "500mg"), so the de-dup branch keeps it as-is rather than appending "500mg" again.
		Visit visit = Context.getVisitService().getVisit(8001);

		List<MedicationEntry> meds = section.gatherData(visit);

		MedicationEntry amoxicillin = findByName(meds, "Amoxicillin 500mg");
		Assertions.assertNotNull(amoxicillin, "Expected 'Amoxicillin 500mg' kept as-is");
		Assertions.assertNull(findByName(meds, "Amoxicillin 500mg 500mg"),
		    "Strength must not be appended when the name already contains it");
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

	@Test
	public void gatherData_drugOrderTypeMissing_throwsIllegalStateException() {
		// Simulate broken metadata: re-uuid the seeded drug order type so the lookup
		// returns null. Plain SQL keeps the real code path (no mocks) and rolls back
		// with the test transaction.
		Context.getAdministrationService().executeSQL(
		    "update order_type set uuid = 'ffffffff-ffff-ffff-ffff-ffffffffffff' where uuid = '"
		            + OrderType.DRUG_ORDER_TYPE_UUID + "'", false);
		Visit visit = Context.getVisitService().getVisit(8001);

		Assertions.assertThrows(IllegalStateException.class, () -> section.gatherData(visit));
	}

	@Test
	public void gatherData_unrecognizedDosingType_stillBuildsBestEffortDosing() {
		// Point order 8004's dosing type at a class that is neither Simple nor
		// FreeText: buildDosing must warn but still render the manual dose build.
		Context.getAdministrationService().executeSQL(
		    "update drug_order set dosing_type = '" + CustomDosingInstructions.class.getName()
		            + "' where order_id = 8004", false);
		Visit visit = Context.getVisitService().getVisit(8001);

		List<MedicationEntry> meds = section.gatherData(visit);

		MedicationEntry ibuprofen = findByName(meds, "Ibuprofen");
		Assertions.assertNotNull(ibuprofen, "Expected order 8004 to still render");
		Assertions.assertEquals("250 mg", ibuprofen.getDosing());
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
		GlobalProperty gp = new GlobalProperty(ENABLED_GP, "false");
		Context.getAdministrationService().saveGlobalProperty(gp);

		Assertions.assertFalse(section.isEnabled());
	}

	@Test
	public void isEnabled_whenNoGlobalPropertySet_returnsTrue() {
		Assertions.assertTrue(section.isEnabled());
	}

	// Minimal custom dosing type — exists only so Hibernate can hydrate a non-Simple/FreeText dosing_type for the unknown-type test.
	public static class CustomDosingInstructions implements DosingInstructions {

		@Override
		public String getDosingInstructionsAsString(Locale locale) {
			throw new UnsupportedOperationException("Test stub: not invoked by MedicationsSection");
		}

		@Override
		public void setDosingInstructions(DrugOrder order) {
			throw new UnsupportedOperationException("Test stub: not invoked by MedicationsSection");
		}

		@Override
		public DosingInstructions getDosingInstructions(DrugOrder order) {
			throw new UnsupportedOperationException("Test stub: not invoked by MedicationsSection");
		}

		@Override
		public void validate(DrugOrder order, Errors errors) {
			throw new UnsupportedOperationException("Test stub: not invoked by MedicationsSection");
		}

		@Override
		public Date getAutoExpireDate(DrugOrder order) {
			throw new UnsupportedOperationException("Test stub: not invoked by MedicationsSection");
		}
	}
}
