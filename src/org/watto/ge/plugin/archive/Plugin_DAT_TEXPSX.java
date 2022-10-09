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
import java.util.Arrays;
import org.watto.Language;
import org.watto.Settings;
import org.watto.component.PreviewPanel;
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.viewer.Viewer_DAT_TEXPSX_TEXPSX;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_TEXPSX extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_TEXPSX() {

    super("DAT_TEXPSX", "DAT_TEXPSX");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("NHL FaceOff 2001");
    setExtensions("dat"); // DAT files as well, but the header should be enough to detect these files
    setPlatforms("PS2");

    setFileTypes(new FileType("texpsx", "TEX PSX Image", FileType.TYPE_IMAGE));

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

      // 8 - Header ("TEX PSX ")
      if (fm.readString(8).equals("TEX PSX ")) {
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

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      long paletteOffset = 0;
      while (fm.getOffset() < arcSize - 12) {
        long startOffset = fm.getOffset();

        //System.out.println(startOffset);

        // 8 - Header ("TEX PSX ")
        String header = fm.readString(8);
        if (header.equals("TEX PSX ")) {
          // OK
        }
        else {
          // probably padding
          startOffset++; // so we don't keep looping over the same offset

          startOffset += calculatePadding(startOffset, 6144);
          fm.relativeSeek(startOffset);
          continue;
        }

        // 4 - Unknown (1)
        // 4 - Unknown (1)
        // 4 - Unknown
        fm.skip(12);

        // 4 - Palette Flag (0/1)
        int paletteFlag = fm.readInt();

        if (paletteFlag == 0) {
          paletteOffset = fm.getOffset();

          // for each color (256)
          // 2 - 16bit Color Value
          fm.skip(256 * 2);

        }

        // 16 - Name (null terminated, filled with nulls)
        String filename = fm.readNullString(16);
        FieldValidator.checkFilename(filename);
        filename += ".texpsx";

        // 4 - Image Width
        int width = fm.readInt();
        FieldValidator.checkWidth(width);

        // 4 - Image Height
        int height = fm.readInt();
        FieldValidator.checkHeight(height);

        // 4 - null
        // 4 - Unknown (64)
        fm.skip(8);

        // X - Image Data
        long dataOffset = fm.getOffset();

        int length = width * height;
        fm.skip(length);

        if (paletteFlag == 1) {
          paletteOffset = fm.getOffset();

          // for each color (256)
          // 2 - 16bit Color Value
          fm.skip(256 * 2);
        }

        // X - null padding to a multiple of 6144 bytes
        int paddingSize = calculatePadding(fm.getOffset(), 6144);
        fm.skip(paddingSize);

        // Create a BlockExporter that has 2 blocks...
        // 1. the Palette
        // 2. the Image Data
        long[] blockOffsets = new long[] { paletteOffset, dataOffset };
        long[] blockLengths = new long[] { 256 * 2, length };
        BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporterDefault, blockOffsets, blockLengths, blockLengths);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, dataOffset, length, length, blockExporter);
        resource.addProperty("Width", width);
        resource.addProperty("Height", height);

        resources[realNumFiles] = resource;
        realNumFiles++;

        TaskProgressManager.setValue(dataOffset);
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

      long arcSize = src.getLength();

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      int realNumFiles = 0;
      while (src.getOffset() < arcSize - 12) {
        long startOffset = src.getOffset();

        // 8 - Header ("TEX PSX ")
        String header = src.readString(8);
        if (header.equals("TEX PSX ")) {
          // OK
          fm.writeString(header);
        }
        else {
          // probably padding
          long nextOffset = startOffset;
          nextOffset++; // so we don't keep looping over the same offset

          nextOffset += calculatePadding(nextOffset, 6144);
          src.relativeSeek(startOffset);

          int length = (int) (nextOffset - startOffset);

          // copy these padding bytes from src to fm
          fm.writeBytes(src.readBytes(length));

          continue;
        }

        Resource resource = resources[realNumFiles];

        // 4 - Unknown (1)
        // 4 - Unknown (1)
        // 4 - Unknown
        fm.writeBytes(src.readBytes(12));

        // 4 - Palette Flag (0 = PaletteBeforeImage / 1 = PaletteAfterImage)
        int paletteFlag = src.readInt();
        fm.writeInt(paletteFlag);

        if (paletteFlag == 0) {
          // PALETTE

          if (!resource.isReplaced()) {
            // unchanged
            fm.writeBytes(src.readBytes(256 * 2));
          }
          else {
            // changed - read the palette from the imported file
            src.skip(256 * 2);

            FileManipulator resourceFM = new FileManipulator(resource.getSource(), false);
            fm.writeBytes(resourceFM.readBytes(256 * 2));
            resourceFM.close();
          }
        }

        // 16 - Name (null terminated, filled with nulls)
        fm.writeBytes(src.readBytes(16));

        // 4 - Image Width
        int width = src.readInt();
        fm.writeInt(width);

        // 4 - Image Height
        int height = src.readInt();
        fm.writeInt(height);

        // 4 - null
        // 4 - Unknown (64)
        fm.writeBytes(src.readBytes(8));

        int length = width * height;

        // X - Pixel Indexes
        if (!resource.isReplaced()) {
          // unchanged
          fm.writeBytes(src.readBytes(length));
        }
        else {
          // changed - read the new data
          src.skip(length);

          FileManipulator resourceFM = new FileManipulator(resource.getSource(), false);
          resourceFM.skip(256 * 2); // skip the color palette
          fm.writeBytes(resourceFM.readBytes(length));
          resourceFM.close();
        }

        if (paletteFlag == 1) {
          // PALETTE

          if (!resource.isReplaced()) {
            // unchanged
            fm.writeBytes(src.readBytes(256 * 2));
          }
          else {
            // changed - read the palette from the imported file
            src.skip(256 * 2);

            FileManipulator resourceFM = new FileManipulator(resource.getSource(), false);
            fm.writeBytes(resourceFM.readBytes(256 * 2));
            resourceFM.close();
          }
        }

        // X - null padding to a multiple of 6144 bytes
        int srcPaddingSize = calculatePadding(src.getOffset(), 6144);
        src.skip(srcPaddingSize);

        int paddingSize = calculatePadding(fm.getOffset(), 6144);
        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(0);
        }

        realNumFiles++;
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
   When replacing TIF images, if the fileToReplaceWith is a different format image (eg DDS, PNG, ...)
   it can be converted into a TIF image. All other files are replaced without conversion
   @param resourceBeingReplaced the Resource in the archive that is being replaced
   @param fileToReplaceWith the file on your PC that will replace the Resource. This file is the
          one that will be converted into a different format, if applicable.
   @return the converted file, if conversion was applicable/successful, else the original fileToReplaceWith
   **********************************************************************************************
   **/
  @Override
  public File convertOnReplace(Resource resourceBeingReplaced, File fileToReplaceWith) {
    String beingReplacedExtension = resourceBeingReplaced.getExtension();
    if (beingReplacedExtension.equalsIgnoreCase("texpsx")) {
      // try to convert

      String toReplaceWithExtension = FilenameSplitter.getExtension(fileToReplaceWith);
      if (toReplaceWithExtension.equalsIgnoreCase("texpsx")) {
        // if the fileToReplace already has a TIF extension, assume it's already a compatible TIF file and doesn't need to be converted
        return fileToReplaceWith;
      }

      //
      //
      // if we're here, we want to scan to see if we can find an Image ViewerPlugin that can read the file into an ImageResource,
      // which we can then convert into a TIF using plugin Viewer_TEX_SOLWORKSTEX_TIF
      //
      //

      // 1. Open the file
      FileManipulator fm = new FileManipulator(fileToReplaceWith, false);

      // 2. Get all the ViewerPlugins that can read this file type
      RatedPlugin[] plugins = PluginFinder.findPlugins(fm, ViewerPlugin.class); // NOTE: This closes the fm pointer!!!
      if (plugins == null || plugins.length == 0) {
        // no viewer plugins found that will accept this file
        return fileToReplaceWith;
      }

      Arrays.sort(plugins);

      // re-open the file - it was closed at the end of findPlugins();
      fm = new FileManipulator(fileToReplaceWith, false);

      // 3. Try each plugin until we find one that can render the file as an ImageResource
      PreviewPanel imagePreviewPanel = null;
      for (int i = 0; i < plugins.length; i++) {
        fm.seek(0); // go back to the start of the file
        imagePreviewPanel = ((ViewerPlugin) plugins[i].getPlugin()).read(fm);

        if (imagePreviewPanel != null) {
          // 4. We have found a plugin that was able to render the image
          break;
        }
      }

      fm.close();

      if (imagePreviewPanel == null) {
        // no plugins were able to open this file successfully
        return fileToReplaceWith;
      }

      //
      //
      // If we're here, we have a rendered image, so we want to convert it into BIN_TEX using Viewer_TEX_SOLWORKSTEX_TIF
      //
      //
      Viewer_DAT_TEXPSX_TEXPSX converterPlugin = new Viewer_DAT_TEXPSX_TEXPSX();
      //File destination = new File(fileToReplaceWith.getAbsolutePath() + "." + converterPlugin.getExtension(0));
      File destination = new File(fileToReplaceWith.getAbsolutePath() + "." + beingReplacedExtension);
      if (destination.exists()) {
        destination.delete();
      }

      FileManipulator fmOut = new FileManipulator(destination, true);
      converterPlugin.write(imagePreviewPanel, fmOut);
      fmOut.close();

      return destination;
    }
    else {
      return fileToReplaceWith;
    }
  }

}
