package org.wso2.service;

import org.wso2.interfaces.CommonValidator;

public abstract class ValidateFactory {
    abstract CommonValidator getCommonValidation(String filename);
}
