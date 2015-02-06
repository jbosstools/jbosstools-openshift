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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.jboss.tools.openshift.internal.common.ui.ImageRepository;

public class OpenShiftImages {

	private static final String ICONS_FOLDER = "icons/";

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
}