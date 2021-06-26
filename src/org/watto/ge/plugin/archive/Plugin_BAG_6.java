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
import java.util.Arrays;
import org.watto.Language;
import org.watto.component.PreviewPanel;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.viewer.Viewer_BAG_TEX;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BAG_6 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_BAG_6() {

    super("BAG_6", "BAG_6");

    //         read write replace rename
    setProperties(true, true, true, false);

    setGames("Brian Lara International Cricket 2005",
        "Ricky Ponting International Cricket 2005");
    setExtensions("bag");
    setPlatforms("PS2");

    // We can convert some images into TEX format when replacing
    setCanConvertOnReplace(true);

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

  }

  /**
   **********************************************************************************************
   When replacing TEX images, if the fileToReplaceWith is a different format image (eg DDS, PNG, ...)
   it can be converted into a TEX image. All other files are replaced without conversion
   @param resourceBeingReplaced the Resource in the archive that is being replaced
   @param fileToReplaceWith the file on your PC that will replace the Resource. This file is the
          one that will be converted into a different format, if applicable.
   @return the converted file, if conversion was applicable/successful, else the original fileToReplaceWith
   **********************************************************************************************
   **/
  @Override
  public File convertOnReplace(Resource resourceBeingReplaced, File fileToReplaceWith) {
    if (resourceBeingReplaced.getExtension().equalsIgnoreCase("tex")) {
      // try to convert

      if (FilenameSplitter.getExtension(fileToReplaceWith).equalsIgnoreCase("tex")) {
        // if the fileToReplace already has a TEX extension, assume it's already a compatible TEX file and doesn't need to be converted
        return fileToReplaceWith;
      }

      //
      //
      // if we're here, we want to scan to see if we can find an Image ViewerPlugin that can read the file into an ImageResource,
      // which we can then convert into a TEX using plugin Viewer_BAG_TEX
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
      // If we're here, we have a rendered image, so we want to convert it into TEX using Viewer_BAG_TEX
      //
      //
      Viewer_BAG_TEX converterPlugin = new Viewer_BAG_TEX();
      File destination = new File(fileToReplaceWith.getAbsolutePath() + "." + converterPlugin.getExtension(0));
      if (destination.exists()) {
        destination.delete();
      }

      String imageType = resourceBeingReplaced.getProperty("ImageType");

      boolean imageTypePS2 = false;
      FileManipulator existingFM = null;
      if (imageType != null && imageType.equals("PS2")) {
        imageTypePS2 = true;

        converterPlugin.setImageType("PS2");

        // extract the existing resource into a byte[] array for quick re-use
        existingFM = new FileManipulator(new ByteBuffer((int) resourceBeingReplaced.getDecompressedLength()));
        resourceBeingReplaced.extract(existingFM);
        existingFM.seek(0);
      }

      FileManipulator fmOut = new FileManipulator(destination, true);

      boolean extraPadding = false;
      if (imageTypePS2) {
        // copy the header data from the existing resource

        int startOfImageData = 84;
        String startOfImageDataProperty = resourceBeingReplaced.getProperty("StartOfImageData");
        if (startOfImageDataProperty != null && !startOfImageDataProperty.equals("")) {
          try {
            startOfImageData = Integer.parseInt(startOfImageDataProperty);
          }
          catch (Throwable t) {
            startOfImageData = 84;
          }
        }

        if (startOfImageData != 84) {
          converterPlugin.setExtraPadding(true);
          extraPadding = true;
        }

        fmOut.writeBytes(existingFM.readBytes(startOfImageData));
      }

      converterPlugin.write(imagePreviewPanel, fmOut);

      if (imageTypePS2) {
        // skip over the existing image data
        // 4 - Image Width
        int imageWidth = existingFM.readInt();

        // 4 - Image Height
        int imageHeight = existingFM.readInt();

        // 4 - Number Of Mipmaps (4)
        int numMipmaps = existingFM.readInt();

        // 4 - Unknown (5)
        existingFM.skip(4);

        // 12 - null Padding
        existingFM.skip(12);

        if (extraPadding) {
          // 4 - null
          existingFM.skip(4);
        }

        // skip all the mipmaps
        int pixelLength = 0;
        int interWidth = imageWidth;
        int interHeight = imageHeight;
        for (int i = 0; i < numMipmaps; i++) {
          pixelLength += (interWidth * interHeight);
          interWidth /= 2;
          interHeight /= 2;
        }
        existingFM.skip(pixelLength);

        // skip the color palette
        existingFM.skip(256 * 4);

        // copy the footer data from the existing resource
        fmOut.writeBytes(existingFM.readBytes((int) existingFM.getRemainingLength()));
      }

      if (existingFM != null) {
        existingFM.close();
      }

      fmOut.close();

      return destination;
    }
    else {
      return fileToReplaceWith;
    }
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

      // 4 - Version (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // 4 - Image Data Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Number of Images
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      // 4 - Version (1)
      fm.skip(4);

      // 4 - Image Data Length
      int length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      // 4 - Number of Images
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      FieldValidator.checkLength(length * numFiles, arcSize); // this should be just a little bit smaller than the archive length

      // 2036 - null Padding to a multiple of 2048 bytes
      fm.getBuffer().setBufferSize(128);

      long offset = 2048;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        fm.seek(offset);

        String filename = Resource.generateFilename(i);

        // 3 - Image Header (optional)
        boolean imageTypePS2 = false;
        if (fm.readString(3).equals("PS2")) {
          imageTypePS2 = true;
          // 1 - null
          // 16 - Unknown
          fm.skip(17);

          // 64 - Filename (null terminated, filled with nulls)
          filename = fm.readNullString(64);
          FieldValidator.checkFilename(filename);

          // check to see that this is the right filename
          if (fm.readInt() == 0) {
            // skip these few bytes then read the _real_ filename
            fm.skip(24);
            //System.out.println(fm.getOffset());
            filename = fm.readNullString(64);
            FieldValidator.checkFilename(filename);

            // check to see that this is the right filename (again)
            if (fm.readInt() == 0) {
              // skip these few bytes then read the _real_ filename
              fm.skip(28);
              //System.out.println(fm.getOffset());
              filename = fm.readNullString(64);
              FieldValidator.checkFilename(filename);
            }
          }
        }

        filename += ".tex";

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        if (imageTypePS2) {
          resources[i].addProperty("ImageType", "PS2");
          resources[i].addProperty("StartOfImageData", "" + (fm.getOffset() - offset));
        }

        TaskProgressManager.setValue(i);

        offset += length;
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
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // GLOBALS
      int paddingMultiple = 2048;

      // First, find the maximum file length, so we know what the block size will be
      int blockSize = 0;
      for (int i = 0; i < numFiles; i++) {
        int length = (int) resources[i].getDecompressedLength();
        if (length > blockSize) {
          blockSize = length;
        }
      }
      // Now pad it to a multiple of 2048 bytes
      blockSize += calculatePadding(blockSize, paddingMultiple);

      // Write Header Data

      // 4 - Version (1)
      fm.writeInt(1);

      // 4 - Maximum Length of the Data in each Image
      fm.writeInt(blockSize);

      // 4 - Number Of Files
      fm.writeInt(numFiles);

      // 2036 - null Padding to a multiple of 2048 bytes
      for (int i = 12; i < paddingMultiple; i++) {
        fm.writeByte(0);
      }

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      // Write Files
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // X - File Data
        write(resource, fm);

        // X - null padding to BlockSize
        int remainingSize = (int) (blockSize - decompLength);
        for (int p = 0; p < remainingSize; p++) {
          fm.writeByte(0);
        }
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
