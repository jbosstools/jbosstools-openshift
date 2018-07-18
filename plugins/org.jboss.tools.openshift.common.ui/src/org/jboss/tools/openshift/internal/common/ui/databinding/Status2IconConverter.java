/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.databinding;

import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Image;

public class Status2IconConverter extends Converter {

	public Status2IconConverter() {
		super(IStatus.class, Image.class);
	}

	@Override
	public Object convert(Object fromObject) {
		switch (((IStatus) fromObject).getSeverity()) {
		case IStatus.WARNING:
			return JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_WARNING);
		case IStatus.ERROR:
			return JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_ERROR);
		case Status.INFO:
			return Dialog.getImage(Dialog.DLG_IMG_MESSAGE_INFO);
		default:
			return null;
		}
	}
}
