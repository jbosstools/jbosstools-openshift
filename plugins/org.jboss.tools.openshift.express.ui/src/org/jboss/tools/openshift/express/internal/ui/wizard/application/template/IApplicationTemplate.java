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

import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * @author Andre Dietisheim
 */
public interface IApplicationTemplate {

	public String PROPERTY_NAME = "name";
	public String PROPERTY_CHILDREN = "children";

	public String getName();

	public void setName(String name);

	public String getDescription();

	public void clearChildren();

	public List<IApplicationTemplate> getChildren();

	public IApplicationTemplate addChild(IApplicationTemplate child);

	public IApplicationTemplate addChildren(List<IStandaloneCartridge> cartridges);

	public boolean isMatching(String expression);
}