/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.wizard.deployimage;

import org.jboss.tools.openshift.internal.ui.wizard.deployimage.DockerImage2OpenshiftResourceConverter;
import org.junit.Test;

import com.openshift.restclient.images.DockerImageURI;

import static org.junit.Assert.assertEquals;

/**
 * @author Jeff Maury
 *
 */
public class DockerImage2OpenshiftResourceConverterTest {
    
    private final DockerImage2OpenshiftResourceConverter converter = new DockerImage2OpenshiftResourceConverter();
    
    @Test
    public void testLowerCase() {
        String resourceName = converter.convert(new DockerImageURI("myimage"));
        assertEquals("myimage", resourceName);
    }

    @Test
    public void testUpperCase() {
        String resourceName = converter.convert(new DockerImageURI("MYIMAGE"));
        assertEquals("MYIMAGE", resourceName);
    }
    
    @Test
    public void testUnderscore() {
        String resourceName = converter.convert(new DockerImageURI("image_sub"));
        assertEquals("image-sub", resourceName);
    }
    
    @Test
    public void testDash() {
        String resourceName = converter.convert(new DockerImageURI("image-sub"));
        assertEquals("image-sub", resourceName);
    }
    
    @Test
    public void testNonAllowed() {
        String resourceName = converter.convert(new DockerImageURI("image+sub"));
        assertEquals("imagesub", resourceName);
    }
    
    @Test
    public void testTooLong() {
        String resourceName = converter.convert(new DockerImageURI("image01234567890123456789"));
        assertEquals("image0123456789012345678", resourceName);
    }
    
    @Test
    public void testLeadingUnderscore() {
        String resourceName = converter.convert(new DockerImageURI("_image"));
        assertEquals("image", resourceName);
    }
    
    @Test
    public void testTrailingUnderscore() {
        String resourceName = converter.convert(new DockerImageURI("image_"));
        assertEquals("image", resourceName);
    }
    
    @Test
    public void testDoubleLeadingUnderscore() {
        String resourceName = converter.convert(new DockerImageURI("__image"));
        assertEquals("image", resourceName);
    }
    
    @Test
    public void testDoubleTrailingUnderscore() {
        String resourceName = converter.convert(new DockerImageURI("image__"));
        assertEquals("image", resourceName);
    }
    
    @Test
    public void testDoubleUnderscore() {
        String resourceName = converter.convert(new DockerImageURI("image__sub"));
        assertEquals("image-sub", resourceName);
    }
    
    @Test
    public void testDoubleDash() {
        String resourceName = converter.convert(new DockerImageURI("image--sub"));
        assertEquals("image-sub", resourceName);
    }
    
    @Test
    public void testSingleNumeric() {
        String resourceName = converter.convert(new DockerImageURI("1"));
        assertEquals("a1", resourceName);
    }
}
