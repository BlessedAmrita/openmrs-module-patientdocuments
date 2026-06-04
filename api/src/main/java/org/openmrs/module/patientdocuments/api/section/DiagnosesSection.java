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

import java.util.ArrayList;
import java.util.List;

import org.openmrs.Diagnosis;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientdocuments.api.model.DiagnosisEntry;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component
public class DiagnosesSection extends TypedSection<List<DiagnosisEntry>> {

	private static final int DEFAULT_ORDER = 400;

	@Override
	protected int getDefaultOrder() {
		return DEFAULT_ORDER;
	}

	@Override
	public String getSectionKey() {
		return "diagnoses";
	}

	@Override
	protected List<DiagnosisEntry> gatherData(Visit visit) {
		List<DiagnosisEntry> diagnoses = new ArrayList<>();

		List<Diagnosis> diagnosisList = Context.getDiagnosisService()
		        .getDiagnosesByVisit(visit, false, false);
		if (diagnosisList == null) {
			return diagnoses;
		}
		for (Diagnosis diagnosis : diagnosisList) {
			String name = "";
			if (diagnosis.getDiagnosis() != null) {
				if (diagnosis.getDiagnosis().getCoded() != null
				        && diagnosis.getDiagnosis().getCoded().getName() != null) {
					name = diagnosis.getDiagnosis().getCoded().getName().getName();
				} else if (diagnosis.getDiagnosis().getNonCoded() != null) {
					name = diagnosis.getDiagnosis().getNonCoded();
				}
			}
			String certainty = diagnosis.getCertainty() != null ? diagnosis.getCertainty().name() : "";
			String rank = diagnosis.getRank() != null ? String.valueOf(diagnosis.getRank()) : "";
			diagnoses.add(new DiagnosisEntry(name, certainty, rank));
		}

		return diagnoses;
	}

	@Override
	protected void renderXml(Document doc, Element root, List<DiagnosisEntry> diagnoses) {
		Element section = doc.createElement("diagnoses");
		section.setAttribute("heading", "Diagnoses");
		section.setAttribute("col-name", "Diagnosis");
		section.setAttribute("col-certainty", "Certainty");
		section.setAttribute("col-rank", "Rank");
		root.appendChild(section);

		for (DiagnosisEntry diag : diagnoses) {
			Element diagEl = doc.createElement("diagnosis");
			diagEl.setAttribute("name", nvl(diag.getName()));
			diagEl.setAttribute("certainty", nvl(diag.getCertainty()));
			diagEl.setAttribute("rank", nvl(diag.getRank()));
			section.appendChild(diagEl);
		}
	}
}
