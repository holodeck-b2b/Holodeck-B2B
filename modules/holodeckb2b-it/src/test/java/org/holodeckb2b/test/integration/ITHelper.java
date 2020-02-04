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
import java.nio.file.Path;
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
import org.apache.logging.log4j.LogManager;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.holodeckb2b.common.testhelpers.TestUtils;
import org.holodeckb2b.testhelpers.FilesUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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

	private static final String HB2B_DISTRO_MODULE = "holodeckb2b-distribution";
	
    private static String projectVersion;
    private static String distributionPath;

    private static Path   workingDirPath;

    private static Process processA;
    private static Process processB;

    static {
    	final Path projectHome = TestUtils.getPath(".").resolve("../..");
    	MavenProject project;
    	
    	final File pomFile = projectHome.resolve("pom.xml").toFile();
    	try (FileReader reader = new FileReader(pomFile)) {
    		final MavenXpp3Reader mavenReader = new MavenXpp3Reader();
    		final Model model = mavenReader.read(reader);
    		model.setPomFile(pomFile);
    		project = new MavenProject(model);
    	} catch(Exception ex) {
    		LogManager.getLogger().fatal("Could not read project meta-data. ABORTING integration test!");
    		throw new RuntimeException();
    	}
    	projectVersion = project.getVersion();
    	    
    	distributionPath =  projectHome.resolve("../" + HB2B_DISTRO_MODULE 
    												+ "/target/holodeckb2b-distribution-" + projectVersion + ".zip")
    										.toString();        

    	workingDirPath = TestUtils.getPath("integ");
    }

    /**
     * Unpacks HolodeckB2B distribution and renames the distribution directory to <code>instanceDir</code>
     * 
     * @param instanceDir HolodeckB2B instance folder name
     */
    void unzipHolodeckDistribution(String instanceDir) {
        FilesUtility fu = new FilesUtility();
        try {
            fu.unzip(distributionPath, workingDirPath.toString());
            Files.move(workingDirPath.resolve("holodeckb2b-" + projectVersion), workingDirPath.resolve(instanceDir));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Copies <code>pmodeFileName</code> to <code>distrDirName</code>/conf/pmodes directory
     * 
     * @param instanceDir HolodeckB2B instance folder name
     * @param pmodeFileName pmode configuration file name
     */
    void copyPModeDescriptor(String instanceDir, String pmodeFileName) {
        Path srcPModeFile = workingDirPath.resolve(instanceDir + "/examples/pmodes/" + pmodeFileName);
        Path destPmodeFile = workingDirPath.resolve(instanceDir + "/repository/pmodes/" + pmodeFileName);
        
        try {
            Files.copy(srcPModeFile, destPmodeFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Changes the receiver port of the HolodeckB2B instance
     * @param distrDirName HolodeckB2B instance folder name
     * @param port
     */
    void modifyServerPorts(String distrDirName, String msgPort, String rmiPort) {
        File hb2bXml =  workingDirPath.resolve(distrDirName + "/conf/holodeckb2b.xml").toFile();
        assertTrue(hb2bXml.exists());
        changeMainPort(hb2bXml.getPath(), msgPort);

        File workersXml = workingDirPath.resolve(distrDirName + "/conf/workers.xml").toFile();
        assertTrue(workersXml.exists());
    	addRMIPortWorkersXml(workersXml.getPath(), rmiPort);        
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
            	Path startScriptAPath = workingDirPath.resolve(dADirName + "/bin/startServer.sh");
            	Path startScriptBPath = workingDirPath.resolve(dBDirName + "/bin/startServer.sh");
            	
                command = "chmod +x " + startScriptAPath.toString();
                p = Runtime.getRuntime().exec(command);
                p.waitFor();
                command = "chmod +x " + startScriptBPath.toString();
                p = Runtime.getRuntime().exec(command);
                p.waitFor();

                ProcessBuilder pb = new ProcessBuilder("/bin/bash", startScriptAPath.getFileName().toString());
                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                pb.redirectError(workingDirPath.resolve("error.log").toFile());
                pb.environment().put("JAVA_HOME", System.getProperty("java.home"));
                pb.directory(startScriptAPath.getParent().toFile());
                processA = pb.start();
                pb.directory(startScriptBPath.getParent().toFile());
                processB = pb.start();
            } else if (SystemUtils.IS_OS_WINDOWS) {
            	Path startScriptAPath = workingDirPath.resolve(dADirName + "/bin/startServer.bat");
            	Path startScriptBPath = workingDirPath.resolve(dBDirName + "/bin/startServer.bat");
            	
            	ProcessBuilder pbA = new ProcessBuilder(startScriptAPath.toString());
                pbA.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                pbA.redirectError(workingDirPath.resolve("error.log").toFile());
                pbA.environment().put("JAVA_HOME", System.getProperty("java.home"));
                processA = pbA.start();

                ProcessBuilder pbB = new ProcessBuilder(startScriptBPath.toString());
                pbB.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                pbA.redirectError(workingDirPath.resolve("error.log").toFile());
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
                    if (line.contains("HolodeckB2BServer")) {
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
        File msgsDir = workingDirPath.resolve(distrDirName + "/examples/msgs").toFile();
        Path msgOutDir = workingDirPath.resolve(distrDirName + "/data/msg_out");
        
        try {
            for (File s : msgsDir.listFiles()) {
                if(s.isDirectory()) {
                	msgOutDir.resolve(s.getName()).toFile().mkdir();
                	for(File c : s.listFiles())
                    	Files.copy(c.toPath(), msgOutDir.resolve(s.getName() + "/" + c.getName()));
                } else 
                	Files.copy(s.toPath(), msgOutDir.resolve(s.getName()));
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Could not copy message data");
        }
    }

    /**
     *
     * @param distrDirName HolodeckB2B instance folder name
     */
    void copyKeystores(String distrDirName) {
        File msgsDir = workingDirPath.resolve(distrDirName + "/examples/certs").toFile();
        Path msgOutDir = workingDirPath.resolve(distrDirName + "/repository/certs");
        try {
            for (File s : msgsDir.listFiles()) 
                Files.copy(s.toPath(), msgOutDir.resolve(s.getName()), StandardCopyOption.REPLACE_EXISTING);                                                
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Could not copy key stores");
        }
    }

    /**
     * Deletes contents of msg_out &amp; msg_in folders of the HolodeckB2B instance
     * @param distrDirName HolodeckB2B instance folder name
     */
    void clearMsgOutAndMsgInDirs(String distrDirName) {
        FilesUtility fu = new FilesUtility();
        try {
            fu.deleteFolderContent(workingDirPath.resolve(distrDirName + "/data/msg_out"), false);
            fu.deleteFolderContent(workingDirPath.resolve(distrDirName + "/data/msg_in"), false);
        } catch (IOException e) {
//            e.printStackTrace();
        }
    }

    /**
     * Changes msg file extension to .mmd
     * @param msgFileName message file name
     * @param distrDirName HolodeckB2B instance folder name
     */
    boolean changeMsgExtensionToMMD(String msgFileName, String distrDirName) {
        
    	File file = workingDirPath.resolve(distrDirName + "/data/msg_out/" + msgFileName).toFile();
        int index = file.getName().lastIndexOf(".");
        String fileName = file.getName().substring(0, index);
        String newFileName = fileName + ".mmd";
        File newFile = new File(file.getParentFile().getAbsolutePath() + File.separator + newFileName);
        return file.renameTo(newFile);
    }

    /**
     * Deletes <code>distrDirName</code> directory
     * @param distrDirName HolodeckB2B instance folder name
     */
    void deleteDistDir(String distrDirName) {
        FilesUtility fu = new FilesUtility();
        Path distrDir = workingDirPath.resolve(distrDirName);
        if(Files.exists(distrDir)) {
            try {
                fu.deleteFolderContent(distrDir, true);
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
    private void changeMainPort(String filePath, String port) {
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
     * Adds port parameter to UI RMI Server in workers.xml
     * @param filePath
     * @param port
     */
    private void addRMIPortWorkersXml(String filePath, String port) {
    	try {
    		DocumentBuilderFactory docFactory =
    				DocumentBuilderFactory.newInstance();
    		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    		Document doc = docBuilder.parse(filePath);
    		NodeList workers = doc.getElementsByTagName("worker");
    		Node rmiWorker = workers.item(workers.getLength() - 1);

    		Element portParam = rmiWorker.getOwnerDocument().createElement("parameter");
    		portParam.setAttribute("name", "port");
    		portParam.setTextContent(port);
    		rmiWorker.appendChild(portParam);
    		
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

