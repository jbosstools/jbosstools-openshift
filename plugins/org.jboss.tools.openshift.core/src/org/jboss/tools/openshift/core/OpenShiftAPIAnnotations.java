/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core;

/**
 * The list of well known annotations that
 * can be consumed by the tooling
 * @author jeff.cantrill
 *
 */
public interface OpenShiftAPIAnnotations {
	
	static final String GENERATED_BY = "openshift.io/generated-by";
}
