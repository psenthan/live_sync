package org.wso2.patch.validator;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc2.SvnCheckout;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.wso2.interfaces.CommonValidator;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.wso2.service.SyncService;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

public class PatchZipValidator implements CommonValidator {
    private static final int BUFFER_SIZE = 4096;
    private static final Logger LOG = LoggerFactory.getLogger(CommonValidator.class);
    private Properties prop = new Properties();
    private static String login = "";
    private static String password ="";

    @Override
    public String CheckReadMe(String filePath, String patchId) throws IOException {
        String errorMessage = "";
        Boolean jar = false;
        Boolean war = false;
        Boolean jag = false;
        File dir = new File(filePath);
        if (!dir.exists()) {
            for (File file : dir.listFiles()) {
                if(file.getName().endsWith((".jar")))
                    jar = true;
                if(file.getName().endsWith((".war")))
                    war = true;
                if(file.getName().endsWith((".jag")))
                    jag = true;
            }
        }

        String filepath = filePath + "README.txt";
        File file = new File(filepath);
        if (!file.exists()) {
            return "Relevant README.txt does not exist\n";
        }

        List<String> lines = FileUtils.readLines(file, "UTF-8");

        String[] line = lines.get(0).split("-");
        if (!Objects.equals(patchId, line[4]) || Objects.equals(lines.get(0), "Patch ID         : patchId"))
            errorMessage = "'Patch ID' line in the README.txt has an error\n";

        line = lines.get(1).split(": ");
        if (line.length < 2 || Objects.equals(line[1], "productList"))
            errorMessage = errorMessage + "'Applies To' line in the README.txt has an error\n";

        line = lines.get(2).split(": ");
        if (line.length < 2 ||  Objects.equals(line[1], "publicJIRA"))
            errorMessage = errorMessage + "'Associated JIRA' line in the README.txt has an error\n";

        for(int i=3; i< lines.size(); i++){
            if(lines.get(i).startsWith("DESCRIPTION")){
                if(lines.get(i+1).startsWith("Patch description goes here"))
                    errorMessage = errorMessage + "DESCRIPTION section in the README.txt is not in the correct format\n";
                i++;
            }
            if(lines.get(i).startsWith("INSTALLATION INSTRUCTIONS")){
                boolean jaggeryInstruction = false;
                for(int j=i+1; j< lines.size(); j++){
                    if(jag &&
                            lines.get(j).startsWith(
                                    " Merge and Replace resource/store to " +
                                            "<CARBON_SERVER>/repository/deployment/server/jaggeryapps/store") &&
                            lines.get(j+1).startsWith(
                            " Merge and Replace resource/publisher to " +
                                    "<CARBON_SERVER>/repository/deployment/server/jaggeryapps/publisher")){
                        jaggeryInstruction = true;
                        break;

                    }
                    if(lines.get(j).startsWith("Copy the patchNumber to"))
                        errorMessage = errorMessage + "INSTALLATION INSTRUCTIONS section " +
                                "in the README.txt is not in the correct format\n";
                    if(lines.get(j).startsWith("Copy the patch")) {
                        if(!lines.get(j).startsWith("Copy the patch" + patchId + " to"))
                            errorMessage = errorMessage + "INSTALLATION INSTRUCTIONS section " +
                                "in the README.txt is not in the correct format\n";
                    }
                    i++;
                }
                if(jag && !jaggeryInstruction){
                    errorMessage = errorMessage + "Jaggery instructions are not in the correct format";
                }
            }
        }
        return errorMessage;
    }

    @Override
    public String CheckLicense(String filepath) throws IOException {
        prop.load(PatchZipValidator.class.getClassLoader().getResourceAsStream("application.properties"));
        final String license = prop.getProperty("license");

        File file = new File(filepath);
        if (!file.exists()) {
            return "Relevant LICENSE.txt  does not exist\n";
        }
        FileInputStream fis = new FileInputStream(new File(filepath));
        String md5 = md5Hex(fis);
        fis.close();
        if(Objects.equals(md5, license)) return "";
        return "LICENSE.txt is not in the correct format";
    }

