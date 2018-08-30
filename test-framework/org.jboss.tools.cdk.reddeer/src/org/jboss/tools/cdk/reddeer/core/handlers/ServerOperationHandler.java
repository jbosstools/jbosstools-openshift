/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.core.handlers;

import static org.junit.Assert.fail;

import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersViewException;
import org.jboss.tools.cdk.reddeer.server.exception.CDKServerException;
import org.jboss.tools.cdk.reddeer.utils.CDKUtils;

/**
 * Class representing server's operation to be called using lambda expression
 * @author Ondrej Dockal
 */
public class ServerOperationHandler {

	private static final Logger log = Logger.getLogger(ServerOperationHandler.class);
	
	private static ServerOperationHandler instance = null;
	
	private ServerOperationHandler() {
	}

	public static ServerOperationHandler getInstance() {
		if (instance == null) {
			instance = new ServerOperationHandler();
		}
		return instance;
	}
	
	/**
	 * Call given void method (operation) on server and catch possible problems
	 * 
	 * @param operation action to be called on server
	 * @param cond      condition to be executed before operation action
	 * @param rethrow   boolean param distinguishing whether re-throw caught
	 *                  exception
	 */
	public void handleOperation(Runnable operation, Runnable cond, boolean rethrow) {
		cond.run();
		try {
			operation.run();
		} catch (ServersViewException serversExc) {
			log.error(serversExc.getMessage(), serversExc);
		} catch (CDKServerException exc) {
			String cause = exc.getMessage() + "\r\nConsole Output:\r\n" + CDKUtils.collectConsoleOutput(log, true);
			if (rethrow) {
				throw new CDKServerException(cause);
			} else {
				fail(cause);
			}
		} catch (WaitTimeoutExpiredException waitExc) {
			String cause = waitExc.getMessage() + "\r\n" + CDKUtils.collectConsoleOutput(log, true);
			if (rethrow) {
				throw new WaitTimeoutExpiredException(cause);
			} else {
				fail(cause);
			}
		}
		CDKUtils.collectConsoleOutput(log, false);
	}

	/**
	 * Call given void method (operation) on server and catch possible problems
	 */
	public void handleOperation(Runnable operation, Runnable cond) {
		handleOperation(operation, cond, false);
	}
}
