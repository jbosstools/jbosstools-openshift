/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.newapp;

import java.beans.PropertyChangeListener;

import org.jboss.tools.openshift.internal.ui.job.IResourcesModelJob;

/**
 * A model for creating application microservices from
 * a given source type (e.g. template, imagestream)
 *   
 * @author jeff.cantrill
 *
 */
public interface IApplicationSourceModel extends PropertyChangeListener{

	IResourcesModelJob createFinishJob();
}
