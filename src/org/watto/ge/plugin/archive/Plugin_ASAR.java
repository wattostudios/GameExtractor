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

import org.watto.ErrorLogger;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;
import org.watto.xml.JSONNode;
import org.watto.xml.JSONReader;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ASAR extends ArchivePlugin {

  int realNumFiles = 0;

  long fileDataOffset = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ASAR() {

    super("ASAR", "ASAR");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Jawns",
        "Subserial Network");
    setExtensions("asar"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("coffee", "conf", "css", "editorconfig", "eslintignore", "eslintrc", "firebaserc", "floo", "flooignore", "gitattributes", "gitignore", "gitkeep", "hbs", "jshintrc", "markdown", "md", "npmignore", "opts", "patch", "pem", "ts", "yml", "babelrc", "bnf", "map"); // LOWER CASE

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

      // Version? (4)
      if (fm.readInt() == 4) {
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

      long arcSize = fm.getLength();

      // 4 - Version? (4)
      fm.skip(4);

      // 4 - Directory Length [-8]
      fileDataOffset = fm.readInt() + 8;
      FieldValidator.checkOffset(fileDataOffset, arcSize);

      // 4 - Directory Length [-4]
      fm.skip(4);

      // 4 - Directory Length
      int dirLength = fm.readInt() + 10;
      FieldValidator.checkLength(dirLength, arcSize);

      // X - JSON Directory
      String jsonData = fm.readString(dirLength);
      JSONNode jsonNode = JSONReader.read(jsonData);

      //jsonNode.outputTree();

      int numFiles = Archive.getMaxFiles();
      realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      readDirectory(path, jsonNode, "", resources);

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
   
   **********************************************************************************************
   **/

  public void readDirectory(File path, JSONNode directoryNode, String parentsName, Resource[] resources) {
    try {

      int childCount = directoryNode.getChildCount();

      // Loop through directory
      for (int i = 0; i < childCount; i++) {

        JSONNode node = directoryNode.getChild(i);
        if (node.getTag().equals("files")) {
          // a folder
          String dirName = parentsName + directoryNode.getTag() + "\\";
          readDirectory(path, node, dirName, resources);
          continue;
        }
        // a file or sub-folder - not sure yet

        if (node.getChildCount() == 1 && node.getChild(0).getTag().equals("files")) {
          // a sub-folder
          String dirName = parentsName;
          readDirectory(path, node, dirName, resources);
          continue;
        }
        else {
          int childCountLocal = node.getChildCount();
          if (childCountLocal == 2 || childCountLocal == 3) {
            // a file, provided the children are Size and Offset
            long length = 0;
            long offset = 0;

            for (int c = 0; c < childCountLocal; c++) {
              JSONNode childNode = node.getChild(c);
              String tag = childNode.getTag();
              if (tag.equals("size")) {
                length = childNode.getContentLong();
              }
              else if (tag.equals("offset")) {
                offset = childNode.getContentLong();
              }
              else if (tag.equals("integrity")) {
                // skip
              }
              else if (tag.equals("unpacked")) {
                // skip - only found in 4 files so far, with content = "true"
              }
              else {
                ErrorLogger.log("[ASAR]: Unknown JSON tag: " + tag);
              }
            }

            if (length == 0 && offset == 0) {
              // not sure why - probably an error from above
              continue;
            }
            else {
              offset += fileDataOffset;
            }

            String filename = parentsName + node.getTag();

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);
            realNumFiles++;

            TaskProgressManager.setValue(offset);
          }
        }

      }

    }
    catch (Throwable t) {
      logError(t);

    }
  }

}
