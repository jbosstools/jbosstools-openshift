/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationUpdater;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.graphics.Image;

/**
 * @author Xavier Coulon
 * 
 */
public class CustomControlDecorationUpdater extends ControlDecorationUpdater {

	private final boolean showRequiredDecorator;
	
	/**
	 * Default constructor: provides a 'REQUIRED' decorator when the status is CANCEL
	 */
	public CustomControlDecorationUpdater() {
		this(true);
	}
	
	/**
	 * Default constructor: provides a 'REQUIRED' decorator when the status is CANCEL
	 */
	public CustomControlDecorationUpdater(final boolean showRequiredDecorator) {
		super();
		this.showRequiredDecorator = showRequiredDecorator;
	}
	
	/**
	 * {@inheritDoc} Overrides the standard behaviour: for CANCEL status, items are decorated with the REQUIRED
	 * decorator, not the ERROR one.
	 */
	@Override
	protected Image getImage(IStatus status) {
		if (status == null) {
			return null;
		}
		String fieldDecorationID = null;
		switch (status.getSeverity()) {
		case IStatus.INFO:
			fieldDecorationID = FieldDecorationRegistry.DEC_INFORMATION;
			break;
		case IStatus.WARNING:
			fieldDecorationID = FieldDecorationRegistry.DEC_WARNING;
			break;
		case IStatus.ERROR:
			fieldDecorationID = FieldDecorationRegistry.DEC_ERROR;
			break;
		case IStatus.CANCEL:
			fieldDecorationID = showRequiredDecorator ? FieldDecorationRegistry.DEC_REQUIRED : null;
			break;
		}

		FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(fieldDecorationID);
		return fieldDecoration == null ? null : fieldDecoration.getImage();
	}
}
