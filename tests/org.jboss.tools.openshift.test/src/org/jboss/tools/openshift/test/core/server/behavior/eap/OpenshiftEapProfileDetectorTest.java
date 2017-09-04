package org.jboss.tools.openshift.test.core.server.behavior.eap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.jboss.tools.openshift.core.server.behavior.eap.OpenshiftEapProfileDetector;
import org.junit.Test;

import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.build.IBuildStrategy;
import com.openshift.restclient.model.build.ICustomBuildStrategy;
import com.openshift.restclient.model.build.IDockerBuildStrategy;
import com.openshift.restclient.model.build.ISTIBuildStrategy;
import com.openshift.restclient.model.build.ISourceBuildStrategy;

public class OpenshiftEapProfileDetectorTest {

    private static final String FOO_BAR = "foo.bar";

    @Test
    public void testIsEapStyle() {
        assertIsNotEapStyle(null);
        //docker
        assertIsNotEapStyle(createBuildConfig(IDockerBuildStrategy.class, FOO_BAR));
        assertIsEapStyle(createBuildConfig(IDockerBuildStrategy.class, "foo.wildflybar"));
        //source
        assertIsNotEapStyle(createBuildConfig(ISourceBuildStrategy.class, FOO_BAR));
        assertIsEapStyle(createBuildConfig(ISourceBuildStrategy.class, "foo.bar.eap70"));
        //custom source
        assertIsNotEapStyle(createBuildConfig(ICustomBuildStrategy.class, FOO_BAR));
        assertIsEapStyle(createBuildConfig(ICustomBuildStrategy.class, "foo.bar.EAP64"));
        //deprecated STI
        assertIsNotEapStyle(createBuildConfig(ISTIBuildStrategy.class, FOO_BAR));
        assertIsEapStyle(createBuildConfig(ISTIBuildStrategy.class, "wildflyyy"));

        //fallback on template name check
        assertIsNotEapStyle(createBuildConfig(null, FOO_BAR));
        assertIsEapStyle(createBuildConfig(IBuildStrategy.class, "wildflyyy"));

    }

    @Test
    public void testContainsEap2LikeKeywords() {
        assertNotContainsEapLikeKeywords(null);
        assertNotContainsEapLikeKeywords("");
        assertContainsEapLikeKeywords("jboss-eap64");
        assertContainsEapLikeKeywords("mixed.wildFly.case");
    }

    @SuppressWarnings("deprecation")
    private IBuildConfig createBuildConfig(Class<? extends IBuildStrategy> clazz, String name) {
        IBuildConfig bc = mock(IBuildConfig.class);
        DockerImageURI image = mock(DockerImageURI.class);
        when(image.getName()).thenReturn(name);
        IBuildStrategy strategy = null;
        if (clazz == null) {
            strategy = mock(ISourceBuildStrategy.class);
        } else if (IDockerBuildStrategy.class.isAssignableFrom(clazz)) {
            IDockerBuildStrategy dbs = mock(IDockerBuildStrategy.class);
            when(dbs.getBaseImage()).thenReturn(image);
            strategy = dbs;
        } else if (ICustomBuildStrategy.class.isAssignableFrom(clazz)) {
            ICustomBuildStrategy cbs = mock(ICustomBuildStrategy.class);
            when(cbs.getImage()).thenReturn(image);
            strategy = cbs;
        } else if (ISTIBuildStrategy.class.isAssignableFrom(clazz)) {
            ISTIBuildStrategy sts = mock(ISTIBuildStrategy.class);
            when(sts.getImage()).thenReturn(image);
            strategy = sts;
        } else if (ISourceBuildStrategy.class.isAssignableFrom(clazz)) {
            ISourceBuildStrategy sbs = mock(ISourceBuildStrategy.class);
            when(sbs.getImage()).thenReturn(image);
            strategy = sbs;
        }
        when(bc.getBuildStrategy()).thenReturn(strategy);

        Map<String, String> labels = Collections.singletonMap("template", name);
        when(bc.getLabels()).thenReturn(labels);

        return bc;
    }

    private void assertIsEapStyle(IBuildConfig buildConfig) {
        assertTrue(new OpenshiftEapProfileDetector().isEapStyle(buildConfig));
    }

    private void assertIsNotEapStyle(IBuildConfig buildConfig) {
        assertFalse(new OpenshiftEapProfileDetector().isEapStyle(buildConfig));
    }

    private void assertContainsEapLikeKeywords(String text) {
        assertTrue(new OpenshiftEapProfileDetector().containsEapLikeKeywords(text));
    }

    private void assertNotContainsEapLikeKeywords(String text) {
        assertFalse(new OpenshiftEapProfileDetector().containsEapLikeKeywords(text));
    }
}
