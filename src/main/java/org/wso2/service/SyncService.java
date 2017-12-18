package org.wso2.service;
//todo: .md5,.asc, .sha1 exists >> patch already signed

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
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Properties;

@Path("/{directory}")
public class SyncService {
    private Properties prop = new Properties();
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    private static final Logger LOG = LoggerFactory.getLogger(SyncService.class);

    @GET
    @Path("/{patchId}")
    public String zipPatchValidate(@PathParam("patchId") String patchId,
                                   @PathParam("directory") String version) throws IOException {

        LOG.info("Sync Service running\n");
        prop.load( SyncService.class.getClassLoader().getResourceAsStream("application.properties"));

        final String organization = prop.getProperty("organization");
        final String destFilePath = prop.getProperty("destFilePath");
        final String staticURL = prop.getProperty("staticURL");
        final String url = staticURL + version + "/patches/patch" + patchId + "/";
        String errorMessage = "";
        String timeStamp = String.valueOf(timestamp.getTime());

        if(Objects.equals(version, "wilkes")) version = "4.4.0";
        else if(Objects.equals(version, "perlis")) version ="4.3.0";
        else if(Objects.equals(version, "turing")) version ="4.2.0";

        String destination = destFilePath + version + "/" + timeStamp + "/patch" + patchId + "/";

        String filepath = destination + organization + version + "-" + patchId + ".zip";
        String unzippedFolderPath = destination + organization + version + "-" + patchId + "/";

        PatchValidateFactory patchValidateFactory = PatchValidateService.getPatchValidateFactory(filepath);
        assert patchValidateFactory != null;
        CommonValidator commonValidator = patchValidateFactory.getCommonValidation(filepath);

        commonValidator.DownloadZipFile(url, version, patchId, destination);

        try {
            commonValidator.UnZip(new File(filepath), destination);
            errorMessage = commonValidator.CheckContent(unzippedFolderPath, patchId);
            errorMessage = errorMessage + commonValidator.CheckLicense(unzippedFolderPath + "LICENSE.txt");
            errorMessage = errorMessage + commonValidator.CheckNotAContribution(unzippedFolderPath +
                    "NOT_A_CONTRIBUTION.txt");
            errorMessage = errorMessage + commonValidator.CheckPatch(unzippedFolderPath +
                    "patch" + patchId + "/");
            errorMessage = errorMessage + commonValidator.CheckReadMe(unzippedFolderPath, patchId);
        } catch (IOException e) {
            e.printStackTrace();
            errorMessage = errorMessage + "File unzipping failed\n";
        }
        if(Objects.equals(errorMessage, "")) errorMessage = "SUCCESS";

        if(Objects.equals(errorMessage, "SUCCESS")){
            commonValidator.CommitKeys(url, destination);
        }

        return errorMessage;
    }
}
