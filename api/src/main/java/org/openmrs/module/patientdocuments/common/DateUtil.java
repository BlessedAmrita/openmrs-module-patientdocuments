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

import java.text.SimpleDateFormat;
import java.util.Date;

public final class DateUtil {

	public static final String DATE_PATTERN = "yyyy-MM-dd";

	public static final String TIME_PATTERN = "HH:mm";

	public static final String DATE_TIME_PATTERN = DATE_PATTERN + " " + TIME_PATTERN;

	private DateUtil() {
	}

	public static String formatDate(Date date) {
		return date == null ? null : new SimpleDateFormat(DATE_PATTERN).format(date);
	}

	public static String formatTime(Date date) {
		return date == null ? null : new SimpleDateFormat(TIME_PATTERN).format(date);
	}

	public static String formatDateTime(Date date) {
		return date == null ? null : new SimpleDateFormat(DATE_TIME_PATTERN).format(date);
	}
}
