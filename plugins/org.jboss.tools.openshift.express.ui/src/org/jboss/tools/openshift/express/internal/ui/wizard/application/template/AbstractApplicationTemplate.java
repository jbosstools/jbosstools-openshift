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

import java.util.Collections;
import java.util.Set;

import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;

import com.openshift.client.cartridge.ICartridge;

/**
 * @author Andre Dietisheim
 */
public abstract class AbstractApplicationTemplate extends ObservablePojo implements IApplicationTemplate {

	private String name;
	private String description; 

	protected AbstractApplicationTemplate(String name, String description, IApplicationTemplate... children) {
		this.name = name;
		this.description = description;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		firePropertyChange(PROPERTY_NAME, this.name, this.name = name);
	}
	
	@Override
	public String getDescription() {
		return description;
	}
	
	@Override
	public Set<ICartridge> getAllCartridges() {
		return Collections.emptySet();
	}

	@Override
	public Set<ICartridge> getEmbeddedCartridges() {
		return Collections.emptySet();
	}

	@Override
	public ICartridge getStandaloneCartridge() {
		return null;
	}

	@Override
	public String getInitialGitUrl() {
		return null;
	}

	@Override
	public boolean isMatching(String expression) {
		if (StringUtils.isEmpty(expression)) {
			return true;
		}

		String lowerCaseExpression = StringUtils.toLowerCase(expression);
		
		return isMatching(lowerCaseExpression, StringUtils.toLowerCase(getName()))
				|| isMatching(lowerCaseExpression, StringUtils.toLowerCase(getDescription()));
	}
	
	protected boolean isMatching(String expression, String toMatch) {
		if (StringUtils.isEmpty(toMatch)) {
			return false;
		}
		
		return toMatch.indexOf(expression) >= 0;
	}

	@Override
	public boolean canCreateApplication() {
		return true;
	}

}