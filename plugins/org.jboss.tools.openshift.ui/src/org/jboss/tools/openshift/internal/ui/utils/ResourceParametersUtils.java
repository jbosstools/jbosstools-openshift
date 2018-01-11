package org.jboss.tools.openshift.internal.ui.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.osgi.util.NLS;

import com.openshift.restclient.model.template.IParameter;

public class ResourceParametersUtils {
    
    public static final String LABEL_UNKNOWN_PARAMETER = "(Unknown parameter {0})";
    
    private static final Pattern PATTERN = Pattern.compile("\\$\\{[^}]+\\}");
    
    private ResourceParametersUtils() {}

    public static String replaceParametersInString(Map<String, IParameter> templateParameters, String str) {
        if (str == null || str.length() == 0) {
            return "";
        }
        if (templateParameters == null || templateParameters.isEmpty()) {
            return str;
        }
        StringBuffer result = new StringBuffer();
        Matcher m = PATTERN.matcher(str);
        while (m.find()) {
            String parameterVariable = m.group();
            String parameterName = parameterVariable.substring(2, parameterVariable.length() - 1);
            if (templateParameters.containsKey(parameterName)) {
                m.appendReplacement(result, templateParameters.get(parameterName).getValue());
            } else {
                m.appendReplacement(result, NLS.bind(LABEL_UNKNOWN_PARAMETER, parameterName));
            }
        }
        m.appendTail(result);
        return result.toString();
    }
}
