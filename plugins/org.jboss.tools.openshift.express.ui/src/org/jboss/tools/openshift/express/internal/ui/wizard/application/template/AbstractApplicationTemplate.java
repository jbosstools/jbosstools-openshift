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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;

import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * @author Andre Dietisheim
 */
public abstract class AbstractApplicationTemplate extends ObservablePojo implements IApplicationTemplate {

	private String name;
	private String description; 
	private List<IApplicationTemplate> children;

	protected AbstractApplicationTemplate(String name, String description, IApplicationTemplate... children) {
		this.name = name;
		this.description = description;
		this.children = new ArrayList<IApplicationTemplate>(Arrays.asList(children));
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
	public List<IApplicationTemplate> getChildren() {
		return children;
	}

	@Override
	public void clearChildren() {
		children.clear();
		firePropertyChange(PROPERTY_CHILDREN, null, children);
	}

	@Override
	public IApplicationTemplate addChild(IApplicationTemplate child) {
		children.add((IApplicationTemplate) child);
		fireIndexedPropertyChange(PROPERTY_CHILDREN, children.size() - 1, null, child);
		return this;
	}

	@Override
	public IApplicationTemplate addChildren(List<IStandaloneCartridge> cartridges) {
		for (IStandaloneCartridge cartridge : cartridges) {
			addChild(new CartridgeApplicationTemplate(cartridge));
		}
		return this;
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
	

}