/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.ui;

import org.apache.commons.validator.routines.UrlValidator;

/**
 * @author Jeff Maury
 *
 */
public final class OpenshiftUIConstants {
    /*
     * An UrlValidator that recognize strings as urls with http, https, ftp and file schemes.
     */
    public static final UrlValidator URL_VALIDATOR = new UrlValidator(new String[] {"http", "https", "ftp", "file"});
}
