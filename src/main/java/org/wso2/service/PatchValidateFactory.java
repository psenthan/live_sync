package org.wso2.service;

import org.wso2.interfaces.CommonValidator;
import org.wso2.patch.validator.PatchZipValidator;

public class PatchValidateFactory extends ValidateFactory {
    @Override
    CommonValidator getCommonValidation(String filename) {

        if (filename == null) {
            return null;
        }

        if (filename.endsWith(".zip")) {
            return new PatchZipValidator();
        }
        return null;
    }
}
