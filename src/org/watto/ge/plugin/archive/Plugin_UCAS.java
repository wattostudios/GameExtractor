/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.archive;

import java.io.File;
import java.util.Arrays;

import org.watto.ErrorLogger;
import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.UE4Helper;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockVariableExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_Encryption_AES;
import org.watto.ge.plugin.exporter.Exporter_Oodle;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.ge.plugin.resource.Resource_PAK_38;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.buffer.ExporterByteBuffer;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_UCAS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_UCAS() {

    super("UCAS", "UCAS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Unreal Engine 4",
        "Godfall",
        "Splitgate");
    setExtensions("ucas", "utoc"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      String extension = FilenameSplitter.getExtension(fm.getFile()).toLowerCase();
      if (extension.equals("ucas")) {
        getDirectoryFile(fm.getFile(), "utoc");
        rating += 25;
      }
      else if (extension.equals("utoc")) {
        getDirectoryFile(fm.getFile(), "ucas");
        rating += 25;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
   **********************************************************************************************
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @SuppressWarnings("unused")
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      // RESETTING GLOBAL VARIABLES

      File sourcePath = path;

      String extension = FilenameSplitter.getExtension(path).toLowerCase();
      if (extension.equals("ucas")) {
        sourcePath = getDirectoryFile(path, "utoc");
      }
      else if (extension.equals("utoc")) {
        path = getDirectoryFile(path, "ucas");
      }

      long arcSize = path.length();

      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 16 - Unknown
      // 4 - Unknown (3)
      fm.skip(20);

      // 4 - Directory 1 Offset (144)
      int dir1Offset = fm.readInt();
      FieldValidator.checkOffset(dir1Offset, arcSize);

      // 4 - Number of Entries in Directory 1, 2, and 4
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Number of Entries in Directory 3
      int numParts = fm.readInt();
      FieldValidator.checkNumFiles(numParts / 10);

      // 4 - Unknown (12)
      fm.skip(4);

      // 4 - Number of Compression Formats (0/1)
      int numCompressions = fm.readInt();
      FieldValidator.checkRange(numCompressions, 0, 5);

      // 4 - Unknown (32)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown (1)
      // 8 - Unknown
      // 16 - null
      // 8 - Unknown (15)
      // 8 - Unknown (-1)
      // 48 - null

      int dir2Offset = dir1Offset + numFiles * 12;
      FieldValidator.checkOffset(dir2Offset, arcSize);

      int dir3Offset = dir2Offset + numFiles * 10;
      FieldValidator.checkOffset(dir3Offset, arcSize);

      int compressionDirOffset = dir3Offset + numParts * 12;
      FieldValidator.checkOffset(compressionDirOffset, arcSize);

      fm.seek(compressionDirOffset);

      ExporterPlugin[] exporters = new ExporterPlugin[numCompressions + 1];
      exporters[0] = exporterDefault;

      for (int i = 0; i < numCompressions; i++) {
        // 32 - Compression Format ("Oodle" + nulls to fill)
        String compression = fm.readNullString(32);
        if (compression.equalsIgnoreCase("Oodle")) {
          //exporters[i + 1] = new Exporter_Default();
          //exporters[i + 1].setName("Oodle Compression");

          exporters[i + 1] = new Exporter_Oodle();

          //exporters[i + 1] = new Exporter_QuickBMS_Decompression("oodle");
          //exporters[i + 1] = new Exporter_Encryption_AES(ByteArrayConverter.convertLittle(new Hex("D73A797940208F2FB29256BE81A7CBC7B74CBF899441BB277F357F7F4577DBBB")));
        }
        else if (compression.equalsIgnoreCase("Zlib")) {
          exporters[i + 1] = Exporter_ZLib.getInstance();
        }
        else {
          exporters[i + 1] = exporterDefault;
        }
      }

      // 
      // READ THE FILENAMES
      // 
      FileManipulator originalFM = fm; // so we can go back to this FM later, if we had to do a Filename Dir Decryption

      String[] filenames = new String[numFiles];
      Arrays.fill(filenames, null);

      boolean foundNames = false;

      long filenameDirOffset = fm.getOffset();
      long filenameDirLength = (int) (arcSize - filenameDirOffset);

      // 4 - Root Filename Length (including null terminator) (10)
      int rootFilenameLength = fm.readInt();
      if (rootFilenameLength == 512) {
        // This occurs in the Mail Time game
        fm.skip(1024); // 512*2
        fm.skip(numParts * 20);
        rootFilenameLength = fm.readInt();
      }

      try {
        FieldValidator.checkFilenameLength(rootFilenameLength);
      }
      catch (Throwable t) {
        rootFilenameLength = 512;
      }

      if (rootFilenameLength == 512) {
        // either it is 512, or was set above. Either way, it's probably encrypted, so lets try to decrypt it
        // Try all the keys we know about, see if we can find one that works (don't know that this works yet, tbh)

        byte[][] keys = UE4Helper.getAESKeys();
        int numKeys = keys.length;

        byte[] key = null;

        int testLength = 64;
        for (int k = 0; k < numKeys; k++) {
          try {
            key = keys[k];

            /*
            if (k == numKeys - 1) {
              System.out.println("HERE");
            }
            */

            Exporter_Encryption_AES decryptor = new Exporter_Encryption_AES(key);

            Resource dirResource = new Resource(sourcePath, "", filenameDirOffset, testLength, testLength, decryptor);
            ExporterByteBuffer exporterBuffer = new ExporterByteBuffer(dirResource);

            FileManipulator testFM = new FileManipulator(exporterBuffer);

            // 4 - Relative Directory Name Length (including null terminator) (10)
            int nameLength = testFM.readInt();
            FieldValidator.checkRange(nameLength, 0, 64);

            // 9 - Relative Directory Name (../../../)
            // 1 - null Relative Directory Name Terminator
            testFM.readNullString();

            // 4 - Number of Files
            int innerNumFiles = testFM.readInt();
            FieldValidator.checkNumFiles((innerNumFiles / 4) + 1);

            // found one
            break;
          }
          catch (Throwable t2) {
            key = null;
          }
        }

        if (key == null) {
          // ignore for now (just proceed without filenames)
          //throw new WSPluginException("[PAK_UCAS] No matching AES key found.");
        }

        Exporter_Encryption_AES decryptor = new Exporter_Encryption_AES(key);

        Resource dirResource = new Resource(sourcePath, "", filenameDirOffset, filenameDirLength, filenameDirLength, decryptor);
        ExporterByteBuffer exporterBuffer = new ExporterByteBuffer(dirResource);

        //fm.close();
        fm = new FileManipulator(exporterBuffer);

        // 4 - Root Filename Length (including null terminator) (10)
        rootFilenameLength = fm.readInt();
      }

      try { // TRY-CATCH, SO THAT IT'S OK IF NO FILENAMES ARE FOUND

        FieldValidator.checkFilenameLength(rootFilenameLength);

        // 9 - Root Filename (../../../)
        // 1 - null Root Filename Terminator
        fm.skip(rootFilenameLength);

        // 4 - Number of Folder Names
        int numFolderNames = fm.readInt();
        FieldValidator.checkNumFiles(numFolderNames);

        long folderNamesDirOffset = fm.getOffset();

        fm.skip(numFolderNames * 16);

        // 4 - Number of Filenames
        int numFilenames = fm.readInt();
        FieldValidator.checkNumFiles(numFilenames);

        long filenamesDirOffset = fm.getOffset();

        fm.skip(numFilenames * 12);

        // 4 - Number of Name Entries
        int numNames = fm.readInt();
        FieldValidator.checkNumFiles(numNames);

        String[] names = new String[numNames];
        for (int n = 0; n < numNames; n++) {
          // 4 - Name Length (including null terminator)
          int nameLength = fm.readInt();
          FieldValidator.checkFilenameLength(nameLength);

          // X - Name
          // 1 - null Name Terminator
          String name = fm.readNullString(nameLength);
          FieldValidator.checkFilename(name);
          names[n] = name;
        }

        // go back and work out the folder names
        fm.relativeSeek(folderNamesDirOffset);

        String[] folderNames = new String[numFolderNames];
        //int[] folderNameIDs = new int[numFolderNames];
        //int[] nextNameIDs = new int[numFolderNames];
        for (int n = 0; n < numFolderNames; n++) {
          // 4 - Folder Name ID
          int folderNameID = fm.readInt();
          if (folderNameID != -1) {
            FieldValidator.checkRange(folderNameID, 0, numNames);
          }

          //folderNameIDs[n] = folderNameID;

          // 4 - Next Sub-Folder Name ID
          int unknown1 = fm.readInt();
          //nextNameIDs[n] = unknown1;

          //fm.skip(4);
          //System.out.println(fm.readInt());

          // 4 - Parent Folder ID
          /*
          int parentID = fm.readInt();
          if (parentID != -1) {
            FieldValidator.checkRange(parentID, 0, numFolderNames);
          }
          */
          int unknown2 = fm.readInt();

          // 4 - Unknown
          //System.out.println(fm.readInt());
          //fm.skip(4);
          int unknown3 = fm.readInt();

          //System.out.println(folderNameID + "\t" + unknown1 + "\t" + unknown2 + "\t" + unknown3);

          if (folderNameID == -1) {
            folderNames[n] = "";
          }
          else {
            folderNames[n] = names[folderNameID] + "\\";
          }

          /*
          if (parentID != -1) {
            folderNames[n] = folderNames[parentID] + folderNames[n];
          }
          */

        }

        /*
        // re-iterate through each of the subfolders
        for (int n = 0; n < numFolderNames; n++) {
          String name = folderNames[n];
          int nextNameID = nextNameIDs[n];
          while (nextNameID != -1) {
            name += folderNames[nextNameID];
            nextNameID = nextNameIDs[nextNameID];
          }
          folderNames[n] = name;
          System.out.println(name);
        }
        */

        // go back and work out the filenames
        fm.relativeSeek(filenamesDirOffset);

        for (int n = 0; n < numFilenames; n++) {
          // 4 - Filename ID
          int filenameID = fm.readInt();
          FieldValidator.checkRange(filenameID, 0, numNames);

          // 4 - Folder ID
          /*
          int folderID = fm.readInt();
          if (folderID != -1) {
            FieldValidator.checkRange(folderID, 0, numFolderNames);
          }
          */
          fm.skip(4);
          //System.out.println(fm.readInt());

          // 4 - File ID
          int fileID = fm.readInt();
          FieldValidator.checkRange(fileID, 0, numFiles);

          String filename = names[filenameID];
          /*
          if (folderID != -1) {
            filename = folderNames[folderID] + filename;
          }
          */

          filenames[fileID] = filename;
        }

        foundNames = true;

      }
      catch (Throwable t) {
        ErrorLogger.log("[UCAS] No filenames found (or error parsing filenames)");
        ErrorLogger.log(t);
      }

      // 
      // READ ALL THE PARTS
      // 

      // First, make sure we're reading the original file (not a decrypted filename buffer) 
      if (fm != originalFM && fm.getBuffer() instanceof ExporterByteBuffer) {
        fm.close();
      }
      fm = originalFM;

      fm.seek(dir3Offset);

      TaskProgressManager.setMaximum(numParts);

      //long multipleFactor = IntConverter.unsign((long)Integer.MAX_VALUE);
      long multipleFactor = ((long) 1) << 32;

      long[] partOffsets = new long[numParts];
      long[] partLengths = new long[numParts];
      long[] partDecompLengths = new long[numParts];
      ExporterPlugin[] partExporters = new ExporterPlugin[numParts];

      for (int i = 0; i < numParts; i++) {
        // 4 - File Offset (into the UCAS file)
        long offset = IntConverter.unsign(fm.readInt());

        // 1 - Multiple of 4GB
        int multiple = fm.readByte();
        if (multiple != 0) {
          offset += (multiple * multipleFactor);
        }

        FieldValidator.checkOffset(offset, arcSize);

        // 3 - Compressed File Length
        byte[] lengthBytes = new byte[] { fm.readByte(), fm.readByte(), fm.readByte(), 0 };
        int length = IntConverter.convertLittle(lengthBytes);
        FieldValidator.checkLength(length, arcSize);

        // 2 - Decompressed File Length?
        // 1 - Unknown (0/5/16)
        byte[] decompLengthBytes = new byte[] { fm.readByte(), fm.readByte(), fm.readByte(), 0 };
        int decompLength = IntConverter.convertLittle(decompLengthBytes);
        FieldValidator.checkLength(decompLength);

        if (decompLength == 0) {
          decompLength = length;
        }

        // 1 - Compression Flag (0/1)
        int compressionFlag = fm.readByte();
        FieldValidator.checkRange(compressionFlag, 0, numCompressions);

        ExporterPlugin exporter = exporters[compressionFlag];

        partOffsets[i] = offset;
        partLengths[i] = length;
        partDecompLengths[i] = decompLength;
        partExporters[i] = exporter;

        TaskProgressManager.setValue(i);
      }

      // 
      // READ THE FILES
      // 

      fm.seek(dir2Offset);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      int[] firstPartIDs = new int[numFiles];
      int[] numPartsInFiles = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 3 - ID of First Part of this File (BIG)
        byte[] firstPartIDBytes = new byte[] { 0, fm.readByte(), fm.readByte(), fm.readByte() };
        int firstPartID = IntConverter.convertBig(firstPartIDBytes);
        //FieldValidator.checkRange(firstPartID, 0, numParts);

        // 1 - null
        fm.skip(1);

        // 4 - Number of Parts in this File (BIG) (Sometimes needs +1, but not always)
        //int numPartsInFile = IntConverter.changeFormat(fm.readInt());
        //FieldValidator.checkRange(numPartsInFile, 0, numParts);
        fm.skip(4);

        // 2 - Unknown
        fm.skip(2);

        firstPartIDs[i] = firstPartID;
      }

      // Check if the part IDs are multiples of 16 or not (if so, need to / each of them by 16)
      if (firstPartIDs[numFiles - 1] > numParts) {
        FieldValidator.checkRange(firstPartIDs[numFiles - 1] / 16, 0, numParts);

        // if we're here, we probably need to divide each by 16
        for (int i = 0; i < numFiles; i++) {
          int firstPartID = firstPartIDs[i];
          firstPartID /= 16;
          FieldValidator.checkRange(firstPartID, 0, numParts);
          firstPartIDs[i] = firstPartID;
        }
      }

      for (int i = 0; i < numFiles - 1; i++) {
        numPartsInFiles[i] = firstPartIDs[i + 1] - firstPartIDs[i];
      }
      numPartsInFiles[numFiles - 1] = numParts - firstPartIDs[numFiles - 1];

      for (int i = 0; i < numFiles; i++) {

        int firstPartID = firstPartIDs[i];
        int numPartsInFile = numPartsInFiles[i];

        long[] blockOffsets = new long[numPartsInFile];
        long[] blockLengths = new long[numPartsInFile];
        long[] blockDecompLengths = new long[numPartsInFile];
        ExporterPlugin[] blockExporters = new ExporterPlugin[numPartsInFile];

        System.arraycopy(partOffsets, firstPartID, blockOffsets, 0, numPartsInFile);
        System.arraycopy(partLengths, firstPartID, blockLengths, 0, numPartsInFile);
        System.arraycopy(partDecompLengths, firstPartID, blockDecompLengths, 0, numPartsInFile);
        System.arraycopy(partExporters, firstPartID, blockExporters, 0, numPartsInFile);

        int length = 0;
        int decompLength = 0;
        for (int p = 0; p < numPartsInFile; p++) {
          length += blockLengths[p];
          decompLength += blockDecompLengths[p];
        }

        long offset = 0;
        if (numPartsInFile != 0) {
          offset = blockOffsets[0];
        }

        String filename = filenames[i];
        if (filename == null || filename.length() <= 0) {
          filename = Resource.generateFilename(i);
        }

        ExporterPlugin blockExporter = new BlockVariableExporterWrapper(blockExporters, blockOffsets, blockLengths, blockDecompLengths);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource_PAK_38(path, filename, offset, length, decompLength, blockExporter);
        resource.forceNotAdded(true);
        resources[i] = resource;

        TaskProgressManager.setValue(i);
      }

      if (foundNames) {
        // HERE WE DO...
        // 1. For all uasset files, go through and find all the related files (uexp, ubulk, etc) - files with the same name, different extension.
        //    Each related file is added as a "Related Resource" of the uasset file, and is also removed from the overall Resource[].
        // 2. For all uasset files, read a little bit of the file data, work out what the Class is, and therefore what the file type is. 

        // First, sort the resources
        Arrays.sort(resources);

        Resource_PAK_38[] culledResources = new Resource_PAK_38[numFiles];
        int numCulledFiles = 0;

        // Now find any with the same name as a uasset
        Resource_PAK_38 asset = null;
        String assetName = "";
        for (int i = 0; i < numFiles; i++) {
          Resource_PAK_38 resource = (Resource_PAK_38) resources[i];

          if (resource.getExtension().equals("uasset")) {
            // found an asset
            asset = resource;
            assetName = resource.getName();

            culledResources[numCulledFiles] = resource; // we want to keep this file - it's a main uasset file
            numCulledFiles++;

            int dotPos = assetName.lastIndexOf(".uasset");
            if (dotPos > 0) {
              assetName = assetName.substring(0, dotPos + 1);
            }

            // now read a bit of the uasset file to determine the Class
            String className = readUAssetClass(resource);
            if (className != null) {
              String name = resource.getName() + "." + className;
              resource.setName(name);
              resource.setOriginalName(name);
            }

          }
          else {
            // see if the name matches the uasset
            boolean addedLink = false;

            if (asset != null) {
              String resName = resource.getName();
              int dotPos = resName.lastIndexOf(".");
              if (dotPos > 0) {
                resName = resName.substring(0, dotPos + 1);
              }
              if (resName.equals(assetName)) {
                // found a "related" resource
                asset.addRelatedResource(resource);
                addedLink = true;
              }

            }

            if (!addedLink) { // only want to keep files that haven't been related to the uasset file
              culledResources[numCulledFiles] = resource; // we want to keep this file - it's not a uasset file, and not a related file
              numCulledFiles++;
            }

          }

          TaskProgressManager.setValue(i);

        }

        // If any resources were culled, want to shrink the array...
        if (Settings.getBoolean("UE4CullRelatedResources")) { // So that we can easily turn it off to show the ubulk/uexp files and extract them for analysis
          if (numCulledFiles != numFiles) {
            resources = new Resource_PAK_38[numCulledFiles];
            System.arraycopy(culledResources, 0, resources, 0, numCulledFiles);
          }
        }
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   Reads the properties of a UAsset file, looks for the Class, and returns it
   **********************************************************************************************
   **/
  public String readUAssetClass(Resource resource) {
    return null;

    /*
    // If this file is a uAsset, it really shouldn't use OODLE compression, but just in case we want to disable it for now (as it uses QuickBMS)
    ExporterPlugin originalExporter = resource.getExporter();
    
    try {
      long arcSize = resource.getDecompressedLength();
    
      ExporterByteBuffer byteBuffer = new ExporterByteBuffer(resource);
      FileManipulator fm = new FileManipulator(byteBuffer);
    
      // 64 - Unknown
      fm.skip(64);
      
      String[] names = new String[256];
      int numNames = 0;
      
      // 2 - Name Length
      short nameLength = ShortConverter.changeFormat(fm.readShort());
      while (nameLength != 0) {
      FieldValidator.checkFilenameLength(nameLength);
      
      // X - Name
        String name = fm.readString(nameLength);
        names[numNames] = name;
        numNames++;
      
     // 2 - Name Length
        nameLength = ShortConverter.changeFormat(fm.readShort());
      }
      
      // 4 - Number of Names
      int nameCount = fm.readInt();
      try {
        FieldValidator.checkNumFiles(nameCount);
      }
      catch (Throwable t) {
        nameCount = fm.readInt();
        FieldValidator.checkNumFiles(nameCount);
      }
    
      // 4 - Name Directory Offset
      long nameDirOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(nameDirOffset, arcSize);
    
      // 8 - null
      fm.skip(8);
    
      // 4 - Number Of Exports
      int exportCount = fm.readInt();
      FieldValidator.checkNumFiles(exportCount);
    
      // 4 - Exports Directory Offset
      long exportDirOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(exportDirOffset, arcSize);
    
      // 4 - Number Of Imports
      int importCount = fm.readInt();
      FieldValidator.checkNumFiles(importCount);
    
      // 4 - Import Directory Offset
      long importDirOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(importDirOffset, arcSize);
    
      // 16 - null
      // 4 - [optional] null
      // 16 - GUID Hash
      if (importDirOffset == 0) {
        // that skipped 8 "null" bytes probably wasn't in this archive, so correct the import details
        importCount = exportCount;
        importDirOffset = exportDirOffset;
        fm.skip(32 - 8);
      }
      else {
        fm.skip(32);
      }
    
      // 4 - Unknown (1)
      if (fm.readInt() != 1) { // this is to skip the OPTIONAL 4 bytes in MOST circumstances
        fm.skip(4);
      }
    
      // 4 - Unknown (1/2)
      // 4 - Unknown (Number of Names - again?)
      // 36 - null
      // 4 - Unknown
      // 4 - null
      // 4 - Padding Offset
      // 4 - File Length [+4] (not always - sometimes an unknown length/offset)
      // 8 - null
      fm.skip(68);
    
      // 4 - Number of ???
      int numToSkip = fm.readInt();
      if (numToSkip > 0 && numToSkip < 10) {
        // 4 - Unknown
        fm.skip(numToSkip * 4);
      }
    
      // 4 - Unknown (-1)
      fm.skip(4);
    
      // 4 - Files Data Offset
      long filesDirOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(filesDirOffset, arcSize + 1);
    
      // Read the Names Directory
      fm.relativeSeek(nameDirOffset); // VERY IMPORTANT (because seek() doesn't allow going backwards in ExporterByteBuffer)
      UE4Helper.readNamesDirectory(fm, nameCount);
    
      // Read the Import Directory
      fm.relativeSeek(importDirOffset); // VERY IMPORTANT (because seek() doesn't allow going backwards in ExporterByteBuffer)
      UnrealImportEntry[] imports = UE4Helper.readImportDirectory(fm, importCount);
    
      int numFiles = importCount;
    
      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        UnrealImportEntry entry = imports[i];
    
        if (entry.getType().equals("Class")) {
          fm.close();
    
          // put the original exporter back
          resource.setExporter(originalExporter);
    
          return entry.getName();
        }
    
      }
    
      fm.close();
    }
    catch (Throwable t) {
    }
    
    // put the original exporter back
    resource.setExporter(originalExporter);
    return null;
    */
  }

}
