/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientdocuments.renderer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.Obs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EncounterXmlBuilderObsSelectionTest {

	private static final String NS = "rfe-forms";

	private Obs obsWithPath(String formFieldPath) {
		Obs obs = new Obs();
		obs.setFormField(NS, formFieldPath);
		return obs;
	}

	private Map<String, Object> question(String id) {
		Map<String, Object> q = new HashMap<>();
		q.put("id", id);
		return q;
	}

	private Map<String, Object> question(String id, String conceptRef) {
		Map<String, Object> q = question(id);
		Map<String, Object> options = new HashMap<>();
		options.put("concept", conceptRef);
		q.put("questionOptions", options);
		return q;
	}

	/** Builds a one-page, one-section schema declaring the given question ids. */
	private List<Map<String, Object>> schemaWith(String... questionIds) {
		List<Map<String, Object>> questions = new ArrayList<>();
		for (String id : questionIds) {
			questions.add(question(id));
		}
		Map<String, Object> section = new HashMap<>();
		section.put("questions", questions);
		Map<String, Object> page = new HashMap<>();
		page.put("sections", new ArrayList<>(Collections.singletonList(section)));
		return new ArrayList<>(Collections.singletonList(page));
	}

	@Test
	public void selectObsForQuestion_shouldReturnOnlyTheObsForThisFieldWhenConceptRepeatsAcrossSections() {
		EncounterXmlBuilder builder = new EncounterXmlBuilder();
		builder.collectFieldIds(schemaWith("skinColor", "skinColor_1", "skinColor_2"));

		Obs first = obsWithPath("rfe-forms-skinColor");
		Obs second = obsWithPath("rfe-forms-skinColor_1");
		Obs third = obsWithPath("rfe-forms-skinColor_2");
		List<Obs> conceptObs = Arrays.asList(first, second, third);

		Assertions.assertEquals(Collections.singletonList(first),
				builder.selectObsForQuestion(question("skinColor"), conceptObs));
		Assertions.assertEquals(Collections.singletonList(second),
				builder.selectObsForQuestion(question("skinColor_1"), conceptObs));
		Assertions.assertEquals(Collections.singletonList(third),
				builder.selectObsForQuestion(question("skinColor_2"), conceptObs));
	}

	@Test
	public void selectObsForQuestion_shouldKeepMultipleObsSharingOneFieldPath() {
		EncounterXmlBuilder builder = new EncounterXmlBuilder();
		builder.collectFieldIds(schemaWith("symptoms"));

		Obs a = obsWithPath("rfe-forms-symptoms");
		Obs b = obsWithPath("rfe-forms-symptoms");
		List<Obs> conceptObs = Arrays.asList(a, b);

		Assertions.assertEquals(conceptObs, builder.selectObsForQuestion(question("symptoms"), conceptObs));
	}

	@Test
	public void selectObsForQuestion_shouldFallBackToAllObsWhenNoFormFieldPathPresent() {
		EncounterXmlBuilder builder = new EncounterXmlBuilder();
		builder.collectFieldIds(schemaWith("apgarScore"));

		Obs a = new Obs();
		Obs b = new Obs();
		List<Obs> conceptObs = Arrays.asList(a, b);

		Assertions.assertEquals(conceptObs, builder.selectObsForQuestion(question("apgarScore"), conceptObs));
	}

	@Test
	public void selectObsForQuestion_shouldReturnEmptyWhenFieldHasNoMatchingObsButOthersDo() {
		Map<String, Object> skinColor = question("skinColor", "apgarSkin");
		Map<String, Object> skinColor1 = question("skinColor_1", "apgarSkin");
		Map<String, Object> section = new HashMap<>();
		section.put("questions", Arrays.asList(skinColor, skinColor1));
		Map<String, Object> page = new HashMap<>();
		page.put("sections", Collections.singletonList(section));

		EncounterXmlBuilder builder = new EncounterXmlBuilder();
		builder.collectFieldIds(Collections.singletonList(page));

		List<Obs> conceptObs = Collections.singletonList(obsWithPath("rfe-forms-skinColor_1"));

		Assertions.assertTrue(builder.selectObsForQuestion(skinColor, conceptObs).isEmpty());
	}

	@Test
	public void selectObsForQuestion_shouldMatchRepeatingWidgetInstances() {
		EncounterXmlBuilder builder = new EncounterXmlBuilder();
		builder.collectFieldIds(schemaWith("goal"));

		Obs g0 = obsWithPath("rfe-forms-goal_0");
		Obs g1 = obsWithPath("rfe-forms-goal_1");
		List<Obs> conceptObs = Arrays.asList(g0, g1);

		Assertions.assertEquals(conceptObs, builder.selectObsForQuestion(question("goal"), conceptObs));
	}
}
