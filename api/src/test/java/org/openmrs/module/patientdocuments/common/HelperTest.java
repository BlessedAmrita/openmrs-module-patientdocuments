/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientdocuments.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.openmrs.util.OpenmrsUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class HelperTest extends BaseModuleContextSensitiveTest {

	@Test
	public void getFileFromAppDataDir_shouldResolveFileInsideAppDataDir() throws Exception {
		Path dir = Files.createDirectories(
				OpenmrsUtil.getApplicationDataDirectoryAsFile().toPath().resolve("printing"));
		Path stylesheet = dir.resolve("custom.xsl");
		Files.write(stylesheet, "<xsl:stylesheet/>".getBytes());

		File resolved = Helper.getFileFromAppDataDir("printing/custom.xsl");

		Assertions.assertNotNull(resolved);
		Assertions.assertEquals(stylesheet.toRealPath(), resolved.toPath().toRealPath());
	}

	@Test
	public void getFileFromAppDataDir_shouldReturnNullForBlankInput() {
		Assertions.assertNull(Helper.getFileFromAppDataDir(null));
		Assertions.assertNull(Helper.getFileFromAppDataDir(""));
		Assertions.assertNull(Helper.getFileFromAppDataDir("   "));
	}

	@Test
	public void getFileFromAppDataDir_shouldRejectPathTraversal() {
		Assertions.assertNull(Helper.getFileFromAppDataDir("../escape.xsl"));
		Assertions.assertNull(Helper.getFileFromAppDataDir("printing/../../escape.xsl"));
	}

	@Test
	public void getFileFromAppDataDir_shouldRejectAbsolutePaths() throws Exception {
		Path outside = Files.createTempFile("absolute-path-test", ".xsl");
		try {
			Assertions.assertNull(Helper.getFileFromAppDataDir(outside.toString()));
		} finally {
			Files.deleteIfExists(outside);
		}
	}

	@Test
	public void getFileFromAppDataDir_shouldReturnNullWhenFileMissing() {
		Assertions.assertNull(Helper.getFileFromAppDataDir("nonexistent/missing.xsl"));
	}
}
