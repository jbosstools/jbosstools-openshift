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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftCommonImages {

    private static final String ICONS_FOLDER = "icons/";

    private static final ImageRepository repo = new ImageRepository(ICONS_FOLDER, OpenShiftCommonUIActivator.getDefault(),
            OpenShiftCommonUIActivator.getDefault().getImageRegistry());

    public static final Image FOLDER = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
    public static final Image FILE = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);

    public static final Image ERROR = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);

    public static final ImageDescriptor OPENSHIFT_LOGO_DARK = repo.create("openshift-logo-dark.png"); //$NON-NLS-1$ 
    public static final ImageDescriptor OPENSHIFT_LOGO_WHITE_ICON = repo.create("openshift-logo-white-icon.png"); //$NON-NLS-1$ 
    public static final Image OPENSHIFT_LOGO_WHITE_ICON_IMG = repo.getImage("openshift-logo-white-icon.png"); //$NON-NLS-1$ 
    public static final ImageDescriptor OPENSHIFT_LOGO_WHITE_MEDIUM = repo.create("openshift-logo-white-medium.png"); //$NON-NLS-1$ 	
    public static final Image OPENSHIFT_LOGO_WHITE_MEDIUM_IMG = repo.getImage("openshift-logo-white-medium.png"); //$NON-NLS-1$ 	
    public static final ImageDescriptor GLOBE = repo.create("globe.png"); //$NON-NLS-1$ 
    public static final Image GLOBE_IMG = repo.getImage("globe.png"); //$NON-NLS-1$ 
    public static final ImageDescriptor SYSTEM_PROCESS = repo.create("systemprocess.gif"); //$NON-NLS-1$ 
    public static final Image SYSTEM_PROCESS_IMG = repo.getImage("systemprocess.gif"); //$NON-NLS-1$ 

    public static final ImageDescriptor OK = repo.create("pficon-ok.png"); //$NON-NLS-1$ 
    public static final Image OK_IMG = repo.getImage("pficon-ok.png"); //$NON-NLS-1$ 

    public static final ImageDescriptor TRANSPARENT_PIXEL = repo.create("transparent_pixel.png"); //$NON-NLS-1$ 
    public static final Image TRANSPARENT_PIXEL_IMG = repo.getImage("transparent_pixel.png"); //$NON-NLS-1$}

}