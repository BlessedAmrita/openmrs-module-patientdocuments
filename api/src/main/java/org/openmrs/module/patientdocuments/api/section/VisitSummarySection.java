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

import org.openmrs.Visit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * SPI for pluggable visit summary sections.
 * Register implementations as Spring beans; the renderer collects them and calls
 * renderXml() on each enabled section in @Order sequence.
 * Implementing directly (without TypedSection) is valid for sections that don't
 * fit the gather-then-render flow.
 */
public interface VisitSummarySection {

	/**
	 * Unique key — used as the global-property config suffix (e.g. "vitals", "facilityHeader").
	 * Must not contain dots.
	 */
	String getSectionKey();

	/**
	 * Activation rule. Returns true if this section should appear in the PDF.
	 * Core sections read documents.section.&lt;key&gt;.enabled from global properties.
	 * Cross-module sections may additionally check ModuleFactory.
	 */
	boolean isEnabled();

	/**
	 * Build this section's XML elements into the document.
	 * The Visit gives access to both the visit and its patient via visit.getPatient().
	 * Implementations should handle exceptions gracefully. 
	 * TypedSection renders a section-error element on failure for clinical safety visibility.
	 */
	void renderXml(Document doc, Element root, Visit visit);
}
