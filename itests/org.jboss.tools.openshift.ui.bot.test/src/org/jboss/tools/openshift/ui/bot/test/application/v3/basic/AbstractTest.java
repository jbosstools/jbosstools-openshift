/*******************************************************************************
 * Copyright (c) 2007-2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.application.v3.basic;

import org.eclipse.reddeer.workbench.handler.WorkbenchShellHandler;
import org.junit.AfterClass;

/**
 * The purpose of this class is to set up/clean up environment to help isolate tests and to avoid code duplication.
 * @author jkopriva@redhat.com
 *
 */
public abstract class AbstractTest {
	
	@AfterClass
	public static void cleanUpAfterTest() {
		WorkbenchShellHandler.getInstance().closeAllNonWorbenchShells();
	}
	

}
