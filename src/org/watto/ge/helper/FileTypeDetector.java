/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.helper;

import java.io.File;
import java.util.Hashtable;

import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.SingletonManager;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockQuickBMSExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_LZO_SingleBlock;
import org.watto.ge.plugin.exporter.Exporter_Oodle;
import org.watto.ge.plugin.exporter.Exporter_QuickBMSWrapper;
import org.watto.ge.plugin.exporter.Exporter_QuickBMS_Decompression;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameChecker;
import org.watto.io.buffer.ExporterByteBuffer;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;
import org.watto.task.Task_QuickBMSBulkExport;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

public class FileTypeDetector {

  static Hashtable<String, FileType> generic = new Hashtable<String, FileType>();

  static Hashtable<String, FileType> specific = new Hashtable<String, FileType>();

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static void addFileType(FileType fileType) {
    specific.put(fileType.getExtension(), fileType);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static void addFileType(String extension, String description) {
    specific.put(extension, new FileType(extension, description));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static void addFileType(String extension, String description, int type) {
    specific.put(extension, new FileType(extension, description, type));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static void clearSpecificDescriptions() {
    specific = new Hashtable<String, FileType>();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static void determineExtension(Resource resource) {
    // NOT DONE
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static void determineExtension(Resource resource, FileManipulator fm) {
    // NOT DONE
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static void determineExtensions(Resource[] resources, ArchivePlugin readPlugin) {
    try {

      if (readPlugin == null) {
        return;
      }

      int numFiles = resources.length;

      if (numFiles < 1) {
        return;
      }

      // First, go through all resources, look for any that need to be bulk-exported (eg ones that use QuickBMS compression)
      int numResources = resources.length;
      Resource[] bulkResources = new Resource[numResources];
      int numBulkResources = 0;

      for (int i = 0; i < numResources; i++) {
        Resource resource = resources[i];
        ExporterPlugin exporter = resource.getExporter();
        if (exporter instanceof Exporter_QuickBMSWrapper || exporter instanceof Exporter_QuickBMS_Decompression || exporter instanceof BlockQuickBMSExporterWrapper) {
          // add it to the Bulk list
          bulkResources[numBulkResources] = resource;
          numBulkResources++;
        }
        else {
          // don't extract it, we're going to shortcut it in the ExporterByteBuffer down later
        }
      }

      // Now run the bulk extract
      if (numBulkResources > 0) {
        if (numBulkResources != numResources) {
          Resource[] oldResources = bulkResources;
          bulkResources = new Resource[numBulkResources];
          System.arraycopy(oldResources, 0, bulkResources, 0, numBulkResources);
        }

        File directory = new File(new File(Settings.get("TempDirectory")).getAbsolutePath());
        directory = FilenameChecker.correctFilename(directory); // fix funny characters etc.

        Task_QuickBMSBulkExport task = new Task_QuickBMSBulkExport(bulkResources, directory);
        task.redo(); // run it within this Thread, not as a new one

        SingletonManager.add("BulkExport_KeepTempFiles", "See FileTypeDetector.determineExtensions()");
      }

      // Now, we have all the bulk ones extracted, and all the others can be read normally, so we're right to go.

      int readSize = 12; // only really small reads from the beginning of the file

      boolean debugMode = Settings.getBoolean("DebugMode");
      TaskProgressManager.setMessage(Language.get("IdentifyUnknownFileTypes"));

      ExporterPlugin defaultExporter = Exporter_Default.getInstance();

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        long decompLength = resource.getDecompressedLength();
        if (decompLength < readSize) {
          // too small, set to "unknown" and move on
          String extension = "unknown";
          if (decompLength == 0) {
            extension = "empty";
          }
          resource.setExtension(extension);
          resource.setOriginalName(resource.getName()); // so it doesn't think it's been renamed
          continue;
        }

        // We only want to extract the first few bytes, so we want to create a dummy Resource that is a copy of the real Resource
        // but with a small length. That way we can use the exporter plugin and it should, generally, only export a few bytes.
        Resource clonedResource = (Resource) resource.clone();
        ExporterPlugin clonedExporter = clonedResource.getExporter();
        if (clonedExporter instanceof Exporter_Oodle || clonedExporter instanceof Exporter_LZO_SingleBlock) {
          // These decompressions only works when you specify the actual decompLength.
          // (we can't just extract a few bytes, we need to extract the full file)
        }
        else {
          // small quick extract
          clonedResource.setLength(readSize);
        }

        File exportedPath = clonedResource.getExportedPath();
        if (exportedPath != null && exportedPath.exists()) {
          // use the Default Exporter, pointing to the exported file
          clonedResource.setExporter(defaultExporter);
          clonedResource.setSource(exportedPath);
          clonedResource.setOffset(0);
        }

        // Open the file
        ExporterByteBuffer byteBuffer = new ExporterByteBuffer(clonedResource);
        FileManipulator fm = new FileManipulator(byteBuffer);

        // Then we analyse the bytes to determine the file type
        byte[] headerBytes = fm.readBytes(readSize);
        int headerInt1 = IntConverter.convertLittle(new byte[] { headerBytes[0], headerBytes[1], headerBytes[2], headerBytes[3] });
        int headerInt2 = IntConverter.convertLittle(new byte[] { headerBytes[4], headerBytes[5], headerBytes[6], headerBytes[7] });
        int headerInt3 = IntConverter.convertLittle(new byte[] { headerBytes[8], headerBytes[9], headerBytes[10], headerBytes[11] });
        short headerShort1 = ShortConverter.convertLittle(new byte[] { headerBytes[0], headerBytes[1] });
        short headerShort2 = ShortConverter.convertLittle(new byte[] { headerBytes[2], headerBytes[3] });
        short headerShort3 = ShortConverter.convertLittle(new byte[] { headerBytes[4], headerBytes[5] });
        short headerShort4 = ShortConverter.convertLittle(new byte[] { headerBytes[6], headerBytes[7] });
        short headerShort5 = ShortConverter.convertLittle(new byte[] { headerBytes[8], headerBytes[9] });
        short headerShort6 = ShortConverter.convertLittle(new byte[] { headerBytes[10], headerBytes[11] });

        fm.close();

        String extension = null;
        // first, ask the plugin for any game-specific headers we can associate
        extension = readPlugin.guessFileExtension(resource, headerBytes, headerInt1, headerInt2, headerInt3, headerShort1, headerShort2, headerShort3, headerShort4, headerShort5, headerShort6);

        // if no extension found from the plugin, try some standard ones
        if (extension == null) {
          if (headerShort1 == 19778) {
            extension = "bmp";
          }
          else if (headerInt1 == 542327876) {
            extension = "dds";
          }
          else if (headerInt1 == 1130450022) {
            extension = "flac";
          }
          else if (headerInt1 == 944130375) {
            extension = "gif";
          }
          else if (headerInt1 == -503326465) {
            extension = "jpg";
          }
          else if (headerShort4 == 17994 && headerShort5 == 17993) {
            extension = "jpg";
          }
          else if (headerInt1 == 1399285583) {
            extension = "ogg";
          }
          else if (headerInt1 == 1196314761) {
            extension = "png";
          }
          else if (headerInt1 == 1179011410 && headerInt3 == 1163280727) { // RIFF WAVEfmt
            extension = "wav";
          }
          else if (headerInt1 == 1179011410 && headerInt3 == 1346520407) { // RIFF WEBP
            extension = "webp";
          }
          else if (headerInt1 == 1179011410 && headerInt3 == 1095587672) { // RIFF XWMA (XBox Audio)
            extension = "xwma";
          }
          else if (headerInt1 == 1179011410 && headerInt3 == 542524742) { // RIFF FEV
            extension = "fev";
          }
          else if (headerInt1 == 1179011410) {
            extension = "riff"; // GENERIC RIFF, NEEDS TO BE AT THE END OF THE LIST!!! (or at least after WAV and WEBP)
          }
          else if (headerInt1 == 1836597052) {
            extension = "xml";
          }
          else if (headerInt1 == 1178882085) {
            extension = "pdf";
          }
          else if (headerInt1 == 1634038339 && headerInt2 == 1702259060 && headerInt3 == 1768904224) {
            extension = "voc"; // Creative Voice File
          }
          else {
            // if we didn't find any matches...
            if (debugMode) {
              //extension = "unknown" + headerInt1;
              extension = "" + headerInt1;
            }
            else {
              extension = "unknown";
            }
          }
        }

        resource.setExtension(extension);
        resource.setOriginalName(resource.getName()); // so it doesn't think it's been renamed

        TaskProgressManager.setValue(i);

      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static void determineExtensions(Resource[] resources, FileManipulator fm) {
    // NOT DONE
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static FileType getFileType(String caseExtension) {
    String extension = caseExtension.toLowerCase();

    FileType specificType = specific.get(extension);
    if (specificType != null) {
      return specificType;
    }

    FileType genericType = generic.get(extension);
    if (genericType != null) {
      return genericType;
    }

    if (extension.length() == 0) {
      return new FileType(extension, Language.get("Unknown"), -1);
    }
    else {
      return new FileType(extension, caseExtension + " " + Language.get("File"), -1);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static void loadGenericDescriptions() {
    try {

      XMLNode node = XMLReader.read(new File(Settings.get("TypesFile")));
      node = node.getChild("types");

      int numTypes = node.getChildCount();
      for (int i = 0; i < numTypes; i++) {
        XMLNode child = node.getChild(i);

        String extension = child.getAttribute("extension");
        String type = child.getAttribute("function");
        String description = child.getAttribute("description");

        generic.put(extension, new FileType(extension, description, type));
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }

    new FileType(); // load the FileType static Images
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FileTypeDetector() {
    loadGenericDescriptions();
  }

}