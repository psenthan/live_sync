package org.wso2.service;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
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
import java.util.*;

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

        version = prop.getProperty(version);

        String destination = destFilePath + version + "/" + timeStamp+ "/patch" + patchId + "/";
        String patchName = organization + version + "-" + patchId;

        String filepath = destination + patchName + ".zip";
        String unzippedFolderPath = destination + organization + version + "-" + patchId + "/";

        PatchValidateFactory patchValidateFactory = PatchValidateService.getPatchValidateFactory(filepath);
        assert patchValidateFactory != null;
        CommonValidator commonValidator = patchValidateFactory.getCommonValidation(filepath);

        String result = commonValidator.DownloadZipFile(url, version, patchId, destination);
        if(result != ""){
            LOG.info(result);
            return result +"/n"+ errorMessage;
        }

        File fl = new File(destination);

        for (File file : fl.listFiles()) {
            if(file.getName().endsWith(".md5") || file.getName().endsWith((".asc"))
                    || file.getName().endsWith((".sha1"))) {
                errorMessage = "patch" + patchId + " is already signed\n";
                FileUtils.deleteDirectory(new File(destFilePath));
                LOG.info(errorMessage + "\n");
                return errorMessage +"\n";
            }
        }

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
            /*todo: commit keys
            commonValidator.CommitKeys(url, destination);
            sendRequest(patchName,"ReleasedNotInPublicSVN",true, "Promote");*/
        }
        /*else{
            ArrayList<String> toList = null;
            toList.add("xxxxxx@wso2.com");
            commonValidator.SendEmail("patchsigner@wso2.com",toList,null,"Testing","This is a test", errorMessage );
        }*/
        FileUtils.deleteDirectory(new File(destFilePath));
        LOG.info(errorMessage + "\n");
        return errorMessage + "\n";

    }

    private void sendRequest(String patchName,String state, boolean isSuccess, String AdminTestPromote)
            throws IOException {
        String successState;
        if(isSuccess) successState = "true";
        else successState = "false";
        JSONObject json = new JSONObject();
        json.put("patchName", patchName);
        json.put("state", state);
        json.put("isSuccess",successState);
        json.put("AdminTestPromote",AdminTestPromote);

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        try {
            HttpPost request = new HttpPost(prop.getProperty("httpUri"));
            StringEntity params = new StringEntity(json.toString());
            request.addHeader("content-type", prop.getProperty("content-type"));
            request.addHeader("Authentication", prop.getProperty("Authentication"));
            request.addHeader("Cache-Control", prop.getProperty("Cache-Control"));
            request.setEntity(params);
            httpClient.execute(request);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            httpClient.close();
        }
    }
}
