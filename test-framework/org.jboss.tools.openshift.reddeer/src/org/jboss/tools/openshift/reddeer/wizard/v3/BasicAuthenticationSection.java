/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.wizard.v3;

import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.jboss.tools.openshift.reddeer.enums.AuthenticationMethod;

/**
 * Represents Basic authentication selection section
 * @author odockal
 *
 */
public class BasicAuthenticationSection extends AuthenticationMethodSection {
	
	public BasicAuthenticationSection() {
		super(AuthenticationMethod.BASIC);
	}
	
	public LabeledText getUsernameLabel() {
		return new LabeledText("Username:");
	}
	
	public LabeledText getPasswordLabel() {
		return new LabeledText("Password:");
	}
	
	public void setSavePassword(boolean checked) {
		new CheckBox(getComposite(), 
				"Save password (could trigger secure storage login)")
				.toggle(checked);
	}

}
