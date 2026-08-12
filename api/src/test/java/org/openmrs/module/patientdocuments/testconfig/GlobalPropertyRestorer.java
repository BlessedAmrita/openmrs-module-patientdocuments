/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientdocuments.testconfig;

import java.util.LinkedHashMap;
import java.util.Map;

import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;

/**
 * Puts global properties back the way a test found them.
 * <p>
 * {@code ConfigUtil} answers from a static cache that its GlobalPropertyListener updates on
 * every save, so a value written by a test outlives that test's rolled-back transaction and
 * is visible to every later class in the same surefire JVM. Saving the captured value back
 * drives the same listener and repairs the cache; purging evicts the entry instead.
 * <p>
 * What gets restored is always the captured value, never a hardcoded default — cleanup that
 * writes "the default" by hand becomes the next leak the day that default changes.
 */
public final class GlobalPropertyRestorer {

	private final Map<String, String> originalValues = new LinkedHashMap<>();

	private GlobalPropertyRestorer() {
	}

	/**
	 * Records what each key holds right now. Reads the stored property rather than
	 * ConfigUtil, so a value an earlier class left in the cache is not mistaken for the
	 * baseline and written back at the end.
	 */
	public static GlobalPropertyRestorer capture(String... keys) {
		GlobalPropertyRestorer restorer = new GlobalPropertyRestorer();
		AdministrationService adminService = Context.getAdministrationService();
		for (String key : keys) {
			GlobalProperty property = adminService.getGlobalPropertyObject(key);
			restorer.originalValues.put(key, property != null ? property.getPropertyValue() : null);
		}
		return restorer;
	}

	/**
	 * Restores every captured key. A key that was unset when captured is purged rather than
	 * written back as blank, so the section falls back to its coded default exactly as it
	 * did before the test ran.
	 */
	public void restore() {
		AdministrationService adminService = Context.getAdministrationService();
		for (Map.Entry<String, String> original : originalValues.entrySet()) {
			GlobalProperty current = adminService.getGlobalPropertyObject(original.getKey());
			if (original.getValue() == null) {
				if (current != null) {
					adminService.purgeGlobalProperty(current);
				}
			} else if (current == null || !original.getValue().equals(current.getPropertyValue())) {
				adminService.saveGlobalProperty(new GlobalProperty(original.getKey(), original.getValue()));
			}
		}
	}

	/**
	 * Drops each key so its coded default applies again, whatever an earlier class left in
	 * the cache. Saves before purging because purging needs a row to delete, and a write
	 * that has since been rolled back leaves a cached value with no row behind it.
	 */
	public static void clear(String... keys) {
		AdministrationService adminService = Context.getAdministrationService();
		for (String key : keys) {
			adminService.saveGlobalProperty(new GlobalProperty(key, ""));
			adminService.purgeGlobalProperty(adminService.getGlobalPropertyObject(key));
		}
	}
}
