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

import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Tests for the VisitSummarySection default methods (getLabel, isToggleable),
 * exercised through an anonymous implementation the way a downstream module
 * would provide one — no changes in this module, no message bundle.
 */
public class VisitSummarySectionDefaultsTest extends BaseModuleContextSensitiveTest {

	private VisitSummarySection sectionWithKey(String sectionKey) {
		return new VisitSummarySection() {

			@Override
			public String getSectionKey() {
				return sectionKey;
			}

			@Override
			public boolean isEnabled() {
				return true;
			}

			@Override
			public void renderXml(Document doc, Element root, Visit visit) {
				// no-op: these tests exercise getLabel()/isToggleable() only, never rendering
			}
		};
	}

	@BeforeEach
	public void setUp() {
		Context.setLocale(Locale.ENGLISH);
	}

	@Test
	public void getLabel_shouldResolveHeadingMessageForKnownSectionKey() {
		Assertions.assertEquals("Vital Signs", sectionWithKey("vitals").getLabel());
	}

	@Test
	public void getLabel_shouldHumanizeSectionKeyWhenNoHeadingMessageExists() {
		Assertions.assertEquals("Referral Notes", sectionWithKey("referralNotes").getLabel());
	}

	@Test
	public void isToggleable_shouldDefaultToTrue() {
		Assertions.assertTrue(sectionWithKey("referralNotes").isToggleable());
	}

	@Test
	public void humanizeSectionKey_shouldHandleSingleWordAndEmptyKeys() {
		Assertions.assertEquals("Vitals", VisitSummarySection.humanizeSectionKey("vitals"));
		Assertions.assertEquals("", VisitSummarySection.humanizeSectionKey(""));
		Assertions.assertEquals("", VisitSummarySection.humanizeSectionKey(null));
	}
}