    @Override
    public String CheckNotAContribution(String filepath) throws IOException {
        prop.load(PatchZipValidator.class.getClassLoader().getResourceAsStream("application.properties"));
        final String notAContribution = prop.getProperty("notAContribution");

        File file = new File(filepath);
        if (!file.exists()) {
            return "Relevant NOT_A_CONTRIBUTION.txt does not exist\n";
        }
        FileInputStream fis = new FileInputStream(new File(filepath));
        String md5 = md5Hex(fis);
        fis.close();
        if(Objects.equals(md5, notAContribution)) return "";
        return "NOT_A_CONTRIBUTION.txt is not in the correct format";
    }

    @Override
    public String CheckPatch(String filepath) {
        File file = new File(filepath);
        if (!file.exists()) {
            return "patchxxxx file does not exist\n";
        }
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
    public String UnZip(File zipFilePath, String destFilePath) throws IOException {

        if (!zipFilePath.exists()) {
            return "patch does not exist to unzip\n";
        }
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
        return "";
    }

    @Override
    public String CheckContent(String filePath, String patchId) throws IOException {
        String errorMessage = "";

        File destDir = new File(filePath);
        if (!destDir.exists()) {
            return "patch"+ patchId + " content does not exist\n";
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

            check = new File(filePath + "wso2carbon-version.txt").exists();
            if(check) errorMessage = errorMessage + "Unexpected file found: wso2carbon-version.txt\n";

            String[] extensions = new String[] { "tmp", "swp", "DS_Dstore", "_MAX_OS" };
            List<File> files = (List<File>) FileUtils.listFiles(destDir, extensions, true);

            if(files.size() > 0)
                errorMessage = errorMessage + "Unexpected file found: check for temporary, hidden, etc files";

            File[] hiddenFiles = destDir.listFiles((FileFilter) HiddenFileFilter.HIDDEN);
            assert hiddenFiles != null;
            for (File hiddenFile : hiddenFiles) {
                errorMessage = errorMessage + "hidden file: " + hiddenFile.getName() + "\n";

            }
            for (File file : destDir.listFiles()) {
                if(file.getName().endsWith(("~")))
                    errorMessage = errorMessage + "Unexpected file found" + file.getName() + "\n";
            }
            check = new File(filePath + "resources").exists();
            if(check){
                check = new File(filePath + "resources/store").exists();
                if(!check) errorMessage = errorMessage + "inside the resources, store folder does not exist\n";

                check = new File(filePath + "resources/publisher").exists();
                if(!check) errorMessage = errorMessage + "inside the resources, publisher folder does not exist\n";
            }
            return errorMessage;
        }
    }

    @Override
    public void DownloadZipFile(String url, String version, String patchId, String destFilePath) {
        File destinationDirectory = new File(destFilePath);
        if (!destinationDirectory.exists()) {
            destinationDirectory.mkdirs();
        }

        Scanner in = new Scanner(System.in);
        System.out.print("Enter username:  ");
        login = in.nextLine();
        System.out.print("Enter password:  ");
        password = in.nextLine();

        if(SVNConnection(url, login, password)) {
            final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
            try {
                final SvnCheckout checkout = svnOperationFactory.createCheckout();
                checkout.setSource(SvnTarget.fromURL(SVNURL.parseURIDecoded(url)));
                checkout.setSingleTarget(SvnTarget.fromFile(destinationDirectory));
                checkout.run();
            } catch (SVNException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void CommitKeys(String url, String fileLocation) {
        if(SVNConnection(url, login, password)) {
            //todo: delete unzipped folder
            //todo: add all the files
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
        DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory.toPath());
        return !dirStream.iterator().hasNext();
    }

    private boolean SVNConnection(String svnURL, String svnUser, String svnPass){
        DAVRepositoryFactory.setup();
        String url=svnURL;
        String name=svnUser;
        String password=svnPass;
        SVNRepository repository = null;
        try {
            repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(url));
            ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(name, password);
            repository.setAuthenticationManager(authManager);
            repository.testConnection();
            System.out.println("Connection done..");
            return true;
        } catch (SVNException e){
            System.out.println("Not connected");
            e.printStackTrace();
            return false;
        }

    }
}
