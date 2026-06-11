/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientdocuments.reports;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.module.patientdocuments.common.PatientDocumentsConstants;
import org.openmrs.module.patientdocuments.library.VisitSummaryDataSetDefinition;
import org.openmrs.module.patientdocuments.renderer.VisitSummaryXmlReportRenderer;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.manager.BaseReportManager;
import org.springframework.stereotype.Component;

@Component(PatientDocumentsConstants.COMPONENT_REPORTMANAGER_VISIT_SUMMARY)
public class VisitSummaryReportManager extends BaseReportManager {

	public static final String REPORT_DESIGN_UUID = "b2c3d4e5-f6a7-48b9-c0d1-e2f3a4b5c6d7";

	public static final String REPORT_DEFINITION_NAME = "Visit Summary";

	public static final String DATASET_KEY_VISIT_SUMMARY_FIELDS = "visitSummaryFields";

	private static final String VISIT_SUMMARY_PDF_NAME = "Visit Summary PDF";

	@Override
	public String getVersion() {
		return "1.1.0-SNAPSHOT";
	}

	@Override
	public String getUuid() {
		return "a1b2c3d4-e5f6-47a8-b9c0-d1e2f3a4b5c6";
	}

	@Override
	public String getName() {
		return REPORT_DEFINITION_NAME;
	}

	@Override
	public String getDescription() {
		return "";
	}

	private Parameter getVisitParameter() {
		return new Parameter("visitUuid", "Visit UUID", String.class, null, null);
	}

	@Override
	public List<Parameter> getParameters() {
		List<Parameter> params = new ArrayList<Parameter>();
		params.add(getVisitParameter());
		return params;
	}

	@Override
	public ReportDefinition constructReportDefinition() {
		ReportDefinition reportDef = new ReportDefinition();
		reportDef.setUuid(this.getUuid());
		reportDef.setName(REPORT_DEFINITION_NAME);
		reportDef.setDescription(this.getDescription());
		reportDef.setParameters(getParameters());

		VisitSummaryDataSetDefinition dsd = new VisitSummaryDataSetDefinition();
		Map<String, Object> parameterMappings = new HashMap<String, Object>();
		parameterMappings.put("visitUuid", "${visitUuid}");
		reportDef.addDataSetDefinition(DATASET_KEY_VISIT_SUMMARY_FIELDS, dsd, parameterMappings);

		return reportDef;
	}

	@Override
	public List<ReportDesign> constructReportDesigns(ReportDefinition reportDefinition) {
		ReportDesign reportDesign = new ReportDesign();
		reportDesign.setName(VISIT_SUMMARY_PDF_NAME);
		reportDesign.setUuid(REPORT_DESIGN_UUID);
		reportDesign.setReportDefinition(reportDefinition);
		reportDesign.setRendererType(VisitSummaryXmlReportRenderer.class);
		return Arrays.asList(reportDesign);
	}
}
