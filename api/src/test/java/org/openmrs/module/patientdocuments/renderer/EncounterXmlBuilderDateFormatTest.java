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
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.Obs;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;

import java.text.SimpleDateFormat;
import java.util.Date;

public class EncounterXmlBuilderDateFormatTest extends BaseModuleContextSensitiveTest {

	private final EncounterXmlBuilder builder = new EncounterXmlBuilder();

	private Date at(String yyyyMmDdHhMmSs) throws Exception {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(yyyyMmDdHhMmSs);
	}

	private Obs obsWithDatatype(String datatypeUuid, Date value) {
		ConceptDatatype datatype = new ConceptDatatype();
		datatype.setUuid(datatypeUuid);
		Concept concept = new Concept();
		concept.setDatatype(datatype);
		Obs obs = new Obs();
		obs.setConcept(concept);
		obs.setValueDatetime(value);
		return obs;
	}

	@Test
	public void formatDateObsValue_shouldFormatDatetimeWithoutLongStyleArtifacts() throws Exception {
		Obs obs = obsWithDatatype(ConceptDatatype.DATETIME_UUID, at("2026-07-04 09:25:00"));

		String result = builder.formatDateObsValue(obs);

		Assertions.assertEquals("2026-07-04 09:25", result);
	}

	@Test
	public void formatDateObsValue_shouldFormatDateAsIsoWithoutTime() throws Exception {
		Obs obs = obsWithDatatype(ConceptDatatype.DATE_UUID, at("2026-07-01 00:00:00"));

		String result = builder.formatDateObsValue(obs);

		Assertions.assertEquals("2026-07-01", result);
	}

	@Test
	public void formatDateObsValue_shouldFormatTimeAs24Hour() throws Exception {
		Obs obs = obsWithDatatype(ConceptDatatype.TIME_UUID, at("2026-07-01 14:05:00"));

		String result = builder.formatDateObsValue(obs);

		Assertions.assertEquals("14:05", result);
	}

	@Test
	public void formatDateObsValue_shouldReturnNullForNonDateConcept() throws Exception {
		Obs obs = obsWithDatatype(ConceptDatatype.NUMERIC_UUID, null);

		Assertions.assertNull(builder.formatDateObsValue(obs));
	}
}
