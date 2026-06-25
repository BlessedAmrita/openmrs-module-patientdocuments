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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Convenience base for sections that follow the gather-then-render pattern.
 * Subclasses implement typed gatherData() and renderXml(doc, root, T) separately.
 * The VisitSummarySection interface remains open for implementations that don't
 * fit the typed flow (e.g. sections that stream data or delegate to sub-renderers).
 */
public abstract class TypedSection<T> extends AbstractVisitSummarySection {

	private static final Logger log = LoggerFactory.getLogger(TypedSection.class);

	/**
	 * Gather this section's data from the visit.
	 * Return null to skip rendering entirely (no XML output for this section).
	 */
	protected abstract T gatherData(Visit visit);

	/**
	 * Build XML elements for this section's typed data.
	 */
	protected abstract void renderXml(Document doc, Element root, T data);

	/**
	 * Orchestrates gather → render. Final so subclasses can't break the flow.
	 * A failure in either phase is logged and skipped rather than crashing the whole PDF.
	 */
	@Override
	public final void renderXml(Document doc, Element root, Visit visit) {
		try {
			T data = gatherData(visit);
			if (data != null) {
				renderXml(doc, root, data);
			}
		}
		catch (Exception e) {
			log.error("Section '{}' failed to render", getSectionKey(), e);
			Element errorEl = doc.createElement("section-error");
			errorEl.setAttribute("key", getSectionKey());
			errorEl.setAttribute("message", msg("patientdocuments.visitSummary.common.sectionError",
					"Unable to load data for this section"));
			root.appendChild(errorEl);
		}
	}
}
