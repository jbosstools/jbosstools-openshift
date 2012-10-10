/******************************************************************************* 
 * Copyright (c) 2011 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.console;

import com.jcraft.jsch.Logger;

/**
 * Bridge between the JSch logger and the Eclipse logger (to ouput results
 * in the .log files and/or into the 'Error log' view.
 * 
 * @author Xavier Coulon
 * 
 */
public class JschToEclipseLogger implements Logger {

	static java.util.Hashtable<Integer, String> name = new java.util.Hashtable<Integer, String>();
	static {
		name.put(new Integer(DEBUG), "DEBUG: ");
		name.put(new Integer(INFO), "INFO: ");
		name.put(new Integer(WARN), "WARN: ");
		name.put(new Integer(ERROR), "ERROR: ");
		name.put(new Integer(FATAL), "FATAL: ");
	}

	@Override
	public boolean isEnabled(int level) {
		return true;
	}

	@Override
	public void log(int level, String message) {
		switch (level) {
		case DEBUG:
		case INFO:
			org.jboss.tools.openshift.express.internal.ui.utils.Logger.debug(message);
			break;
		case WARN:
			org.jboss.tools.openshift.express.internal.ui.utils.Logger.warn(message);
			break;
		case ERROR:
		case FATAL:
			org.jboss.tools.openshift.express.internal.ui.utils.Logger.error(message);
			break;
		}
	}

}