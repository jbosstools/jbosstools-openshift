/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.property;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.properties.PropertyDescriptor;

/**
 * 
 * @author Viacheslav Kabanovich
 *
 */
public class UneditablePropertyDescriptor extends PropertyDescriptor {

	public UneditablePropertyDescriptor(Object id, String displayName) {
		super(id, displayName);
		
	}

    @Override
	public CellEditor createPropertyEditor(Composite parent) {
        CellEditor editor = new TextCellEditor(parent) {
            @Override
        	protected Control createControl(Composite parent) {
            	Control result = super.createControl(parent);
            	text.setEditable(false);
            	return result;
            }
            @Override
            protected void doSetValue(Object value) {
            	//Since the text field is not used for editing, it does not matter which value is set,
            	//just do the check and conversion here. When toString() is not good enough,
            	//do conversion in property source or extend this class.
            	super.doSetValue(value == null ? "" : value.toString());
            }
        };
        return editor;
    }
}
