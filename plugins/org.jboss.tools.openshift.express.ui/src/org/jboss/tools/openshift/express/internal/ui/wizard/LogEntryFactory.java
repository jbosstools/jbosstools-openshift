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

import org.jboss.tools.openshift.express.internal.ui.wizard.CreationLogDialog.LogEntry;

import com.openshift.client.IApplication;
import com.openshift.client.cartridge.IEmbeddedCartridge;

public class LogEntryFactory {


	public static LogEntry[] create(IApplication application, boolean isTimeouted){
		if (application == null) {
			return new LogEntry[] {};
		}

		return new LogEntry[] {
				new LogEntry(
						application.getName(),
						application.getCreationLog(),
						isTimeouted,
						application) 
				};
	}

	public static LogEntry[] create(Collection<IEmbeddedCartridge> cartridges, boolean isTimeouted) {
		if (cartridges == null
				|| cartridges.isEmpty()) {
			return new LogEntry[] {};
		}
		ArrayList<LogEntry> logEntries = new ArrayList<LogEntry>();
		for (IEmbeddedCartridge cartridge : cartridges) {
			logEntries.add(
					new LogEntry(
							cartridge.getName(), 
							cartridge.getCreationLog(), 
							isTimeouted,
							cartridge));
		}
		return logEntries.toArray(new LogEntry[cartridges.size()]);
	}	
}
