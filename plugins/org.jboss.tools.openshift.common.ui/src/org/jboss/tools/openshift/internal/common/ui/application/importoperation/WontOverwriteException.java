/*******************************************************************************
 * Copyright (c) 2011-2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.application.importoperation;

import java.text.MessageFormat;

/**
 * @author Andre Dietisheim
 */
public class WontOverwriteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public WontOverwriteException(String message, Object... arguments) {
        super(MessageFormat.format(message, arguments));
    }
}
