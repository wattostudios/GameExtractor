/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_RNC2;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_102 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_102() {

    super("DAT_102", "DAT_102");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Turok: Dinosaur Hunter");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("N64");

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
        rating += 25;
      }

      // Number Of Folders
      int numFolders = IntConverter.changeFormat(fm.readInt());
      if (numFolders <= 0) {
        return 0;
      }
      if (FieldValidator.checkNumFiles(numFolders)) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // First Directory Offset
      if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      fm.skip((numFolders - 1) * 4);

      // archive length
      if (IntConverter.changeFormat(fm.readInt()) == arcSize) {
        rating += 5;
      }

      // null
      if (IntConverter.changeFormat(fm.readInt()) == 0) {
        rating += 5;
      }

      return rating;

    } catch (Throwable t) {
      return 0;
    }
  }

  int realNumFiles = 0;

  int[] offsetAdjustments = new int[0];

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

      //ExporterPlugin exporterRNC1 = Exporter_RNC1.getInstance();
      ExporterPlugin exporterRNC1 = Exporter_Default.getInstance();
      ExporterPlugin exporterRNC2 = Exporter_RNC2.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();
      realNumFiles = 0;

      offsetAdjustments = new int[numFiles];

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      readDirectory(fm, resources, path, arcSize, null);

      //calculateFileSizes(resources, arcSize);

      // go through and apply the offset adjustments
      for (int i = 0; i < realNumFiles; i++) {
        int offsetAdjustment = offsetAdjustments[i];
        if (offsetAdjustment != 0) {
          Resource resource = resources[i];
          int length = (int) resource.getLength();
          if (length > offsetAdjustment) {
            resource.setOffset(resource.getOffset() + offsetAdjustment);
            length -= offsetAdjustment;
            resource.setLength(length);
            resource.setDecompressedLength(length);
          }

        }
      }

      // now go through, set the exporters and the lengths/decomp lengths, according to the compression
      fm.getBuffer().setBufferSize(12);

      for (int i = 0; i < realNumFiles; i++) {
        try {
          Resource resource = resources[i];
          long offset = resource.getOffset();

          fm.relativeSeek(offset);

          // 3 - Header (RNC)
          // 1 - Compression Mode (1/2)
          int header = fm.readInt();
          if (header == 37965394 || header == 21188178) {

            // 4 - Decompressed Length
            int decompLength = IntConverter.changeFormat(fm.readInt());
            FieldValidator.checkLength(decompLength);

            // 4 - Compressed Length
            int compLength = IntConverter.changeFormat(fm.readInt());
            FieldValidator.checkLength(compLength, arcSize);

            // 2 - Checksum of Decompressed Data
            // 2 - Checksum of Compressed Data
            // 1 - Unknown
            // 1 - Number of Chunks

            if (header == 37965394) {
              resource.setOffset(offset + 18);
              resource.setDecompressedLength(decompLength);
              resource.setLength(compLength);
              resource.setExporter(exporterRNC2);
            } else if (header == 21188178) {
              // We don't have any RNC1 exporter (or at least, not one that works with this game), so we just leave the file as-is with the RNC header on it
              //resource.setOffset(offset + 18);
              //resource.setDecompressedLength(decompLength);
              //resource.setLength(compLength);
              resource.setExporter(exporterRNC1);
            }

            // X - Compressed Data (RNC Compression)
          }
        } catch (Throwable t) {
          // don't worry about it
          ErrorLogger.log(t);
        }

      }

      // Now filter out all the files of length -28 or 1
      int writePos = 0;
      for (int i = 0; i < realNumFiles; i++) {
        Resource resource = resources[i];
        if (resource.getLength() <= 1) {
          continue;
        } else {
          resources[writePos] = resource;
          writePos++;
        }
      }
      realNumFiles = writePos;

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

    } catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  public void readDirectory(FileManipulator fm, Resource[] resources, File path, long arcSize, String parentDirName) {
    try {

      int relativeOffset = (int) fm.getOffset();

      // 4 - Number Of Files/Folders in this Directory
      int numEntries = fm.readInt();
      if (numEntries == 37965394 || numEntries == 21188178) {
        // a file ("RNC" + (byte)2)
        //System.out.println("File (offset=" + relativeOffset + ")");

        long offset = relativeOffset;

        String filename = parentDirName + Resource.generateFilename(realNumFiles);

        Resource resource = new Resource(path, filename, offset);
        resources[realNumFiles] = resource;

        offsetAdjustments[realNumFiles] = 0;

        realNumFiles++;

        TaskProgressManager.setValue(offset);
      } else {
        numEntries = IntConverter.changeFormat(numEntries);
        // a directory
        //System.out.println("Directory (offset=" + relativeOffset + ")");

        try {
          FieldValidator.checkNumFiles(numEntries);
        } catch (Throwable t) {
          // probably a file (or, at least, not a valid directory)
          //System.out.println("not a directory - changing to a file (offset=" + relativeOffset + ")");
          int offset = relativeOffset;

          String filename = parentDirName + Resource.generateFilename(realNumFiles);

          Resource resource = new Resource(path, filename, offset);
          resources[realNumFiles] = resource;

          offsetAdjustments[realNumFiles] = 0;
          realNumFiles++;

          TaskProgressManager.setValue(offset);
          return;
        }

        int[] offsets = new int[numEntries];
        for (int i = 0; i < numEntries; i++) {

          // 4 - File Offset
          int offset = IntConverter.changeFormat(fm.readInt()) + relativeOffset;

          // double-check that it's not a file
          if (i == 0) {
            // the first offset for files in this directory should be (4 + numEntries*4 + 8) or (4 + numEntries*4 + 4)
            int offsetCalculation = (4 + numEntries * 4 + 4 + relativeOffset);
            if (offset != offsetCalculation && offset != offsetCalculation + 4) {
              // probably a file (or, at least, not a valid directory)

              int offsetAdjustment = 0;

              //System.out.println("not a directory - changing to a file (offset=" + relativeOffset + ")");
              offset = relativeOffset;

              String filename = parentDirName + Resource.generateFilename(realNumFiles);

              // check the next field, which should point to the RNC header
              int headerPointer = IntConverter.changeFormat(fm.readInt());
              if (headerPointer > 12 && headerPointer + relativeOffset < arcSize) {
                fm.skip(headerPointer - 12);
                int header = fm.readInt();
                if (header == 37965394 || header == 21188178) {
                  // a file ("RNC" + (byte)2)
                  offsetAdjustment = headerPointer;
                }
              }

              Resource resource = new Resource(path, filename, offset);
              resources[realNumFiles] = resource;

              offsetAdjustments[realNumFiles] = offsetAdjustment;
              realNumFiles++;

              TaskProgressManager.setValue(offset);
              return;
            }
          }

          FieldValidator.checkOffset(offset, arcSize);
          offsets[i] = offset;
        }

        // 4 - Max Offset for this Sub-Directory
        int maxOffset = IntConverter.changeFormat(fm.readInt()) + relativeOffset;
        FieldValidator.checkOffset(maxOffset, arcSize + 1);

        // work out the lengths
        int[] lengths = new int[numEntries];
        for (int i = 0; i < numEntries - 1; i++) {
          lengths[i] = offsets[i + 1] - offsets[i];
        }
        lengths[numEntries - 1] = maxOffset - offsets[numEntries - 1];

        boolean root = (parentDirName == null && numEntries == 11);
        String[] rootNames = new String[] { "GraphicObjects", "ObjectAttributes", "ObjectTypes", "TextureSets", "ParticleEffects", "SoundEffects", "Levels", "PersistantCounts", "WarpDests", "Binaries", "BinaryTypes" };

        for (int i = 0; i < numEntries; i++) {

          String dirName = "" + i;

          if (root) {
            parentDirName = rootNames[i];
            dirName = "";
          }

          int offset = offsets[i];

          fm.relativeSeek(offset);
          readDirectory(fm, resources, path, arcSize, parentDirName + dirName + "\\");

          // if this entry was a file, set the length
          if (realNumFiles > 0) {
            Resource resource = resources[realNumFiles - 1];
            if (resource.getOffset() == offset) {
              int length = lengths[i];
              resource.setLength(length);
              resource.setDecompressedLength(length);
            }
          }
        }
      }

    } catch (Throwable t) {
      logError(t);
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
