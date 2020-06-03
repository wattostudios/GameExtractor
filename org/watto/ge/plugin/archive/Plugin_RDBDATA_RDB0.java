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
import org.watto.component.WSPluginManager;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RDBDATA_RDB0 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RDBDATA_RDB0() {

    super("RDBDATA_RDB0", "RDBDATA_RDB0");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Age of Conan");
    setExtensions("rdbdata"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/

  public String getFileTypeString(int fileType) {
    if (fileType == 1000001) {
      return "fileDetails";
    }
    else if (fileType == 1000013) {
      return "MB";
    }
    else if (fileType == 1000015) {
      return "KDv2";
    }
    else if (fileType == 1000016) {
      return "KDv3";
    }
    else if (fileType == 1000020) {
      return "RDBH";
    }
    else if (fileType == 1000028) {
      return "KDv2";
    }
    else if (fileType == 1000050) {
      return "territories";
    }
    else if (fileType == 1000053) {
      return "boundedAreas";
    }
    else if (fileType == 1000084) {
      return "scry";
    }
    else if (fileType == 1010004 || fileType == 1066606 || fileType == 3107156) {
      return "FCTX";
    }
    else if (fileType == 1010006) {
      return "dds";
    }
    else if (fileType == 1010008 || fileType == 1066603) {
      return "png";
    }
    else if (fileType == 1010030) {
      return "meshIndex";
    }
    else if (fileType == 1010214) {
      return "effectsSet";
    }
    else if (fileType == 1010505 || fileType == 1010512) {
      return "LMXB";
    }
    else if (fileType == 1020003) {
      return "LIP";
    }
    else if (fileType == 1030002) {
      return "TDC2";
    }
    else if (fileType == 1070003) {
      return "playFieldDescription";
    }

    return "" + fileType;
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

      // Header (RDB0)
      if (fm.readString(4).equals("RDB0")) {
        rating += 50;
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
    if (extension.equalsIgnoreCase("meshIndex") || extension.equalsIgnoreCase("boundedAreas") || extension.equalsIgnoreCase("scry") || extension.equalsIgnoreCase("effectsSet") || extension.equalsIgnoreCase("playFieldDescription")) {
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

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (RDB0)
      fm.skip(4);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 4 - File Type
        int fileType = fm.readInt();
        if (fileType == 0) {
          break; // end of the archive
        }

        String fileTypeString = getFileTypeString(fileType);

        // 4 - Unknown
        fm.skip(4);

        // 4 - Entry Length (without padding)
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Entry Length (with padding) (from the end of this field)
        int paddedLength = fm.readInt();
        if (paddedLength > arcSize) {
          paddedLength = length; // the last file of the archive has a huge padding value!
        }
        FieldValidator.checkLength(paddedLength, arcSize); // still checks for negative values, so still worth having here

        String filename = Resource.generateFilename(realNumFiles) + "." + fileTypeString;

        // X - File Data
        long offset = fm.getOffset();

        if (length <= 4) {
          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          fm.skip(paddedLength);
        }
        else {
          // Check if the file is compressed or not

          // 4 - Compression Header
          int compressionHeader = fm.readInt();
          if (compressionHeader == 830099273) {
            // compressed

            // 4 - Decompressed Length
            int decompLength = fm.readInt();
            FieldValidator.checkLength(decompLength);

            offset += 8;

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
            fm.skip(paddedLength - 8);
          }
          else {
            // uncompressed

            // skip the 12-byte header on some files...
            if (fileType == 1000050 || fileType == 1000053 || fileType == 1000084 || fileType == 1010006 || fileType == 1010008 || fileType == 1010030 || fileType == 1010214 || fileType == 1030002 || fileType == 1066603 || fileType == 1070003) {
              fm.skip(12);
              offset += 12;
              length -= 12;
              paddedLength -= 12;
            }

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);

            fm.skip(paddedLength - 4);
          }

        }

        TaskProgressManager.setValue(offset);
        realNumFiles++;
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
