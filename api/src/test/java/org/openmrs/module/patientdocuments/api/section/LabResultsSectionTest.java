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
import org.openmrs.module.patientdocuments.api.model.LabResult;
import org.openmrs.module.patientdocuments.api.model.LabResultGroup;
import org.openmrs.module.patientdocuments.api.model.LabResultsData;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Integration tests for LabResultsSection.
 *
 * Obs are pre-loaded from labResultsSectionTestDataset.xml rather than created via
 * saveObs(), which triggers a buggy ConceptReferenceRange validator on OpenMRS 2.7.0.
 *
 * Dataset layout:
 *   Visit 501 — patient 2, one encounter with a numeric result (concept-range + HIGH),
 *               a numeric result (obs-snapshot range + LOW), a coded result, a text
 *               result, a Finding-class obs (excluded), a CBC panel (obs group), and a
 *               voided obs (excluded).
 *   Visit 502 — patient 7, encounter with no obs (None recorded).
 *   Visit 503 — patient 2, range-shape and interpretation edge cases (bound-only
 *               ranges, NORMAL interpretation, no range source).
 *   Visit 504 — patient 8, no encounters.
 *   Visit 505 — patient 2, numeric-formatting and flag-localization edge cases, a
 *               valueless numeric obs (placeholder), and a panel whose member concept
 *               has no name (renders "Unknown").
 */
public class LabResultsSectionTest extends BaseModuleContextSensitiveTest {

	private static final String DATASET =
	    "org/openmrs/module/patientdocuments/include/labResultsSectionTestDataset.xml";

	private LabResultsSection section;

