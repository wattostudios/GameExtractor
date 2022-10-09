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
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.component.PreviewPanel;
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.viewer.Viewer_TEX_SOLWORKSTEX_TIF;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_TEX_SOLWORKSTEX extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_TEX_SOLWORKSTEX() {

    super("TEX_SOLWORKSTEX", "TEX_SOLWORKSTEX");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("NHL FaceOff 2001");
    setExtensions("tex"); // DAT files as well, but the header should be enough to detect these files
    setPlatforms("PS2");

    setFileTypes(new FileType("tif", "SolWorks TIF Image", FileType.TYPE_IMAGE));

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

      // 12 - Header (SolWorks Tex)
      if (fm.readString(12).equals("SolWorks Tex")) {
        rating += 50;
      }

      fm.skip(8);

      // Number Of Filenames
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // 4 - Filename Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize - 12) {
        long startOffset = fm.getOffset();

        //System.out.println(startOffset);

        // 12 - Header (SolWorks Tex)
        String header = fm.readString(12);
        if (header.equals("SolWorks Tex") || header.substring(0, 3).equals("CAT")) {
          // OK
        }
        else {
          // probably padding
          startOffset++; // so we don't keep looping over the same offset

          startOffset += calculatePadding(startOffset, 1024);
          fm.relativeSeek(startOffset);
          continue;
        }

        // 4 - Unknown
        // 4 - Unknown
        fm.skip(8);

        // 4 - Number of Filenames (1/2)
        int numFilenames = fm.readInt();
        FieldValidator.checkNumFiles(numFilenames);

        // 4 - Filename Offset (32/40/1064)
        int filenameOffset = fm.readInt();
        FieldValidator.checkOffset(filenameOffset + startOffset, arcSize);

        int numColors = 256;
        if (filenameOffset == 40) {
          // 4 - null
          fm.skip(4);

          // 4 - Number of Colors (256)
          numColors = fm.readInt();

          // 4 - Header Length (40)
          fm.skip(4);
        }
        else if (filenameOffset == 32) {
          // 4 - Unknown
          fm.skip(4);
        }
        else if (filenameOffset == 1064) {
          // 4 - Unknown (1)
          fm.skip(4);

          // 4 - Number of Colors (256)
          numColors = fm.readInt();

          // 4 - Header Length (40)
          fm.skip(4);
        }
        else {
          ErrorLogger.log("[TEX_SOLWORKSTEX] Unknown Filename Offset: " + filenameOffset);
        }

        FieldValidator.checkNumColors(numColors);

        int globalPaletteOffset = 0;
        if (filenameOffset == 1064) {
          // PALETTE
          globalPaletteOffset = (int) fm.getOffset();
        }

        fm.relativeSeek(startOffset + filenameOffset);

        int endOffset = 0;
        for (int f = 0; f < numFilenames; f++) {
          // 32 - Filename (null terminated, filled with nulls)
          String filename = fm.readNullString(32);
          filename = filename.trim(); // some filenames have a space at the end of the extension
          FieldValidator.checkFilename(filename);

          // 4 - Image Width
          int width = fm.readInt();
          FieldValidator.checkWidth(width);

          // 4 - Image Height
          int height = fm.readInt();
          FieldValidator.checkHeight(height);

          // 4 - Unknown (2)
          // 4 - null
          fm.skip(8);

          // 4 - Palette Data Offset (relative to the start of this file entry)
          int paletteOffset = (int) (fm.readInt() + startOffset);
          FieldValidator.checkOffset(paletteOffset, arcSize);

          if (globalPaletteOffset != 0) {
            paletteOffset = globalPaletteOffset;
          }

          // 4 - Image Data Offset (relative to the start of this file entry)
          int dataOffset = (int) (fm.readInt() + startOffset);
          FieldValidator.checkOffset(dataOffset, arcSize);

          int length = width * height;

          int endOfImage = dataOffset + length;
          if (endOfImage > endOffset) {
            endOffset = endOfImage;
          }

          // Create a BlockExporter that has 2 blocks...
          // 1. the Palette
          // 2. the Image Data
          long[] blockOffsets = new long[] { paletteOffset, dataOffset };
          long[] blockLengths = new long[] { numColors * 4, length };
          BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporterDefault, blockOffsets, blockLengths, blockLengths);

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(path, filename, dataOffset, length, length, blockExporter);
          resource.addProperty("ColorCount", numColors);
          resource.addProperty("Width", width);
          resource.addProperty("Height", height);

          resources[realNumFiles] = resource;
          realNumFiles++;

          TaskProgressManager.setValue(dataOffset);
        }

        fm.relativeSeek(endOffset);

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

        // 12 - Header (SolWorks Tex)
        String header = src.readString(12);
        if (header.equals("SolWorks Tex")) {
          // OK
          fm.writeString(header);
        }
        else if (header.substring(0, 3).equals("CAT")) {
          // OK
          fm.writeString(header);
        }
        else {
          // probably padding
          long nextOffset = startOffset;
          nextOffset++; // so we don't keep looping over the same offset

          nextOffset += calculatePadding(nextOffset, 1024);
          src.relativeSeek(startOffset);

          int length = (int) (nextOffset - startOffset);

          // copy these padding bytes from src to fm
          fm.writeBytes(src.readBytes(length));

          continue;
        }

        Resource resource = resources[realNumFiles];

        // 4 - Unknown
        // 4 - Unknown
        fm.writeBytes(src.readBytes(8));

        // 4 - Number of Filenames (1/2)
        int numFilenames = src.readInt();
        FieldValidator.checkNumFiles(numFilenames);
        fm.writeInt(numFilenames);

        // 4 - Filename Offset (32/40/1064)
        int filenameOffset = src.readInt();
        FieldValidator.checkOffset(filenameOffset + startOffset, arcSize);
        fm.writeInt(filenameOffset);

        int numColors = 256;
        if (filenameOffset == 40) {
          // 4 - null
          fm.writeBytes(src.readBytes(4));

          // 4 - Number of Colors (256)
          numColors = src.readInt();
          fm.writeInt(256);

          // 4 - Header Length (40)
          fm.writeBytes(src.readBytes(4));
        }
        else if (filenameOffset == 32) {
          // 4 - Unknown
          fm.writeBytes(src.readBytes(4));
        }
        else if (filenameOffset == 1064) {
          // 4 - Unknown (1)
          fm.writeBytes(src.readBytes(4));

          // 4 - Number of Colors (256)
          numColors = src.readInt();
          fm.writeInt(256);

          // 4 - Header Length (40)
          fm.writeBytes(src.readBytes(4));
        }
        else {
          ErrorLogger.log("[TEX_SOLWORKSTEX] Unknown Filename Offset: " + filenameOffset);
        }

        FieldValidator.checkNumColors(numColors);

        int globalPaletteOffset = 0;
        int[] globalPaletteOriginal = null;
        int[] globalPaletteChanged = null;
        if (filenameOffset == 1064) {
          // PALETTE
          globalPaletteOffset = (int) src.getOffset();

          if (!resource.isReplaced()) {
            // unchanged
            byte[] paletteBytes = src.readBytes(numColors * 4);
            fm.writeBytes(paletteBytes);

            // now read the global palette, in case we need to adjust any subsequent images to fit this palette
            FileManipulator palFM = new FileManipulator(new ByteBuffer(paletteBytes));
            globalPaletteOriginal = ImageFormatReader.readPaletteRGBA(palFM, 256);
            palFM.close();
          }
          else {
            // changed - read the palette from the imported file
            //src.skip(numColors * 4);

            // Read the global palette, in case we need to adjust any subsequent images to fit this palette
            byte[] originalPaletteBytes = src.readBytes(numColors * 4);
            FileManipulator palFM = new FileManipulator(new ByteBuffer(originalPaletteBytes));
            globalPaletteOriginal = ImageFormatReader.readPaletteRGBA(palFM, 256);
            palFM.close();

            // now read the new palette from the edited file
            FileManipulator resourceFM = new FileManipulator(resource.getSource(), false);
            byte[] paletteBytes = resourceFM.readBytes(256 * 4);
            fm.writeBytes(paletteBytes);
            resourceFM.close();

            // now read the changed global palette, in case we need to adjust any subsequent images to fit this palette
            palFM = new FileManipulator(new ByteBuffer(paletteBytes));
            globalPaletteChanged = ImageFormatReader.readPaletteRGBA(palFM, 256);
            palFM.close();
          }
        }

        //src.relativeSeek(startOffset + filenameOffset);

        int endOffset = 0;
        int firstOffset = (int) arcSize;
        long[] paletteOffsets = new long[numFilenames];
        long[] dataOffsets = new long[numFilenames];
        int[] dataLengths = new int[numFilenames];
        int[] widths = new int[numFilenames];
        int[] heights = new int[numFilenames];
        for (int f = 0; f < numFilenames; f++) {

          // 32 - Filename (null terminated, filled with nulls)
          fm.writeBytes(src.readBytes(32));

          // 4 - Image Width
          int width = src.readInt();
          widths[f] = width;
          fm.writeInt(width);

          // 4 - Image Height
          int height = src.readInt();
          heights[f] = height;
          fm.writeInt(height);

          // 4 - Unknown (2)
          // 4 - null
          fm.writeBytes(src.readBytes(8));

          // 4 - Palette Data Offset (relative to the start of this file entry)
          int paletteOffset = (int) (src.readInt());
          fm.writeInt(paletteOffset);
          paletteOffsets[f] = paletteOffset + startOffset;

          int paletteOffsetReal = (int) (paletteOffset + startOffset);
          if (paletteOffset != 0 && paletteOffsetReal < firstOffset) {
            firstOffset = paletteOffsetReal;
          }

          // 4 - Image Data Offset (relative to the start of this file entry)
          int dataOffset = (int) (src.readInt());
          fm.writeInt(dataOffset);
          dataOffsets[f] = dataOffset + startOffset;

          int length = width * height;

          dataLengths[f] = length;

          int endOfImage = (int) (dataOffset + startOffset + length);
          if (endOfImage > endOffset) {
            endOffset = endOfImage;
          }

        }

        if (filenameOffset == 32) {
          // 8 - null
          fm.writeLong(0);
        }
        else if (src.getOffset() != firstOffset) {
          // 8 - null
          fm.writeLong(0);
        }

        for (int f = 0; f < numFilenames; f++) {

          if (f != 0) {
            // need to move to the next resource
            resource = resources[realNumFiles];
          }

          if (globalPaletteOffset == 0) {
            // no global palette

            // PALETTE
            // IMAGE DATA
            write(resource, fm);
            TaskProgressManager.setValue(realNumFiles);

          }
          else {
            // global palette (which has already been written), so just want to write the image data

            FileManipulator resourceFM = new FileManipulator(resource.getSource(), false);
            resourceFM.seek(resource.getOffset());

            int dataLength = (int) resource.getLength();
            int[] paletteToUse = globalPaletteOriginal;
            if (resource.isReplaced()) {
              // if we're reading from the original archive, it already points to the data, so don't need to skip.
              // if we've extracted, however, we need to skip the palette

              //resourceFM.skip(256 * 4);
              paletteToUse = ImageFormatReader.readPaletteRGBA(resourceFM, 256);

              dataLength = (int) (resourceFM.getLength() - resourceFM.getOffset());
            }

            // read in the pixels
            byte[] pixels = resourceFM.readBytes(dataLength);

            int[] newPalette = globalPaletteOriginal;
            if (globalPaletteChanged != null) {
              newPalette = globalPaletteChanged;
            }

            if (newPalette == paletteToUse) {
              // the global palette hasn't changed, and this image hasn't changed either, so keep the colors the same
            }
            else {
              // at least 1 of the palettes has changed, so we need to convert this image to using the new global palette

              //construct the image using the proper palette for this image
              FileManipulator pixelFM = new FileManipulator(new ByteBuffer(pixels));
              ImageResource imageResource = ImageFormatReader.read8BitPaletted(pixelFM, widths[f], heights[f], paletteToUse);
              pixelFM.close();

              // apply the changed global palette
              ImageManipulator im = new ImageManipulator(imageResource);
              im.convertToPaletted();
              im.swapAndConvertPalette(newPalette);

              // convert back to pixels
              pixels = im.getPixelBytes();

            }

            // write out the pixels in the new palette
            fm.writeBytes(pixels);
            resourceFM.close();
          }

          realNumFiles++;
        }

        src.relativeSeek(endOffset);

      }

      // if there's anything left in the archive (eg the CAT at the end), copy that in as well
      int remainingLength = (int) (arcSize - src.getOffset());
      if (remainingLength > 0) {
        fm.writeBytes(src.readBytes(remainingLength));
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
    if (beingReplacedExtension.equalsIgnoreCase("tif")) {
      // try to convert

      String toReplaceWithExtension = FilenameSplitter.getExtension(fileToReplaceWith);
      if (toReplaceWithExtension.equalsIgnoreCase("tif")) {
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
      Viewer_TEX_SOLWORKSTEX_TIF converterPlugin = new Viewer_TEX_SOLWORKSTEX_TIF();
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
