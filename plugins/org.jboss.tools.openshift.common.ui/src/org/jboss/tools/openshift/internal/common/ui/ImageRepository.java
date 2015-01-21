/*******************************************************************************
 * Copyright (c) 2011 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;

/**
 * @author Andre Dietisheim
 */
public class ImageRepository {

	private ImageRegistry imageRegistry;
	private URL baseUrl;
	private Plugin plugin;
	private String imageFolder;

	public ImageRepository(String imageFolder, Plugin plugin, ImageRegistry imageRegistry) {
		this.imageFolder = imageFolder;
		this.plugin = plugin;
		this.imageRegistry = imageRegistry;
	}

	protected URL getBaseUrl() {
		try {
			if (baseUrl == null) {
				this.baseUrl = new URL(plugin.getBundle().getEntry("/"), imageFolder);
			}
			return baseUrl;
		} catch (MalformedURLException e) {
			plugin.getLog().log(
					new Status(IStatus.ERROR, plugin.getBundle().getSymbolicName(),
							NLS.bind("Could not find image folder at {0}", imageFolder), e));
			return null;
		}
	}

	public ImageDescriptor create(String name) {
		return create(imageRegistry, name);
	}

	private ImageDescriptor create(ImageRegistry registry,String name) {
		return create(registry, name, getBaseUrl());
	}

	private ImageDescriptor create(ImageRegistry registry, String name, URL baseUrl) {
		if (baseUrl == null) {
			return null;
		}
		
		ImageDescriptor imageDescriptor =
				ImageDescriptor.createFromURL(createFileURL(name, baseUrl));
		registry.put(name, imageDescriptor);
		return imageDescriptor;
	}

	private URL createFileURL(String name, URL baseUrl) {
		try {
			return new URL(baseUrl, name);
		} catch (MalformedURLException e) {
			plugin.getLog().log(
					new Status(IStatus.ERROR, plugin.getBundle().getSymbolicName(), NLS.bind(
							"Could not create URL for image {0}", name), e));
			return null;
		}
	}

	public Image getImage(String name) {
		return imageRegistry.get(name);
	}
}
