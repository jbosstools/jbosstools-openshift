/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core.docker;

/**
 * A Docker content digest as specified at {@link https://docs.docker.com/registry/spec/api/#content-digests).
 *  
 * @author Andre Dietisheim
 *
 */
public class ContentDigest {

	private static final String SEPARATOR = ":";
	private String algorithm;
	private String hex;

	public ContentDigest(String digest) {
		if (digest == null
				|| digest.length() == 0) {
			throw new IllegalStateException("Empty digest not allowed");
		}
		String[] portions = digest.split(SEPARATOR);
		if (portions.length == 2) {
			this.algorithm = portions[0];
		}
		this.hex = portions[1];
	}

	public String getAlgorithm() {
		return algorithm;
	}

	public String getHex() {
		return hex;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((algorithm == null) ? 0 : algorithm.hashCode());
		result = prime * result + ((hex == null) ? 0 : hex.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ContentDigest other = (ContentDigest) obj;
		if (algorithm == null) {
			if (other.algorithm != null)
				return false;
		} else if (!algorithm.equals(other.algorithm))
			return false;
		if (hex == null) {
			if (other.hex != null)
				return false;
		} else if (!hex.equals(other.hex))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return ((algorithm != null)?  algorithm + SEPARATOR : "") 
				+ hex;
	}

}

