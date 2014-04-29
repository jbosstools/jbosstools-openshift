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
package org.jboss.tools.openshift.express.internal.core;

import com.openshift.client.ApplicationScale;
import com.openshift.client.cartridge.ICartridge;

/**
 * Provides properties for a (new or existing)  application
 * 
 * @author Andre Dietisheim
 */
public interface IApplicationProperties {
	
	public ApplicationScale getApplicationScale();
	
	public ICartridge getStandaloneCartridge();
	
	public String getApplicationName();
	
}
