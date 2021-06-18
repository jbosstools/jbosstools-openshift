/*******************************************************************************
 * Copyright (c) 2015-2020 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.jboss.tools.openshift.internal.common.ui.ImageRepository;

/**
 * @author jeff.cantrill
 * @author Jeff Maury
 */
public class OpenShiftImages {

	private static final String ICONS_FOLDER = "icons/";
	private static final String ICON_NAME_PREFIX = "icon-";

	private static final ImageRepository repo = new ImageRepository(ICONS_FOLDER, OpenShiftUIActivator.getDefault(),
			OpenShiftUIActivator.getDefault().getImageRegistry());

	public static final ImageDescriptor BLOCKS = repo.create("blocks.png"); //$NON-NLS-1$ 
	public static final Image BLOCKS_IMG = repo.getImage("blocks.png"); //$NON-NLS-1$ 
	public static final ImageDescriptor BUILD = repo.create("build.png"); //$NON-NLS-1$
	public static final Image BUILD_IMG = repo.getImage("build.png"); //$NON-NLS-1$
	public static final ImageDescriptor BUILDCONFIG = repo.create("buildconfig.png"); //$NON-NLS-1$ 
	public static final Image BUILDCONFIG_IMG = repo.getImage("buildconfig.png"); //$NON-NLS-1$ 
	public static final ImageDescriptor COPY_TO_CLIPBOARD = repo.create("copy-to-clipboard.gif"); //$NON-NLS-1$ 
	public static final Image COPY_TO_CLIPBOARD_IMG = repo.getImage("copy-to-clipboard.gif"); //$NON-NLS-1$ 
	public static final ImageDescriptor IMAGE = repo.create("image.png"); //$NON-NLS-1$ 
	public static final Image IMAGE_IMG = repo.getImage("image.png"); //$NON-NLS-1$ 
	public static final ImageDescriptor OPEN_WEB_CONSOLE = repo.create("open-web-console.gif"); //$NON-NLS-1$ 
	public static final Image OPEN_WEB_CONSOLE_IMG = repo.getImage("open-web-console.gif"); //$NON-NLS-1$ 
	public static final ImageDescriptor PROJECT = repo.create("project.png"); //$NON-NLS-1$
	public static final Image PROJECT_IMG = repo.getImage("project.png"); //$NON-NLS-1$
	public static final ImageDescriptor SERVICE = repo.create("service.png"); //$NON-NLS-1$ 
	public static final Image SERVICE_IMG = repo.getImage("service.png"); //$NON-NLS-1$ 
	public static final ImageDescriptor ROUTE = repo.create("route.png"); //$NON-NLS-1$
	public static final Image ROUTE_IMG = repo.getImage("route.png"); //$NON-NLS-1$
	public static final ImageDescriptor TEMPLATE = repo.create("template.png"); //$NON-NLS-1$
	public static final Image TEMPLATE_IMG = repo.getImage("template.png"); //$NON-NLS-1$
	public static final ImageDescriptor REPLICATION_CONTROLLER = repo.create("replicator.png"); //$NON-NLS-1$
	public static final Image REPLICATION_CONTROLLER_IMG = repo.getImage("replicator.png"); //$NON-NLS-1$
	public static final ImageDescriptor PROJECT_NEW = PROJECT;
	public static final Image PROJECT_NEW_IMG = PROJECT_IMG;
	public static final ImageDescriptor CHECKED = repo.create("checked.png"); //$NON-NLS-1$
	public static final Image CHECKED_IMG = repo.getImage("checked.png"); //$NON-NLS-1$
	public static final ImageDescriptor UNCHECKED = repo.create("unchecked.png"); //$NON-NLS-1$
	public static final Image UNCHECKED_IMG = repo.getImage("unchecked.png"); //$NON-NLS-1$
	public static final ImageDescriptor TREND_UP = repo.create("trend-up.png"); //$NON-NLS-1$ 
	public static final ImageDescriptor TREND_DOWN = repo.create("trend-down.png"); //$NON-NLS-1$ 

	public static final ImageDescriptor OPENSHIFT_LOGO_DESC = repo.create("openshift-logo-white-icon.png"); //$NON-NLS-1$
	public static final Image OPENSHIFT_LOGO_IMG = repo.getImage("openshift-logo-white-icon.png"); //$NON-NLS-1$
	
	/*
	 * Application Explorer
	 */
	public static final ImageDescriptor APPLICATION = repo.create("application.png"); //$NON-NLS-1$
	public static final Image APPLICATION_IMG = repo.getImage("application.png"); //$NON-NLS-1$
	public static final ImageDescriptor COMPONENT = repo.create("component.png"); //$NON-NLS-1$
	public static final Image COMPONENT_IMG = repo.getImage("component.png"); //$NON-NLS-1$
	public static final ImageDescriptor STORAGE = repo.create("storage.png"); //$NON-NLS-1$
	public static final Image STORAGE_IMG = repo.getImage("storage.png"); //$NON-NLS-1$
	public static final ImageDescriptor URL = repo.create("url-node.png"); //$NON-NLS-1$
	public static final Image URL_IMG = repo.getImage("url-node.png"); //$NON-NLS-1$
	public static final ImageDescriptor URL_SECURE = repo.create("url-node-secure.png"); //$NON-NLS-1$
	public static final Image URL_SECURE_IMG = repo.getImage("url-node-secure.png"); //$NON-NLS-1$
	public static final ImageDescriptor COMPONENT_TYPE = repo.create("component-type-light.png"); //$NON-NLS-1$
  public static final Image COMPONENT_TYPE_IMG = repo.getImage("component-type-light.png"); //$NON-NLS-1$
  public static final ImageDescriptor STARTER = repo.create("start-project-light.png"); //$NON-NLS-1$
  public static final Image STARTER_IMG = repo.getImage("start-project-light.png"); //$NON-NLS-1$
  public static final ImageDescriptor REGISTRY = repo.create("registry.png"); //$NON-NLS-1$
  public static final Image REGISTRY_IMG = repo.getImage("registry.png"); //$NON-NLS-1$
  
	private static Map<String, ImageDescriptor> descriptorsByName = new HashMap<>();

	/**
	 * Get an image to represent an application image (e.g. template details)
	 * @param name
	 * @return the image
	 */
	public static final Image getAppImage(String name) {
		if (name.startsWith(ICON_NAME_PREFIX)) {
			name = name.substring(ICON_NAME_PREFIX.length());
		}
		final String imagePath = NLS.bind("apps/{0}.png", name);
		ImageDescriptor desc = descriptorsByName.get(name);
		if (desc == null) {
			desc = repo.create(imagePath, false);
			Image image = desc.createImage(false);
			if (image == null) {
				image = BLOCKS_IMG;
			}
			repo.create(imagePath, image);
			descriptorsByName.put(name, desc);
		}
		return repo.getImage(imagePath);
	}

}