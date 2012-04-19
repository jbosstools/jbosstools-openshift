/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.console;

import java.util.Map;

import com.openshift.client.IUser;

@Deprecated
public interface IPasswordPrompter {
	
	public enum PromptResult {
		PASSWORD_VALUE, SAVE_PASSWORD_VALUE;
	}
	/**
	 * Returns a map of the values entered by the user. The value indexed with {@link IPasswordPrompter.PromptResult.PASSWORD_VALUE} in the
	 * returning array is the input password, the value indexed with indexed with {@link IPasswordPrompter.PromptResult.SAVE_PASSWORD_VALUE} is the Boolean stating
	 * whether the password should be saved in the secured storage or not.
	 * 
	 * @param user
	 * @return map with password value (as String) and 'save password' (as Boolean) 
	 */
	public Map<PromptResult, Object> getPasswordFor(IUser user);
}
