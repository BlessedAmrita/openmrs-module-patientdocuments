/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientdocuments.web.rest.controller;

import static org.openmrs.module.patientdocuments.common.PatientDocumentsConstants.MODULE_ARTIFACT_ID;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST controller that exposes a PDF export endpoint for visit summaries.
 * <p>
 * Accessible at: {@code GET /rest/v1/patientdocuments/visitSummary?visitUuid=&lt;uuid&gt;}
 * <p>
 * Supports {@code inline} parameter to control Content-Disposition behaviour:
 * {@code true} (default) renders the PDF inline in the browser; {@code false} triggers a download.
 */
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/" + MODULE_ARTIFACT_ID + "/visitSummary")
public class VisitSummaryPdfExportController extends BaseRestController {

	private static final Logger logger = LoggerFactory.getLogger(VisitSummaryPdfExportController.class);

	private static final String VISIT_SUMMARY_ID = "visitSummary";

	private void writeResponse(HttpServletResponse response, String visitUuid, boolean inline) throws IOException {
		// TODO: Wire to VisitSummaryPdfReport.generatePdf(visitUuid)
		String minimalPdf = "%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
		        + "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
		        + "3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R>>endobj\n"
		        + "xref\n0 4\n0000000000 65535 f \n0000000009 00000 n \n"
		        + "0000000058 00000 n \n0000000115 00000 n \n"
		        + "trailer<</Size 4/Root 1 0 R>>\nstartxref\n190\n%%EOF";
		byte[] pdfBytes = minimalPdf.getBytes();

		response.setContentType("application/pdf");
		String disposition = inline ? "inline" : "attachment";
		response.setHeader("Content-Disposition", disposition + "; filename=\"" + VISIT_SUMMARY_ID + ".pdf\"");
		response.setContentLength(pdfBytes.length);
		response.getOutputStream().write(pdfBytes);
	}

	@RequestMapping(method = RequestMethod.GET)
	public void getVisitSummary(HttpServletResponse response,
	        @RequestParam(value = "visitUuid") String visitUuid,
	        @RequestParam(value = "inline", required = false, defaultValue = "true") boolean inline) {

		try {
			writeResponse(response, visitUuid, inline);
		}
		catch (Exception e) {
			logger.error("An error occurred while processing the request", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
}
