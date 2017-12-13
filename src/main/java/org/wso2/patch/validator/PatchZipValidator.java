package org.wso2.patch.validator;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.interfaces.CommonValidator;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;


public class PatchZipValidator implements CommonValidator {
    private static final int BUFFER_SIZE = 4096;
    private static final Logger LOG = LoggerFactory.getLogger(CommonValidator.class);
    private static final String license = "a9797e0ad8421cd32062fca85c01885b";
    private static final String notAContribution = "030bfbe6fc3fba153be183e0112eba84";

    @Override
    public String CheckReadMe(String filepath, String patchId) throws IOException {
        String errorMessage = "";
        File file = new File(filepath);
        System.out.println("Reading files using Apache IO:");
        List<String> lines = FileUtils.readLines(file, "UTF-8");
        String[] id = lines.get(0).split(":");
        if (!Objects.equals(patchId, id[1]))
        for (int i = 0; i< lines.size(); i++) {
            lines.get(0);
        }
        return null;
    }

    @Override
    public String CheckLicense(String filepath) throws IOException {
        FileInputStream fis = new FileInputStream(new File(filepath));
        String md5 = md5Hex(fis);
        fis.close();
        if(Objects.equals(md5, license)) return "";
        return "LICENSE.txt is not in the correct format";
    }

    @Override
    public String CheckNotAContribution(String filepath) throws IOException {
        FileInputStream fis = new FileInputStream(new File(filepath));
        String md5 = md5Hex(fis);
        fis.close();
        if(Objects.equals(md5, notAContribution)) return "";
        return "NOT_A_CONTRIBUTION.txt is not in the correct format";
    }

    @Override
    public String CheckPatch(String filepath) {
        try {
            boolean empty = isDirEmpty(new File(filepath));
            if(empty){
                if(Files.list(Paths.get(filepath)).count() > 4) return "";
                else return "No patch found\n";
            }
            else return "";
        } catch (IOException e) {
            e.printStackTrace();
            return "filepath error when checking the patch folder content\n";
        }
    }

    @Override
    public void UnZip(File zipFilePath, String destFilePath) throws IOException {
        File destDir = new File(destFilePath);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while (zipEntry != null) {
            String filePath = destDir + File.separator + zipEntry.getName();
            if (!zipEntry.isDirectory()) {
                new File(filePath).getParentFile().mkdirs();
                extractFile(zipInputStream, filePath);
                LOG.info("Extracting "+ filePath);

            } else {
                File dir = new File(filePath);
                LOG.info("Extracting "+ filePath);
                dir.mkdirs();
            }
            zipInputStream.closeEntry();
            zipEntry = zipInputStream.getNextEntry();
        }
        zipInputStream.close();
    }

    @Override
    public String CheckContent(String filePath, String patchId) throws IOException {
        String errorMessage = "";

        File destDir = new File(filePath);
        if (!destDir.exists()) {
            return "Directory does not exist";
        }
        else {
            boolean check = new File(filePath + "LICENSE.txt").exists();
            if(!check) errorMessage = errorMessage + "LICENSE.txt does not exist\n";

            check = new File(filePath + "README.txt").exists();
            if(!check) errorMessage = errorMessage + "README.txt does not exist\n";

            check = new File(filePath + "NOT_A_CONTRIBUTION.txt").exists();
            if(!check) errorMessage = errorMessage + "NOT_A_CONTRIBUTION.txt does not exist\n";

            check = new File(filePath + "patch" + patchId).exists();
            if(!check) errorMessage = errorMessage + "patch folder does not exist\n";

            return errorMessage;
        }
    }

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }
    private static boolean isDirEmpty(final File directory) throws IOException {
        try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory.toPath())) {
            return !dirStream.iterator().hasNext();
        }
    }
}
