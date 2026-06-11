/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientdocuments.library;

import org.openmrs.Visit;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.dataset.DataSet;
import org.openmrs.module.reporting.dataset.DataSetColumn;
import org.openmrs.module.reporting.dataset.DataSetRow;
import org.openmrs.module.reporting.dataset.SimpleDataSet;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.evaluator.DataSetEvaluator;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.springframework.stereotype.Component;

/**
 * Evaluates a {@link VisitSummaryDataSetDefinition} by resolving the Visit from its UUID
 * and placing it in the DataSet. Each section gathers and renders its own data from the Visit.
 */
@Component
@Handler(supports = VisitSummaryDataSetDefinition.class, order = 50)
public class VisitSummaryDataSetEvaluator implements DataSetEvaluator {

	@Override
	public DataSet evaluate(DataSetDefinition dataSetDefinition, EvaluationContext evalContext) {
		SimpleDataSet dataSet = new SimpleDataSet(dataSetDefinition, evalContext);

		String visitUuid = (String) evalContext.getParameterValue("visitUuid");
		if (visitUuid == null) {
			return dataSet;
		}

		Visit visit = Context.getVisitService().getVisitByUuid(visitUuid);
		if (visit == null) {
			return dataSet;
		}

		DataSetRow row = new DataSetRow();
		row.addColumnValue(new DataSetColumn("visit", "Visit", Visit.class), visit);
		dataSet.addRow(row);

		return dataSet;
	}
}
