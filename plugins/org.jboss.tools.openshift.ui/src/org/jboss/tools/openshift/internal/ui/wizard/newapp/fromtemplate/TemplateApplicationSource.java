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
package org.jboss.tools.openshift.internal.ui.wizard.newapp.fromtemplate;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.jboss.tools.openshift.internal.ui.wizard.newapp.IApplicationSource;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.ITags;
import com.openshift.restclient.model.template.ITemplate;

/**
 * Application source implementation for templates
 * @author jeff.cantrill
 *
 */
public class TemplateApplicationSource implements IApplicationSource {

	private final ITemplate template;

	public TemplateApplicationSource(ITemplate template) {
		if(template == null) throw new OpenShiftException("ITemplate instance was null while trying to Instantiate a %s", TemplateApplicationSource.class);
		this.template = template;
	}

	@Override
	public String getName() {
		return this.template.getName();
	}

	@Override
	public String getNamespace() {
		return this.template.getNamespace();
	}

	@SuppressWarnings("unchecked")
	@Override
	public ITemplate getSource() {
		return this.template;
	}
	
	@Override
	public String getKind() {
		return template.getKind();
	}

	@Override
	public Collection<String> getTags() {
		return this.template.accept(new CapabilityVisitor<ITags, Collection<String>>() {
			@Override
			public Collection<String> visit(ITags capability) {
				return capability.getTags();
			}
		}, Collections.emptyList());
	}

	@Override
	public boolean isAnnotatedWith(String key) {
		return this.template.isAnnotatedWith(key);
	}

	@Override
	public String getAnnotation(String key) {
		return this.template.getAnnotation(key);
	}

	@Override
	public Map<String, String> getAnnotations() {
		return this.template.getAnnotations();
	}
	
	

}
