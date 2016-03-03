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

import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.VagrantPoller;
import org.junit.Test;

import junit.framework.TestCase;

public class VagrantPollerTest extends TestCase {
	
	@Test
	public void testOutputParsing() {
		VagrantPollerSub sub = new VagrantPollerSub();

		String[] output = vagrant18_out(CDKConstants.STATE_RUNNING);
		assertEquals(IStatus.OK, sub.parseOutput2(output));
		
		output = vagrant18_out(CDKConstants.STATE_POWEROFF);
		assertEquals(IStatus.ERROR, sub.parseOutput2(output));
		
		output = vagrant18_out(CDKConstants.STATE_SHUTOFF);
		assertEquals(IStatus.ERROR, sub.parseOutput2(output));
		
		output = vagrant17_out(CDKConstants.STATE_RUNNING);
		assertEquals(IStatus.OK, sub.parseOutput2(output));
		output = vagrant17_out(CDKConstants.STATE_POWEROFF);
		assertEquals(IStatus.ERROR, sub.parseOutput2(output));
		output = vagrant17_out(CDKConstants.STATE_SHUTOFF);
		assertEquals(IStatus.ERROR, sub.parseOutput2(output));
		
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
	
	
	private class VagrantPollerSub extends VagrantPoller {
		public int parseOutput2(String[] lines) {
			return parseOutput(lines);
		}
	}
}
