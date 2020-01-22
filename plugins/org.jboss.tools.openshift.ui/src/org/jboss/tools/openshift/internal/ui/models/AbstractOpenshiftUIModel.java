/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.swt.widgets.Display;

abstract public class AbstractOpenshiftUIModel<R, T extends AbstractOpenshiftUIModel<R, T>> extends AbstractOpenshiftUIElement<R, T, T> implements IOpenshiftUIModel<R, T> {

	/**
	 * @param parent
	 * @param wrapped
	 */
	public AbstractOpenshiftUIModel(T parent, R wrapped) {
		super(parent, wrapped);
	}

	private List<IElementListener> listeners = new ArrayList<IElementListener>();

	@Override
	public void addListener(IElementListener l) {
		synchronized (listeners) {
			listeners.add(l);
		}
	}

	@Override
	public void removeListener(IElementListener l) {
		synchronized (listeners) {
			int lastIndex = listeners.lastIndexOf(l);
			if (lastIndex >= 0) {
				listeners.remove(lastIndex);
			}
		}
	}
	
	@Override
	protected void fireChanged(IOpenshiftUIElement<?, ?, T> source) {
		if (Display.getCurrent() != null) {
			dispatchChange(source);
		} else {
			Display.getDefault().asyncExec(new Runnable() {

				@Override
				public void run() {
					dispatchChange(source);
				}
			});
		}
	}

	private void dispatchChange(IOpenshiftUIElement<?, ?, T> source) {
		Collection<IElementListener> copy = new ArrayList<>();
		synchronized (listeners) {
			copy.addAll(listeners);
		}
		copy.forEach(l -> l.elementChanged(source));
	}

}
