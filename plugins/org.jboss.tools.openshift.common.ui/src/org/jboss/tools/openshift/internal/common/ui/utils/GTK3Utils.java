/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package org.jboss.tools.openshift.internal.common.ui.utils;

import java.lang.reflect.Field;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.TableViewer;

/**
 * @author Snjezana Peco
 */
public class GTK3Utils {

	private static final String ENV_SWT_GTK3 = "ENV_SWT_GTK3"; //$NON-NLS-1$

	private GTK3Utils() {
	}

	public static boolean isRunning() {
		if (Platform.WS_GTK.equals(Platform.getWS())) {
			try {
				Class<?> clazz = Class.forName("org.eclipse.swt.internal.gtk.OS"); //$NON-NLS-1$
				Field field = clazz.getDeclaredField("GTK3"); //$NON-NLS-1$
				boolean gtk3 = field.getBoolean(field);
				return gtk3;
			} catch (ClassNotFoundException e) {
				return isGTK3Env();
			} catch (NoSuchFieldException e) {
				return false;
			} catch (SecurityException e) {
				return isGTK3Env();
			} catch (IllegalArgumentException e) {
				return isGTK3Env();
			} catch (IllegalAccessException e) {
				return isGTK3Env();
			}
		}
		return false;
	}

	private static boolean isGTK3Env() {
		String gtk3 = System.getProperty(ENV_SWT_GTK3);
		if (gtk3 == null) {
			gtk3 = System.getenv(ENV_SWT_GTK3);
		}
		return !"0".equals(gtk3); //$NON-NLS-1$
	}

	/**
	 * In GTK3 table viewer in some cases (e.g. if it is invisible,
	 * on the next wizard page, and has vertical scroll) is not updated
	 * after its content is changed even with refresh(true).
	 * This method makes it to update in a radical way. It will 
	 * take effect if invoked after tree of widgets including 
	 * the table gets visible (e.g. on wizard page activated).
	 * @param viewer
	 */
	public static void refreshTableViewer(TableViewer viewer) {
		if(isRunning() && viewer != null && viewer.getControl() != null && !viewer.getControl().isDisposed()) {
			Object input = viewer.getInput();
			if(input != null) {
				viewer.setInput(null);
				viewer.setInput(input);
			}
		}
	}
	
}
