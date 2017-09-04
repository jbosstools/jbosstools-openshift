/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
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
 * The list of well known selectors that can be consumed by the tooling
 * 
 * @author Andre Dietisheim
 */
public interface OpenShiftResourceSelectors {
    /** as in eap templates **/
    static final String DEPLOYMENT_CONFIG = "deploymentconfig";
}
