/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.hasMatchingLabels;
import static org.mockito.Mockito.doReturn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.tools.openshift.internal.common.core.util.KeyValueFilterFactory;
import org.jboss.tools.openshift.internal.common.core.util.KeyValueFilterFactory.KeyValueFilter;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
import org.jboss.tools.openshift.test.util.ResourceMocks;
import org.junit.Before;
import org.junit.Test;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IService;

public class ResourceUtilsIsMatchingLabelsTest {

	private IService service;

	@SuppressWarnings("serial")
	@Before
	public void before() {
		this.service = ResourceMocks.createResource(IService.class, ResourceKind.SERVICE, resource -> {
			doReturn(new HashMap<String, String>() {
				{
					put("42", "whoknowswhy");
					put("84", "notatall");
				}
			}).when(resource).getLabels();
		});
	}

	@Test
	public void shouldReturnMatchingLabels() {
		// given
		List<KeyValueFilter> filters = KeyValueFilterFactory.create("8=at");
		// when
		Map<String, String> labels = ResourceUtils.getMatchingLabels(filters.get(0), service);
		// then
		assertThat(labels).hasSize(1);
		assertThat(labels.get("84")).isEqualTo("notatall");
	}

	@Test
	public void shouldReturnEmptyIfNoLabelsMatch() {
		// given
		List<KeyValueFilter> filters = KeyValueFilterFactory.create("smurfs=blue");
		// when
		Map<String, String> labels = ResourceUtils.getMatchingLabels(filters.get(0), service);
		// then
		assertThat(labels).isEmpty();
	}

	@Test
	public void shouldReturnAllLabelsIfFilterIsNull() {
		// given
		// when
		Map<String, String> labels = ResourceUtils.getMatchingLabels(null, service);
		// then
		assertThat(labels).hasSize(2);
		assertThat(labels.get("42")).isEqualTo("whoknowswhy");
		assertThat(labels.get("84")).isEqualTo("notatall");
	}

	@Test
	public void shouldReturnEmptyLabelsMapIfFilterIsNullAndNoLabelsPresent() {
		// given
		IService service = ResourceMocks.createResource(IService.class, ResourceKind.SERVICE, resource -> {
			doReturn(new HashMap<String, String>()).when(resource).getLabels();
		});
		// when
		Map<String, String> labels = ResourceUtils.getMatchingLabels(null, service);
		// then
		assertThat(labels).isEmpty();
	}

	@Test
	public void shouldMatchLabels() {
		// given
		List<KeyValueFilter> filters = KeyValueFilterFactory.create("8=at");
		// when
		boolean isMatching = ResourceUtils.hasMatchingLabels(filters.get(0), service);
		// then
		assertThat(isMatching).isTrue();
	}

	@Test
	public void shouldMatchLabelsIfFilterIsNull() {
		// given
		// when
		boolean isMatching = ResourceUtils.hasMatchingLabels(null, service);
		// then
		assertThat(isMatching).isTrue();
	}

	@Test
	public void shouldMatchLabelsIfFilterIsNullAndNoLabelsExists() {
		// given
		IService service = ResourceMocks.createResource(IService.class, ResourceKind.SERVICE, resource -> {
			doReturn(new HashMap<String, String>()).when(resource).getLabels();
		});
		// when
		boolean isMatching = ResourceUtils.hasMatchingLabels(null, service);
		// then
		assertThat(isMatching).isTrue();
	}
	
	@Test
	public void filterWithoutKeyNorValueExpressionShouldBeEmpty() {
		// given
		KeyValueFilter filter = new KeyValueFilter();
		// when
		boolean empty = filter.isEmpty();
		// then
		assertThat(empty).isTrue();
	}
	
	@Test
	public void emptyLabelResourceHasMatchingLabelsIfFilterIsEmptyNull() {
		// given
		IService service = ResourceMocks.createResource(IService.class, ResourceKind.SERVICE, resource -> {
			doReturn(new HashMap<String, String>()).when(resource).getLabels();
		});
		assertThat(hasMatchingLabels(new KeyValueFilter(), service)).isTrue();
	}
}
