package org.wso2.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.interfaces.CommonValidator;
import org.wso2.patch.validator.PatchValidateFactory;
import org.wso2.patch.validator.PatchValidateService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.io.File;
import java.io.IOException;

@Path("/zip")
public class SyncService {
    private static String errorMessage = null;
    private static final Logger LOG = LoggerFactory.getLogger(SyncService.class);
    private static final String filepath = "/home/vimukthi/Downloads/WSO2-CARBON-PATCH-4.4.0-0020.zip";
    private static final String destFilePath = "/home/vimukthi/Downloads/";
    private static final String unzippedFolderPath = "/home/vimukthi/Downloads/WSO2-CARBON-PATCH-4.4.0-0020/";

    @GET
    @Path("/{patchId}")
    public String zipPatchValidate(@PathParam("patchId") String patchId){
        LOG.info("Sync Service running\n");
        PatchValidateFactory patchValidateFactory = PatchValidateService.getPatchValidateFactory(filepath);
        assert patchValidateFactory != null;
        CommonValidator commonValidator = patchValidateFactory.getCommonValidation(filepath);

        try {
            errorMessage = "";
            commonValidator.UnZip(new File(filepath), destFilePath);
            errorMessage = commonValidator.CheckContent(unzippedFolderPath, patchId);
            errorMessage = errorMessage + commonValidator.CheckLicense(unzippedFolderPath + "LICENSE.txt");
            errorMessage = errorMessage + commonValidator.CheckNotAContribution(unzippedFolderPath +
                    "NOT_A_CONTRIBUTION.txt");
            errorMessage = errorMessage + commonValidator.CheckPatch(unzippedFolderPath +
                    "patch" + patchId + "/");
            errorMessage = errorMessage + commonValidator.CheckReadMe(unzippedFolderPath + "README.txt", patchId);
        } catch (IOException e) {
            e.printStackTrace();
            errorMessage = errorMessage + "File unzipping failed\n";
        }

        return errorMessage;
    }
}
