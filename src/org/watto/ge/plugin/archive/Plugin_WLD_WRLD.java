/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import org.watto.Settings;
import org.watto.component.PreviewPanel;
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.viewer.Viewer_WLD_WRLD_TEXP;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_WLD_WRLD extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WLD_WRLD() {

    super("WLD_WRLD", "WLD_WRLD");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("The Sting!");
    setExtensions("wld"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("texp", "Texture Image", FileType.TYPE_IMAGE));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    //setCanScanForFileTypes(true);

    setCanConvertOnReplace(true);

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

      // Header
      if (fm.readString(4).equals("WRLD")) {
        rating += 50;
      }

      if (fm.readInt() == 0) {
        rating += 5;
      }

      if (fm.readString(4).equals("TEXP")) {
        rating += 5;
      }

      if (fm.readInt() == 0) {
        rating += 5;
      }

      if (fm.readString(4).equals("PAGE")) {
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

      FileManipulator fm = new FileManipulator(path, false, 8); //  small quick reads

      long arcSize = fm.getLength();

      // 4 - Header (WRLD)
      // 4 - null
      fm.skip(8);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 4 - Header
        String header = fm.readString(4);

        // 4 - null
        fm.skip(4);

        if (header.equals("TEXP")) {
          // Textures

          int entryNumber = 0;
          while (fm.getOffset() < arcSize) {
            // 4 - Header
            String entryHeader = fm.readString(4);

            // 4 - Entry Length
            int length = IntConverter.changeFormat(fm.readInt());
            FieldValidator.checkLength(length, arcSize);

            if (entryHeader.equals("PAGE")) {
              // a texture
              long offset = fm.getOffset();

              String filename = Resource.generateFilename(entryNumber) + ".texp";

              //path,name,offset,length,decompLength,exporter
              resources[realNumFiles] = new Resource(path, filename, offset, length);
              realNumFiles++;

              TaskProgressManager.setValue(offset);

              entryNumber++;
            }
            else if (entryHeader.equals("END ")) {
              // end of directory
              break;
            }
            else {
              ErrorLogger.log("[WLD_WRLD] Unknown TEXP entry header: " + header + " at " + (fm.getOffset() - 8));
            }

            fm.skip(length);
          }

        }
        else if (header.equals("GROU")) {
          // Groups

          int entryNumber = 0;
          while (fm.getOffset() < arcSize) {
            // 4 - Header
            String entryHeader = fm.readString(4);

            // 4 - Entry Length
            int length = IntConverter.changeFormat(fm.readInt());
            FieldValidator.checkLength(length, arcSize);

            if (entryHeader.equals("ENTR")) {
              // a group entry
              long offset = fm.getOffset();

              String filename = Resource.generateFilename(entryNumber) + ".grou";

              //path,name,offset,length,decompLength,exporter
              resources[realNumFiles] = new Resource(path, filename, offset, length);
              realNumFiles++;

              TaskProgressManager.setValue(offset);

              entryNumber++;
            }
            else if (entryHeader.equals("END ")) {
              // end of directory
              break;
            }
            else {
              ErrorLogger.log("[WLD_WRLD] Unknown GROU entry header: " + header + " at " + (fm.getOffset() - 8));
            }

            fm.skip(length);
          }
        }
        else if (header.equals("LIST")) {
          // Meshes

          int entryNumber = 0;
          while (fm.getOffset() < arcSize) {
            // 4 - Header
            String entryHeader = fm.readString(4);

            // 4 - Entry Length
            int length = IntConverter.changeFormat(fm.readInt());
            FieldValidator.checkLength(length, arcSize);

            if (entryHeader.equals("MODL")) {
              // a model entry
              long offset = fm.getOffset();

              String filename = Resource.generateFilename(entryNumber) + ".modl";

              //path,name,offset,length,decompLength,exporter
              resources[realNumFiles] = new Resource(path, filename, offset, length);
              realNumFiles++;

              TaskProgressManager.setValue(offset);

              entryNumber++;
            }
            else if (entryHeader.equals("END ")) {
              // end of directory
              break;
            }
            else {
              ErrorLogger.log("[WLD_WRLD] Unknown LIST entry header: " + header + " at " + (fm.getOffset() - 8));
            }

            fm.skip(length);
          }
        }
        else if (header.equals("OBGR")) {
          // German Language

          int entryNumber = 0;
          while (fm.getOffset() < arcSize) {
            // 4 - Header
            String entryHeader = fm.readString(4);

            // 4 - Entry Length
            int length = IntConverter.changeFormat(fm.readInt());
            FieldValidator.checkLength(length, arcSize);

            if (entryHeader.equals("ENTR")) {
              // a language entry
              long offset = fm.getOffset();

              String filename = Resource.generateFilename(entryNumber) + ".obgr";

              //path,name,offset,length,decompLength,exporter
              resources[realNumFiles] = new Resource(path, filename, offset, length);
              realNumFiles++;

              TaskProgressManager.setValue(offset);

              entryNumber++;
            }
            else if (entryHeader.equals("END ")) {
              // end of directory
              break;
            }
            else {
              ErrorLogger.log("[WLD_WRLD] Unknown OBGR entry header: " + header + " at " + (fm.getOffset() - 8));
            }

            fm.skip(length);
          }
        }
        else if (header.equals("OBJS")) {
          // Objects

          int entryNumber = 0;
          while (fm.getOffset() < arcSize) {
            // 4 - Header
            String entryHeader = fm.readString(4);

            // 4 - Entry Length
            int length = IntConverter.changeFormat(fm.readInt());
            FieldValidator.checkLength(length, arcSize);

            if (entryHeader.equals("OBJ ")) {
              // an object
              long offset = fm.getOffset();

              String filename = Resource.generateFilename(entryNumber) + ".objs";

              //path,name,offset,length,decompLength,exporter
              resources[realNumFiles] = new Resource(path, filename, offset, length);
              realNumFiles++;

              TaskProgressManager.setValue(offset);

              entryNumber++;
            }
            else if (entryHeader.equals("END ")) {
              // end of directory
              break;
            }
            else {
              ErrorLogger.log("[WLD_WRLD] Unknown OBJS entry header: " + header + " at " + (fm.getOffset() - 8));
            }

            fm.skip(length);
          }
        }
        else if (header.equals("MAKL")) {
          // MAKL

          while (fm.getOffset() < arcSize) {
            // 4 - Header
            String entryHeader = fm.readString(4);

            // 4 - Entry Length
            int length = IntConverter.changeFormat(fm.readInt());
            FieldValidator.checkLength(length, arcSize);

            if (entryHeader.equals("END ")) {
              // end of directory
              break;
            }
            else {
              ErrorLogger.log("[WLD_WRLD] Unknown MAKL entry header: " + header + " at " + (fm.getOffset() - 8));
            }

            fm.skip(length);
          }
        }
        else if (header.equals("TREE")) {
          // Tree

          int entryNumber = 0;
          while (fm.getOffset() < arcSize) {
            // 4 - Header
            String entryHeader = fm.readString(4);

            // 4 - Entry Length
            int length = IntConverter.changeFormat(fm.readInt());
            FieldValidator.checkLength(length, arcSize);

            if (entryHeader.equals("NODE")) {
              // a tree node
              long offset = fm.getOffset();

              String filename = Resource.generateFilename(entryNumber) + ".tree";

              //path,name,offset,length,decompLength,exporter
              resources[realNumFiles] = new Resource(path, filename, offset, length);
              realNumFiles++;

              TaskProgressManager.setValue(offset);

              entryNumber++;
            }
            else if (entryHeader.equals("END ")) {
              // end of directory
              break;
            }
            else {
              ErrorLogger.log("[WLD_WRLD] Unknown TREE entry header: " + header + " at " + (fm.getOffset() - 8));
            }

            fm.skip(length);
          }
        }
        else if (header.equals("EOF ")) {
          // End of Archive
          break;
        }
        else {
          ErrorLogger.log("[WLD_WRLD] Unknown header: " + header + " at " + (fm.getOffset() - 8));
          break;
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

  /**
   **********************************************************************************************
   * Writes an [archive] File with the contents of the Resources. The archive is written using
   * data from the initial archive - it isn't written from scratch.
   **********************************************************************************************
   **/
  @Override
  public void replace(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      long srcLength = src.getLength();

      // Write Header Data

      // 4 - Header (WRLD)
      // 4 - null
      fm.writeBytes(src.readBytes(8));

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      int realFileNum = 0;
      while (src.getOffset() < srcLength) {
        // 4 - Header
        String header = src.readString(4);
        fm.writeString(header);

        // 4 - null
        fm.writeBytes(src.readBytes(4));

        if (header.equals("EOF ")) {
          // nothing - end of source file
          break;
        }
        else {

          while (src.getOffset() < srcLength) {
            // 4 - Entry Header
            String entryHeader = src.readString(4);
            fm.writeString(entryHeader);

            // 4 - Entry Length
            int entryLength = IntConverter.changeFormat(src.readInt());

            if (entryHeader.equals("END ")) {
              // 4 - null
              fm.writeInt(0);
              break;
            }
            else {
              // an entry - replace the file
              Resource resource = resources[realFileNum];
              long length = resource.getDecompressedLength();

              fm.writeInt(IntConverter.changeFormat((int) length));

              // X - Entry Data
              if (resource.isReplaced()) {
                // replaced - skip in Source and add in actual changed file
                write(resource, fm);
                src.skip(entryLength);
              }
              else {
                // not replaced - copy from Source
                fm.writeBytes(src.readBytes(entryLength));
              }

              realFileNum++;
            }

          }

        }

      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
   **********************************************************************************************
   When replacing files, if the file is of a certain type, it will be converted before replace
   @param resourceBeingReplaced the Resource in the archive that is being replaced
   @param fileToReplaceWith the file on your PC that will replace the Resource. This file is the
          one that will be converted into a different format, if applicable.
   @return the converted file, if conversion was applicable/successful, else the original fileToReplaceWith
   **********************************************************************************************
   **/
  @Override
  public File convertOnReplace(Resource resourceBeingReplaced, File fileToReplaceWith) {
    try {

      PreviewPanel imagePreviewPanel = loadFileForConversion(resourceBeingReplaced, fileToReplaceWith, "texp");
      if (imagePreviewPanel == null) {
        // no conversion needed, or wasn't able to be converted
        return fileToReplaceWith;
      }

      // The plugin that will do the conversion
      Viewer_WLD_WRLD_TEXP converterPlugin = new Viewer_WLD_WRLD_TEXP();

      String beingReplacedExtension = resourceBeingReplaced.getExtension();
      File destination = new File(fileToReplaceWith.getAbsolutePath() + "." + beingReplacedExtension);
      if (destination.exists()) {
        destination.delete();
      }

      FileManipulator fmOut = new FileManipulator(destination, true);
      converterPlugin.replace(resourceBeingReplaced, imagePreviewPanel, fmOut);
      fmOut.close();

      return destination;

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return fileToReplaceWith;
    }
  }

}
