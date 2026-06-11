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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientdocuments.api.model.FooterInfo;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component
public class FooterSection extends TypedSection<FooterInfo> {

	private static final int DEFAULT_ORDER = 900;

	@Override
	protected int getDefaultOrder() {
		return DEFAULT_ORDER;
	}

	@Override
	public String getSectionKey() {
		return "footer";
	}

	/** Footer is always rendered; audit trail must appear on every PDF. */
	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	protected FooterInfo gatherData(Visit visit) {
		User currentUser = Context.getAuthenticatedUser();
		String printedBy = "";
		if (currentUser != null) {
			if (currentUser.getPersonName() != null) {
				printedBy = currentUser.getPersonName().getFullName();
			} else {
				printedBy = currentUser.getUsername();
			}
		}
		String systemId = currentUser != null ? nvl(currentUser.getSystemId()) : "";
		String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		return new FooterInfo(printedBy, systemId, timestamp);
	}

	@Override
	protected void renderXml(Document doc, Element root, FooterInfo data) {
		Element footer = doc.createElement("footer");
		footer.setAttribute("lbl-printed-by", "Printed by:");
		footer.setAttribute("lbl-system-id", "System ID:");
		root.appendChild(footer);
		addTextElement(doc, footer, "printedBy", data.getPrintedBy());
		addTextElement(doc, footer, "timestamp", data.getTimestamp());
		addTextElement(doc, footer, "systemId", data.getSystemId());
	}
}
