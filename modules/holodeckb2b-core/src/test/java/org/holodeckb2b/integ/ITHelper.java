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
package org.holodeckb2b.integ;

import org.apache.commons.lang.SystemUtils;
import org.holodeckb2b.testhelpers.FilesUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static org.junit.Assert.assertTrue;

/**
 * Created at 9:56 29.11.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class ITHelper {

    private static String dFilePath;
    private static String dFileName;
    private static String dDirName;
    private static String workingDirPath;

    private static Process processA;
    private static Process processB;

    static {
        dFilePath = ITHelper.class.getClassLoader()
                .getResource("").getPath();
        dFileName = "holodeckb2b-distribution-next-SNAPSHOT-3-full.zip";
        dFilePath += "/../../../holodeckb2b-distribution/target/"
                + dFileName;
        dDirName = "holodeck-b2b-next-SNAPSHOT-3";
        workingDirPath = ITHelper.class.getClassLoader()
                .getResource("integ").getPath();
    }

    /**
     * Unpacks HolodeckB2B distribution and renames the distribution directory
     * to <code>distrDirName</code>
     * @param distrDirName
     */
    void unzipHolodeckDistribution(String distrDirName) {
        // unzip first instance of the distribution zip file
        FilesUtility fu = new FilesUtility();
        try {
            fu.unzip(dFilePath, workingDirPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // rename distr dir
        File distrDir = new File(workingDirPath +"/"+ dDirName);
        assertTrue(distrDir.exists());
        if(distrDir.exists()) {
            distrDir.renameTo(new File(workingDirPath +"/"+ distrDirName));
        }
    }

    /**
     * Creates <code>distrDirName</code>/data/msg_out &
     * <code>distrDirName</code>/data/msg_in directories
     * @param distrDirName
     */
    void createDataMsgDirs(String distrDirName) {
        File msgOutDir = new File(workingDirPath + "/" + distrDirName
                + "/" + "data" + "/" + "msg_out");
        msgOutDir.mkdir();
        File msgInDir = new File(workingDirPath + "/" + distrDirName
                + "/" + "data" + "/" + "msg_in");
        msgInDir.mkdir();
    }

    /**
     * Copies <code>pmodeFileName</code> to <code>distrDirName</code>/conf/pmodes directory
     * @param distrDirName
     * @param pmodeFileName
     */
    void copyPModeDescriptor(String distrDirName, String pmodeFileName) {
        File pmodeXml = new File(workingDirPath + "/" + distrDirName + "/"
                + "examples" + "/" + "pmodes" + "/" + pmodeFileName);
        File newPmodeXml = new File(workingDirPath + "/" + distrDirName + "/"
                + "conf" + "/" + "pmodes" + "/" + pmodeFileName);
        try {
            Files.copy(pmodeXml.toPath(), newPmodeXml.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Changes the receiver port of the HolodeckB2B instance
     * @param distrDirName
     * @param port
     */
    void modifyAxisServerPort(String distrDirName, String port) {
        File axisXml = new File(workingDirPath + "/" + distrDirName + "/"
                + "conf" + "/" + "axis2.xml");
        assertTrue(axisXml.exists());
        if(axisXml.exists()) {
            changePortInAxis2Xml(axisXml.getPath(), port);
        }
    }

    /**
     * Starts sender & receiver HolodeckB2B instances
     * @param dADirName sender HolodeckB2B instance
     * @param dBDirName receiver HolodeckB2B instance
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
                pb.redirectOutput(new File(workingDirPath + "/output.log"));
                pb.redirectError(new File(workingDirPath + "/error.log"));

                pb.directory(
                        new File(workingDirPath + "/" + dADirName + "/" + "bin"));
                processA = pb.start();
                pb.directory(
                        new File(workingDirPath + "/" + dBDirName + "/" + "bin"));
                processB = pb.start();
            } else if (SystemUtils.IS_OS_WINDOWS) {
                // todo check this in Windows OS
                ProcessBuilder pb =
                        new ProcessBuilder("startServer.bat");
                pb.redirectOutput(new File(workingDirPath + "/output.log"));
                pb.redirectError(new File(workingDirPath + "/error.log"));
                pb.directory(
                        new File(workingDirPath + "/" + dADirName + "/" + "bin"));
                processA = pb.start();
                pb.directory(
                        new File(workingDirPath + "/" + dBDirName + "/" + "bin"));
                processB = pb.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Stops HolodeckB2B receiver and sender instances
     * @param dADirName
     * @param dBDirName
     */
    void stopHolodeckB2BInstances(String dADirName, String dBDirName) {
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
//                    System.out.println("pid: " + pid);
                        String command = "kill -9 " + pid;
                        Process pr = Runtime.getRuntime().exec(command);
                        pr.waitFor();
                    }
                }
                p.waitFor();
            } else if (SystemUtils.IS_OS_WINDOWS) {
                // todo
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Copies the example data files to <code>distrDirName</code>/data/msg_out directory
     * @param distrDirName
     */
    void copyExampleDataToMsgOutDir(String distrDirName) {
        File msgsDir = new File(workingDirPath + "/" + distrDirName + "/"
                + "examples" + "/" + "msgs");
        File msgOutDir =
                new File(workingDirPath + "/" + distrDirName
                        + "/" + "data" + "/" + "msg_out");
        File[] msgsFiles = msgsDir.listFiles();
        try {
            for (int i = 0; i < msgsFiles.length; i++) {
                File newMsgsFile =
                        new File(msgOutDir.getPath() + "/"
                                + msgsFiles[i].getName());
                Files.copy(msgsFiles[i].toPath(), newMsgsFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                if(msgsFiles[i].isDirectory()) {
                    File[] files = msgsFiles[i].listFiles();
                    for(int j = 0; j < files.length;j ++) {
                        newMsgsFile =
                                new File(msgOutDir.getPath() + "/"
                                        + msgsFiles[i].getName()
                                        + "/" + files[j].getName());
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
     * Changes msg file extension to .mmd
     * @param msgFileName
     * @param dADirName
     */
    boolean changeMsgExtensionToMMD(String msgFileName,
                                                  String dADirName) {
        File file = new File(workingDirPath + "/" + dADirName
                + "/data/msg_out/" + msgFileName);
        int index = file.getName().lastIndexOf(".");
        String fileName = file.getName().substring(0, index);
        String newFileName = fileName + ".mmd";
        File newFile = new File(file.getParentFile().getAbsolutePath()
                + "/" + newFileName);
        return file.renameTo(newFile);
    }

    /**
     * Deletes <code>distrDirName</code> directory
     * @param distrDirName
     */
    void deleteDistDir(String distrDirName) {
        FilesUtility fu = new FilesUtility();
        File distrDir = new File(workingDirPath+"/"+distrDirName);
        if(distrDir.exists()) {
            try {
                fu.deleteFolderAndItsContent(distrDir.toPath());
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
    void changePortInAxis2Xml(String filePath, String port) {
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
}
