/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.application.template;

import java.util.List;

/**
 * @author Andre Dietisheim
 */
public interface IApplicationTemplateCategory extends IApplicationTemplate {

	public void clearChildren();

	public List<IApplicationTemplate> getChildren();

	public IApplicationTemplate addChild(IApplicationTemplate child);

	public IApplicationTemplate addChildren(List<IApplicationTemplate> cartridges);

}