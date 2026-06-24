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

import org.openmrs.api.context.Context;
import org.openmrs.module.patientdocuments.common.PatientDocumentsConstants;
import org.openmrs.util.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Convenience base for sections that read their enabled flag and render order
 * from global properties via ConfigUtil.
 */
public abstract class AbstractVisitSummarySection implements VisitSummarySection {

	private static final Logger log = LoggerFactory.getLogger(AbstractVisitSummarySection.class);

	protected boolean isConfigEnabled(String globalPropertyKey, boolean defaultValue) {
		String value = ConfigUtil.getProperty(globalPropertyKey);
		if (value != null) {
			value = value.trim();
			if ("true".equalsIgnoreCase(value)) {
				return true;
			} else if ("false".equalsIgnoreCase(value)) {
				return false;
			}
			log.warn("Global property '{}' has invalid value '{}'; defaulting to {}",
					globalPropertyKey, value, defaultValue);
		}
		return defaultValue;
	}

	/**
	 * Returns the default integer order for this section.
	 * Subclasses override this to return their section-specific constant.
	 * Falls through to Integer.MAX_VALUE so unknown/test sections sort last.
	 */
	protected int getDefaultOrder() {
		return Integer.MAX_VALUE;
	}

	/**
	 * Reads the render position from
	 * report.visitSummary.section.&lt;sectionKey&gt;.order, falling back to
	 * getDefaultOrder(). Invalid (non-integer) values are logged and ignored.
	 */
	@Override
	public int getOrder() {
		String key = PatientDocumentsConstants.VISIT_SUMMARY_SECTION_PREFIX + getSectionKey() + ".order";
		String defaultStr = String.valueOf(getDefaultOrder());
		String valueStr = ConfigUtil.getProperty(key, defaultStr);
		try {
			return Integer.parseInt(valueStr.trim());
		}
		catch (NumberFormatException e) {
			log.warn("Global property '{}' has invalid value '{}'; defaulting to {}",
					key, valueStr, getDefaultOrder());
			return getDefaultOrder();
		}
	}

	/**
	 * Reads report.visitSummary.section.&lt;sectionKey&gt;.enabled (defaults true).
	 * Override in subclasses that need additional checks (e.g. ModuleFactory for billing).
	 */
	@Override
	public boolean isEnabled() {
		return isConfigEnabled(
				PatientDocumentsConstants.VISIT_SUMMARY_SECTION_PREFIX + getSectionKey() + ".enabled", true);
	}

	protected void addTextElement(Document doc, Element parent, String tag, String value) {
		Element el = doc.createElement(tag);
		el.setTextContent(value != null ? value : "");
		parent.appendChild(el);
	}

	protected String nvl(String value) {
		return value != null ? value : "";
	}

	// Looks up a message by key for the current locale; returns the fallback (not the raw key) if the key is missing.
	protected String msg(String key, String fallback) {
		return Context.getMessageSourceService().getMessage(key, null, fallback, Context.getLocale());
	}

	// "ENTERED_IN_ERROR" -> "Entered In Error", "CONFIRMED" -> "Confirmed"; "" for null/empty.
	protected String formatEnumName(String rawEnumName) {
		if (rawEnumName == null || rawEnumName.isEmpty()) {
			return "";
		}
		String[] parts = rawEnumName.split("_");
		StringBuilder sb = new StringBuilder();
		for (String part : parts) {
			if (part.isEmpty()) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append(" ");
			}
			sb.append(Character.toUpperCase(part.charAt(0)))
					.append(part.substring(1).toLowerCase());
		}
		return sb.toString();
	}
}
