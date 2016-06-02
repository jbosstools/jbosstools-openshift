/*******************************************************************************
 * Copyright (c) 2016 Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/

package org.jboss.tools.openshift.internal.ui.dockerutils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.docker.core.IDockerImage;

/**
 * Utility class for {@link IDockerImage}s
 */
public class DockerImageUtils {

	/**
	 * Checks if an image with the given {@code repo} and {@code tag} exists in
	 * the given {@code dockerConnection}
	 * <p>
	 * Workaround until https://bugs.eclipse.org/bugs/show_bug.cgi?id=495243 is
	 * fixed.
	 * </p>
	 * 
	 * @param dockerConnection
	 *            the {@link IDockerConnection}
	 * @param repoName
	 *            the repository/name of the image to look-up
	 * @param tag
	 *            the image tag
	 * @return <code>true</code> if match found, <code>false</code> otherwise
	 */
	public static boolean hasImage(final IDockerConnection dockerConnection, final String repoName, final String tag) {
		for (IDockerImage image : dockerConnection.getImages()) {
			final Map<String, List<String>> repoTags = extractTagsByRepo(image.repoTags());
			for (Entry<String, List<String>> entry : repoTags.entrySet()) {
				final String repo = entry.getKey();
				final List<String> tags = entry.getValue();
				if (repo != null && repo.equals(repoName) && tags != null && tags.contains(tag)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Extracts the org/repo and all the associated tags from the given
	 * {@code repoTags}, assuming that the given repoTags elements have the
	 * following format: {@code [org/]repo[:tag]}. Tags are sorted by their
	 * natural order.
	 * 
	 * @param repoTags
	 *            the list of repo/tags to analyze
	 * @return the tags indexed by org/repo
	 */
	public static Map<String, List<String>> extractTagsByRepo(final List<String> repoTags) {
		final Map<String, List<String>> results = new HashMap<>();
		for (String entry : repoTags) {
			final int indexOfColonChar = entry.lastIndexOf(':');
			final String repo = (indexOfColonChar > -1) ? entry.substring(0, indexOfColonChar) : entry;
			if (!results.containsKey(repo)) {
				results.put(repo, new ArrayList<String>());
			}
			if (indexOfColonChar > -1) {
				results.get(repo).add(entry.substring(indexOfColonChar + 1));
			}
		}
		// now sort the tags
		for (Entry<String, List<String>> entry : results.entrySet()) {
			Collections.sort(entry.getValue());
		}
		return results;
	}

	/**
	 * Retrieves the {@code 'name:tag'} from {@code '[[registry/]repo/]name:tag'} format of the given {@code imageName}
	 * @param imageName
	 * @return the {@code 'name:tag'}
	 */
	public static String extractImageNameAndTag(final String imageName) {
		final int lastIndexOfSlash = imageName.lastIndexOf('/');
		if(lastIndexOfSlash == -1) {
			return imageName;
		}
		return imageName.substring(lastIndexOfSlash + 1);
	}
}
