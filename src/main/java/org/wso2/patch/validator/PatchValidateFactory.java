package org.wso2.patch.validator;

import org.wso2.interfaces.CommonValidator;

public class PatchValidateFactory extends ValidateFactory {
    @Override
    public CommonValidator getCommonValidation(String filename) {
        if (filename != null) {
            if (filename.endsWith(".zip")) {
                return new PatchZipValidator();
            }
            return null;
        } else {
            return null;
        }
    }
}
