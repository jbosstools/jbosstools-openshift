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

import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.swt.impl.combo.LabeledCombo;
import org.eclipse.reddeer.swt.impl.group.DefaultGroup;
import org.jboss.tools.openshift.reddeer.enums.AuthenticationMethod;

/**
 * Representation of UI objects of auth. section in OS connection
 * @author odockal
 *
 */
public class AuthenticationMethodSection {

	private LabeledCombo protocol;
	private AuthenticationMethod method;
	
	public AuthenticationMethodSection(AuthenticationMethod auth) {
		protocol = new LabeledCombo("Protocol:");
		method = auth;
		protocol.setSelection(method.toString());
	}
	
	public AuthenticationMethod getMethod() {
		return method;
	}
	
	public ReferencedComposite getComposite() {
		return new DefaultGroup("Authentication");
	}
	
	public void setMethod() {
		protocol.setSelection(method.toString());
	}

}
