/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.test.common.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.jboss.tools.openshift.internal.common.core.util.KeyValueFilterFactory;
import org.jboss.tools.openshift.internal.common.core.util.KeyValueFilterFactory.KeyValueFilter;
import org.jboss.tools.openshift.internal.common.core.util.KeyValueFilterFactory.KeyValueFilter.Range;
import org.junit.Test;

/**
 * @author Andre Dietisheim
 */
public class KeyValueFilterFactoryTest {

	@Test
	public void shouldReturn1Filter() {
		// given
		// when
		List<KeyValueFilter> filters = KeyValueFilterFactory.create("foo=bar");
		// then
		assertThat(filters.size()).isEqualTo(1);
	}

	@Test
	public void shouldReturn3FiltersIfExpressionIsDelimitedWithColon() {
		// given
		// when
		List<KeyValueFilter> filters = KeyValueFilterFactory.create("foo=bar, 42=42,gargamel=no_smurf");
		// then
		assertThat(filters.size()).isEqualTo(3);
	}

	@Test
	public void shouldNotRecognize3rdExpressionIfIsDelimitedWithSpace() {
		// given
		// when
		List<KeyValueFilter> filters = KeyValueFilterFactory.create("foo=bar,42=42 gargamel=no_smurf");
		// then
		assertThat(filters.size()).isEqualTo(2);
	}

	@Test
	public void shouldReturn3FiltersIfUsingSpaceInKeyOrValues() {
		// given
		// when
		List<KeyValueFilter> filters = KeyValueFilterFactory.create("fo o=bar,42= 42 gargamel=no smurf,");
		// then
		assertThat(filters.size()).isEqualTo(2);
	}

	@Test
	public void shouldReturnIgnoreDelimitedEmptyExpression() {
		// given
		// when
		List<KeyValueFilter> filters = KeyValueFilterFactory.create("foo=bar,");
		// then
		assertThat(filters.size()).isEqualTo(1);
	}

	@Test
	public void shouldAcceptKeyAndValueWithSpaces() {
		// given
		// when
		List<KeyValueFilter> filters = KeyValueFilterFactory.create("fo o= bar tender");
		// then
		assertThat(filters).isNotEmpty();
		KeyValueFilter filter = filters.get(0);
		assertThat(filter.matchesKey("fo o")).isTrue();
		assertThat(filter.matchesValue("bar tender")).isTrue();
	}

	@Test
	public void shouldIgnoreSpaceAfterColon() {
		// given
		// when
		List<KeyValueFilter> filters = KeyValueFilterFactory.create("fo o= bar tender , smurfs= blue");
		// then
		assertThat(filters).hasSize(2);
		KeyValueFilter filter = filters.get(1);
		assertThat(filter.matchesKey("smurfs")).isTrue();
		assertThat(filter.matchesValue("blue")).isTrue();
	}

	@Test
	public void shouldCreateValueOnlyFilterIfExpressionhasNoEquals() {
		// given
		// when
		List<KeyValueFilter> filters = KeyValueFilterFactory.create("blue");
		// then
		assertThat(filters).isNotEmpty();
		KeyValueFilter filter = filters.get(0);
		assertThat(filter.matchesKey("smurfs")).isTrue();
		assertThat(filter.matchesKey("42")).isTrue();
		assertThat(filter.matchesValue("blue")).isTrue();
	}

	@Test
	public void shouldCreateFilterThatMatchesSubstrings() {
		// given
		// when
		List<KeyValueFilter> filters = KeyValueFilterFactory.create("smurfs=blue");
		// then
		assertThat(filters).isNotEmpty();
		KeyValueFilter filter = filters.get(0);
		assertThat(filter.matchesKey("all the smurfs")).isTrue();
		assertThat(filter.matchesValue("are very much blue")).isTrue();
	}

	@Test
	public void shouldReportMatchRangesForKey() {
		// given
		String filterExpr = "smurfs";
		String filteredText = "all the smurfs";
		int matchStart = filteredText.indexOf(filterExpr);
		int matchLength = filterExpr.length();
		// when
		List<KeyValueFilter> filters = KeyValueFilterFactory.create(filterExpr + "=anyvallue");
		// then
		assertThat(filters).isNotEmpty();
		KeyValueFilter filter = filters.get(0);
		Range range = filter.getMatchingRangeForKey(filteredText);
		assertThat(range).isNotNull();
		assertThat(range.start).isEqualTo(matchStart);
		assertThat(range.length).isEqualTo(matchLength);
	}

	@Test
	public void shouldReportMatchRangesForValue() {
		// given
		String filterExpr = "blue";
		String filteredText = "very much blue in the face";
		int matchStart = filteredText.indexOf(filterExpr);
		int matchLength = filterExpr.length();
		// when
		List<KeyValueFilter> filters = KeyValueFilterFactory.create("smurfs=" + filterExpr);
		// then
		assertThat(filters).isNotEmpty();
		KeyValueFilter filter = filters.get(0);
		Range range = filter.getMatchingRangeForValue(filteredText);
		assertThat(range).isNotNull();
		assertThat(range.start).isEqualTo(matchStart);
		assertThat(range.length).isEqualTo(matchLength);
	}

	@Test
	public void shouldReportMatchRangesForKeyAndValue() {
		// given
		String keyExpr = "smurfs";
		String keyText = "all the smurfs";
		int keyMatchStart = keyText.indexOf(keyExpr);
		int keyMatchLength = keyExpr.length();
		String valueExpr = "blue";
		String valueText = "very much blue in the face";
		int valueMatchStart = valueText.indexOf(valueExpr);
		int valueMatchLength = valueExpr.length();
		// when
		List<KeyValueFilter> filters = KeyValueFilterFactory.create(keyExpr + "=" + valueExpr);
		// then
		assertThat(filters).isNotEmpty();
		KeyValueFilter filter = filters.get(0);
		Range keyRange = filter.getMatchingRangeForKey(keyText);
		assertThat(keyRange).isNotNull();
		assertThat(keyRange.start).isEqualTo(keyMatchStart);
		assertThat(keyRange.length).isEqualTo(keyMatchLength);
		Range valueRange = filter.getMatchingRangeForValue(valueText);
		assertThat(valueRange).isNotNull();
		assertThat(valueRange.start).isEqualTo(valueMatchStart);
		assertThat(valueRange.length).isEqualTo(valueMatchLength);
	}

	@Test
	public void shouldReportEmptyRangeForKeyIfFilteringOnValueOnly() {
		// given
		// when
		List<KeyValueFilter> filters = KeyValueFilterFactory.create("blue");
		// then
		assertThat(filters).isNotEmpty();
		KeyValueFilter filter = filters.get(0);
		Range range = filter.getMatchingRangeForKey("smurfs=blue");
		assertThat(range).isNull();
	}
}
