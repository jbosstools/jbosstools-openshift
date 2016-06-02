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

package org.jboss.tools.openshift.internal.ui.wizard.deployimage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.docker.core.IDockerImage;
import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.internal.ui.dockerutils.DockerImageUtils;

/**
 * {@link WizardPage} to list and select a Docker Image.
 */
public class ListDockerImagesWizardModel extends ObservablePojo {

	private final IDockerConnection dockerConnection;

	public static final String FILTER_NAME = "filterName";

	public static final String DOCKER_IMAGES = "dockerImages";

	public static final String SELECTED_DOCKER_IMAGE = "selectedDockerImage";

	private String filterName;

	private List<DockerImageTag> dockerImages;

	private DockerImageTag selectedDockerImage;

	public ListDockerImagesWizardModel(final IDockerConnection dockerConnection, final String imageName) {
		this.dockerConnection = dockerConnection;
		this.filterName = imageName;
	}

	public IDockerConnection getDockerConnection() {
		return this.dockerConnection;
	}

	public String getFilterName() {
		return this.filterName;
	}

	public void setImageName(final String filterName) {
		firePropertyChange(FILTER_NAME, this.filterName, this.filterName = filterName);
	}

	public List<DockerImageTag> getDockerImages() {
		return this.dockerImages;
	}
	
	public void setDockerImages(final List<IDockerImage> dockerImages) {
		final List<IDockerImage> topLevelImages = dockerImages.stream()
				.filter(image -> !image.isDangling() && !image.isIntermediateImage()).collect(Collectors.toList());
		final List<DockerImageTag> imageTags = new ArrayList<>();
		for(IDockerImage topLevelImage : topLevelImages) {
			final Map<String, List<String>> repoTags = DockerImageUtils.extractTagsByRepo(topLevelImage.repoTags());
			for(Entry<String, List<String>> entry : repoTags.entrySet()) {
				final String repo = entry.getKey();
				final List<String> tags = entry.getValue();
				for(String tag : tags) {
					imageTags.add(new DockerImageTag(topLevelImage.id(), repo, tag));
				}
			}
		}
				
		Collections.sort(imageTags, new Comparator<DockerImageTag>() {
			@Override
			public int compare(DockerImageTag image1, DockerImageTag image2) {
				return image1.getRepoName().compareTo(image2.getRepoName());
			}
		});
		firePropertyChange(DOCKER_IMAGES, this.dockerImages,
				this.dockerImages = imageTags);
	}

	public DockerImageTag getSelectedDockerImage() {
		return this.selectedDockerImage;
	}
	
	public void setSelectedDockerImage(final DockerImageTag selectedDockerImage) {
		firePropertyChange(SELECTED_DOCKER_IMAGE, this.selectedDockerImage,
				this.selectedDockerImage = selectedDockerImage);
	}
	
	static class DockerImageTag {

		/** the corresponding image id. */
		private final String id;
		
		/** repository name of the image. */
		private final String repoName;

		/** the tag for this specific image. */
		private final String tag;

		public DockerImageTag(final String id, final String repoName, final String tag) {
			this.id = id.startsWith("sha256:") ? id.substring("sha256:".length(), "sha256:".length() + 12) : id.substring(0,  12);
			this.repoName = repoName;
			this.tag = tag;
		}

		/**
		 * @return the id
		 */
		public String getId() {
			return id;
		}

		/**
		 * @return the repoName
		 */
		public String getRepoName() {
			return repoName;
		}

		/**
		 * @return the tag
		 */
		public String getTag() {
			return tag;
		}
		
	}
	
}
