/**
 * 
 * Copyright (c)  2016, California Institute of Technology ("Caltech"). U.S. Government
sponsorship acknowledged. All rights reserved.
Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory, nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 

 */
package gov.nasa.jpl.tmt.plugins.magicdraw.sysml.monteCarlo.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.jena.ext.com.google.common.io.Files;

/**
 * @author sherzig
 *
 */
public class PluginPackager {

	public static void main(String[] args) throws Exception {
		createPluginStructure();
		zipPlugin("montecarlo-1.0.0alpha.zip");
	}
	
	public static void createPluginStructure() throws IOException {
		// Temporary
		File targetDir = new File("target/distribution");
		
		if (!targetDir.exists())
			targetDir.mkdirs();
		
		// Create plugin structure
		File dataDir = new File(targetDir.getAbsoluteFile() + "/data/resourcemanager");
		
		if (!dataDir.exists())
			dataDir.mkdirs();
		
		File pluginDir = new File(targetDir.getAbsoluteFile() + "/plugins/gov.nasa.jpl.tmt.plugins.magicdraw.sysml.monteCarlo");
		
		if (!pluginDir.exists())
			pluginDir.mkdirs();
		
		// Copy plugin.xml to target
		File pluginXML = new File("plugin.xml");
		Files.copy(pluginXML, new File(pluginDir + "/plugin.xml"));
		
		// Copy jar to target
		File jar = new File("monteCarlo.jar");
		Files.copy(jar, new File(pluginDir + "/monteCarlo.jar"));
		
		// Copy descriptor
		File descriptor = new File("MDR_Plugin_MonteCarloTools_8573_descriptor.xml");
		Files.copy(descriptor, new File(dataDir + "/MDR_Plugin_MonteCarloTools_8573_descriptor.xml"));
	}
	
	public static void zipPlugin(String zipFile) {
		List<String> fileList = new ArrayList<String>();
		File distribution = new File("target/distribution");
		
		File z = new File(zipFile);
		if (z.exists())
			z.delete();
		
		fileList = recursivelyAddFiles(distribution);
		
		byte[] buffer = new byte[1024];
		String source = "";
		FileOutputStream fos = null;
		ZipOutputStream zos = null;
		try {
			try {
				source = distribution.getAbsolutePath().substring(distribution.getAbsolutePath().lastIndexOf("\\") + 1, distribution.getAbsolutePath().length());
			} catch (Exception e) {
				source = distribution.getAbsolutePath();
			}
			fos = new FileOutputStream(zipFile);
			zos = new ZipOutputStream(fos);

			System.out.println("Output to Zip : " + zipFile);
			FileInputStream in = null;

			for (String file : fileList) {
				System.out.println("File Added : " + file);
				ZipEntry ze = new ZipEntry(source + File.separator + file);
				zos.putNextEntry(ze);
				try {
					in = new FileInputStream(distribution.getAbsolutePath() + File.separator + file);
					int len;
					while ((len = in.read(buffer)) > 0) {
						zos.write(buffer, 0, len);
					}
				} finally {
					in.close();
				}
			}

			zos.closeEntry();
			System.out.println("Folder successfully compressed");

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				zos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static ArrayList<String> recursivelyAddFiles(File f) {
		ArrayList<String> fileList = new ArrayList<String>();
		
		if (f.isFile())
			fileList.add(generateZipEntry(f.getAbsolutePath()));
		else {
			for (File subF : f.listFiles())
				fileList.addAll(recursivelyAddFiles(subF));
		}
		
		return fileList;
	}
	
	private static String generateZipEntry(String file)
	{
	   return file.substring((int) (new File("target/distribution").getAbsolutePath().length() + 1), file.length());
	}
	
}
