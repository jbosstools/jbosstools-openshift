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
package org.jboss.tools.openshift.internal.ui.wizard.newapp.fromimage;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.IApplicationSource;

import com.openshift.restclient.model.IImageStream;
import com.openshift.restclient.model.image.ITagReference;

/**
 * Application Source based on an imagestream and tag
 * @author jeff.cantrill
 *
 */
public class ImageStreamApplicationSource implements IApplicationSource {

	private final IImageStream is;
	private final ITagReference tag;

	public ImageStreamApplicationSource(IImageStream is, ITagReference tag) {
		this.is = is;
		this.tag = tag;
	}

	@Override
	public String getName() {
		return NLS.bind("{0}:{1}", is.getName(), tag.getName());
	}

	@Override
	public String getNamespace() {
		return is.getNamespace();
	}

	@SuppressWarnings("unchecked")
	@Override
	public IImageStream getSource() {
		return is;
	}
	
	@Override
	public String getKind() {
		return is.getKind();
	}

	public ITagReference getSourceTag() {
		return tag;
	}

	@Override
	public Collection<String> getTags() {
		if(tag.isAnnotatedWith(OpenShiftAPIAnnotations.TAGS)) {
			return Arrays.asList(tag.getAnnotation(OpenShiftAPIAnnotations.TAGS).split(","));
		}
		return Collections.emptyList();
	}

	@Override
	public boolean isAnnotatedWith(String key) {
		return tag.isAnnotatedWith(key);
	}

	@Override
	public String getAnnotation(String key) {
		return tag.getAnnotation(key);
	}

	@Override
	public Map<String, String> getAnnotations() {
		return tag.getAnnotations();
	}
	
	

}
