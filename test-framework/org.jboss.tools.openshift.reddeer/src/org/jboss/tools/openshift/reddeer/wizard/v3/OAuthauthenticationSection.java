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
import org.jboss.tools.openshift.reddeer.utils.AuthenticationTokenRetrival;

/**
 * Represents OAuth authentication selection section
 * 
 * @author odockal
 *
 */
public class OAuthauthenticationSection extends AuthenticationMethodSection {
	
	public OAuthauthenticationSection() {
		super(AuthenticationMethod.OAUTH);
	}
	
	public void setToken(String token) {
		new LabeledText("Token").setText(token); 
	}
	
	public void setSaveToken(boolean checked) {
		new CheckBox(getComposite(), 
				"Save token (could trigger secure storage login)") 
				.toggle(checked);
	}
	
	public String retrieveAuthToken(String username, String password) {
		AuthenticationTokenRetrival retrival = new AuthenticationTokenRetrival(username, password);
		return retrival.retrieveToken();
	}

}
