/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.databinding;

import java.io.File;

import org.eclipse.core.databinding.conversion.Converter;
import org.jboss.tools.openshift.common.core.utils.FileUtils;

/**
 * A converter that returns {@code true} if the given file exists. Returns
 * {@code false} otherwise.
 * 
 * @author Andre Dietisheim
 */
public class FileExistsConverter extends Converter {
	
	public FileExistsConverter() {
		super(File.class, Boolean.class);
	}

	@Override
	public Object convert(Object fromObject) {
		if (!(fromObject instanceof File)) {
			return false;
		}
		return FileUtils.exists((File) fromObject);
	}
}
