/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.cdk.server.test.internal;

import java.util.ArrayList;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.VagrantPoller;
import org.junit.Test;

import junit.framework.TestCase;

public class VagrantPollerTest extends TestCase {
	
	@Test
	public void testOutputParsing() {
		VagrantPollerSub sub = new VagrantPollerSub();

		String[] output = vagrant18_out(CDKConstants.STATE_RUNNING);
		assertEquals(IStatus.OK, sub.parseOutput2(output).getSeverity());
		
		output = vagrant18_out(CDKConstants.STATE_POWEROFF);
		assertEquals(IStatus.ERROR, sub.parseOutput2(output).getSeverity());
		
		output = vagrant18_out(CDKConstants.STATE_SHUTOFF);
		assertEquals(IStatus.ERROR, sub.parseOutput2(output).getSeverity());
		
		output = vagrant17_out(CDKConstants.STATE_RUNNING);
		assertEquals(IStatus.OK, sub.parseOutput2(output).getSeverity());
		output = vagrant17_out(CDKConstants.STATE_POWEROFF);
		assertEquals(IStatus.ERROR, sub.parseOutput2(output).getSeverity());
		output = vagrant17_out(CDKConstants.STATE_SHUTOFF);
		assertEquals(IStatus.ERROR, sub.parseOutput2(output).getSeverity());
		
	}
	
	@Test
	public void testErrorStatus() {
		final ArrayList<IStatus> statList = new ArrayList<>();
		ILogListener listener = new ILogListener() {
			@Override
			public void logging(IStatus status, String plugin) {
				statList.add(status);
			}
		};
		CDKCoreActivator.getDefault().getLog().addLogListener(listener);

		
		String[] output = vagrant18_error();
		assertTrue(statList.size() == 0);
		VagrantPollerSub sub = new VagrantPollerSub();
		assertTrue(statList.size() == 0);
		IStatus result = sub.parseOutput2(output);
		CDKCoreActivator.getDefault().getLog().removeLogListener(listener);
		
		assertEquals(IStatus.ERROR, result.getSeverity());
		assertTrue(statList.size() == 1);
		
	}
	
	private String[] vagrant18_out(String state) {
		return new String[]{"1457045944,cdk,state," + state, 
				"1457045944,cdk,state-human-short," + state,
				"1457045944,cdk,state-human-long,The blahblahtruncated",
				"1457045944,,ui,info,Current machine states:\\n\\ncdk blahblah truncated\n"};
	}
	private String[] vagrant17_out(String state) {
		return new String[]{"1457045944,cdk,state," + state, 
				"1457045944,cdk,state-human-short," + state,
				"1457045944,cdk,state-human-long,The blahblahtruncated\n"};
	}
	private String[] vagrant18_error() {
		return new String[]{"1457536005,,error-exit,Vagrant::Errors::ProviderNotUsable,The provider 'virtualbox' that was requested to back the machine\n'cdk' is reporting that it isn't usable on this system. The\nreason is shown below:\n\nVagrant could not detect VirtualBox! Make sure VirtualBox is properly installed.\nVagrant uses the `VBoxManage` binary that ships with VirtualBox%!(VAGRANT_COMMA) and requires\nthis to be available on the PATH. If VirtualBox is installed%!(VAGRANT_COMMA) please find the\n`VBoxManage` binary and add it to the PATH environmental variable"};
	}
	
	private class VagrantPollerSub extends VagrantPoller {
		public IStatus parseOutput2(String[] lines) {
			return parseOutput(lines);
		}
	}
}
