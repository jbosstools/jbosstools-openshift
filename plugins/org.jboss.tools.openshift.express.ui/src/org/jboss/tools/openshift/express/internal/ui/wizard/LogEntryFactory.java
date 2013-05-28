/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.CreationLogDialog.LogEntry;

import com.openshift.client.IApplication;
import com.openshift.client.Message;
import com.openshift.client.cartridge.IEmbeddedCartridge;

public class LogEntryFactory {


	public static LogEntry[] create(IApplication application, boolean isTimeouted){
		LogEntry[] logEntry = new LogEntry[]{};
		if (application != null) {
			String text = getMessageText(application.getMessage(Message.FIELD_RESULT));
			if (!StringUtils.isEmpty(text)) {
				logEntry = new LogEntry[] {
				new LogEntry(
						application.getName(),
						text,
						isTimeouted,
						application) 
				};
			}
		}
		return logEntry;
	}

	public static LogEntry[] create(Collection<IEmbeddedCartridge> cartridges, boolean isTimeouted) {
		if (cartridges == null
				|| cartridges.isEmpty()) {
			return new LogEntry[] {};
		}
		
		List<LogEntry> logEntries = new ArrayList<LogEntry>();
		for (IEmbeddedCartridge cartridge : cartridges) {
			String text = getMessageText(cartridge.getMessage(Message.FIELD_RESULT));
			if (StringUtils.isEmpty(text)) {
				continue;
			}
			logEntries.add(
					new LogEntry(
							cartridge.getName(), 
							text, 
							isTimeouted,
							cartridge));
		}
		return logEntries.toArray(new LogEntry[cartridges.size()]);
	}	
	
	private static String getMessageText(Message message) {
		if (message == null) {
			return null;
		}
		
		return message.getText();
	}
}
