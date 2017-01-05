/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.models;

/**
 * Running pods may be adapted to this interface with Adapters.adapt().
 */
public interface IRunningPodHolder {
	public IOpenshiftUIElement<?, IOpenshiftUIElement<?,?>> getPodUIElement();
}
