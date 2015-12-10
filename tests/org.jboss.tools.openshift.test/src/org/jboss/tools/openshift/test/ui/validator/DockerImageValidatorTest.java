/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.validator;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.tools.openshift.internal.ui.validator.DockerImageValidator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author Fred Bricon
 */
@RunWith(Parameterized.class)
public class DockerImageValidatorTest {

  private DockerImageValidator validator = new DockerImageValidator();

  @Parameters(name = "{index}: validate({0})={1}")
    public static Collection<Object[]> data() {
      Map<String, Boolean> dataSet = new LinkedHashMap<>();
      dataSet.putAll(getData(Arrays.asList(
          "foo",
          "foo/name",
          "foo.bar/name",
          "foo.bar/namespace/name",
          "foo.bar.io/namespace/name",
          "127.0.0.1:1234/namespace/name",
          "127.0.0.1:1/namespace/name:tag",
          "127.0.0.1:5000/foo",
          "foo:bar",
          "jboss/wildfly:h23gfsd23"

      ), Boolean.TRUE));
      dataSet.putAll(getData(Arrays.asList(
          "https://foo.bar/namespace/name",
          "foo/Name",
          "-foo",
          "bar/foo-",
          "-foo.io/bar/name",
          "foo///name",
          "foo.io/bar/Name",
          "foo.io/bar///name",
          "jbo:ss/wildfly:h23gfsd23",
          "jboss/wildfly:h23gf&sd23"
      ), Boolean.FALSE));
      return dataSet.entrySet().stream().map(e -> new Object[] {e.getKey(), e.getValue()}).collect(Collectors.toList());
    }

    private static Map<String, Boolean> getData(Collection<String> elements, Boolean result) {
      return elements.stream().collect(Collectors.toMap(Function.identity(), s -> result));
    }

    @Parameter
    public String name;

    @Parameter(value = 1)
    public boolean expectedResult;

    @Test
    public void test() {
        assertEquals(expectedResult, validator.validateImageName(name).isOK());
    }

}
