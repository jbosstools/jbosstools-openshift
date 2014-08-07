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

import java.util.Set;

import com.openshift.client.cartridge.ICartridge;



/**
 * @author Andre Dietisheim
 */
public interface IApplicationTemplate {

	public String PROPERTY_NAME = "name";
	public String PROPERTY_CHILDREN = "children";

	public String getName();

	public void setName(String name);

	public String getDescription();
	
	/**
	 * Returns <code>true</code> if this template matches the given expression.
	 * 
	 * @param expression
	 * @return
	 */
	public boolean isMatching(String expression);

	/**
	 * Returns <code>true</code> if this template is a valid template to start
	 * an application from.
	 * 
	 * @return
	 */
	public boolean canCreateApplication();
	
	/**
	 * Returns <code>true</code> if one can add/remove cartridges from this
	 * template.
	 * 
	 * @return
	 */
	public boolean canAddRemoveCartridges();
	
	/**
	 * Returns all (embedded and standalone) cartridges for this template.
	 * 
	 * @return
	 */
	public Set<ICartridge> getAllCartridges();
	
	public Set<ICartridge> getEmbeddedCartridges();

	public ICartridge getStandaloneCartridge();

	public String getInitialGitUrl();
	
	public boolean isInitialGitUrlEditable();
	
	public boolean isCodeAnything();


}