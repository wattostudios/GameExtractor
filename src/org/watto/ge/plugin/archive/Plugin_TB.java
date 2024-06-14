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
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.resource.Resource_Property;
import org.watto.ge.plugin.viewer.Viewer_TB_TBTEX;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_TB extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_TB() {

    super("TB", "TB");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Die Hard Trilogy 2");
    setExtensions("tb"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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

      // 2 - Unknown (1)
      if (fm.readShort() == 1) {
        rating += 5;
      }

      // 2 - Number of Names
      if (FieldValidator.checkNumFiles(fm.readShort())) {
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
  @SuppressWarnings("unused")
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

      // 2 - Unknown (1)
      fm.skip(2);

      // 2 - Number of Names
      short numNames = fm.readShort();

      String[] names = new String[numNames];
      int[] nameIDs = new int[numNames];
      int[] nameIDsSorted = new int[numNames];
      for (int i = 0; i < numNames; i++) {
        // 8 - Filename (null terminated, filled with nulls)
        String name = fm.readNullString(8);
        FieldValidator.checkFilename(name);
        names[i] = name;

        // 4 - First File ID with this name?
        int nameID = fm.readInt();
        nameIDs[i] = nameID;
        nameIDsSorted[i] = nameID;
      }

      Arrays.sort(nameIDsSorted);

      // 4 - Unknown (10)
      fm.skip(4);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (1)
      // 4 - Unknown
      fm.skip(8);

      // 4 - Details Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Details Directory Length
      fm.skip(4);

      // 4 - File Data Offset
      int dataOffset = fm.readInt();
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 4 - Length of File Data [-OffsetToTheNextField]
      // 4 - Length from This Field to the End of the Archive
      // 4 - Offset to the Previous Field
      fm.relativeSeek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int currentNameIndex = 0;
      int currentNameID = nameIDsSorted[currentNameIndex];
      String currentName = "";
      for (int n = 0; n < numNames; n++) {
        if (nameIDs[n] == currentNameID) {
          currentName = names[n] + File.separatorChar;
          break;
        }
      }

      int imageCounter = 1;
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {

        // 1 - Image Height/Width
        int width = ByteConverter.unsign(fm.readByte());
        if (width == 0) {
          width = 256;
        }
        int height = width; // they're all squares

        // 1 - Unknown
        fm.skip(1);

        // 2 - Image Type (0/1=Image, 2=Animation)
        short imageType = fm.readShort();

        // 2 - Number of Frames
        short numFrames = fm.readShort();

        // 2 - Unknown
        fm.skip(2);

        // 4 - File Offset (relative to the start of the File Data)
        int offset = fm.readInt();

        // 4 - Unknown
        fm.skip(4);

        if (offset == -1 || offset == -858993460) {
          continue;
        }
        offset += dataOffset;
        FieldValidator.checkOffset(offset, arcSize);
        //System.out.println(offset);

        //String filename = currentName + "Image" + imageCounter + ".tb_tex";
        String filename = Resource.generateFilename(realNumFiles) + ".tb_tex";

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset);
        resource.addProperty("Width", width);
        resource.addProperty("Height", height); // height and width are the same

        if (numFrames > 4) { // only seems to be a single image that I've found where this isn't set correctly
          imageType = 2;
        }

        int length = 0;
        if (imageType == 2) {
          resource.addProperty("FrameCount", numFrames);
          resource.addProperty("MipmapCount", 1);

          length = (width * height * 2) * numFrames;
        }
        else {
          resource.addProperty("FrameCount", 1);
          resource.addProperty("MipmapCount", numFrames);

          int mipmapWidth = width;
          int mipmapHeight = height;
          for (int m = 0; m < numFrames; m++) {
            length += (mipmapWidth * mipmapHeight * 2);
            mipmapWidth *= 2;
            mipmapHeight *= 2;
          }
        }

        resource.setLength(length);
        resource.setDecompressedLength(length);

        resource.addProperty("OriginalOffset", offset);
        resource.addProperty("OriginalLength", length);

        resources[realNumFiles] = resource;
        realNumFiles++;

        TaskProgressManager.setValue(i);

        imageCounter++;

        if (i >= currentNameID && i + 1 < numFiles && currentNameIndex + 1 < numNames) {
          // move to the next nameID
          currentNameIndex++;
          currentNameID = nameIDsSorted[currentNameIndex];
          for (int n = 0; n < numNames; n++) {
            if (nameIDs[n] == currentNameID) {
              currentName = names[n] + File.separatorChar;
              imageCounter = 1;
              break;
            }
          }
        }
      }

      if (realNumFiles < numFiles) {
        resources = resizeResources(resources, realNumFiles);
      }

      numFiles = realNumFiles;

      //calculateFileSizes(resources, arcSize);
      for (int i = 0; i < numFiles - 1; i++) {
        long calcLength = resources[i + 1].getOffset() - resources[i].getOffset();
        long actualLength = resources[i].getLength();

        if (calcLength < actualLength) {
          Resource resource = resources[i];

          // probably specified mipmaps when it should be frames
          String numFrames = resource.getProperty("MipmapCount");

          // check if the size matches frames instead of mipmaps
          int checkLength = Integer.parseInt(resource.getProperty("Width")) * Integer.parseInt(resource.getProperty("Height")) * 2 * Integer.parseInt(numFrames);
          if (checkLength == calcLength) {
            resource.setLength(checkLength);
            resource.setDecompressedLength(checkLength);
            resource.setProperty("OriginalLength", "" + checkLength);

            //actualLength = checkLength;

            Resource_Property[] properties = resource.getProperties();
            for (int p = 0; p < properties.length; p++) {
              if (properties[p].getCode().equals("FrameCount")) {
                properties[p].setValue(numFrames);
              }
              else if (properties[p].getCode().equals("MipmapCount")) {
                properties[p].setValue("1");
              }
            }
          }
          else {
            // a funny one - looks like the width/height needs to be doubled, the numMipmaps-=1, and had 128 bytes of padding
            resource.setLength(calcLength);
            resource.setDecompressedLength(calcLength);
            resource.setProperty("OriginalLength", "" + calcLength);

            //actualLength = calcLength;

            Resource_Property[] properties = resource.getProperties();
            for (int p = 0; p < properties.length; p++) {
              if (properties[p].getCode().equals("Width")) {
                properties[p].setValue((Integer.parseInt(properties[p].getValue()) * 2) + "");
              }
              else if (properties[p].getCode().equals("Height")) {
                properties[p].setValue((Integer.parseInt(properties[p].getValue()) * 2) + "");
              }
              else if (properties[p].getCode().equals("MipmapCount")) {
                properties[p].setValue((Integer.parseInt(properties[p].getValue()) - 1) + "");
              }
            }
          }
        }

        //System.out.println((calcLength == actualLength) + "\t" + calcLength + "\t" + actualLength + "\t" + resources[i].getName());

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

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long filesSize = 0;
      for (int i = 0; i < numFiles; i++) {
        filesSize += resources[i].getDecompressedLength();
      }

      // Write Header Data

      // HEADER
      // 2 - Unknown (1)
      fm.writeBytes(src.readBytes(2));

      // NAMES DIRECTORY
      // 2 - Number of Names
      short numNames = src.readShort();
      fm.writeShort(numNames);

      // for each entry
      //   8 - Filename (null terminated, filled with nulls)
      //   4 - First File ID with this name?
      fm.writeBytes(src.readBytes(numNames * 12));

      // DETAILS HEADER
      //   4 - Unknown (10)
      fm.writeBytes(src.readBytes(4));

      //   4 - Number of Files
      int numFilesInSource = src.readInt();
      fm.writeInt(numFilesInSource);

      //   4 - Unknown (1)
      //   4 - Unknown
      //   4 - Details Directory Offset
      //   4 - Details Directory Length
      //   4 - File Data Offset
      //   4 - Length of File Data [-OffsetToTheNextField]
      //   4 - Length from This Field to the End of the Archive
      //   4 - Offset to the Previous Field
      fm.writeBytes(src.readBytes(32));

      // DETAILS DIRECTORY 
      // for each file
      //   1 - Image Height/Width
      //   1 - Unknown
      //   2 - Image Type (0/1=Image, 2=Animation)
      //   2 - Number of Frames/Mipmaps
      //   2 - Unknown
      //   4 - File Offset (relative to the start of the File Data)
      //   4 - Unknown
      fm.writeBytes(src.readBytes(numFilesInSource * 16));

      long endOfFilesOffset = src.getOffset() + filesSize;

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      //write(resources, fm);
      Resource previousResource = null;
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];

        if (previousResource != null) {
          // sometimes there is something else between the files, that we need to copy across.

          int previousOriginalOffset = -1;
          int previousOriginalLength = -1;
          int thisOriginalOffset = -1;

          try {
            previousOriginalOffset = Integer.parseInt(previousResource.getProperty("OriginalOffset"));
            previousOriginalLength = Integer.parseInt(previousResource.getProperty("OriginalLength"));
            thisOriginalOffset = Integer.parseInt(resource.getProperty("OriginalOffset"));
          }
          catch (Throwable t) {
          }

          if (previousOriginalOffset != -1 && previousOriginalLength != -1 && thisOriginalOffset != -1) {
            //long expectedOffset = previousResource.getOffset() + previousResource.getDecompressedLength();
            //long actualOffset = resource.getOffset();

            long expectedOffset = previousOriginalOffset + previousOriginalLength;
            long actualOffset = thisOriginalOffset;

            int difference = (int) (actualOffset - expectedOffset);
            //System.out.println(previousOriginalOffset + "\t" + previousOriginalLength + "\t" + difference);
            if (difference > 0) {
              src.seek(expectedOffset);
              fm.writeBytes(src.readBytes(difference));
            }
          }
        }

        write(resource, fm);
        previousResource = resource;

        TaskProgressManager.setValue(i);
      }

      int previousOriginalOffset = -1;
      int previousOriginalLength = -1;

      try {
        previousOriginalOffset = Integer.parseInt(previousResource.getProperty("OriginalOffset"));
        previousOriginalLength = Integer.parseInt(previousResource.getProperty("OriginalLength"));
      }
      catch (Throwable t) {
      }

      if (previousOriginalOffset != -1 && previousOriginalLength != -1) {
        endOfFilesOffset = previousOriginalOffset + previousOriginalLength;
      }

      //src.skip(filesSize);
      src.seek(endOfFilesOffset);

      // FOOTER
      // for each Unknown
      //   4 - Unknown
      int remainingSize = (int) (src.getLength() - src.getOffset());
      fm.writeBytes(src.readBytes(remainingSize));

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
   **********************************************************************************************
   When replacing tb_tex images, if the fileToReplaceWith is a different format image (eg DDS, PNG, ...)
   it can be converted into a tb_tex image. All other files are replaced without conversion
   @param resourceBeingReplaced the Resource in the archive that is being replaced
   @param fileToReplaceWith the file on your PC that will replace the Resource. This file is the
          one that will be converted into a different format, if applicable.
   @return the converted file, if conversion was applicable/successful, else the original fileToReplaceWith
   **********************************************************************************************
   **/
  @Override
  public File convertOnReplace(Resource resourceBeingReplaced, File fileToReplaceWith) {
    try {
      String beingReplacedExtension = resourceBeingReplaced.getExtension();
      if (beingReplacedExtension.equalsIgnoreCase("tb_tex")) {
        // try to convert

        String toReplaceWithExtension = FilenameSplitter.getExtension(fileToReplaceWith);
        if (toReplaceWithExtension.equalsIgnoreCase("tb_tex")) {
          // if the fileToReplace already has a tb_tex extension, assume it's already a compatible tb_tex file and doesn't need to be converted
          return fileToReplaceWith;
        }

        //
        //
        // if we're here, we want to scan to see if we can find an Image ViewerPlugin that can read the file into an ImageResource,
        // which we can then convert into a tb_tex using plugin Viewer_TB_TBTEX
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

            // 5. Need to see if there are multiple frames, and if so, try to render them all
            try {
              if (fileToReplaceWith.getName().contains("_ge_frame_")) {
                // probably multiple frames

                // find all the frame files (max 1000)
                String baseName = fileToReplaceWith.getAbsolutePath();
                String extension = "";
                int numberPos = baseName.lastIndexOf("_ge_frame_");
                int dotPos = baseName.lastIndexOf('.');
                if (numberPos > 0) {
                  if (dotPos > numberPos) {
                    extension = baseName.substring(dotPos);
                    baseName = baseName.substring(0, numberPos + 10);
                  }
                }
                File[] imageFiles = new File[1000];
                int numImages = 0;
                for (int f = 0; f < 1000; f++) {
                  File imageFile = new File(baseName + f + extension);
                  if (imageFile.exists()) {
                    imageFiles[f] = imageFile;
                    numImages++;
                  }
                  else {
                    // no more images
                    break;
                  }
                }

                // now have all the image files, read each of them into an ImageResource
                ViewerPlugin plugin = (ViewerPlugin) plugins[i].getPlugin();
                ImageResource[] images = new ImageResource[numImages];
                for (int f = 0; f < numImages; f++) {
                  FileManipulator singleFM = new FileManipulator(imageFiles[f], false);
                  PreviewPanel previewPanel = plugin.read(singleFM);
                  singleFM.close();

                  if (previewPanel != null && previewPanel instanceof PreviewPanel_Image) {
                    images[f] = ((PreviewPanel_Image) previewPanel).getImageResource();
                  }
                }

                // now set next/previous frames
                for (int f = 0; f < numImages; f++) {
                  ImageResource image = images[f];
                  if (f == numImages - 1) {
                    image.setNextFrame(images[0]);
                  }
                  else {
                    image.setNextFrame(images[f + 1]);
                  }

                  if (f == 0) {
                    image.setPreviousFrame(images[numImages - 1]);
                  }
                  else {
                    image.setPreviousFrame(images[f - 1]);
                  }
                }

                // put the first frame onto the original previewpanel
                ImageResource firstImage = images[0];
                firstImage.setManualFrameTransition(true);

                ((PreviewPanel_Image) imagePreviewPanel).setImageResource(firstImage);

                // now the imagePreviewPanel contains the first frame, with links to all the subsequent frames, all loaded from files.
              }
            }
            catch (Throwable t) {
              ErrorLogger.log(t);
            }
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
        // If we're here, we have a rendered image, so we want to convert it into tb_tex using Viewer_TB_TBTEX
        //
        //

        Viewer_TB_TBTEX converterPlugin = new Viewer_TB_TBTEX();

        File destination = new File(fileToReplaceWith.getAbsolutePath() + "." + beingReplacedExtension);
        if (destination.exists()) {
          destination.delete();
        }

        FileManipulator fmOut = new FileManipulator(destination, true);
        converterPlugin.replace(resourceBeingReplaced, imagePreviewPanel, fmOut);
        fmOut.close();

        return destination;

      }
      else {
        return fileToReplaceWith;
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return fileToReplaceWith;
    }
  }

}
