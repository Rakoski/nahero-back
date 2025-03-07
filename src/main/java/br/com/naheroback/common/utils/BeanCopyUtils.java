package br.com.naheroback.common.utils;


import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BeanCopyUtils {

    public static void copyNonNullProperties(Object source, Object destination, String... ignoreProperties) {
        Set<String> ignorePropertiesSet = getNullPropertyNames(source);
        Collections.addAll(ignorePropertiesSet, ignoreProperties);
        BeanUtils.copyProperties(source, destination, ignorePropertiesSet.toArray(new String[0]));
    }

    public static Set<String> getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();
        Set<String> emptyNames = new HashSet<>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) {
                emptyNames.add(pd.getName());
            }
        }
        return emptyNames;
    }
}
