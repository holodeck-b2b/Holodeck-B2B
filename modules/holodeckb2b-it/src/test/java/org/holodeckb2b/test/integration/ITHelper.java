/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.test.integration;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.holodeckb2b.testhelpers.FilesUtility;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Created at 9:56 29.11.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class ITHelper {

    private static String projectVersion;
    private static String dFilePath;
    private static String dFileName;
    private static String dDirName;
    private static String workingDirPath;

    private static Process processA;
    private static Process processB;

    static {
        projectVersion = getProjectVersion();
        dFileName = "holodeckb2b-distribution-"+projectVersion+"-default.zip";
        dFilePath = ITHelper.class.getClassLoader().getResource("").getPath();
        dFilePath += "/../../../holodeckb2b-distribution/target/" + dFileName;
        System.out.println("version: " + projectVersion);
        // todo dir name should be taken from pom.xml
        dDirName = "holodeck-b2b-"+projectVersion;
        workingDirPath = ITHelper.class.getClassLoader().getResource("integ").getPath();
        System.out.println("working dir: " + workingDirPath);
    }

    /**
     *
     * @return
     */
    private static String getProjectVersion() {
        File corePomfile = new File(
                ITHelper.class.getClassLoader().getResource("").getPath()
                        + "/../../pom.xml");
        Model model = null;
        FileReader reader;
        MavenXpp3Reader mavenReader = new MavenXpp3Reader();
        try {
            reader = new FileReader(corePomfile);
            model = mavenReader.read(reader);
            model.setPomFile(corePomfile);
        } catch(Exception ex){}
        MavenProject project = new MavenProject(model);
        return project.getVersion();
    }

    /**
     * Unpacks HolodeckB2B distribution and renames the distribution directory
     * to <code>distrDirName</code>
     * @param distrDirName HolodeckB2B instance folder name
     */
    void unzipHolodeckDistribution(String distrDirName) {
        FilesUtility fu = new FilesUtility();
        try {
            fu.unzip(dFilePath, workingDirPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        File distrDir = new File(workingDirPath +File.separator+ dDirName);
        assertTrue(distrDir.exists());
        if(distrDir.exists()) {
            distrDir.renameTo(new File(workingDirPath +File.separator+ distrDirName));
        }
    }

    /**
     * Copies <code>pmodeFileName</code> to <code>distrDirName</code>/conf/pmodes directory
     * @param distrDirName HolodeckB2B instance folder name
     * @param pmodeFileName pmode configuration file name
     */
    void copyPModeDescriptor(String distrDirName, String pmodeFileName) {
        File pmodeXml = new File(workingDirPath + File.separator + distrDirName + File.separator
                + "examples" + File.separator + "pmodes" + File.separator + pmodeFileName);
        File newPmodeXml = new File(workingDirPath + File.separator + distrDirName + File.separator
                + "conf" + File.separator + "pmodes" + File.separator + pmodeFileName);
        try {
            Files.copy(pmodeXml.toPath(), newPmodeXml.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Changes the receiver port of the HolodeckB2B instance
     * @param distrDirName HolodeckB2B instance folder name
     * @param port
     */
    void modifyAxisServerPort(String distrDirName, String port) {
        File axisXml = new File(workingDirPath + File.separator + distrDirName + File.separator
                + "conf" + File.separator + "axis2.xml");
        assertTrue(axisXml.exists());
        if(axisXml.exists()) {
            changePortInAxis2Xml(axisXml.getPath(), port);
        }
    }

    /**
     * Starts sender &amp; receiver HolodeckB2B instances
     * @param dADirName initiator HolodeckB2B instance folder name
     * @param dBDirName responder HolodeckB2B instance folder name
     */
    void startHolodeckB2BInstances(String dADirName, String dBDirName) {
        String command;
        Process p;
        try {
            if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) {
                command = "chmod +x " + workingDirPath + "/" + dADirName + "/"
                        + "bin" + "/" + "startServer.sh";
                p = Runtime.getRuntime().exec(command);
                p.waitFor();
                command = "chmod +x " + workingDirPath + "/" + dBDirName + "/"
                        + "bin" + "/" + "startServer.sh";
                p = Runtime.getRuntime().exec(command);
                p.waitFor();

                ProcessBuilder pb =
                        new ProcessBuilder("/bin/bash", "./startServer.sh");
                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                pb.redirectError(new File(workingDirPath + "/error.log"));
                pb.environment().put("JAVA_HOME", System.getProperty("java.home"));
                pb.directory(
                        new File(workingDirPath + "/" + dADirName + "/" + "bin"));
                processA = pb.start();
                pb.directory(
                        new File(workingDirPath + "/" + dBDirName + "/" + "bin"));
                processB = pb.start();
            } else if (SystemUtils.IS_OS_WINDOWS) {
                ProcessBuilder pbA = new ProcessBuilder(workingDirPath + File.separator + dADirName + File.separator
                                + "bin" + File.separator + "startServer.bat");
                pbA.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                pbA.redirectError(new File(workingDirPath + File.separator + "error.log"));
                pbA.environment().put("JAVA_HOME", System.getProperty("java.home"));
                processA = pbA.start();

                ProcessBuilder pbB = new ProcessBuilder(workingDirPath + File.separator + dBDirName + File.separator
                                + "bin" + File.separator + "startServer.bat");
                pbB.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                pbB.redirectError(new File(workingDirPath + File.separator + "error.log"));
                pbB.environment().put("JAVA_HOME", System.getProperty("java.home"));
                processB = pbB.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Stops HolodeckB2B receiver and sender instances
     */
    void stopHolodeckB2BInstances() {
        try {
            processA.destroy();
            processB.destroy();
            if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) {
                Process p = Runtime.getRuntime().exec("jps");
                BufferedReader in =
                        new BufferedReader(
                                new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.contains("SimpleAxis2Server")) {
                        String pid = line.split(" ")[0];
                        String command = "kill -9 " + pid;
                        Process pr = Runtime.getRuntime().exec(command);
                        pr.waitFor();
                    }
                }
                p.waitFor();
            } else if (SystemUtils.IS_OS_WINDOWS) {
                Process p = Runtime.getRuntime().exec("wmic process where \"CommandLine Like '%SimpleAxis2Server%'\" call terminate");
                p.waitFor();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Copies the example data files to <code>distrDirName</code>/data/msg_out directory
     * @param distrDirName HolodeckB2B instance folder name
     */
    void copyExampleDataToMsgOutDir(String distrDirName) {
        File msgsDir = new File(workingDirPath + File.separator + distrDirName + File.separator
                + "examples" + File.separator + "msgs");
        File msgOutDir =
                new File(workingDirPath + File.separator + distrDirName
                        + File.separator + "data" + File.separator + "msg_out");
        File[] msgsFiles = msgsDir.listFiles();
        try {
            for (int i = 0; i < msgsFiles.length; i++) {
                File newMsgsFile =
                        new File(msgOutDir.getPath() + File.separator
                                + msgsFiles[i].getName());
                Files.copy(msgsFiles[i].toPath(), newMsgsFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                if(msgsFiles[i].isDirectory()) {
                    File[] files = msgsFiles[i].listFiles();
                    for(int j = 0; j < files.length;j ++) {
                        newMsgsFile =
                                new File(msgOutDir.getPath() + File.separator
                                        + msgsFiles[i].getName()
                                        + File.separator + files[j].getName());
                        Files.copy(files[j].toPath(), newMsgsFile.toPath(),
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param distrDirName HolodeckB2B instance folder name
     */
    void copyKeystores(String distrDirName) {
        File keysDir = new File(workingDirPath + File.separator + distrDirName + File.separator
                + "examples" + File.separator + "certs");
        File repoKeysDir =
                new File(workingDirPath + File.separator + distrDirName
                        + File.separator + "repository" + File.separator + "certs");
        File[] keysFiles = keysDir.listFiles();
//                keysDir.listFiles(new FilenameFilter() {
//            @Override
//            public boolean accept(File dir, String name) {
//                if(name.endsWith("jks"))
//                    return true;
//                return false;
//            }
//        });
        try {
            for (File f : keysFiles) {
                File newF =
                        new File(repoKeysDir.getPath() + File.separator + f.getName());
                Files.copy(f.toPath(), newF.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes contents of msg_out &amp; msg_in folders of the HolodeckB2B instance
     * @param distrDirName HolodeckB2B instance folder name
     */
    void clearMsgOutAndMsgInDirs(String distrDirName) {
        File msgOutDir =
                new File(workingDirPath + File.separator + distrDirName
                        + File.separator + "data" + File.separator + "msg_out");
        File msgInDir =
                new File(workingDirPath + File.separator + distrDirName
                        + File.separator + "data" + File.separator + "msg_in");
        FilesUtility fu = new FilesUtility();
        try {
            fu.deleteFolderContent(msgOutDir.toPath(), false);
            fu.deleteFolderContent(msgInDir.toPath(), false);
        } catch (IOException e) {
//            e.printStackTrace();
        }
    }

    /**
     * Changes msg file extension to .mmd
     * @param msgFileName message file name
     * @param distrDirName HolodeckB2B instance folder name
     */
    boolean changeMsgExtensionToMMD(String msgFileName,
                                                  String distrDirName) {
        File file = new File(workingDirPath + File.separator + distrDirName
                + File.separator + "data"+ File.separator + "msg_out" + File.separator + msgFileName);
        int index = file.getName().lastIndexOf(".");
        String fileName = file.getName().substring(0, index);
        String newFileName = fileName + ".mmd";
        File newFile = new File(file.getParentFile().getAbsolutePath()
                + File.separator + newFileName);
        return file.renameTo(newFile);
    }

    /**
     * Deletes <code>distrDirName</code> directory
     * @param distrDirName HolodeckB2B instance folder name
     */
    void deleteDistDir(String distrDirName) {
        FilesUtility fu = new FilesUtility();
        File distrDir = new File(workingDirPath+File.separator+distrDirName);
        if(distrDir.exists()) {
            try {
                fu.deleteFolderContent(distrDir.toPath(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Changes receiver port in axis2.xml
     * @param filePath
     * @param port
     */
    private void changePortInAxis2Xml(String filePath, String port) {
        try {
            DocumentBuilderFactory docFactory =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(filePath);
            Node transportReceiver =
                    doc.getElementsByTagName("transportReceiver").item(0);
            NodeList nodes = transportReceiver.getChildNodes();
            int nodesAmount = nodes.getLength();
            for(int i = 0; i < nodesAmount; i++) {
                Node n = nodes.item(i);
                String nN = n.getNodeName();
                if(nN.equals("parameter")) {
                    n.setTextContent(port);
                }
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(filePath));
            transformer.transform(source, result);
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (SAXException sae) {
            sae.printStackTrace();
        }
    }

    /**
     * Set the pulling interval of the pull worker
     * @param distrDirName HolodeckB2B instance dir
     * @param interval time interval in seconds
     */
    void setPullingInterval(String distrDirName, int interval) {
        File axisXml = new File(workingDirPath + File.separator + distrDirName + File.separator
                + "conf" + File.separator + "pulling_configuration.xml");
        assertTrue(axisXml.exists());
        if(axisXml.exists()) {
            changeIntervalInPullingConfigurationXml(axisXml.getPath(), interval);
        }
    }

    /**
     * Change the value of the pulling interval in seconds
     * @param filePath path to configuration file
     * @param interval time interval in seconds
     */
    private void changeIntervalInPullingConfigurationXml(String filePath, int interval) {
        try {
            DocumentBuilderFactory docFactory =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(filePath);
            Node pulling = doc.getElementsByTagName("pulling").item(0);
            NodeList nodes = pulling.getChildNodes();
            int nodesAmount = nodes.getLength();
            for(int i = 0; i < nodesAmount; i++) {
                Node n = nodes.item(i);
                String nN = n.getNodeName();
                if(nN.equals("default")) {
                    NamedNodeMap attributes = n.getAttributes();
                    attributes.getNamedItem("interval")
                            .setTextContent(String.valueOf(interval));
                }
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(filePath));
            transformer.transform(source, result);
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (SAXException sae) {
            sae.printStackTrace();
        }
    }

    /**
     * Checks if the file <code>fileName</code> exists in <code>dirName</code>
     * @param fileName
     * @param dirName
     * @return
     */
    boolean fileExistsInDirectory(String fileName, String dirName) {
        File file = new File(workingDirPath + File.separator + dirName + File.separator + fileName);
        return file.exists();
    }

    /**
     *
     * @param dirName directory name
     * @return true - if the <code>dirName</code> directory is not empty,
     *          false - otherwise
     */
    boolean dirIsNotEmpty(String dirName) {
        boolean res = false;
        File dir = new File(workingDirPath + File.separator + dirName);
        if(dir.exists()) {
            res = dir.listFiles().length > 0;
        }
        return res;
    }
}
