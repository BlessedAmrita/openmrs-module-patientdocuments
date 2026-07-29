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

import java.util.List;

import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientdocuments.common.PatientDocumentsConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * SPI for pluggable visit summary sections.
 * Register implementations as Spring beans; the renderer collects them and calls
 * renderXml() on each enabled section sorted by getOrder().
 * Implementing directly (without TypedSection) is valid for sections that don't
 * fit the gather-then-render flow.
 * Config UIs list the same beans via getLabel(), isEnabled(), getOrder() and
 * isToggleable() — sections need no extra registration to appear there.
 * The same beans also drive the sample preview via renderSampleXml(), which has a
 * working default so existing and downstream implementations need no changes.
 */
public interface VisitSummarySection {

	/**
	 * Unique key — used as the global-property config suffix (e.g. "vitals", "facilityHeader").
	 * Must not contain dots.
	 */
	String getSectionKey();

	/**
	 * Activation rule. Returns true if this section should appear in the PDF.
	 * Core sections read report.visitSummary.section.&lt;key&gt;.enabled from global properties.
	 * Cross-module sections may additionally check ModuleFactory.
	 */
	boolean isEnabled();

	/**
	 * Section render position — lower values appear earlier in the document.
	 * Implementations read from config key report.visitSummary.section.&lt;key&gt;.order
	 * and fall back to a hardcoded default defined in the implementation class.
	 */
	default int getOrder() {
		return Integer.MAX_VALUE;
	}

	/**
	 * Human-readable name for this section, localized for the current user locale.
	 * Resolves the same heading message key the PDF rendering uses
	 * (patientdocuments.visitSummary.section.&lt;key&gt;.heading), falling back to a
	 * humanized form of getSectionKey() when no message is defined — so downstream
	 * sections get a usable label without shipping message bundles.
	 */
	default String getLabel() {
		String key = PatientDocumentsConstants.MODULE_ARTIFACT_ID + "." + PatientDocumentsConstants.VISIT_SUMMARY_ID
				+ ".section." + getSectionKey() + ".heading";
		String fallback = humanizeSectionKey(getSectionKey());
		try {
			return Context.getMessageSourceService().getMessage(key, null, fallback, Context.getLocale());
		}
		catch (Exception e) {
			return fallback;
		}
	}

	/**
	 * Whether administrators may switch this section off via configuration.
	 * Sections that must appear on every PDF (facility identity, patient identity,
	 * audit footer) return false so config UIs can disable their toggle.
	 */
	default boolean isToggleable() {
		return true;
	}

	/**
	 * Build this section's XML elements into the document.
	 * The Visit gives access to both the visit and its patient via visit.getPatient().
	 * Implementations should handle exceptions gracefully.
	 * TypedSection renders a section-error element on failure for clinical safety visibility.
	 */
	void renderXml(Document doc, Element root, Visit visit);

	/**
	 * Build this section's XML elements using sample content, for the settings-page
	 * preview. No patient data is involved: the preview never loads a Visit from the
	 * database, so implementations must not query for one either.
	 * <p>
	 * Add a message to {@code notices} for anything worth flagging on the preview; the
	 * caller renders each one as a section-notice element. Throwing is safe — the caller
	 * catches it and renders a visible section-error, so a broken sample never silently
	 * removes the section from the preview.
	 * <p>
	 * The default feeds the transient sample Visit built by
	 * {@link VisitSummarySampleData#newSampleVisit()} straight into renderXml(), which
	 * keeps unmigrated and downstream sections rendering populated rather than blank.
	 * Sections whose data comes from a service rather than the visit object graph should
	 * override (TypedSection subclasses override gatherSampleData instead) so the preview
	 * neither depends on a concept dictionary nor touches the database.
	 */
	default void renderSampleXml(Document doc, Element root, List<String> notices) {
		renderXml(doc, root, VisitSummarySampleData.newSampleVisit());
	}

	/**
	 * Splits a camelCase section key into capitalized words, e.g. "labResults" becomes
	 * "Lab Results". Used as the getLabel() fallback when no heading message exists.
	 */
	static String humanizeSectionKey(String sectionKey) {
		if (sectionKey == null || sectionKey.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(Character.toUpperCase(sectionKey.charAt(0)));
		for (int i = 1; i < sectionKey.length(); i++) {
			char c = sectionKey.charAt(i);
			if (Character.isUpperCase(c)) {
				sb.append(' ');
			}
			sb.append(c);
		}
		return sb.toString();
	}
}
