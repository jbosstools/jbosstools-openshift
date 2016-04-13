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

import java.util.Collection;
import java.util.Map;

import com.openshift.restclient.model.IResource;

/**
 * The resource from which a microservice and its
 * supporting resource will be created (e.g. template, imagestream + imagestreamtag
 * 
 * @author jeff.cantrill
 *
 */
public interface IApplicationSource {

	/**
	 * The name of this application source
	 * @return
	 */
	String getName();
	
	/**
	 * Convenience method to the resource namespace
	 * @return
	 */
	String getNamespace();
	
	/**
	 * The source resource (e.g. template, imagestream)
	 * @return 
	 */
	<T extends IResource> T getSource();
	
	/**
	 * Get the kind of resource for this
	 * application source
	 * @return
	 */
	String getKind();
	
	/**
	 * The list of tags associated with a source
	 * @return
	 */
	Collection<String> getTags();
	
	boolean isAnnotatedWith(String key);
	
	String getAnnotation(String key);
	
	Map<String, String> getAnnotations();
}
