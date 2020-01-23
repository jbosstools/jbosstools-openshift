/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.odo;

import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.internal.core.StreamsProxy;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

/**
 * @author Jeff MAURY
 *
 */
public class OdoConsole extends IOConsole {

	private Process process;

	public OdoConsole(Process p, String...command ) {
		super(String.join(" ", command), IDebugUIConstants.ID_PROCESS_CONSOLE_TYPE, null);
		this.process = p;
		connectStreams();
	}

	/**
	 * 
	 */
	private void connectStreams() {
		StreamsProxy streamsProxy = new StreamsProxy(process, getEncoding());
		IStreamMonitor monitor = streamsProxy.getOutputStreamMonitor();
		if (monitor != null) {
			new StreamListener(newOutputStream(), monitor);
		}
		monitor = streamsProxy.getErrorStreamMonitor();
		if (monitor != null) {
			new StreamListener(newOutputStream(), monitor);
		}
	}
	
	private class StreamListener implements IStreamListener {
		private IOConsoleOutputStream stream;
		private IStreamMonitor monitor;

		private StreamListener(IOConsoleOutputStream stream, IStreamMonitor monitor) {
			this.stream = stream;
			this.monitor = monitor;
			monitor.addListener(this);
		}

		@Override
		public void streamAppended(String text, IStreamMonitor monitor) {
			try {
				stream.write(text);
			} catch (IOException e) {
				OpenShiftUIActivator.log(IStatus.ERROR, e.getLocalizedMessage(), e);
			}
		}
	}


}
