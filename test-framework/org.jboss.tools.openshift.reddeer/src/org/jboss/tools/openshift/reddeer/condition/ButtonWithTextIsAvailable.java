/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.condition;

import org.jboss.reddeer.common.condition.AbstractWaitCondition;
import org.jboss.reddeer.core.exception.CoreLayerException;
import org.jboss.reddeer.swt.impl.button.PushButton;

public class ButtonWithTextIsAvailable extends AbstractWaitCondition {

	private String buttonText;

	public ButtonWithTextIsAvailable(String buttonText) {
		this.buttonText = buttonText;
	}

	@Override
	public boolean test() {
		try {
			new PushButton(buttonText);
			return true;
		} catch (CoreLayerException ex) {
			return false;
		}
	}

	@Override
	public String description() {
		return " button with text " + buttonText + " is available";
	}

}