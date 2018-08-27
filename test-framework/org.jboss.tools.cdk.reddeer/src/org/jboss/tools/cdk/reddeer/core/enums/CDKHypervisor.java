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
package org.jboss.tools.cdk.reddeer.core.enums;

import org.jboss.tools.cdk.reddeer.core.enums.CDKRuntimeOS;
import org.jboss.tools.cdk.reddeer.server.exception.CDKException;

/**
 * CDK/Minishift hypervisor
 * @author odockal
 *
 */
public enum CDKHypervisor {
	
	KVM("kvm"),
	VIRTUALBOX("virtualbox"),
	XHYVE("xhyve"),
	HYPERV("hyperv"),
	EMPTY("");
	
	String hypervisor;
	
	CDKHypervisor(String value) {
		this.hypervisor = value;
	}
	
	@Override
	public String toString() {
		return this.hypervisor;
	}
	
	public static CDKHypervisor getHypervisor(String item) {
		switch(item) {
			case "kvm":
				return KVM;
			case "xhyve":
				return XHYVE;
			case "virtualbox":
				return VIRTUALBOX;
			case "hyperv":
				return HYPERV;
			default:
				throw new CDKException("No such CDKHypervisor: " + item);
		}
	}
	
	public static CDKHypervisor getDefaultHypervisor() {
		String os = CDKRuntimeOS.get().getRuntimeName();
		switch(os) {
			case "linux":
				return KVM;
			case "darwin":
				return XHYVE;
			case "win":
				return HYPERV;
			default:
				throw new CDKException("This OS: " + os + " does not have defined default hypervisor");
		}
	}

}
