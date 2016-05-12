/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.util;

import org.eclipse.swt.widgets.Display;

public class UITestUtils {

	public static final long DEFAULT_TIMEOUT = 10_000;
	
	/**
	 * Wraps a call to <code>Display.getDefault().readAndDispatch()</code>, thus waits for events 
	 * created by {@link Display#syncExec(Runnable)} or {@link Display#asyncExec(Runnable)} 
	 * to be processed. 
	 * This method will wait for all events to be processed or until timeout is reached, and will simply return.
	 * 
	 * @param timeout maximum time to wait UI events to be processed, in milliseconds
	 */
	public static void waitForDeferredEvents(long timeout) {
		boolean wait = true;
		long start = System.currentTimeMillis();
		while (wait) {
			if (!Display.getDefault().readAndDispatch() || 
					(System.currentTimeMillis() - start) > timeout) {
				wait = false;
			}
		}
	}

	
	/**
	 * Wraps a call to <code>Display.getDefault().readAndDispatch()</code>, thus waits for events 
	 * created by {@link Display#syncExec(Runnable)} or {@link Display#asyncExec(Runnable)} 
	 * to be processed. 
	 * This method will wait for all events to be processed or until the default timeout is reached (i.e. 10 seconds), and will simply return.
	 * 
	 */
	public static void waitForDeferredEvents() {
		waitForDeferredEvents(DEFAULT_TIMEOUT);
	}

}
