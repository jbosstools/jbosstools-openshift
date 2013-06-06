/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.databinding;

import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.runtime.Assert;

/**
 * @author André Dietisheim
 */
public class TrimTrailingSlashConverter extends Converter {

	public TrimTrailingSlashConverter() {
		super(String.class, String.class);
	}

	@Override
	public Object convert(Object fromObject) {
		Assert.isLegal(fromObject instanceof String);
		String url = (String) fromObject;
		if (url.charAt(url.length() - 1) == '/') {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}
}