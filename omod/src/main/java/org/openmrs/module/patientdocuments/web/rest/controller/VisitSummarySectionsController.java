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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.module.patientdocuments.api.section.VisitSummarySection;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.openmrs.util.PrivilegeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * REST controller that lists the registered visit summary sections with their
 * effective configuration, for admin/config UIs.
 * <p>
 * Accessible at: {@code GET /rest/v1/patientdocuments/visitSummary/sections}
 * <p>
 * Returns the standard REST collection shape — a {@code results} array of
 * {@code {sectionKey, label, enabled, order, toggleable}} objects sorted by
 * effective order, the same order the PDF renderer applies.
 */
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/" + MODULE_ARTIFACT_ID + "/" + VISIT_SUMMARY_ID
        + "/sections")
public class VisitSummarySectionsController extends BaseRestController {

	private static final Logger logger = LoggerFactory.getLogger(VisitSummarySectionsController.class);

	@Autowired(required = false)
	private List<VisitSummarySection> sections;

	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<Object> getSections() {
		try {
			Context.requirePrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);

			List<SimpleObject> results = new ArrayList<>();
			if (sections != null) {
				List<VisitSummarySection> ordered = new ArrayList<>(sections);
				ordered.sort(Comparator.comparingInt(VisitSummarySection::getOrder));
				for (VisitSummarySection section : ordered) {
					SimpleObject entry = new SimpleObject();
					entry.add("sectionKey", section.getSectionKey());
					entry.add("label", section.getLabel());
					entry.add("enabled", section.isEnabled());
					entry.add("order", section.getOrder());
					entry.add("toggleable", section.isToggleable());
					results.add(entry);
				}
			}
			return new ResponseEntity<>(new SimpleObject().add("results", results), HttpStatus.OK);
		}
		catch (APIAuthenticationException | ContextAuthenticationException e) {
			logger.warn("Privilege check failed for visit summary sections request: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(MediaType.TEXT_PLAIN).body("Access denied");
		}
		catch (Exception e) {
			logger.error("An error occurred while processing the request", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.TEXT_PLAIN)
			        .body("Error listing visit summary sections");
		}
	}
}
