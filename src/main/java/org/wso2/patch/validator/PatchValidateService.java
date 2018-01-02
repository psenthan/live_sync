package org.wso2.patch.validator;


public class PatchValidateService {
    public static PatchValidateFactory getPatchValidateFactory(String filepath) {
        if (filepath.endsWith(".zip")) {
            return new PatchValidateFactory();
        }
        return null;
    }
}
