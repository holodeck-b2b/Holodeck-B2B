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

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisModule;
import org.apache.commons.lang.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.testhelpers.FilesUtility;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created at 13:45 06.11.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class OutFlowIT {
    private static String dFilePath;
    private static String dFileName;
    private static String dDirName;
    private static String workingDirPath;

    private static String dADirName = "HolodeckB2B-A";
    private static String dBDirName = "HolodeckB2B-B";

    private static Process processA;
    private static Process processB;

    private static final Logger log = LogManager.getLogger(OutFlowIT.class);

    @BeforeClass
    public static void setUpClass() {
        dFilePath = OutFlowIT.class.getClassLoader()
                .getResource("").getPath();
        dFileName = "holodeckb2b-distribution-next-SNAPSHOT-3-full.zip";
        dDirName = "holodeck-b2b-next-SNAPSHOT-3";
        dFilePath += "/../../../holodeckb2b-distribution/target/"
                + dFileName;
        workingDirPath = OutFlowIT.class.getClassLoader()
                .getResource("integ").getPath();

        unzipHolodeckDistribution(dADirName);
        unzipHolodeckDistribution(dBDirName);
        createDataMsgDirs(dADirName);
        createDataMsgDirs(dBDirName);
        copyPModeDescriptor(dADirName, "ex-pm-push-init.xml");
        copyPModeDescriptor(dBDirName, "ex-pm-push-resp.xml");
        modifyAxisServerPort(dBDirName, "9090");
        startHolodeckB2BInstances(dADirName, dBDirName);
        copyExampleDataToMsgOutDir(dADirName);
    }

    @AfterClass
    public static void tearDownClass() {
        stopHolodeckB2BInstances(dADirName, dBDirName);
//        deleteDistDir(dADirName);
//        deleteDistDir(dBDirName);
    }

    @Test
    public void testOneWayPush() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // change ex-mmd-push.accepted ext to .mmd
        File msgFile = new File(workingDirPath + "/" + dADirName
                + "/data/msg_out/" + "ex-mmd-push.accepted");
        assertTrue(changeMsgExtensionToMMD(msgFile));

        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // todo check the results in A/msg_out & A/msg_in & B/msg_in

        // todo check A/msg_out

        // ex-mmd-push.accepted should be present

        // todo check B/msg_in

        // message xml should be present

        // todo check A/msg_in

        // receipt message xml should be present

    }


    /**
     * Unpacks HolodeckB2B distribution and renames the distribution directory
     * to <code>distrDirName</code>
     * @param distrDirName
     */
    private static void unzipHolodeckDistribution(String distrDirName) {
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
    private static void createDataMsgDirs(String distrDirName) {
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
    private static void copyPModeDescriptor(String distrDirName, String pmodeFileName) {
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
    private static void modifyAxisServerPort(String distrDirName, String port) {
        File axisXml = new File(workingDirPath + "/" + distrDirName + "/"
                + "conf" + "/" + "axis2.xml");
        assertTrue(axisXml.exists());
        if(axisXml.exists()) {
            changePortInAxisXml(axisXml.getPath(), port);
        }
    }

    /**
     * Starts sender & receiver HolodeckB2B instances
     * @param dADirName sender HolodeckB2B instance
     * @param dBDirName receiver HolodeckB2B instance
     */
    private static void startHolodeckB2BInstances(String dADirName, String dBDirName) {
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
    private static void stopHolodeckB2BInstances(String dADirName, String dBDirName) {
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
    private static void copyExampleDataToMsgOutDir(String distrDirName) {
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
     * Changes file extension to .mmd
     * @param file
     */
    public static boolean changeMsgExtensionToMMD(File file) {
        boolean result = false;
        int index = file.getName().lastIndexOf(".");
        String fileName = file.getName().substring(0, index);
        //System.out.println(fileName);
        String ext = file.getName().substring(index);
        //System.out.println(ext);
        String newFileName = fileName + ".mmd";
        //System.out.println("newFileName: "
//                + file.getParentFile().getAbsolutePath() + "/" + newFileName);
        File newFile = new File(file.getParentFile().getAbsolutePath()
                + "/" + newFileName);
        result = file.renameTo(newFile);
        return result;
    }

    /**
     * Deletes <code>distrDirName</code> directory
     * @param distrDirName
     */
    private static void deleteDistDir(String distrDirName) {
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
    private static void changePortInAxisXml(String filePath, String port) {
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
