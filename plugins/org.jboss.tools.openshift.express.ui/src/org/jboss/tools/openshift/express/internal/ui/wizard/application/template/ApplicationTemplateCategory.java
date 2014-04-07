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
import java.util.List;

import org.jboss.tools.openshift.express.internal.core.util.StringUtils;

/**
 * @author Andre Dietisheim
 */
public class ApplicationTemplateCategory extends AbstractApplicationTemplate implements IApplicationTemplateCategory {

	private List<IApplicationTemplate> children;

	public ApplicationTemplateCategory(String name, String description, IApplicationTemplate... children) {
		super(name, description, children);
		this.children = new ArrayList<IApplicationTemplate>();
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
	public IApplicationTemplate addChildren(List<IApplicationTemplate> templates) {
		children.addAll(templates);
		firePropertyChange(PROPERTY_CHILDREN, null, children);
		return this;
	}
	
	@Override
	public boolean isMatching(String expression) {
		boolean matching = super.isMatching(expression);
		if (matching) {
			return true;
		}

		return isMatchingChildren(StringUtils.toLowerCase(expression));
	}

	private boolean isMatchingChildren(String expression) {
		for (IApplicationTemplate template : children) {
			if (template.isMatching(expression)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean canAddRemoveCartridges() {
		return false;
	}

	@Override
	public boolean isTemplate() {
		return false;
	}
}