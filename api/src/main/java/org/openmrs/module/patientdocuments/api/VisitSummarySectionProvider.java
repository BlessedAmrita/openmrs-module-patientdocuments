/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientdocuments.api;

import org.openmrs.Patient;
import org.openmrs.Visit;

/**
 * SPI interface for extensible visit summary sections.
 * Any OpenMRS module can implement this interface and register
 * as a Spring bean to contribute a section to the visit summary PDF.
 */
public interface VisitSummarySectionProvider {

	/**
	 * @return unique section key used in Initializer config and XML element names, e.g. "billing"
	 */
	String getSectionKey();

	/**
	 * @return true if this section should appear by default when no Initializer key is set
	 */
	boolean isEnabledByDefault();

	/**
	 * Gather data for this section.
	 *
	 * @param visit the visit to gather data for
	 * @param patient the patient associated with the visit
	 * @return a Map or List to be rendered as XML elements by the renderer, or null if no data exists
	 */
	Object getSectionData(Visit visit, Patient patient);
}