	@BeforeEach
	public void setUp() throws Exception {
		executeDataSet(DATASET);
		// ConfigUtil caches GP values in-memory outside the test transaction; reset the
		// default before each test so a prior test's override does not leak.
		Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(
		    LabResultsSection.CONCEPT_CLASSES_PROPERTY, LabResultsSection.DEFAULT_LAB_CONCEPT_CLASSES));
		section = new LabResultsSection();
	}

	private LabResult findStandalone(LabResultsData data, String name) {
		return data.getStandalone().stream()
		    .filter(r -> name.equals(r.getName())).findFirst().orElse(null);
	}

	// ── gatherData tests ──────────────────────────────────────────────────────

	@Test
	public void gatherData_numericResultWithConceptRangeAndInterpretation_formatsValueRangeAndFlag() {
		Visit visit = Context.getVisitService().getVisit(501);

		LabResult glucose = findStandalone(section.gatherData(visit), "Serum Glucose");

		Assertions.assertNotNull(glucose, "Expected a Serum Glucose result");
		Assertions.assertEquals("9.5", glucose.getValue());
		Assertions.assertEquals("mmol/L", glucose.getUnits());
		Assertions.assertEquals("4 – 7 mmol/L", glucose.getRange());
		Assertions.assertEquals("High", glucose.getFlag());
	}

	@Test
	public void gatherData_numericResultWithObsSnapshotRange_prefersSnapshotOverConcept() {
		Visit visit = Context.getVisitService().getVisit(501);

		LabResult hemoglobin = findStandalone(section.gatherData(visit), "Hemoglobin");

		Assertions.assertNotNull(hemoglobin, "Expected a Hemoglobin result");
		Assertions.assertEquals("8", hemoglobin.getValue());
		Assertions.assertEquals("12 – 16 g/dL", hemoglobin.getRange());
		Assertions.assertEquals("Low", hemoglobin.getFlag());
	}

	@Test
	public void gatherData_codedResult_hasCodedDisplayAndBlankRange() {
		Visit visit = Context.getVisitService().getVisit(501);

		LabResult malaria = findStandalone(section.gatherData(visit), "Malaria Smear");

		Assertions.assertNotNull(malaria, "Expected a Malaria Smear result");
		Assertions.assertEquals("Positive", malaria.getValue());
		Assertions.assertEquals("", malaria.getUnits());
		Assertions.assertEquals("", malaria.getRange());
	}

	@Test
	public void gatherData_textResult_hasTextValueAndBlankRange() {
		Visit visit = Context.getVisitService().getVisit(501);

		LabResult notes = findStandalone(section.gatherData(visit), "Culture Notes");

		Assertions.assertNotNull(notes, "Expected a Culture Notes result");
		Assertions.assertEquals("No growth after 48 hours", notes.getValue());
		Assertions.assertEquals("", notes.getRange());
	}

	@Test
	public void gatherData_findingClassObs_isExcludedFromLabs() {
		Visit visit = Context.getVisitService().getVisit(501);

		LabResult weight = findStandalone(section.gatherData(visit), "Weight");

		Assertions.assertNull(weight, "Finding-class obs (Weight) must not appear in labs");
	}

	@Test
	public void gatherData_voidedObs_isExcluded() {
		Visit visit = Context.getVisitService().getVisit(501);

		List<LabResult> glucoseResults = section.gatherData(visit).getStandalone().stream()
		    .filter(r -> "Serum Glucose".equals(r.getName()))
		    .collect(java.util.stream.Collectors.toList());

		Assertions.assertEquals(1, glucoseResults.size(), "Voided glucose obs must be excluded");
		Assertions.assertEquals("9.5", glucoseResults.get(0).getValue());
	}

	@Test
	public void gatherData_obsGroup_isRenderedAsPanelWithMembers() {
		Visit visit = Context.getVisitService().getVisit(501);

		LabResultsData data = section.gatherData(visit);

		Assertions.assertEquals(1, data.getGroups().size(), "Expected one lab panel");
		LabResultGroup cbc = data.getGroups().get(0);
		Assertions.assertEquals("Complete Blood Count", cbc.getHeading());
		Assertions.assertEquals(2, cbc.getResults().size());

		boolean hasWbc = cbc.getResults().stream()
		    .anyMatch(r -> "White Blood Cell Count".equals(r.getName()) && "6.5".equals(r.getValue()));
		boolean hasPlatelets = cbc.getResults().stream()
		    .anyMatch(r -> "Platelet Count".equals(r.getName()) && "250".equals(r.getValue()));
		Assertions.assertTrue(hasWbc, "Expected WBC member in the CBC panel");
		Assertions.assertTrue(hasPlatelets, "Expected Platelets member in the CBC panel");

		// Panel members must not also appear as standalone rows
		Assertions.assertNull(findStandalone(data, "White Blood Cell Count"),
		    "Panel members must not be duplicated as standalone results");
	}

	@Test
	public void gatherData_visitWithNoLabObs_returnsEmptyData() {
		// Visit 502's encounter has no obs
		Visit visit = Context.getVisitService().getVisit(502);

		LabResultsData data = section.gatherData(visit);

		Assertions.assertTrue(data.getGroups().isEmpty());
		Assertions.assertTrue(data.getStandalone().isEmpty());
	}

	@Test
	public void gatherData_visitWithNoEncounters_returnsEmptyData() {
		// Visit 504 has no encounters at all (the encounters.isEmpty() early return)
		Visit visit = Context.getVisitService().getVisit(504);

		LabResultsData data = section.gatherData(visit);

		Assertions.assertTrue(data.getGroups().isEmpty());
		Assertions.assertTrue(data.getStandalone().isEmpty());
	}

	@Test
	public void gatherData_numericRangeWithLowBoundOnly_formatsWithGreaterOrEqual() {
		Visit visit = Context.getVisitService().getVisit(503);

		LabResult sodium = findStandalone(section.gatherData(visit), "Sodium");

		Assertions.assertNotNull(sodium, "Expected a Sodium result");
		Assertions.assertEquals("≥ 135 mmol/L", sodium.getRange());
	}

	@Test
	public void gatherData_numericRangeWithHighBoundOnly_formatsWithLessOrEqual() {
		Visit visit = Context.getVisitService().getVisit(503);

		LabResult bilirubin = findStandalone(section.gatherData(visit), "Total Bilirubin");

		Assertions.assertNotNull(bilirubin, "Expected a Total Bilirubin result");
		Assertions.assertEquals("≤ 1.2 mg/dL", bilirubin.getRange());
	}

	@Test
	public void gatherData_normalInterpretation_producesNoFlag() {
		Visit visit = Context.getVisitService().getVisit(503);

		LabResult potassium = findStandalone(section.gatherData(visit), "Potassium");

		Assertions.assertNotNull(potassium, "Expected a Potassium result");
		Assertions.assertEquals("", potassium.getFlag());
		Assertions.assertEquals("3.5 – 5 mmol/L", potassium.getRange());
	}

	@Test
	public void gatherData_numericWithNoRangeSource_hasBlankRangeAndNoFlag() {
		Visit visit = Context.getVisitService().getVisit(503);

		LabResult lactate = findStandalone(section.gatherData(visit), "Lactate");

		Assertions.assertNotNull(lactate, "Expected a Lactate result");
		Assertions.assertEquals("mmol/L", lactate.getUnits());
		Assertions.assertEquals("", lactate.getRange());
		// No stored interpretation -> no flag
		Assertions.assertEquals("", lactate.getFlag());
	}

	@Test
	public void gatherData_smallDecimalValue_keepsPrecisionWithoutRounding() {
		Visit visit = Context.getVisitService().getVisit(505);

		LabResult tsh = findStandalone(section.gatherData(visit), "Thyroid Stimulating Hormone");

		Assertions.assertNotNull(tsh, "Expected a TSH result");
		Assertions.assertEquals("0.04", tsh.getValue());
	}

	@Test
	public void gatherData_largeWholeValue_isNotNarrowedToInt() {
		Visit visit = Context.getVisitService().getVisit(505);

		LabResult viralLoad = findStandalone(section.gatherData(visit), "HIV Viral Load");

		Assertions.assertNotNull(viralLoad, "Expected an HIV Viral Load result");
		// Value exceeds Integer.MAX_VALUE; the old int narrowing capped it at 2147483647
		Assertions.assertEquals("3000000000", viralLoad.getValue());
	}

	@Test
	public void gatherData_wholeNumberValue_hasNoTrailingDecimal() {
		Visit visit = Context.getVisitService().getVisit(505);

		LabResult viralLoad = findStandalone(section.gatherData(visit), "HIV Viral Load");

		Assertions.assertNotNull(viralLoad, "Expected an HIV Viral Load result");
		Assertions.assertFalse(viralLoad.getValue().contains("."), "Whole numbers must not carry a trailing decimal");
	}

	@Test
	public void gatherData_numericObsWithNoValue_rendersPlaceholderWithoutUnits() {
		Visit visit = Context.getVisitService().getVisit(505);

		LabResult amylase = findStandalone(section.gatherData(visit), "Serum Amylase");

		Assertions.assertNotNull(amylase, "Expected a Serum Amylase result");
		Assertions.assertEquals("—", amylase.getValue());
		// The concept defines units (U/L); they must not dangle next to a missing value
		Assertions.assertEquals("", amylase.getUnits());
		// A stored interpretation is real data and stays visible even without a value
		Assertions.assertEquals("High", amylase.getFlag());
	}

	@Test
	public void gatherData_panelMemberWithUnresolvableConceptName_rendersUnknown() {
		Visit visit = Context.getVisitService().getVisit(505);

		LabResultsData data = section.gatherData(visit);
		LabResultGroup panel = data.getGroups().stream()
		    .filter(g -> "Basic Metabolic Panel".equals(g.getHeading())).findFirst().orElse(null);

		Assertions.assertNotNull(panel, "Expected the Basic Metabolic Panel group");
		Assertions.assertEquals(1, panel.getResults().size());
		LabResult member = panel.getResults().get(0);
		Assertions.assertEquals("Unknown", member.getName());
		Assertions.assertEquals("42", member.getValue());
	}

	@Test
	public void gatherData_mappedInterpretation_resolvesFlagThroughMessageKey() {
		Visit visit = Context.getVisitService().getVisit(505);

		LabResult ferritin = findStandalone(section.gatherData(visit), "Ferritin");

		Assertions.assertNotNull(ferritin, "Expected a Ferritin result");
		// "Off-scale High" (hyphen) only comes from the message key; formatEnumName would yield "Off Scale High"
		Assertions.assertEquals("Off-scale High", ferritin.getFlag());
	}

	@Test
	public void gatherData_unmappedInterpretation_fallsBackToFormattedEnumName() {
		Visit visit = Context.getVisitService().getVisit(505);

		LabResult culture = findStandalone(section.gatherData(visit), "Aerobic Culture");

		Assertions.assertNotNull(culture, "Expected an Aerobic Culture result");
		// RESISTANT has no flag message key, so it falls back to the title-cased enum name
		Assertions.assertEquals("Resistant", culture.getFlag());
	}

	@Test
	public void gatherData_someConceptClassesUnresolvable_surfacesNoticeAndKeepsGood() {
		Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(
		    LabResultsSection.CONCEPT_CLASSES_PROPERTY, "Test,NoSuchClass"));
		Visit visit = Context.getVisitService().getVisit(501);
		List<String> notices = new ArrayList<>();

		LabResultsData data = section.gatherData(visit, notices);

		Assertions.assertNotNull(findStandalone(data, "Serum Glucose"),
		    "Test-class results should still resolve when a sibling class name is invalid");
		Assertions.assertEquals(1, notices.size(), "Expected one notice for the unresolved class");
		Assertions.assertTrue(notices.get(0).contains("NoSuchClass"),
		    "Notice should name the unresolved concept class");
	}

	@Test
	public void renderXml_someConceptClassesUnresolvable_emitsNoticeAlongsideData() throws Exception {
		// The notice must be a sibling of the data element — data renders AND the gap is visible.
		Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(
		    LabResultsSection.CONCEPT_CLASSES_PROPERTY, "Test,NoSuchClass"));
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = doc.createElement("root");
		doc.appendChild(root);
		Visit visit = Context.getVisitService().getVisit(501);

		section.renderXml(doc, root, visit);

		Assertions.assertEquals(2, root.getChildNodes().getLength());
		Element dataEl = (Element) root.getChildNodes().item(0);
		Assertions.assertEquals("labResults", dataEl.getNodeName());
		Assertions.assertTrue(dataEl.getElementsByTagName("lab").getLength() > 0,
		    "Resolved-class labs should still render");
		Element noticeEl = (Element) root.getChildNodes().item(1);
		Assertions.assertEquals("section-notice", noticeEl.getNodeName());
		Assertions.assertEquals("labResults", noticeEl.getAttribute("key"));
		Assertions.assertTrue(noticeEl.getAttribute("message").contains("NoSuchClass"),
		    "Notice message should name the unresolved concept class");
	}

	@Test
	public void gatherData_whenNoConceptClassesResolve_throws() {
		Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(
		    LabResultsSection.CONCEPT_CLASSES_PROPERTY, "NoSuchClass"));
		Visit visit = Context.getVisitService().getVisit(501);

		Assertions.assertThrows(IllegalStateException.class, () -> section.gatherData(visit),
		    "Expected IllegalStateException when no configured concept classes resolve");
	}

	@Test
	public void renderXml_whenConfigInvalid_emitsSectionErrorForLabResults() throws Exception {
		// End-to-end: a gatherData failure must surface as the labResults section-error banner,
		// never as an empty (silently "no labs") section.
		Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(
		    LabResultsSection.CONCEPT_CLASSES_PROPERTY, "NoSuchClass"));
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = doc.createElement("root");
		doc.appendChild(root);
		Visit visit = Context.getVisitService().getVisit(501);

		section.renderXml(doc, root, visit);

		Assertions.assertEquals(1, root.getChildNodes().getLength());
		Element errorEl = (Element) root.getChildNodes().item(0);
		Assertions.assertEquals("section-error", errorEl.getNodeName());
		Assertions.assertEquals("labResults", errorEl.getAttribute("key"));
	}

	// ── renderXml tests ───────────────────────────────────────────────────────

	@Test
	public void renderXml_withGroupsAndStandalone_producesCorrectXmlStructure() throws Exception {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = doc.createElement("root");
		doc.appendChild(root);
		LabResultGroup group = new LabResultGroup("Complete Blood Count", Arrays.asList(
		    new LabResult("White Blood Cell Count", "6.5", "10^3/uL", "", "")));
		LabResult standalone =
		    new LabResult("Serum Glucose", "9.5", "mmol/L", "4 – 7 mmol/L", "High");
		LabResultsData data =
		    new LabResultsData(Collections.singletonList(group), Collections.singletonList(standalone));

		section.renderXml(doc, root, data);

		Assertions.assertEquals(1, root.getChildNodes().getLength());
		Element labsEl = (Element) root.getChildNodes().item(0);
		Assertions.assertEquals("labResults", labsEl.getNodeName());
		Assertions.assertEquals(2, labsEl.getChildNodes().getLength());

		Element groupEl = (Element) labsEl.getChildNodes().item(0);
		Assertions.assertEquals("lab-group", groupEl.getNodeName());
		Assertions.assertEquals("Complete Blood Count", groupEl.getAttribute("heading"));
		Assertions.assertEquals(1, groupEl.getChildNodes().getLength());

		Element standaloneEl = (Element) labsEl.getChildNodes().item(1);
		Assertions.assertEquals("lab", standaloneEl.getNodeName());
		Assertions.assertEquals("Serum Glucose", standaloneEl.getAttribute("name"));
		Assertions.assertEquals("9.5", standaloneEl.getAttribute("value"));
		Assertions.assertEquals("mmol/L", standaloneEl.getAttribute("units"));
		Assertions.assertEquals("4 – 7 mmol/L", standaloneEl.getAttribute("range"));
		Assertions.assertEquals("High", standaloneEl.getAttribute("flag"));
	}

	@Test
	public void renderXml_withEmptyData_producesLabResultsElementWithNoChildren() throws Exception {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = doc.createElement("root");
		doc.appendChild(root);

		section.renderXml(doc, root, new LabResultsData(Collections.emptyList(), Collections.emptyList()));

		Assertions.assertEquals(1, root.getChildNodes().getLength());
		Element labsEl = (Element) root.getChildNodes().item(0);
		Assertions.assertEquals("labResults", labsEl.getNodeName());
		Assertions.assertEquals(0, labsEl.getChildNodes().getLength());
	}

	// ── isEnabled tests ───────────────────────────────────────────────────────

	@Test
	public void isEnabled_whenGlobalPropertySetToFalse_returnsFalse() {
		Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(
		    "report.visitSummary.section.labResults.enabled", "false"));

		Assertions.assertFalse(section.isEnabled());
	}
}
