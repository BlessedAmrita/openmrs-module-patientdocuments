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

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.GlobalProperty;
import org.openmrs.Visit;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Tests the configurable, locale-aware date formatters shared by every visit
 * summary section, exercised through a minimal concrete subclass because the
 * formatters are protected members of AbstractVisitSummarySection.
 */
public class AbstractVisitSummarySectionTest extends BaseModuleContextSensitiveTest {

	private static final String DATE_FORMAT_PROPERTY = "report.visitSummary.dateFormat";

	private static final String DATETIME_FORMAT_PROPERTY = "report.visitSummary.datetimeFormat";

	/** Minimal section that exposes the inherited formatters to the test. */
	private static class TestSection extends AbstractVisitSummarySection {

		@Override
		public String getSectionKey() {
			return "test";
		}

		@Override
		public boolean isEnabled() {
			return true;
		}

		@Override
		public void renderXml(Document doc, Element root, Visit visit) {
			// no-op: these tests exercise the date formatters only, never rendering
		}

		String date(Date value) {
			return formatDate(value);
		}

		String datetime(Date value) {
			return formatDatetime(value);
		}
	}

	private TestSection section;

	private Date date;

	@BeforeEach
	public void setUp() {
		section = new TestSection();
		Context.setLocale(Locale.ENGLISH);

		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set(2026, Calendar.JUNE, 24, 14, 30, 5);
		date = calendar.getTime();
	}

	/**
	 * Purges the date-format global properties this class sets. The
	 * AdministrationService caches global-property values in memory, so a value
	 * saved here would otherwise outlive the test's rolled-back transaction and
	 * leak into later test classes that format dates. Purging evicts the cache so
	 * ConfigUtil falls back to the default pattern again.
	 */
	@AfterEach
	public void tearDown() {
		AdministrationService adminService = Context.getAdministrationService();
		for (String key : new String[] { DATE_FORMAT_PROPERTY, DATETIME_FORMAT_PROPERTY }) {
			GlobalProperty property = adminService.getGlobalPropertyObject(key);
			if (property != null) {
				adminService.purgeGlobalProperty(property);
			}
		}
		Context.setLocale(Locale.ENGLISH);
	}

	private void setProperty(String key, String value) {
		Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(key, value));
	}

	@Test
	public void formatDate_shouldUseDefaultPatternWhenPropertyUnset() {
		Assertions.assertEquals("2026-06-24", section.date(date));
	}

	@Test
	public void formatDatetime_shouldUseDefaultPatternWhenPropertyUnset() {
		Assertions.assertEquals("2026-06-24 14:30:05", section.datetime(date));
	}

	@Test
	public void formatDate_shouldHonourConfiguredPattern() {
		setProperty(DATE_FORMAT_PROPERTY, "dd/MM/yyyy");

		Assertions.assertEquals("24/06/2026", section.date(date));
	}

	@Test
	public void formatDatetime_shouldHonourConfiguredPattern() {
		setProperty(DATETIME_FORMAT_PROPERTY, "dd/MM/yyyy HH:mm");

		Assertions.assertEquals("24/06/2026 14:30", section.datetime(date));
	}

	@Test
	public void formatDate_shouldRenderMonthNamesInTheUserLocale() {
		setProperty(DATE_FORMAT_PROPERTY, "dd MMMM yyyy");
		Context.setLocale(Locale.FRENCH);

		Assertions.assertEquals("24 juin 2026", section.date(date));
	}

	@Test
	public void formatDate_shouldFallBackToDefaultWhenPatternIsInvalid() {
		setProperty(DATE_FORMAT_PROPERTY, "yyyy-MM-dd'");

		Assertions.assertEquals("2026-06-24", section.date(date));
	}

	@Test
	public void formatDatetime_shouldFallBackToDefaultWhenPatternIsInvalid() {
		setProperty(DATETIME_FORMAT_PROPERTY, "invalid'pattern");

		Assertions.assertEquals("2026-06-24 14:30:05", section.datetime(date));
	}

	@Test
	public void formatters_shouldReturnEmptyStringForNullDates() {
		Assertions.assertEquals("", section.date(null));
		Assertions.assertEquals("", section.datetime(null));
	}
}
