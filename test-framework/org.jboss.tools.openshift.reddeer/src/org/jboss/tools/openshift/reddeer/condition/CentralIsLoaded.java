/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.condition;

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.swt.impl.browser.InternalBrowser;

/**
 * Wait until JBoss Central is loaded.
 * 
 * @author rhopp
 *
 */

public class CentralIsLoaded extends AbstractWaitCondition {
	
	@Override
	public boolean test() {
		InternalBrowser internalBrowser = null;
		try {
			internalBrowser = new InternalBrowser();
		} catch (CoreLayerException ex) {
			return false;
		}
		return internalBrowser.isPageLoaded();
	}

	@Override
	public String description() {
		return "Waiting for Central to load";
	}

}