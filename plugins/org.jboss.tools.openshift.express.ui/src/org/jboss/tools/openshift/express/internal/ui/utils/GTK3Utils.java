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

package org.jboss.tools.openshift.express.internal.ui.utils;

import java.lang.reflect.Field;

import org.eclipse.core.runtime.Platform;

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

	
}
