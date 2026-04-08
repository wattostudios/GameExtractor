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
import org.watto.Settings;
import org.watto.component.PreviewPanel;
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Palette;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.viewer.Viewer_MTT_MTLB_PIC_MTDT;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_MTT_MTLB extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_MTT_MTLB() {

    super("MTT_MTLB", "MTT_MTLB");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Superbike World Championship");
    setExtensions("mtt"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(new FileType("mtl", "Material", FileType.TYPE_OTHER),
        new FileType("pic", "Picture Image", FileType.TYPE_IMAGE),
        new FileType("plt", "Color Palette", FileType.TYPE_PALETTE));

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
      if (fm.readString(4).equals("MTLB")) {
        rating += 50;
      }

      if (fm.readInt() == 112) {
        rating += 5;
      }

      fm.skip(4);

      if (fm.readString(4).equals("MTLS")) {
        rating += 5;
      }

      fm.skip(4);

      // Number Of Files
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

      // 4 - Header (MTLB)
      // 4 - Version (112)
      // 4 - Unknown (-1)
      // 4 - Header (MTLS)
      fm.skip(16);

      // 4 - Block Length
      int nextBlock = fm.readInt() + 24;
      FieldValidator.checkOffset(nextBlock, arcSize);

      int numFiles = Archive.getMaxFiles();

      // 4 - Number of Files in this Block
      int numFilesInBlock = fm.readInt();
      FieldValidator.checkNumFiles(numFilesInBlock);

      int numMaterials = numFilesInBlock;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      String[] filenames = new String[numFilesInBlock];
      long[] offsets = new long[numFilesInBlock + 1];
      offsets[numFilesInBlock] = nextBlock;
      for (int i = 0; i < numFilesInBlock; i++) {
        // 32 - Filename (null terminated, filled with nulls and artifacts)
        String filename = fm.readNullString(32);
        FieldValidator.checkFilename(filename);
        filename += ".mtl";
        filenames[i] = filename;

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 4 - Unknown (-1)
        fm.skip(4);

        TaskProgressManager.setValue(offset);
      }

      for (int i = 0; i < numFilesInBlock; i++) {
        String filename = filenames[i];
        long offset = offsets[i];
        long length = offsets[i + 1] - offset;

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(offset);
      }

      fm.seek(nextBlock);

      // 4 - Skip Header (SKIP)
      fm.skip(4);

      // 4 - Block Length
      nextBlock += fm.readInt() + 12;
      FieldValidator.checkOffset(nextBlock, arcSize);

      // 4 - Unknown (-1)
      // 4 - Header (PICL)
      // 4 - Unknown
      fm.skip(12);

      // 4 - Number of Files in this Block
      numFilesInBlock = fm.readInt();
      FieldValidator.checkNumFiles(numFilesInBlock);

      int numPictures = numFilesInBlock;

      // Loop through directory
      filenames = new String[numFilesInBlock];
      offsets = new long[numFilesInBlock + 1];
      offsets[numFilesInBlock] = nextBlock;
      for (int i = 0; i < numFilesInBlock; i++) {
        // 32 - Filename (null terminated, filled with nulls and artifacts)
        String filename = fm.readNullString(32);
        FieldValidator.checkFilename(filename);
        filename += ".pic";
        filenames[i] = filename;

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 4 - Unknown (-1)
        fm.skip(4);

        TaskProgressManager.setValue(offset);
      }

      for (int i = 0; i < numFilesInBlock; i++) {
        String filename = filenames[i];
        long offset = offsets[i];
        long length = offsets[i + 1] - offset;

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(offset);
      }

      fm.seek(nextBlock);

      // 4 - Header (PLT)
      // 4 - Unknown (-1)
      fm.skip(8);

      // 4 - Number of Files in this Block
      numFilesInBlock = fm.readInt();
      FieldValidator.checkNumFiles(numFilesInBlock);

      int numPalettes = numFilesInBlock;

      // Loop through directory
      filenames = new String[numFilesInBlock];
      offsets = new long[numFilesInBlock + 1];
      offsets[numFilesInBlock] = arcSize;
      for (int i = 0; i < numFilesInBlock; i++) {
        // 32 - Filename (null terminated, filled with nulls and artifacts)
        String filename = fm.readNullString(32);
        FieldValidator.checkFilename(filename);
        filename += ".plt";
        filenames[i] = filename;

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 4 - Unknown (-1)
        fm.skip(4);

        TaskProgressManager.setValue(offset);
      }

      for (int i = 0; i < numFilesInBlock; i++) {
        String filename = filenames[i];
        long offset = offsets[i];
        long length = offsets[i + 1] - offset;

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(offset);
      }

      resources = resizeResources(resources, realNumFiles);

      try {
        // Now the offsets[] contains the offsets to the palettes, and numPalettes is the number of color palettes.
        // We also have numMaterials, so lets go through each material file, read the data to work out the dimensions of each PIC
        // and the PLT that is used for the PIC. Store those details against the PIC so that we can display them
        int firstPic = numMaterials;
        int lastPic = firstPic + numPictures;
        for (int i = 0; i < numMaterials; i++) {
          fm.seek(resources[i].getOffset());

          //System.out.println("Reading MTL " + i + " of " + numMaterials + " at " + resources[i].getOffset());

          // 4 - Header (MTDT)
          // 4 - File Length [+12]
          // 4 - Unknown (-1)
          // 32 - Name 1 (null terminated, filled with nulls)
          // 4 - Unknown
          // 32 - Name 2 (null terminated, filled with nulls)
          // 4 - Unknown
          // 4 - Unknown (4)
          // 100 - Unknown
          fm.skip(188);

          // 4 - Number of Pictures
          int numPicsInMaterial = fm.readInt();
          FieldValidator.checkRange(numPicsInMaterial, 0, numPictures);

          // 92 - Unknown
          fm.skip(92);

          for (int p = 0; p < numPicsInMaterial; p++) {
            // 32 - Picture Filename (null terminated, filled with nulls)
            String picFilename = fm.readNullString(32) + ".pic";

            // 4 - Unknown
            fm.skip(4);

            // 4 - Image Width
            int width = fm.readInt();
            FieldValidator.checkWidth(width);

            // 4 - Image Height
            int height = fm.readInt();
            FieldValidator.checkHeight(height);

            // 4 - Bits Per Pixel? (8)
            // 40 - Unknown
            fm.skip(44);

            // 4 - Palette Number?
            int paletteNumber = fm.readInt();
            /*
            if (paletteNumber == -1) {
              // true color
            }
            else {
              FieldValidator.checkRange(paletteNumber, 0, numPalettes);
            }
            */

            // 16 - null
            fm.skip(16);

            // Find the resource that contains this picture
            for (int f = firstPic; f < lastPic; f++) {
              Resource resource = resources[f];
              if (resource.getName().equals(picFilename)) {
                // found the PIC

                // Note the SET (not ADD) because multiple MTL can reference this PIC, but they should all have the same details, so we just overwrite
                // the details with the most recent values.
                resource.setProperty("Width", "" + width);
                resource.setProperty("Height", "" + height);
                resource.setProperty("PaletteID", "" + paletteNumber);

                // Also store the MTL that contains a reference to this PIC, so that if we change the PIC, we can update all the related MTL files.
                // Note it's an ADD, because there can be multiple MTL referencing this PIC
                resource.addProperty("MTLResource", i);

                break;
              }
            }
          }
        }

      }
      catch (Throwable t) {
        logError(t);
      }

      // Now the offsets[] contains the offsets to the palettes, lets load them all into the PaletteManager so we can use them in the PICs
      if (numPalettes > 1) {
        PaletteManager.clear();
      }
      for (int p = 0; p < numPalettes; p++) {
        fm.seek(offsets[p]);
        // 40 - Header Data
        fm.skip(40);

        // 512 - Color Palette
        int[] palette = ImageFormatReader.readRGB565(fm, 256, 1).getImagePixels();
        PaletteManager.addPalette(new Palette(palette));
      }

      //System.out.println(PaletteManager.getNumPalettes());

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
  @SuppressWarnings("unused")
  @Override
  public void replace(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      int currentResource = 0;

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      // 4 - Header (MTLB)
      // 4 - Version (112)
      // 4 - Unknown (-1)
      fm.writeBytes(src.readBytes(12));

      // MATERIAL BLOCK
      // 4 - Header (MTLS)
      fm.writeBytes(src.readBytes(4));

      // 4 - Block Length
      // 4 - Number of Files in this Block
      int srcBlockLength = src.readInt();
      int numMTL = src.readInt();

      // calculate the block length for the numMTL files
      int blockSize = numMTL * 40;
      int endResource = currentResource + numMTL;
      for (int s = currentResource; s < endResource; s++) {
        blockSize += resources[s].getDecompressedLength();
      }

      fm.writeInt(blockSize);
      fm.writeInt(numMTL);

      long srcSkipOffset = src.getOffset() + srcBlockLength;

      // DETAILS DIRECTORY for MATERIAL
      // for each file in the MATERIAL block
      long offset = 24 + (numMTL * 40);
      for (int s = currentResource; s < endResource; s++) {
        // 32 - Filename (null terminated, filled with nulls and artifacts)
        fm.writeBytes(src.readBytes(32));

        // 4 - File Offset
        src.skip(4);
        fm.writeInt(offset);

        // 4 - Unknown (-1)
        fm.writeBytes(src.readBytes(4));

        offset += resources[s].getDecompressedLength();
      }

      // FILE DATA for MATERIAL
      // for each file in the MATERIAL block
      // X - File Data
      for (int s = currentResource; s < endResource; s++) {
        write(resources[s], fm);
      }
      currentResource = endResource;
      src.seek(srcSkipOffset);

      // 4 - Skip Header (SKIP)
      fm.writeBytes(src.readBytes(4));

      offset += 4;

      // PICTURE BLOCK
      // 4 - Block Length (starting from, and including, the PICL Header field)
      // 4 - Unknown (-1)
      // 4 - Header (PICL)
      // 4 - Unknown
      // 4 - Number of Files in this Block
      srcBlockLength = src.readInt();
      int srcUnknown1 = src.readInt();
      long srcPltlOffset = src.getOffset() + srcBlockLength;
      src.skip(4);
      int srcUnknown2 = src.readInt();
      int numPIC = src.readInt();

      // calculate the block length for the numPIC files
      blockSize = 12 + (numPIC * 40);
      endResource = currentResource + numPIC;
      for (int s = currentResource; s < endResource; s++) {
        blockSize += resources[s].getDecompressedLength();
      }

      fm.writeInt(blockSize);
      fm.writeInt(srcUnknown1);
      fm.writeString("PICL");
      fm.writeInt(srcUnknown2);
      fm.writeInt(numPIC);

      // DETAILS DIRECTORY for PICTURE
      // for each file in the PICTURE block
      offset += 20 + (numPIC * 40);
      for (int s = currentResource; s < endResource; s++) {
        // 32 - Filename (null terminated, filled with nulls and artifacts)
        fm.writeBytes(src.readBytes(32));

        // 4 - File Offset
        src.skip(4);
        fm.writeInt(offset);

        // 4 - Unknown (-1)
        fm.writeBytes(src.readBytes(4));

        offset += resources[s].getDecompressedLength();
      }

      // FILE DATA for PICTURE
      // for each file in the PICTURE block
      // X - File Data
      for (int s = currentResource; s < endResource; s++) {
        write(resources[s], fm);
      }
      currentResource = endResource;
      src.seek(srcPltlOffset);

      // 4 - Header (PLTL)
      // 4 - Unknown (-1)
      fm.writeBytes(src.readBytes(8));

      // 4 - Number of Files in this Block
      int numPLT = src.readInt();
      //fm.writeInt(numPLT);

      // We can add more palettes if we change the PICs, so we need the new number here.
      int newNumPLT = PaletteManager.getNumPalettes();
      fm.writeInt(newNumPLT);

      //endResource = currentResource + numPLT;
      endResource = currentResource + newNumPLT;

      // DETAILS DIRECTORY for PALETTE
      // for each file in the PALETTE block
      //offset += 12 + (numPLT * 40);
      offset += 12 + (newNumPLT * 40);
      for (int s = currentResource; s < endResource; s++) {
        // 32 - Filename (null terminated, filled with nulls and artifacts)
        //fm.writeBytes(src.readBytes(32));
        src.skip(32);
        String filename = resources[s].getFilename();
        int filenameLength = filename.length();
        fm.writeString(filename);
        int padding = 32 - filenameLength;
        for (int p = 0; p < padding; p++) {
          fm.writeByte(0);
        }

        // 4 - File Offset
        src.skip(4);
        fm.writeInt(offset);

        // 4 - Unknown (-1)
        //fm.writeBytes(src.readBytes(4));
        src.skip(4);
        fm.writeInt(-1);

        offset += resources[s].getDecompressedLength();
      }

      // FILE DATA for PALETTE
      // for each file in the PALETTE block
      // X - File Data
      for (int s = currentResource; s < endResource; s++) {
        write(resources[s], fm);
      }
      currentResource = endResource;

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

      PreviewPanel imagePreviewPanel = loadFileForConversion(resourceBeingReplaced, fileToReplaceWith, "pic");
      if (imagePreviewPanel == null) {
        // no conversion needed, or wasn't able to be converted
        return fileToReplaceWith;
      }

      // The plugin that will do the conversion
      Viewer_MTT_MTLB_PIC_MTDT converterPlugin = new Viewer_MTT_MTLB_PIC_MTDT();

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
