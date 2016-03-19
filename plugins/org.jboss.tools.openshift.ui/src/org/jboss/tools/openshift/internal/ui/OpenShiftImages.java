/*******************************************************************************
 * Copyright (c) 2015 Red Hat Inc..
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

import org.apache.commons.lang.ObjectUtils;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.jboss.tools.openshift.internal.common.ui.ImageRepository;

/**
 * @author jeff.cantrill
 */
public class OpenShiftImages {

	private static final String ICONS_FOLDER = "icons/";
	private static final String ICON_NAME_PREFIX = "icon-";
	
	private static final ImageRepository repo =
			new ImageRepository(
					ICONS_FOLDER, OpenShiftUIActivator.getDefault(), OpenShiftUIActivator.getDefault().getImageRegistry());

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
	public static final ImageDescriptor PROJECT_NEW = PROJECT;
	public static final Image PROJECT_NEW_IMG = PROJECT_IMG;

	private static Map<String, ImageDescriptor> descriptorsByName = new HashMap<>();

	/**
	 * Get an image to represent an application image (e.g. template details)
	 * @param name
	 * @return the image
	 */
	public static final Image getAppImage(String name) {
		if(name.startsWith(ICON_NAME_PREFIX)) {
			name = name.substring(ICON_NAME_PREFIX.length());
		}
		final String imagePath = NLS.bind("apps/{0}.png", name);
		if(!descriptorsByName.containsKey(name)) {
			descriptorsByName.put(name, repo.create(imagePath));
		}
		return (Image) ObjectUtils.defaultIfNull(repo.getImage(imagePath), BLOCKS_IMG);
	}
	
}