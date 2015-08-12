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
	public static final ImageDescriptor BUILDCONFIG = repo.create("buildconfig.png"); //$NON-NLS-1$ 
	public static final Image BUILDCONFIG_IMG = repo.getImage("buildconfig.png"); //$NON-NLS-1$ 
	public static final ImageDescriptor GEAR = repo.create("gear.png"); //$NON-NLS-1$ 
	public static final Image GEAR_IMG = repo.getImage("gear.png"); //$NON-NLS-1$ 
	public static final ImageDescriptor LAYER = repo.create("layer.png"); //$NON-NLS-1$ 
	public static final Image LAYER_IMG = repo.getImage("layer.png"); //$NON-NLS-1$ 
	public static final ImageDescriptor COPY_TO_CLIPBOARD = repo.create("copy-to-clipboard.png"); //$NON-NLS-1$ 
	public static final Image COPY_TO_CLIPBOARD_IMG = repo.getImage("copy-to-clipboard.png"); //$NON-NLS-1$ 
	
	private static Map<String, ImageDescriptor> descriptorsByName = new HashMap<String, ImageDescriptor>();

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