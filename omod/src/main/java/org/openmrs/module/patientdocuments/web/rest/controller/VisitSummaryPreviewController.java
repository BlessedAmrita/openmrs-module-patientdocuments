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
import static org.openmrs.module.patientdocuments.common.PatientDocumentsConstants.VISIT_SUMMARY_ID;
import static org.openmrs.module.patientdocuments.common.PatientDocumentsConstants.VISIT_SUMMARY_PREVIEW_ID;

import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.module.patientdocuments.reports.VisitSummaryPdfReport;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST controller serving the visit summary sample preview PDF for the settings page.
 * <p>
 * Accessible at: {@code GET /rest/v1/patientdocuments/visitSummary/preview}
 * <p>
 * Deliberately a separate endpoint rather than a flag on the real visit summary endpoint:
 * that one takes a visitUuid and returns patient data, this one takes nothing and never
 * touches a patient record. The PDF is built from sample content through the same
 * renderer, stylesheet and FOP configuration, honouring the same enabled/order
 * configuration, so what an administrator sees here is what clinicians will get.
 * <p>
 * Supports {@code inline} exactly as the real PDF endpoint does: {@code true} (default)
 * renders in the browser, {@code false} triggers a download.
 */
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/" + MODULE_ARTIFACT_ID + "/" + VISIT_SUMMARY_ID
        + "/" + VISIT_SUMMARY_PREVIEW_ID)
public class VisitSummaryPreviewController extends BaseRestController {

	private static final Logger logger = LoggerFactory.getLogger(VisitSummaryPreviewController.class);

	private final VisitSummaryPdfReport pdfReport;

	@Autowired
	public VisitSummaryPreviewController(VisitSummaryPdfReport pdfReport) {
		this.pdfReport = pdfReport;
	}

	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<byte[]> getVisitSummaryPreview(
	        @RequestParam(value = "inline", required = false, defaultValue = "true") boolean inline) {
		try {
			byte[] pdfBytes = pdfReport.generateSamplePdf();

			HttpHeaders headers = new HttpHeaders();
			headers.set("Content-Type", "application/pdf");
			String disposition = inline ? "inline" : "attachment";
			headers.add("Content-Disposition",
			    disposition + "; filename=\"" + VISIT_SUMMARY_ID + "-" + VISIT_SUMMARY_PREVIEW_ID + ".pdf\"");
			headers.setContentLength(pdfBytes.length);

			return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
		}
		// Both types are caught: an unprivileged user raises APIAuthenticationException and an
		// unauthenticated one raises ContextAuthenticationException, which is not a subclass of
		// it — catching only the first would turn a logged-out caller into a 500.
		catch (APIAuthenticationException | ContextAuthenticationException e) {
			logger.warn("Privilege check failed for visit summary preview request: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(MediaType.TEXT_PLAIN)
			        .body("Access denied".getBytes());
		}
		catch (Exception e) {
			logger.error("An error occurred while processing the request", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.TEXT_PLAIN)
			        .body("Error generating preview PDF".getBytes());
		}
	}
}
