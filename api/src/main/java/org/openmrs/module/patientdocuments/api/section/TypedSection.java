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

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.openmrs.Visit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Convenience base for sections that follow the gather-then-render pattern.
 * Subclasses implement typed gatherData() and renderXml(doc, root, T) separately.
 * The VisitSummarySection interface remains open for implementations that don't
 * fit the typed flow (e.g. sections that stream data or delegate to sub-renderers).
 * The sample preview reuses the same typed renderXml(): see gatherSampleData().
 */
@Slf4j
public abstract class TypedSection<T> extends AbstractVisitSummarySection {

	/**
	 * Gather this section's data from the visit.
	 * Return null to skip rendering entirely (no XML output for this section).
	 */
	protected abstract T gatherData(Visit visit);

	/**
	 * Gather variant for sections with partial-failure modes: add a message to
	 * {@code notices} for anything configured that could not be loaded, and it renders
	 * as a warning banner alongside the section's data. The collector is per-call
	 * because sections are singleton beans and renders can be concurrent.
	 */
	protected T gatherData(Visit visit, List<String> notices) {
		return gatherData(visit);
	}

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
		List<String> notices = new ArrayList<>();
		try {
			T data = gatherData(visit, notices);
			if (data != null) {
				renderXml(doc, root, data);
			}
			for (String message : notices) {
				Element noticeEl = doc.createElement("section-notice");
				noticeEl.setAttribute("key", getSectionKey());
				noticeEl.setAttribute("message", message);
				root.appendChild(noticeEl);
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

	/**
	 * Sample content for this section, for the settings-page preview.
	 * The default runs the real gather against the transient sample Visit, so a section
	 * that reads the visit object graph is populated without writing any sample code.
	 * Sections that gather through a service should override and return in-memory data
	 * instead — that keeps the preview off the database and out of the concept dictionary.
	 * Returning null skips the section, exactly as on the real path; throwing surfaces as
	 * a visible section-error, so a broken sample is never silently dropped.
	 */
	protected T gatherSampleData(List<String> notices) {
		return gatherData(VisitSummarySampleData.newSampleVisit(), notices);
	}

	/**
	 * Renders sample content through the same typed renderXml() the real path uses, so
	 * the preview cannot drift from the real layout. Exceptions deliberately propagate:
	 * the preview renderer catches them uniformly, covering non-TypedSection
	 * implementations too.
	 */
	@Override
	public void renderSampleXml(Document doc, Element root, List<String> notices) {
		T data = gatherSampleData(notices);
		if (data != null) {
			renderXml(doc, root, data);
		}
	}
}
