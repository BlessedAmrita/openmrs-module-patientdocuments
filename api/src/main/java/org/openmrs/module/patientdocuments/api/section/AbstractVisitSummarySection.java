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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Convenience base for sections that read their enabled flag from a global property.
 * At runtime we read from global properties via AdministrationService — NOT InitializerService.
 */
public abstract class AbstractVisitSummarySection implements VisitSummarySection {

	/**
	 * Reads a boolean from the global properties table.
	 */

	private static final Logger log = LoggerFactory.getLogger(AbstractVisitSummarySection.class);

	protected boolean isConfigEnabled(String globalPropertyKey, boolean defaultValue) {
		String value = Context.getAdministrationService()
				.getGlobalProperty(globalPropertyKey, String.valueOf(defaultValue));
		
		if ("true".equalsIgnoreCase(value != null ? value.trim() : "")) {
			return true;
		}
		if ("false".equalsIgnoreCase(value != null ? value.trim() : "")) {
			return false;
		}
		// Value is neither "true" nor "false" 
		log.warn("Global property '{}' has invalid boolean value '{}'; defaulting to {}",
				globalPropertyKey, value, defaultValue);
		return defaultValue;
	}

	/**
	 * Default: reads documents.section.&lt;key&gt;.enabled (defaults to true).
	 * Override in subclasses that need additional checks (e.g. ModuleFactory for billing).
	 */
	@Override
	public boolean isEnabled() {
		return isConfigEnabled("documents.section." + getSectionKey() + ".enabled", true);
	}

	protected void addTextElement(Document doc, Element parent, String tag, String value) {
		Element el = doc.createElement(tag);
		el.setTextContent(value != null ? value : "");
		parent.appendChild(el);
	}

	protected String nvl(String value) {
		return value != null ? value : "";
	}
}
