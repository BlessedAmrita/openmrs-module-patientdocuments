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

import org.openmrs.Patient;
import org.openmrs.Visit;

/**
 * SPI for pluggable visit summary sections.
 * Register implementations as Spring beans; the document builder collects them
 * and assembles the data map in @Order sequence.
 *
 * TODO: BillingSection — stub activation check wired, data gathering pending billing module integration
 * TODO: MedicationsSection, LabResultsSection, ConditionsSection — to be implemented
 */
public interface VisitSummarySection {

	/**
	 * Unique key — used as the global-property config suffix and XML element name (e.g. "vitals").
	 * Must be lowercase and contain no dots.
	 */
	String getSectionKey();

	/**
	 * Activation rule. Returns true if this section should appear in the PDF.
	 * Core sections read documents.section.&lt;key&gt;.enabled from global properties.
	 * Cross-module sections may additionally check ModuleFactory.
	 *
	 * TODO: confirm naming convention (documents.section.*.enabled)
	 */
	boolean isEnabled();

	/**
	 * Gather this section's data for the given visit.
	 * Returns a Map, List, or structured object. Null means nothing to render.
	 */
	Object getSectionData(Visit visit, Patient patient);
}