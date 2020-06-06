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

package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Settings;
import org.watto.component.WSPluginManager;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.resource.Resource_VPK;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_VPK_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_VPK_2() {

    super("VPK_2", "Valve Pack (Version 2)");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Counter-Strike: Global Offensive",
        "Counter-Strike: Source",
        "Day of Defeat: Source",
        "Day Of Infamy",
        "Half-Life: Source",
        "Half-Life 2",
        "Half-Life 2: Deathmatch",
        "Half-Life: Episode 1",
        "Half-Life: Episode 2",
        "Half-Life: Lost Coast",
        "Insurgency",
        "Portal",
        "The Ship",
        "Team Fortress 2",
        "Transmissions Element 120");
    setExtensions("vpk"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(
        new FileType("pcf", "Particle Data", FileType.TYPE_DOCUMENT),
        new FileType("res", "Resource List", FileType.TYPE_DOCUMENT),
        new FileType("vmt", "Material Type", FileType.TYPE_DOCUMENT),
        new FileType("vtf", "Texture Image", FileType.TYPE_IMAGE),
        new FileType("vtx", "Vertex", FileType.TYPE_OTHER),
        new FileType("vvd", "Vertex Data", FileType.TYPE_OTHER),
        new FileType("phy", "Physics Data", FileType.TYPE_OTHER),
        new FileType("mdl", "Model", FileType.TYPE_OTHER),
        new FileType("ani", "Animation", FileType.TYPE_OTHER),
        new FileType("gam", "Game Info", FileType.TYPE_DOCUMENT),
        new FileType("lst", "List", FileType.TYPE_DOCUMENT),
        new FileType("vcd", "Choreography Data", FileType.TYPE_DOCUMENT),
        new FileType("image", "Image", FileType.TYPE_IMAGE));

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

      // Check that either this is the Directory file, or that there is a corresponding directory file
      String inputFile = fm.getFilePath();
      String filename = FilenameSplitter.getFilenameAndExtension(inputFile);
      String path = FilenameSplitter.getDirectory(inputFile);

      if (filename.endsWith("_dir.vpk")) {
        // this is the directory file - check to see if there is at least 1 archive file
        filename = filename.substring(0, filename.indexOf("_dir.vpk"));
        File archiveFile = new File(path + File.separator + filename + "_000.vpk");
        if (archiveFile.exists()) {
          rating += 50;
        }
        else {
          return 0; // there is no corresponding archive file
        }
      }
      else {
        // this is one of the archive files - check to see whether there is a directory file
        int filenameLength = filename.length();
        if (filenameLength < 8) {
          return 0; // filename needs to be > 8 characters long to accommodate the naming standard
        }
        filename = filename.substring(0, filenameLength - 8);
        File directoryFile = new File(path + File.separator + filename + "_dir.vpk");
        if (directoryFile.exists()) {
          rating += 50;
          return rating; // no point checking any further, as the remaining fields are about the directory file
        }
        else {
          return 0; // there is no corresponding directory file
        }
      }

      // 4 - Header ((bytes)85, 170, 18, 52)
      int headerByte1 = ByteConverter.unsign(fm.readByte());
      int headerByte2 = ByteConverter.unsign(fm.readByte());
      int headerByte3 = ByteConverter.unsign(fm.readByte());
      int headerByte4 = ByteConverter.unsign(fm.readByte());
      if (headerByte1 == 85 && headerByte2 == 170 && headerByte3 == 18 && headerByte4 == 52) {
        rating += 5;
      }

      // 4 - Version (2)
      if (fm.readInt() == 2) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // 4 - Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
   **********************************************************************************************
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("res") || extension.equalsIgnoreCase("vmt") || extension.equalsIgnoreCase("vcd") || extension.equalsIgnoreCase("gam") || extension.equalsIgnoreCase("lst") || extension.equalsIgnoreCase("db") || extension.equalsIgnoreCase("dfx") || extension.equalsIgnoreCase("ain") || extension.equalsIgnoreCase("rad") || extension.equalsIgnoreCase("rc") || extension.equalsIgnoreCase("scr") || extension.equalsIgnoreCase("vbsp") || extension.equalsIgnoreCase("vdf") || extension.equalsIgnoreCase("vmf") || extension.equalsIgnoreCase("vmx") || extension.equalsIgnoreCase("dxf")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
  }

  /**
   **********************************************************************************************
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // First up, if they clicked on one of the archive files, point to the DIRECTORY file instead
      String inputFile = path.getAbsolutePath();
      String filename = FilenameSplitter.getFilenameAndExtension(inputFile);
      String filePath = FilenameSplitter.getDirectory(inputFile);

      if (filename.endsWith("_dir.vpk")) {
        // this is the directory file
      }
      else {
        // this is one of the archive files - check to see whether there is a directory file
        int filenameLength = filename.length();
        if (filenameLength < 8) {
          return null; // filename needs to be > 8 characters long to accommodate the naming standard
        }
        filename = filename.substring(0, filenameLength - 8);
        File directoryFile = new File(filePath + File.separator + filename + "_dir.vpk");
        path = directoryFile;
      }

      // Now we know we're pointing to the DIRECTORY file

      // Find and get the size of each archive file
      inputFile = path.getAbsolutePath();
      filename = FilenameSplitter.getFilenameAndExtension(inputFile);
      filePath = FilenameSplitter.getDirectory(inputFile);

      // strip off the suffix
      filename = filename.substring(0, filename.indexOf("_dir.vpk"));

      File[] archiveFiles = new File[250];
      long[] archiveFileLengths = new long[250];
      int numArchiveFiles = 0;
      for (int i = 0; i < 250; i++) {
        File archiveFile;
        if (i < 10) {
          archiveFile = new File(filePath + File.separator + filename + "_00" + i + ".vpk");
        }
        else if (i < 100) {
          archiveFile = new File(filePath + File.separator + filename + "_0" + i + ".vpk");
        }
        else {
          archiveFile = new File(filePath + File.separator + filename + "_" + i + ".vpk");
        }

        if (archiveFile.exists()) {
          archiveFiles[i] = archiveFile;
          archiveFileLengths[i] = archiveFile.length();
        }
        else {
          // this file doesn't exist, so we have already found all of them, exit the loop
          numArchiveFiles = i;
          break;
        }
      }

      // Now shrink the arrays to the right size, and work out the offsets for each archive file
      File[] oldArchiveFiles = archiveFiles;
      archiveFiles = new File[numArchiveFiles];
      System.arraycopy(oldArchiveFiles, 0, archiveFiles, 0, numArchiveFiles);

      long[] archiveOffsets = new long[numArchiveFiles];
      long currentOffset = 0;
      for (int i = 0; i < numArchiveFiles; i++) {
        archiveOffsets[i] = currentOffset;
        currentOffset += archiveFileLengths[i];
      }
      long totalArcSizes = currentOffset;

      //
      // Now we have all the archive files, the offsets at which they start, and we're reading the directory file
      //

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header ((bytes)85, 170, 18, 52)
      // 4 - Version (2)
      fm.skip(8);

      // 4 - Directory Length
      long fileDataOffset = fm.readInt() + 12;
      FieldValidator.checkOffset(fileDataOffset, arcSize + 1);

      // 4 - Internal File Data Length (in this file only)
      // 4 - External Checksum Directory Length
      // 4 - Internal Checksum Directory Length
      // 4 - Signature Directory Length
      fm.skip(16);

      // Guess the number of files
      int numFiles = Settings.getInt("MaxNumberOfFiles4");
      int realNumFiles = 0;
      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      while (fm.getOffset() < fileDataOffset) {
        // X - File Type Name (res)
        // 1 - null File Type Name Terminator
        String fileType = fm.readNullString();

        // for each directory in this File Type
        String directoryName = "";
        while (fm.getOffset() < fileDataOffset) {
          // X - Directory Name
          // 1 - null Directory Name Terminator
          directoryName = fm.readNullString();
          if (directoryName.length() <= 0) {
            // end of this file type
            break;
          }
          else {

            if (directoryName.equals(" ")) {
              directoryName = "";
            }

            // now read the files in the directory
            while (fm.getOffset() < fileDataOffset) {
              // X - Filename
              // 1 - null Filename Terminator
              filename = fm.readNullString();
              if (filename.length() <= 0) {
                // end of the current directory
                break;
              }

              //System.out.println(fm.getOffset() + "\t" + filename);

              // 4 - CRC
              fm.skip(4);

              // 2 - Number of Preload Bytes (data in this file)
              int preloadDataSize = ShortConverter.unsign(fm.readShort());

              // 2 - Number of Archive Package File
              int archiveFileNumber = fm.readShort();

              File archiveFile = null;
              if (archiveFileNumber == 32767) { // special code meaning "in this directory file"
                archiveFile = path;
              }
              else {
                FieldValidator.checkRange(archiveFileNumber, 0, numArchiveFiles);
                archiveFile = archiveFiles[archiveFileNumber];
              }

              // 4 - File Offset
              int offset = fm.readInt();
              if (archiveFileNumber == 32767) { // special code meaning "in this directory file"
                offset += fileDataOffset;
              }
              FieldValidator.checkOffset(offset, totalArcSizes);

              // 4 - File Length
              int length = fm.readInt();
              FieldValidator.checkLength(length, totalArcSizes);

              // 2 - Entry Terminator (-1)
              fm.skip(2);

              String fullFilename = filename;
              if (fileType != null && fileType.length() > 0 && !fileType.equals(" ")) {
                fullFilename += "." + fileType;
              }
              if (directoryName != null && directoryName.length() > 0) {
                fullFilename = directoryName + "/" + fullFilename;
              }

              if (preloadDataSize > 0) {
                // X - Preload Data
                long preloadDataOffset = fm.getOffset();
                fm.skip(preloadDataSize);

                Resource_VPK resource = new Resource_VPK(archiveFile, fullFilename, offset, length);
                resource.setPreloadData(path, preloadDataOffset, preloadDataSize);

                resources[realNumFiles] = resource;
              }
              else {
                resources[realNumFiles] = new Resource(archiveFile, fullFilename, offset, length);
              }

              // Now that we have the offset, need to work out which archive file the data is stored in
              /*
              File archiveFile = archiveFiles[0];
              long offsetStart = archiveOffsets[0];
              for (int f = 1; f < numArchiveFiles; f++) { // we've forced file "0" above, so the loop only needs to start at "1"
                if (offset >= archiveOffsets[f]) {
                  archiveFile = archiveFiles[f];
                  offsetStart = archiveOffsets[f];
                }
                else {
                  // found the archive file in the previous iteration of the loop
                  break;
                }
              }
              
              // Now we have to subtract the offsetStart (where this archive file starts) from the offset of the file,
              // so that the offset becomes relative to the start of this particular archive file
              offset -= offsetStart;
              */

              //path,name,offset,length,decompLength,exporter

              resources[realNumFiles].forceNotAdded(true);

              TaskProgressManager.setValue(realNumFiles);
              realNumFiles++;
            }
          }
        }
      }

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
