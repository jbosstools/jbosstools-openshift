/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.express.test.core;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.jboss.tools.openshift.common.core.ICredentialsPrompter;
import org.jboss.tools.openshift.express.core.ExpressCoreUIIntegration;
import org.jboss.tools.openshift.express.internal.core.LazyCredentialsPrompter;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Jeff Cantrill
 */
@RunWith(MockitoJUnitRunner.class)
public class LazyCredentialsPrompterTest {

	@Mock
	private ICredentialsPrompter defaultPrompter;
	@Mock
	private ICredentialsPrompter altPrompter;
	private LazyCredentialsPrompter lazyPrompter;
	private ExpressConnection connection;
	
	@Before
	public void setup(){
		ExpressCoreUIIntegration.getDefault().setCredentialPrompter(null);
		connection = new ExpressConnection((String) null, (String) null);
		when(defaultPrompter.promptAndAuthenticate(any(ExpressConnection.class), any())).thenReturn(true);
		when(altPrompter.promptAndAuthenticate(any(ExpressConnection.class), any())).thenReturn(true);
	}
	
	@After
	public void teardown(){
		ExpressCoreUIIntegration.getDefault().setCredentialPrompter(null);
	}
	@Test
	public void testConstructionOfPrompterThrowsWhenInitializedWithSelf(){
		boolean exception = false;
		try{
			new LazyCredentialsPrompter(new LazyCredentialsPrompter(null));
		}catch(IllegalArgumentException e){
			exception = true;
		}
		assertTrue("Expected an exception when trying to initialize with a lazy cred prompter", exception);
	}
	@Test
	public void testPromptAndAuthenticateWhenInitializedWithAPrompter() {
		lazyPrompter = new LazyCredentialsPrompter(defaultPrompter);
		
		assertTrue("Exp. to prompt for creds", lazyPrompter.promptAndAuthenticate(connection, null));
		verify(defaultPrompter).promptAndAuthenticate(any(ExpressConnection.class), any());
		verify(altPrompter, never()).promptAndAuthenticate(any(ExpressConnection.class), any());
	}

	@Test
	public void testPromptAndAuthenticateDeferredLoadsAndPromptsWhenInitializedWithNull() {
		ExpressCoreUIIntegration.getDefault().setCredentialPrompter(altPrompter);
		lazyPrompter = new LazyCredentialsPrompter(null);
		
		assertTrue("Exp. to prompt for creds", lazyPrompter.promptAndAuthenticate(connection, null));
		verify(altPrompter).promptAndAuthenticate(any(ExpressConnection.class), any());
		verify(defaultPrompter, never()).promptAndAuthenticate(any(ExpressConnection.class), any());
	}

	@Test
	public void testPromptAndAuthenticateReturnsFalseWhenItCantGetAPrompter() {
		lazyPrompter = new LazyCredentialsPrompter(null);
		
		assertFalse("Exp. to not prompt for creds", lazyPrompter.promptAndAuthenticate(connection,null));
		verify(altPrompter, never()).promptAndAuthenticate(any(ExpressConnection.class), any());
		verify(defaultPrompter, never()).promptAndAuthenticate(any(ExpressConnection.class), any());
	}
	
	

}
