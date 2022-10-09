/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.ErrorLogger;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockVariableExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_LZ4;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_VFS0_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_VFS0_2() {

    super("VFS0_2", "VFS0_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Metro: Last Light Redux");
    setExtensions("vfs0", "vfs1"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(new FileType("1024", "1024 Image", FileType.TYPE_IMAGE),
        new FileType("512", "512 Image", FileType.TYPE_IMAGE),
        new FileType("2048", "2048 Image", FileType.TYPE_IMAGE),
        new FileType("vba", "OGG Audio", FileType.TYPE_AUDIO));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    //setCanScanForFileTypes(true);

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

      File dirFile = new File(fm.getFile().getParent() + File.separator + "content.vfx");
      if (dirFile.exists()) {
        rating += 25;
      }
      else {
        return 0;
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
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();
      ExporterPlugin exporterLZ4 = Exporter_LZ4.getInstance();

      // RESETTING GLOBAL VARIABLES

      long arcSize = (int) path.length();

      String parentPath = path.getParent() + File.separator;

      File sourcePath = new File(parentPath + "content.vfx");
      if (!sourcePath.exists()) {
        return null;
      }

      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Unknown (1)
      // 4 - Unknown (1)
      // 16 - CRC?
      fm.skip(24);

      // 4 - Number of Archives
      int numArchives = fm.readInt();
      FieldValidator.checkRange(numArchives, 1, 200);

      // 4 - Number of Directory 1 Entries?
      int numFiles1 = fm.readInt();
      FieldValidator.checkNumFiles(numFiles1 / 3);

      // 4 - Number of Directory 2 Entries
      int numFiles2 = fm.readInt();
      FieldValidator.checkNumFiles(numFiles2);

      int numFiles = numFiles1 + numFiles2;

      File[] arcFiles = new File[numArchives];
      long[] arcLengths = new long[numArchives];

      for (int a = 0; a < numArchives; a++) {
        // X - Archive Filename
        // 1 - null Archive Filename Terminator
        String arcName = fm.readNullString();
        File arcFile = new File(parentPath + arcName);
        if (!arcFile.exists()) {
          ErrorLogger.log("[VFS0_2] Missing archive file: " + arcFile);
          return null;
        }
        arcFiles[a] = arcFile;

        // 4 - Number of Folder Names in this Archive
        int numNames = fm.readInt();
        FieldValidator.checkNumFiles(numNames);

        // for each folder in this archive
        for (int n = 0; n < numNames; n++) {
          // X - Folder Name
          // 1 - null Folder Name Terminator
          fm.readNullString();
        }

        // 4 - Archive File Length
        long arcLength = IntConverter.unsign(fm.readInt());
        arcLengths[a] = arcLength;
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      //
      // DIRECTORY 1
      //
      long startOffset = fm.getOffset();
      String[] names = new String[numFiles1];
      long[] entryOffsets = new long[numFiles1];
      boolean[] isFolder = new boolean[numFiles1];
      for (int i = 0; i < numFiles1; i++) {
        entryOffsets[i] = fm.getOffset();

        // 2 - Entry Type Indicator? (8=folder, 0=file)
        short entryType = fm.readShort();

        if (entryType == 0) {
          // File
          isFolder[i] = false;

          // 2 - Archive ID Number for the Archive File that contains this File Data
          short arcID = fm.readShort();
          FieldValidator.checkRange(arcID, 0, numArchives);

          arcSize = arcLengths[arcID];
          File arcFile = arcFiles[arcID];

          // 4 - File Offset
          long offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - Decompressed File Length
          long decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 4 - Compressed File Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 1 - Filename Length (including null terminator)
          int filenameLength = ByteConverter.unsign(fm.readByte()) - 1; // -1 for the null terminator

          // 1 - Filename XOR Value
          int filenameKey = ByteConverter.unsign(fm.readByte());

          // X - Filename (XOR with the value above)
          // 1 - null Terminator
          String filename = "";
          if (filenameLength == -1 || filenameLength == 0) {
            //names[i] = "";
          }
          else {
            byte[] filenameBytes = fm.readBytes(filenameLength);
            for (int b = 0; b < filenameLength; b++) {
              filenameBytes[b] ^= filenameKey;
            }
            filename = StringConverter.convertLittle(filenameBytes);
          }
          fm.skip(1); // null terminator

          names[i] = filename;

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(arcFile, filename, offset, length, decompLength);
        }
        else if (entryType == 8) {
          // Folder
          isFolder[i] = true;

          // 2 - Number of Entries in this Folder
          // 4 - Entry ID of the First Entry in this Folder
          fm.skip(6);

          // 1 - Folder Name Length (including null terminator)
          int filenameLength = ByteConverter.unsign(fm.readByte()) - 1; // -1 for the null terminator

          // 1 - Folder Name XOR Value
          int filenameKey = ByteConverter.unsign(fm.readByte());

          // X - Folder Name (XOR with the value above)
          // 1 - null Terminator
          String filename = "";
          if (filenameLength == -1 || filenameLength == 0) {
            //names[i] = "";
          }
          else {
            byte[] filenameBytes = fm.readBytes(filenameLength);
            for (int b = 0; b < filenameLength; b++) {
              filenameBytes[b] ^= filenameKey;
            }
            filename = StringConverter.convertLittle(filenameBytes);
          }
          fm.skip(1); // null terminator

          names[i] = filename;

        }
        else {
          ErrorLogger.log("[VFS0_2] Unknown entry type 1: " + entryType);
        }

        TaskProgressManager.setValue(i);
      }

      long endOffset = fm.getOffset();

      // now put the names into the tree
      fm.seek(startOffset);

      // 2 - Entry Type Indicator for the Root
      fm.skip(2);

      // 2 - Number of Entries in this Folder
      int numEntries = ShortConverter.unsign(fm.readShort());

      // 4 - Entry ID of the First Entry in this Folder
      int firstEntry = fm.readInt();
      FieldValidator.checkRange(firstEntry, 0, numFiles1);

      fm.relativeSeek(entryOffsets[firstEntry]);
      processNames(fm, names, entryOffsets, "", firstEntry, numEntries);

      // now go back and set all the names properly on the resources
      Resource[] trimmedResources = new Resource[numFiles];
      int realNumFiles = 0;
      for (int i = 0; i < numFiles1; i++) {
        if (isFolder[i]) {
          // Folder
          continue;
        }
        else {
          // File
          String name = names[i];
          Resource resource = resources[i];
          resource.setName(name);
          resource.setOriginalName(name);
          resource.forceNotAdded(true);
          trimmedResources[realNumFiles] = resource;
          realNumFiles++;
        }
      }

      resources = trimmedResources;

      //
      // DIRECTORY 2
      //
      fm.seek(endOffset);

      // Loop through directory
      for (int i = 0; i < numFiles2; i++) {
        // 2 - null
        fm.skip(2);

        // 2 - Archive ID Number for the Archive File that contains this File Data
        short arcID = fm.readShort();
        FieldValidator.checkRange(arcID, 0, numArchives);

        arcSize = arcLengths[arcID];
        File arcFile = arcFiles[arcID];

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed File Length
        long decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown
        fm.skip(4);

        String filename = Resource.generateFilename(realNumFiles);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(arcFile, filename, offset, length, decompLength);
        resource.forceNotAdded(true);
        resources[realNumFiles] = resource;
        realNumFiles++;

        TaskProgressManager.setValue(i);
      }

      resources = resizeResources(resources, realNumFiles);

      numFiles = realNumFiles;

      //
      // Now go in to each file and work out the decompressed blocks
      //
      File currentFile = null;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        long compLength = resource.getLength();
        long decompLength = resource.getDecompressedLength();

        if (compLength == decompLength) {
          // not compressed
          continue;
        }

        File arcFile = resource.getSource();

        if (arcFile.equals(currentFile)) {
          // FM already opened
        }
        else {
          fm.close();
          fm = new FileManipulator(arcFile, false, 8); //small quick reads 
          currentFile = arcFile;
        }

        fm.seek(resource.getOffset());

        int numBlocks = (int) (decompLength / 196608);
        if (decompLength % 196608 != 0) {
          numBlocks++;
        }

        long[] blockOffsets = new long[numBlocks];
        long[] blockLengths = new long[numBlocks];
        long[] blockDecompLengths = new long[numBlocks];
        ExporterPlugin[] blockExporters = new ExporterPlugin[numBlocks];

        for (int b = 0; b < numBlocks; b++) {
          // 4 - Compressed Block Length (including this 8-byte header)
          int blockLength = fm.readInt();
          FieldValidator.checkLength(blockLength, compLength);

          // 4 - Decompressed Block Length
          int blockDecompLength = fm.readInt();
          FieldValidator.checkLength(blockDecompLength, decompLength);

          // X - Compressed Block
          if (blockLength == blockDecompLength) {
            //System.out.println("Same");
            blockLength -= 8; // skip the 8-byte compression header
            blockDecompLength -= 8; // skip the 8-byte compression header

            blockOffsets[b] = fm.getOffset();
            blockLengths[b] = blockLength;
            blockDecompLengths[b] = blockDecompLength;
            blockExporters[b] = exporterDefault;
          }
          else {
            blockLength -= 8; // skip the 8-byte compression header

            blockOffsets[b] = fm.getOffset();
            blockLengths[b] = blockLength;
            blockDecompLengths[b] = blockDecompLength;
            blockExporters[b] = exporterLZ4;
            //blockExporters[b] = exporterDefault;
          }

          fm.skip(blockLength);
        }

        BlockVariableExporterWrapper blockExporter = new BlockVariableExporterWrapper(blockExporters, blockOffsets, blockLengths, blockDecompLengths);
        resource.setExporter(blockExporter);

        TaskProgressManager.setValue(i);
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
  
  **********************************************************************************************
  **/
  public void processNames(FileManipulator fm, String[] names, long[] offsets, String dirName, int firstEntry, int numEntries) {
    for (int i = 0; i < numEntries; i++) {

      //System.out.println(i + " of " + numEntries + " in " + dirName);

      int entryNumber = firstEntry + i;

      // 2 - Entry Type Indicator? (8=folder, 0=file)
      short entryType = fm.readShort();

      if (entryType == 0) {
        // File

        // 2 - Archive ID Number for the Archive File that contains this File Data
        // 4 - File Offset
        // 4 - Decompressed File Length
        // 4 - Compressed File Length
        fm.skip(14);

        // 1 - Filename Length (including null terminator)
        int filenameLength = ByteConverter.unsign(fm.readByte());

        // 1 - Filename XOR Value
        fm.skip(1);

        // X - Filename (XOR with the value above)
        // 1 - null Terminator
        fm.skip(filenameLength);

        names[entryNumber] = dirName + names[entryNumber];
      }
      else if (entryType == 8) {
        // Folder

        // 2 - Number of Entries in this Folder
        int numSubEntries = ShortConverter.unsign(fm.readShort());

        // 4 - Entry ID of the First Entry in this Folder
        int firstSubEntry = fm.readInt();
        try {
          FieldValidator.checkRange(firstSubEntry, 0, offsets.length);
        }
        catch (Throwable t) {
          ErrorLogger.log(t);
          return;
        }

        // 1 - Folder Name Length (including null terminator)
        int filenameLength = ByteConverter.unsign(fm.readByte());

        // 1 - Folder Name XOR Value
        fm.skip(1);

        // X - Folder Name (XOR with the value above)
        // 1 - null Terminator

        fm.skip(filenameLength);

        String folderName = dirName + names[entryNumber] + File.separatorChar;
        names[entryNumber] = folderName;

        // seek to this folder, process it, then come back again
        long thisOffset = fm.getOffset();
        fm.relativeSeek(offsets[firstSubEntry]);

        processNames(fm, names, offsets, folderName, firstSubEntry, numSubEntries);

        fm.relativeSeek(thisOffset);
      }
      else {
        ErrorLogger.log("[VFS0_2] Unknown entry type 2: " + entryType);
      }

      TaskProgressManager.setValue(i);
    }
  }

  /**
  **********************************************************************************************
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
