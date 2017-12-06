/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core.docker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DockerManifestDigest {

	private static final Pattern REGEX_MANIFEST_DIGEST = Pattern.compile("([^:]*:\\/\\/)([^@]*)@(.*)");

	private String prefix;
	private ContentDigest digest;

	private String repository;

	public DockerManifestDigest(String imageId) {
		if (imageId == null || imageId.length() == 0) {
			throw new IllegalArgumentException("The imageId is empty.");
		}

		Matcher matcher = REGEX_MANIFEST_DIGEST.matcher(imageId);
		if (!matcher.matches()) {
			throw new IllegalArgumentException(imageId + "is not a docker manifest digest.");
		}

		this.prefix = matcher.group(1);
		this.repository = matcher.group(2);

	}

	public String getPrefix() {
		return prefix;
	}

	public String getRepository() {
		return repository;
	}

	public ContentDigest getDigest() {
		return digest;
	}
}
