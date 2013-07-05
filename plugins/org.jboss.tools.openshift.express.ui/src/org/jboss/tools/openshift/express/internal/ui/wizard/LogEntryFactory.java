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
import com.openshift.client.IField;
import com.openshift.client.ISeverity;
import com.openshift.client.Message;
import com.openshift.client.Messages;
import com.openshift.client.cartridge.IEmbeddedCartridge;

public class LogEntryFactory {


	public static LogEntry[] create(IApplication application, boolean isTimeouted){
		LogEntry[] logEntry = new LogEntry[]{};
		if (application != null) {
			List<LogEntry> entries = new ArrayList<LogEntry>();
			Collection<Message> messages = getMessages(application.getMessages());
			if (messages != null) {
				for(Message message : messages) {
					if (message != null
							&& !StringUtils.isEmpty(message.getText())) {
						entries.add(				new LogEntry(
								application.getName(),
								message.getText(),
								isTimeouted,
								application));
					}
				}
			}
			return entries.toArray(new LogEntry[entries.size()]);
		}
		return logEntry;
	}

	
	public static LogEntry[] create(Collection<IEmbeddedCartridge> cartridges, boolean isTimeouted) {
		if (cartridges == null
				|| cartridges.isEmpty()) {
			return new LogEntry[] {};
		}
		
		List<LogEntry> entries = new ArrayList<LogEntry>();
		for (IEmbeddedCartridge cartridge : cartridges) {
			Collection<Message> messages = getMessages(cartridge.getMessages());
			if (messages != null) {
				for (Message message : messages) {
					if (message != null
							&& !StringUtils.isEmpty(message.getText())) {
						entries.add(new LogEntry(
								cartridge.getName(),
								message.getText(),
								isTimeouted,
								cartridge));
					}
				}
			}
		}
			
		return entries.toArray(new LogEntry[entries.size()]);
	}	
	
	private static List<Message> getMessages(Messages messages) {
		List<Message> resultMessages = messages.getBy(IField.RESULT);
		// workaround(s) for https://issues.jboss.org/browse/JBIDE-15115
		if (resultMessages == null
				|| resultMessages.isEmpty()) {
			resultMessages = messages.getBy(IField.DEFAULT, ISeverity.RESULT);
		}
		return resultMessages;
	}
}
