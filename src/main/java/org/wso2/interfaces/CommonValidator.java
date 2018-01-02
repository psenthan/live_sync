package org.wso2.interfaces;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public interface CommonValidator {
    String CheckReadMe(String filepath, String patchId) throws IOException;

    String CheckLicense(String filepath) throws IOException;

    String CheckNotAContribution(String filepath) throws IOException;

    String CheckPatch(String filepath);

    void UnZip(File zipFilepath, String destFilePath) throws IOException;

    String CheckContent(String filePath, String patchId) throws IOException;

    String DownloadZipFile(String url, String version, String patchId, String destFilePath);

    void CommitKeys(String url, String fileLocation);

    String SendEmail(String fromAddress, ArrayList<String> toList, ArrayList<String> ccList,
                     String subject, String body, String logMessage);
}
