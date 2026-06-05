/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientdocuments.renderer;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.openmrs.util.OpenmrsUtil;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class EncounterPdfReportRendererStylesheetTest extends BaseModuleContextSensitiveTest {

	private final EncounterPdfReportRenderer renderer = new EncounterPdfReportRenderer();

	@Test
	public void getStylesheetStream_shouldFallBackToDefaultWhenNothingConfigured() throws Exception {
		try (InputStream s = renderer.getStylesheetStream(null)) {
			Assertions.assertNotNull(s, "default classpath stylesheet must be readable");
			String body = IOUtils.toString(s, StandardCharsets.UTF_8);
			Assertions.assertTrue(body.contains("xsl:stylesheet"));
		}
	}

	@Test
	public void getStylesheetStream_shouldFallBackToDefaultWhenConfiguredPathIsBlank() throws Exception {
		try (InputStream s = renderer.getStylesheetStream("   ")) {
			Assertions.assertNotNull(s);
			Assertions.assertTrue(IOUtils.toString(s, StandardCharsets.UTF_8).contains("xsl:stylesheet"));
		}
	}

	@Test
	public void getStylesheetStream_shouldLoadConfiguredFileFromAppDataDir() throws Exception {
		Path dir = Files.createDirectories(
				OpenmrsUtil.getApplicationDataDirectoryAsFile().toPath().resolve("printing"));
		Path stylesheet = dir.resolve("custom.xsl");
		String marker = "<!-- custom-encounter-marker -->";
		Files.write(stylesheet, marker.getBytes(StandardCharsets.UTF_8));

		try (InputStream s = renderer.getStylesheetStream("printing/custom.xsl")) {
			Assertions.assertEquals(marker, IOUtils.toString(s, StandardCharsets.UTF_8));
		}
	}

	@Test
	public void getStylesheetStream_shouldFallBackWhenConfiguredFileMissing() throws Exception {
		try (InputStream s = renderer.getStylesheetStream("printing/missing.xsl")) {
			Assertions.assertNotNull(s, "must fall back to the default classpath stylesheet");
			Assertions.assertTrue(IOUtils.toString(s, StandardCharsets.UTF_8).contains("xsl:stylesheet"));
		}
	}

	@Test
	public void getStylesheetStream_shouldFallBackOnPathTraversal() throws Exception {
		try (InputStream s = renderer.getStylesheetStream("../etc/passwd")) {
			Assertions.assertNotNull(s);
			Assertions.assertTrue(IOUtils.toString(s, StandardCharsets.UTF_8).contains("xsl:stylesheet"));
		}
	}
}
