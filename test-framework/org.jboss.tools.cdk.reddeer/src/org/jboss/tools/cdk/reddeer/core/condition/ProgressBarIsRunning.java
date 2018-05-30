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
package org.jboss.tools.cdk.reddeer.core.condition;

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.swt.impl.progressbar.DefaultProgressBar;

/**
 * Wait condition for parameterized progress bar object
 * @author odockal
 *
 */
public class ProgressBarIsRunning extends AbstractWaitCondition {

	private String name;
	
	public ProgressBarIsRunning(String name) {
		this.name = name;
	}
	
	@Override
	public boolean test() {
		try {
			new DefaultProgressBar(name);
			return true;
		} catch(CoreLayerException e) {
			return false;
		}
	}
	
	@Override
	public String description() {
		return name + " progress bar is running";
	}

	@Override
	public String errorMessageUntil() {
		return name + " progress bar is not running";
	}

}
