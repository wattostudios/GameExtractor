/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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
import org.watto.Language;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_GZ_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_GZ_2() {

    super("GZ_2", "GZ_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Harry Potter and the Deathly Hallows: Part 1",
        "Harry Potter and the Deathly Hallows: Part 2");
    setExtensions("gz"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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
        rating += 5; // 5 only
      }

      if (fm.getFile().getName().toLowerCase().endsWith(".bin.gz")) {
        rating += 25;
      }
      else {
        rating = 0;
      }

      if (fm.readByte() == 120) {
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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      FileManipulator decompFM = decompressArchive(fm);
      if (decompFM != null) {
        fm.close(); // close the original archive
        fm = decompFM; // now we're going to read from the decompressed file instead
        fm.seek(0); // go to the same point in the decompressed file as in the compressed file

        path = fm.getFile(); // So the resources are stored against the decompressed file
      }

      String baseName = path.getName();
      int baseDotPos = baseName.indexOf('.');
      if (baseDotPos > 0) {
        baseName = baseName.substring(0, baseDotPos);
      }

      long arcSize = fm.getLength();

      // 4 - Filename Directory Length
      int filenameDirLength = fm.readInt();
      FieldValidator.checkLength(filenameDirLength, arcSize);

      // 4 - Number of Names
      int numNames = fm.readInt();
      FieldValidator.checkNumFiles(numNames);

      String[] names = new String[numNames];

      for (int i = 0; i < numNames; i++) {
        // 4 - Name ID
        fm.skip(4);

        // X - Name
        // 1 - null Name Terminator
        String name = fm.readNullString();
        FieldValidator.checkFilename(name);

        name = name.replaceAll("\\|", "/");

        names[i] = name;
      }

      fm.relativeSeek(filenameDirLength + 4);

      // 4 - Number of Properties
      int numProperties = fm.readInt();
      FieldValidator.checkNumFiles(numProperties + 1); // allow zero properties

      // for each property
      //   4 - Property Name ID
      fm.skip(numProperties * 4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory

      int totalUnknownEntryCount = 0;
      String[] filenames = new String[numFiles];
      int[] nameIDs = new int[numFiles];
      int[] referenceIDs = new int[numFiles];
      int[] lengths = new int[numFiles];
      int[] headerLengths = new int[numFiles];
      int[] externalFiles = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {

        // 4 - Name ID
        int nameID = fm.readInt();
        FieldValidator.checkRange(nameID, 0, numNames);
        String filename = names[nameID];

        int dotPos = filename.indexOf(':');
        if (dotPos != 0) {
          String extension = filename.substring(0, dotPos);
          if (extension.equals(baseName)) {
            extension = "data";
          }

          if (extension.equals("FILE")) {
            filename = filename.substring(dotPos + 1); // already has a correct extension
          }
          else {
            filename = filename.substring(dotPos + 1) + "." + extension;
          }
        }

        filenames[i] = filename;
        nameIDs[i] = nameID;

        // 4 - Reference Name ID (eg TEX files point to a DDS image) (-1 = no reference image)
        int referenceNameID = fm.readInt();
        referenceIDs[i] = referenceNameID;

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        lengths[i] = length;

        // 4 - Flags (16/128)
        fm.skip(4);

        // 4 - File Header Length (can be 0)
        int headerLength = fm.readInt();
        headerLengths[i] = headerLength;

        // 4 - External File? (0=file in archive, 1=file is external / not in archive)
        int externalFile = fm.readInt();
        externalFiles[i] = externalFile;

        // 4 - Number of Entries in the Unknown Directory for this File
        int unknownEntryCount = fm.readInt();
        totalUnknownEntryCount += unknownEntryCount;

        // 12 - null
        fm.skip(12);
      }

      for (int i = 0; i < numFiles; i++) {
        int referenceNameID = referenceIDs[i];

        if (referenceNameID == -1) {
          // no reference
        }
        else {
          FieldValidator.checkRange(referenceNameID, 0, numNames);

          String referencedFilename = filenames[i];

          int length = lengths[i];
          if (length != 40 && length != 44 && length != 48) {
            continue;
          }

          // find the referenced file
          for (int j = 0; j < numFiles; j++) {
            if (nameIDs[j] == referenceNameID) {
              // found the referenced file

              String actualFilename = names[j];//filenames[j];

              //System.out.println(referencedFilename + " points to " + actualFilename);

              int dotPos = actualFilename.indexOf(':');
              int isReferenceFile = -1;
              try {
                isReferenceFile = Integer.parseInt(actualFilename.substring(dotPos + 1));
              }
              catch (Throwable t) {
              }

              if (isReferenceFile != -1) {
                // yep, a reference file, so just rename this file completely to the new name, and set the other file as a .reference
                filenames[i] += ".reference";
                filenames[j] = referencedFilename;

                //System.out.println("  > Name name " + names[i] + " and " + names[j]);
              }

              /*
              String filename = names[i];
              int dotPos = filename.indexOf(':');
              String extension = null;
              if (dotPos > 0) {
                extension = "." + filename.substring(0, dotPos);
                filename = filename.substring(dotPos + 1);
              }
              
              actualFilename = names[j];
              dotPos = actualFilename.indexOf(':');
              if (dotPos > 0) {
                actualFilename = actualFilename.substring(dotPos + 1) + "." + actualFilename.substring(0, dotPos);
              }
              
              filename = filename + " (" + actualFilename + ")";
              if (extension != null) {
                filename += extension;
              }
              
              filenames[j] = filename;
              
              filenames[i] += ".reference";
              */
              break;
            }
          }
        }
      }

      // UNKNOWN DIRECTORY
      // for each entry
      //   4 - Unknown
      fm.skip(totalUnknownEntryCount * 4);

      long offset = fm.getOffset();
      for (int i = 0; i < numFiles; i++) {
        String filename = filenames[i];

        if (externalFiles[i] == 1) {
          // an external file, not in this archive
          int length = 0;

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(path, filename, offset, length);
          resource.forceNotAdded(true);
          resources[i] = resource;

          TaskProgressManager.setValue(i);
        }
        else {
          // a file in this archive

          int length = lengths[i];

          int headerLength = headerLengths[i];
          offset += headerLength;

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(path, filename, offset, length);
          resource.forceNotAdded(true);
          resources[i] = resource;

          TaskProgressManager.setValue(i);

          offset += length;
          //offset += calculatePadding(length, padding[i]);
          offset += calculatePadding(length, 16);
          //offset += headerLength; // headerLength doesn't contribute to the padding calculation
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
   Decompressed an archive, where the whole archive is compressed.
   Reads the compressed block information first, then processes the compressed blocks themselves.
   Writes the output to a file with the same name, but with "_ge_decompressed" at the end of it.
   The decompressed file contains the same header as the compressed file, so you can open
   the decompressed file in GE directly, without needing to re-decompress anything.
   If the decompressed file already exists, we use that, we don't re-decompress.
   **********************************************************************************************
   **/
  public FileManipulator decompressArchive(FileManipulator fm) {
    try {
      // Build a new "_ge_decompressed" archive file in the current directory
      File origFile = fm.getFile();

      String pathOnly = FilenameSplitter.getDirectory(origFile);
      String filenameOnly = FilenameSplitter.getFilename(origFile);
      String extensionOnly = FilenameSplitter.getExtension(origFile);

      File decompFile = new File(pathOnly + File.separatorChar + filenameOnly + "_ge_decompressed" + "." + extensionOnly);
      if (decompFile.exists()) {
        // we've already decompressed this file before - open and return it
        return new FileManipulator(decompFile, false);
      }

      FileManipulator decompFM = new FileManipulator(decompFile, true);

      long arcSize = fm.getLength();

      fm.seek(0);

      int compLength = (int) fm.getLength();

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(arcSize); // progress bar
      TaskProgressManager.setIndeterminate(true);

      Exporter_ZLib_CompressedSizeOnly exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();
      exporter.open(fm, compLength, compLength);

      while (exporter.available()) {
        decompFM.writeByte(exporter.read());
      }

      // Force-write out the decompressed file to write it to disk, then change the buffer to read-only.
      decompFM.close();
      decompFM = new FileManipulator(decompFile, false);

      TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive")); // progress bar
      TaskProgressManager.setIndeterminate(false);

      // Return the file pointer to the beginning, and return the decompressed file
      decompFM.seek(0);
      return decompFM;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
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
