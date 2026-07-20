/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientdocuments.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtilTest {

	private Date at(String value) throws Exception {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(value);
	}

	@Test
	public void formatDate_shouldFormatAsIsoDate() throws Exception {
		Assertions.assertEquals("2026-07-04", DateUtil.formatDate(at("2026-07-04 09:25:00")));
	}

	@Test
	public void formatTime_shouldFormatAs24HourTime() throws Exception {
		Assertions.assertEquals("09:25", DateUtil.formatTime(at("2026-07-04 09:25:00")));
	}

	@Test
	public void formatDateTime_shouldFormatAsIsoDateAndTime() throws Exception {
		Assertions.assertEquals("2026-07-04 09:25", DateUtil.formatDateTime(at("2026-07-04 09:25:00")));
	}

	@Test
	public void formatters_shouldReturnNullForNullDate() {
		Assertions.assertNull(DateUtil.formatDate(null));
		Assertions.assertNull(DateUtil.formatTime(null));
		Assertions.assertNull(DateUtil.formatDateTime(null));
	}
}
