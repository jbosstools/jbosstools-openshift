/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.utils;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

/**
 * @author Andre Dietisheim
 */
public abstract class LinkSelectionAdapter extends SelectionAdapter {

	private String text;

	public LinkSelectionAdapter(String text) {
		Assert.isLegal(text != null
				&& !text.isEmpty());
		this.text = text;
	}

	@Override
	public final void widgetSelected(SelectionEvent e) {
		if (text.equals(e.text)) {
			doWidgetSelected(e);
		}
	}

	protected abstract void doWidgetSelected(SelectionEvent e);

	
}
