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
package org.jboss.tools.openshift.reddeer.condition;

import org.eclipse.reddeer.common.condition.WaitCondition;

/**
 * Wait condition class representing waiting for login page to load
 * Is using TestCondition interface with test method defined
 * so that anyone can pass individual appropriate test method
 * 
 * @author mlabuda, odockal
 *
 */
public class LoginPageIsLoaded implements WaitCondition {

	private TestCondition myTest;

	public LoginPageIsLoaded(TestCondition myTest) {
		this.myTest = myTest;
	}
	
	@Override
	public boolean test() {
		return myTest.test();
	}

	@Override
	public <T> T getResult() {
		return null;
	}

	@Override
	public String description() {
		return null;
	}

	@Override
	public String errorMessageWhile() {
		return null;
	}

	@Override
	public String errorMessageUntil() {
		return null;
	}
	
	public interface TestCondition {
		public boolean test();
	}	

}
