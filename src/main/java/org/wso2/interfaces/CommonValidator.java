package org.wso2.interfaces;

import java.io.File;
import java.io.IOException;

public interface CommonValidator {
    String CheckReadMe(String filepath, String patchId) throws IOException;

    String CheckLicense(String filepath) throws IOException;

    String CheckNotAContribution(String filepath) throws IOException;

    String CheckPatch(String filepath);

    String UnZip(File zipFilepath, String destFilePath) throws IOException;

    String CheckContent(String filePath, String patchId) throws IOException;

    void DownloadZipFile(String url, String version, String patchId, String destFilePath);

    void CommitKeys(String url, String fileLocation);
}
