/******************************************************************************* 
 * Copyright (c) 2016-2019 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.ui.preferences;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.util.Display;
import org.eclipse.reddeer.common.util.ResultRunnable;
import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.jface.preference.PreferencePage;
import org.eclipse.reddeer.swt.api.StyledText;
import org.eclipse.reddeer.swt.api.TableItem;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.table.DefaultTable;
import org.jboss.tools.cdk.reddeer.core.certificate.UntrustedSSLCertificate;

public class OpenShift3SSLCertificatePreferencePage extends PreferencePage {
	
	private static Logger log = Logger.getLogger(OpenShift3SSLCertificatePreferencePage.class);
	
	public OpenShift3SSLCertificatePreferencePage(ReferencedComposite composite) {
		super(composite, "JBoss Tools", "OpenShift", "SSL certificates");
	}
	
	public void printCertificates() {
		DefaultTable table = new DefaultTable();
		for (int i = 0; i < table.rowCount(); i++) {
			log.info("On index " + i + " is : " + table.getItem(i).getText(1));
		}
	}
	
	public static Properties readCertificateDataFromTableItem(TableItem item) {
		Object itemData = Display.syncExec(new ResultRunnable<Object>() {
			@Override
			public Object run() {
				return item.getSWTWidget().getData();
			}
		});
		List<String[]> ar = Arrays.asList(itemData.toString().split(System.lineSeparator())).stream().map(s -> {
			String replaced = s.replace("\t", "");
			return replaced.split(":", 2);
			}).collect(Collectors.toList());
		Properties props = new Properties();
		ar.stream().forEach(x -> props.put(x[0], x[1]));
		return props;
	}
	
	public static UntrustedSSLCertificate processCertificate(TableItem item) {
		Properties props = readCertificateDataFromTableItem(item);
		return new UntrustedSSLCertificate(
				props.get("Issued To").toString().split(": ")[1], 
				props.get("Issued By").toString().split(": ")[1], 
				props.get("SHA1 Fingerprint").toString());
	}
	
	public static UntrustedSSLCertificate processCertificate(StyledText text) {
		String[] lines = text.getText().split(System.lineSeparator());
		return new UntrustedSSLCertificate(
				lines[getIndexOfValue(lines, "Issued To") + 1].split(": ")[1], 
				lines[getIndexOfValue(lines, "Issued By") + 1].split(": ")[1], 
				lines[getIndexOfValue(lines, "SHA1 Fingerprint") + 1]);
		
	}
	
	private static int getIndexOfValue(String [] lines, String value) {
		for(int i = 0; i < lines.length; i++) {
			if (lines[i].contains(value)) {
				return i;
			}
		}
		return -1;
	}
	
	public void deleteAll() {
		DefaultTable table = new DefaultTable();
		List<TableItem> tableItems = table.getItems();
		if (tableItems.isEmpty()) {
			return;
		}
		table.selectAll();
		new PushButton("Delete").click();
	}
}
