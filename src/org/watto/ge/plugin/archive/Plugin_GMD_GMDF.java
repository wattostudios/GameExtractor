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

import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_GMD_GMDF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_GMD_GMDF() {

    super("GMD_GMDF", "GMD_GMDF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Michigan: Report from Hell");
    setExtensions("gmf", "gmd"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("tme", "Texture Image", FileType.TYPE_IMAGE));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

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
      if (fm.readString(4).equals("GMDF")) {
        rating += 50;
      }

      if (fm.readInt() == 8) {
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

      // 4 - Header (GMDF)
      // 4 - Unknown (8)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - null
      fm.skip(20);

      // 4 - Vertex Data Offset
      int vertexOffset = fm.readInt();
      FieldValidator.checkOffset(vertexOffset, arcSize);

      // 4 - Optional Block Offset (can be null)
      int unknownOffset = fm.readInt();
      FieldValidator.checkOffset(unknownOffset, arcSize);

      // 4 - Image Data Offset
      int imageOffset = fm.readInt();
      FieldValidator.checkOffset(imageOffset, arcSize);

      // 32 - null Padding to a multiple of 64 bytes
      fm.relativeSeek(64);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      int realNumFiles = 0;
      int earliestOffset = (int) arcSize;

      // Add the Parts
      while (fm.getOffset() < earliestOffset) {

        // 4 - Part Offset
        int offset = fm.readInt();
        if (offset == 0) {
          break;
        }
        FieldValidator.checkOffset(offset, arcSize);

        String filename = Resource.generateFilename(realNumFiles) + ".part";

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset);
        realNumFiles++;

        TaskProgressManager.setValue(offset);

        if (offset < earliestOffset) {
          earliestOffset = offset;
        }
      }

      for (int i = 0; i < realNumFiles - 1; i++) {
        Resource resource = resources[i];
        int length = (int) (resources[i + 1].getOffset() - resource.getOffset());
        resource.setLength(length);
        resource.setDecompressedLength(length);
      }
      Resource lastResource = resources[realNumFiles - 1];
      int lastLength = (int) (vertexOffset - lastResource.getOffset());
      lastResource.setLength(lastLength);
      lastResource.setDecompressedLength(lastLength);

      if (unknownOffset != 0) {
        // add the Vertex

        String filename = Resource.generateFilename(realNumFiles);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, vertexOffset, unknownOffset - vertexOffset);
        realNumFiles++;

        // add the Unknown

        filename = Resource.generateFilename(realNumFiles);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, unknownOffset, imageOffset - unknownOffset);
        realNumFiles++;
      }
      else {
        // add the vertex

        String filename = Resource.generateFilename(realNumFiles);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, vertexOffset, imageOffset - vertexOffset);
        realNumFiles++;
      }

      // add the Images
      fm.seek(imageOffset);

      // 4 - Unknown (16)
      // 4 - Offset to First Loop of Unknown Size (relative to the start of the IMAGES block)
      // 4 - Offset to Second Loop of Unknown Size (relative to the start of the IMAGES block)
      fm.skip(12);

      // 4 - Number of Images
      int numImages = fm.readInt();
      FieldValidator.checkNumFiles(numImages);

      int[] offsets = new int[numImages + 1];
      for (int i = 0; i < numImages; i++) {
        // 4 - Image Offset (relative to the start of the IMAGES block)
        int offset = fm.readInt() + imageOffset;
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;
      }
      offsets[numImages] = (int) arcSize; // to allow us to easily calculate the lengths

      for (int i = 0; i < numImages; i++) {
        int offset = offsets[i];
        int length = offsets[i + 1] - offset;

        String filename = Resource.generateFilename(realNumFiles) + ".tme";

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
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
