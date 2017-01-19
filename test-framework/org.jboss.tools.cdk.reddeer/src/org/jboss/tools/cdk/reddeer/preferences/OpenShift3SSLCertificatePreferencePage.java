/******************************************************************************* 
 * Copyright (c) 2016-2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.preferences;

import java.util.List;

import org.jboss.reddeer.common.logging.Logger;
import org.jboss.reddeer.jface.preference.PreferencePage;
import org.jboss.reddeer.swt.api.TableItem;
import org.jboss.reddeer.swt.impl.button.PushButton;
import org.jboss.reddeer.swt.impl.table.DefaultTable;

public class OpenShift3SSLCertificatePreferencePage extends PreferencePage {
	
	private static Logger log = Logger.getLogger(OpenShift3SSLCertificatePreferencePage.class);
	
	public OpenShift3SSLCertificatePreferencePage() {
		super(new String[] {"JBoss Tools", "OpenShift 3", "SSL certificates"});
	}
	
	public void printCertificates() {
		DefaultTable table = new DefaultTable();
		for (int i = 0; i < table.rowCount(); i++) {
			log.info("On index " + i + " is : " + table.getItem(i).getText(1));
		}
	}
	
	public void deleteAll() {
		DefaultTable table = new DefaultTable();
		List<TableItem> tableItems = table.getItems();
		if (tableItems.isEmpty()) {
			return;
		}
		for (TableItem item : tableItems) {
			item.select();
			new PushButton("Delete").click();
		}
	}
}
