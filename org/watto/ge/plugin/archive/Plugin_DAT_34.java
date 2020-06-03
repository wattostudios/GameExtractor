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
import org.watto.datatype.Palette;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.resource.Resource_DAT_34;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_34 extends ArchivePlugin {

  /** The color palettes used by the images **/
  static Palette[] palettes = null;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_DAT_34() {

    super("DAT_34", "DAT_34");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Nexus: The Kingdom Of The Winds");
    setExtensions("dat");
    setPlatforms("PC");

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

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // First File Offset
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
   * Performs special reading of the char###.dat archives and returns the list of images
   **********************************************************************************************
   **/
  public Resource[] processCharArchives(File path) {
    try {

      // Find the main char.dat archive, which contains the TBL and PAL files in it
      String sourcePath = path.getParent();

      String charDatPath = sourcePath + File.separator + "char.dat";
      File charDatFile = new File(charDatPath);
      if (!charDatFile.exists()) {
        ErrorLogger.log("[Plugin_DAT_34]: Missing char.dat file");
        return null;
      }

      // Strip off the numbers from the end of the filename
      String partName = FilenameSplitter.getFilename(path);
      while (partName.length() > 2) {
        try {
          Integer.parseInt(partName.substring(partName.length() - 1));
          partName = partName.substring(0, partName.length() - 1);
        }
        catch (Throwable t) {
          break;
        }
      }

      String dscFilename = partName + ".dsc";
      String palFilename = partName + ".pal";

      // Read the char.dat file to get the DSC and PAL files
      FileManipulator fm = new FileManipulator(charDatFile, false);

      long arcSize = (int) fm.getLength();

      // 4 - Number Of Files
      int numFiles = fm.readInt() - 1;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      Resource dscResource = null;
      Resource palResource = null;
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 13 - Filename (null terminated, filled with junk)
        String filename = fm.readNullString(13);
        FieldValidator.checkFilename(filename);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        if (filename.equalsIgnoreCase(dscFilename)) {
          dscResource = resources[i];
        }
        else if (filename.equalsIgnoreCase(palFilename)) {
          palResource = resources[i];
        }

        TaskProgressManager.setValue(i);
      }

      calculateFileSizes(resources, arcSize);

      fm.close();

      if (dscResource == null || palResource == null) {
        ErrorLogger.log("[Plugin_DAT_34]: Missing DSC or PAL resource");
        return null;
      }

      // Now read all the color palettes
      fm = new FileManipulator(charDatFile, false);
      fm.seek(palResource.getOffset());

      arcSize = (int) palResource.getLength();

      // 4 - Number of Color Palettes
      int numPalettes = fm.readInt();
      FieldValidator.checkNumFiles(numPalettes);

      palettes = new Palette[numPalettes];

      TaskProgressManager.setMaximum(numPalettes);

      // Loop through directory
      for (int i = 0; i < numPalettes; i++) {
        // 9 - Header (DLPalette)
        // 15 - Unknown
        fm.skip(24);

        // 1 - Animation Color Count
        int numAnimations = ByteConverter.unsign(fm.readByte());
        // 7 - Unknown
        fm.skip(7);

        // for each Animation Color
        // 2 - Animation Color
        fm.skip(numAnimations * 2);

        int[] colors = new int[256];
        for (int c = 0; c < 256; c++) {
          // 1 - Blue
          int b = ByteConverter.unsign(fm.readByte());
          // 1 - Green
          int g = ByteConverter.unsign(fm.readByte());
          // 1 - Red
          int r = ByteConverter.unsign(fm.readByte());
          // 1 - Padding
          fm.skip(1);
          colors[c] = (255 << 24 | r | g << 8 | b << 16);
        }

        palettes[i] = new Palette(colors);

        TaskProgressManager.setValue(i);
      }

      fm.close();

      // Now read the DSC to find all the tiles and map the color palettes to them
      fm = new FileManipulator(charDatFile, false);
      fm.seek(dscResource.getOffset());

      arcSize = (int) dscResource.getLength();

      // 15 - Header (PartDescription)
      // 7 - null
      // 1 - Unknown (1)
      fm.skip(23);

      // 4 - Number of Parts
      int numParts = fm.readInt();
      FieldValidator.checkNumFiles(numParts);

      // Loop through directory
      int[] paletteNumbers = new int[numParts];
      int[] firstTileInParts = new int[numParts];
      int[] numTilesInParts = new int[numParts];
      for (int i = 0; i < numParts; i++) {
        // 4 - Part ID (incremental from 0)
        fm.skip(4);

        // 4 - Palette ID Number
        int paletteNumber = fm.readInt();
        FieldValidator.checkRange(paletteNumber, 0, numPalettes);
        paletteNumbers[i] = paletteNumber;

        // 4 - First Tile Number for this Part
        int firstTileInPart = fm.readInt();
        firstTileInParts[i] = firstTileInPart;

        // 4 - Number of Tiles for this Part
        int numTilesInPart = fm.readInt();
        numTilesInParts[i] = numTilesInPart;

        // 1 - Unknown (2)
        // 4 - Unknown
        // 1 - Unknown (4)
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(14);

        // 4 - Number of Chunks (12)
        int numChunks = fm.readInt();
        FieldValidator.checkNumFiles(numChunks);

        for (int c = 0; c < numChunks; c++) {
          // 4 - Unknown ID
          // 4 - Unknown (-1/0)
          fm.skip(8);

          // 4 - Number of Blocks (4x4, then 4x2, then 4x6, to make the total of 12 blocks)
          int numBlocks = fm.readInt();
          FieldValidator.checkNumFiles(numBlocks);

          // for each block
          // 1 - Tile Number?
          // 4 - null
          // 4 - Unknown (-1)
          fm.skip(numBlocks * 9);
        }

      }

      fm.close();

      // now work out how many tiles there are...
      int numTiles = firstTileInParts[numParts - 1] + numTilesInParts[numParts - 1];
      FieldValidator.checkNumFiles(numTiles);

      Resource[] tileResources = new Resource[numTiles];

      TaskProgressManager.setMaximum(numTiles);

      // Find the first char###.dat file that contains these tiles
      int sourceNumber = -1;
      int numRemainingInSource = 0;
      File sourceFile = null;
      int outPos = 0;

      // Loop through directory
      while (outPos < numTiles) {

        // find the next source file
        sourceNumber++;
        String sourceFilePath = sourcePath + File.separator + partName + sourceNumber + ".dat";
        sourceFile = new File(sourceFilePath);
        if (!sourceFile.exists()) {
          ErrorLogger.log("[Plugin_DAT_34]: Missing source file " + sourceFilePath);
          return null; // missing one of the source files
        }
        // Open the source file and find the number of tiles in there

        FileManipulator sourceFM = new FileManipulator(sourceFile, false);
        long sourceLength = sourceFM.getLength();

        sourceFM.skip(4);

        // 4 - File Offset
        long offset = sourceFM.readInt();
        FieldValidator.checkOffset(offset, sourceLength);
        sourceFM.seek(offset);

        // 2 - Part Count
        numRemainingInSource = sourceFM.readShort();
        FieldValidator.checkNumFiles(numRemainingInSource);

        sourceFM.skip(6);

        // 4 - Pixel Data Length
        long pixelLength = sourceFM.readInt();
        FieldValidator.checkLength(pixelLength, sourceLength);

        long pixelOffset = sourceFM.getOffset();

        sourceFM.skip(pixelLength);

        for (int t = 0; t < numRemainingInSource && outPos < numTiles; t++) {
          // 2 - Y Position
          int yPos = sourceFM.readShort();
          //if (yPos < 0) {
          yPos = 0 - yPos;
          //}

          // 2 - X Position
          int xPos = sourceFM.readShort();
          //if (xPos < 0) {
          xPos = 0 - xPos;
          //}

          // 2 - Height
          short height = sourceFM.readShort();
          //if (height < 0) {
          //  height = (short) (0 - height);
          //}
          //height -= yPos;
          height += yPos;
          FieldValidator.checkHeight(height + 1); // to allow 0 height

          // 2 - Width
          short width = sourceFM.readShort();
          //if (width < 0) {
          //  width = (short) (0 - width);
          //}
          //width -= xPos;
          width += xPos;
          FieldValidator.checkWidth(width + 1); // to allow 0 width

          // 4 - Pixel Data Offset (relative to the start of the pixel data above)
          long tileOffset = sourceFM.readInt() + pixelOffset;
          FieldValidator.checkLength(tileOffset, sourceLength);

          // 4 - Stencil Data Offset (relative to the start of the pixel data above)
          sourceFM.skip(4);

          tileResources[outPos] = new Resource_DAT_34(sourceFile, partName + " Part " + (outPos + 1) + ".tile", tileOffset, 0, null, width, height);
          tileResources[outPos].forceNotAdded(true);

          // set the length of the previous resource
          if (t != 0) {
            tileResources[outPos - 1].setLength(tileOffset - tileResources[outPos - 1].getOffset());
          }

          TaskProgressManager.setValue(outPos);
          outPos++;
        }

        // set the length of the last resource in this source file
        tileResources[outPos - 1].setLength(pixelLength - tileResources[outPos - 1].getOffset());

        sourceFM.close();

      }

      // Now go through and set the color palettes
      for (int i = 0; i < numParts; i++) {
        int firstTileInPart = firstTileInParts[i];
        int numTilesInPart = numTilesInParts[i];
        int lastTileInPart = firstTileInPart + numTilesInPart;
        Palette palette = palettes[paletteNumbers[i]];

        for (int p = firstTileInPart; p < lastTileInPart; p++) {
          //System.out.println("Setting palette for resource " + p + " of " + numTiles);
          ((Resource_DAT_34) tileResources[p]).setPalette(palette);
        }
      }

      return tileResources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * Performs special reading of the efx###.dat archives and returns the list of images
   **********************************************************************************************
   **/
  public Resource[] processEfxArchives(File path) {
    try {

      // Find the main efx.dat archive, which contains the FRM and PAL files in it
      String sourcePath = path.getParent();

      String efxDatPath = sourcePath + File.separator + "efx.dat";
      File efxDatFile = new File(efxDatPath);
      if (!efxDatFile.exists()) {
        ErrorLogger.log("[Plugin_DAT_34]: Missing efx.dat file");
        return null;
      }

      String frmFilename = "effect.frm";
      String palFilename = "effect.pal";

      // Read the efx.dat file to get the FRM and PAL files
      FileManipulator fm = new FileManipulator(efxDatFile, false);

      long arcSize = (int) fm.getLength();

      // 4 - Number Of Files
      int numFiles = fm.readInt() - 1;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      Resource frmResource = null;
      Resource palResource = null;
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 13 - Filename (null terminated, filled with junk)
        String filename = fm.readNullString(13);
        FieldValidator.checkFilename(filename);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        if (filename.equalsIgnoreCase(frmFilename)) {
          frmResource = resources[i];
        }
        else if (filename.equalsIgnoreCase(palFilename)) {
          palResource = resources[i];
        }

        TaskProgressManager.setValue(i);
      }

      calculateFileSizes(resources, arcSize);

      fm.close();

      if (frmResource == null || palResource == null) {
        ErrorLogger.log("[Plugin_DAT_34]: Missing FRM or PAL resource");
        return null;
      }

      // Now read all the color palettes
      fm = new FileManipulator(efxDatFile, false);
      fm.seek(palResource.getOffset());

      arcSize = (int) palResource.getLength();

      // 4 - Number of Color Palettes
      int numPalettes = fm.readInt();
      FieldValidator.checkNumFiles(numPalettes);

      palettes = new Palette[numPalettes];

      TaskProgressManager.setMaximum(numPalettes);

      // Loop through directory
      for (int i = 0; i < numPalettes; i++) {
        // 9 - Header (DLPalette)
        // 15 - Unknown
        fm.skip(24);

        // 1 - Animation Color Count
        int numAnimations = ByteConverter.unsign(fm.readByte());
        // 7 - Unknown
        fm.skip(7);

        // for each Animation Color
        // 2 - Animation Color
        fm.skip(numAnimations * 2);

        int[] colors = new int[256];
        for (int c = 0; c < 256; c++) {
          // 1 - Blue
          int b = ByteConverter.unsign(fm.readByte());
          // 1 - Green
          int g = ByteConverter.unsign(fm.readByte());
          // 1 - Red
          int r = ByteConverter.unsign(fm.readByte());
          // 1 - Padding
          fm.skip(1);
          colors[c] = (255 << 24 | r | g << 8 | b << 16);
        }

        palettes[i] = new Palette(colors);

        TaskProgressManager.setValue(i);
      }

      fm.close();

      // Now read the FRM to find all the tiles and map the color palettes to them
      fm = new FileManipulator(efxDatFile, false);
      fm.seek(frmResource.getOffset());

      arcSize = (int) frmResource.getLength();

      // 4 - Number of Effects
      int numEffects = fm.readInt();
      FieldValidator.checkNumFiles(numEffects);

      // Loop through directory
      int[] paletteNumbers = new int[numEffects];
      for (int i = 0; i < numEffects; i++) {
        // 4 - Palette Number
        int paletteNumber = fm.readInt();
        paletteNumbers[i] = paletteNumber;
      }

      fm.close();

      Resource[] tileResources = new Resource[numEffects];

      TaskProgressManager.setMaximum(numEffects);

      // Find the first mon###.dat file that contains these tiles
      int sourceNumber = -1;
      int numRemainingInSource = 0;
      File sourceFile = null;
      int outPos = 0;

      // Loop through directory
      while (outPos < numEffects) {

        // find the next source file
        sourceNumber++;
        String sourceFilePath = sourcePath + File.separator + "efx" + sourceNumber + ".dat";
        sourceFile = new File(sourceFilePath);
        if (!sourceFile.exists()) {
          ErrorLogger.log("[Plugin_DAT_34]: Missing source file " + sourceFilePath);
          break;
          //return null; // missing one of the source files
        }
        // Open the source file and find the number of tiles in there

        FileManipulator sourceFM = new FileManipulator(sourceFile, false);
        long sourceLength = sourceFM.getLength();

        sourceFM.skip(4);

        // 4 - File Offset
        long offset = sourceFM.readInt();
        FieldValidator.checkOffset(offset, sourceLength);
        sourceFM.seek(offset);

        // 2 - Part Count
        numRemainingInSource = sourceFM.readShort();
        FieldValidator.checkNumFiles(numRemainingInSource);

        sourceFM.skip(6);

        // 4 - Pixel Data Length
        long pixelLength = sourceFM.readInt();
        FieldValidator.checkLength(pixelLength, sourceLength);

        long pixelOffset = sourceFM.getOffset();

        sourceFM.skip(pixelLength);

        for (int t = 0; t < numRemainingInSource && outPos < numEffects; t++) {
          // 2 - Y Position
          int yPos = sourceFM.readShort();
          //if (yPos < 0) {
          yPos = 0 - yPos;
          //}

          // 2 - X Position
          int xPos = sourceFM.readShort();
          //if (xPos < 0) {
          xPos = 0 - xPos;
          //}

          // 2 - Height
          short height = sourceFM.readShort();
          //if (height < 0) {
          //  height = (short) (0 - height);
          //}
          //height -= yPos;
          height += yPos;
          FieldValidator.checkHeight(height + 1); // to allow 0 height

          // 2 - Width
          short width = sourceFM.readShort();
          //if (width < 0) {
          //  width = (short) (0 - width);
          //}
          //width -= xPos;
          width += xPos;
          FieldValidator.checkWidth(width + 1); // to allow 0 width

          // 4 - Pixel Data Offset (relative to the start of the pixel data above)
          long tileOffset = sourceFM.readInt() + pixelOffset;
          FieldValidator.checkLength(tileOffset, sourceLength);

          // 4 - Stencil Data Offset (relative to the start of the pixel data above)
          sourceFM.skip(4);

          tileResources[outPos] = new Resource_DAT_34(sourceFile, "Effect " + (outPos + 1) + ".tile", tileOffset, 0, palettes[paletteNumbers[outPos]], width, height);
          tileResources[outPos].forceNotAdded(true);

          // set the length of the previous resource
          if (t != 0) {
            tileResources[outPos - 1].setLength(tileOffset - tileResources[outPos - 1].getOffset());
          }

          TaskProgressManager.setValue(outPos);
          outPos++;
        }

        // set the length of the last resource in this source file
        tileResources[outPos - 1].setLength(pixelLength - tileResources[outPos - 1].getOffset());

        sourceFM.close();

      }

      // Shrink the resources array to the right number of tiles
      if (outPos < numEffects) {
        Resource[] oldResources = tileResources;
        tileResources = new Resource[outPos];
        System.arraycopy(oldResources, 0, tileResources, 0, outPos);
        numEffects = outPos;
      }

      return tileResources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * Performs special reading of the mon###.dat archives and returns the list of images
   **********************************************************************************************
   **/
  @SuppressWarnings("unused")
  public Resource[] processMonArchives(File path) {
    try {

      // Find the main mon.dat archive, which contains the DNA and PAL files in it
      String sourcePath = path.getParent();

      String monDatPath = sourcePath + File.separator + "mon.dat";
      File monDatFile = new File(monDatPath);
      if (!monDatFile.exists()) {
        ErrorLogger.log("[Plugin_DAT_34]: Missing mon.dat file");
        return null;
      }

      String dnaFilename = "monster.dna";
      String palFilename = "monster.pal";

      // Read the char.dat file to get the DNA and PAL files
      FileManipulator fm = new FileManipulator(monDatFile, false);

      long arcSize = (int) fm.getLength();

      // 4 - Number Of Files
      int numFiles = fm.readInt() - 1;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      Resource dnaResource = null;
      Resource palResource = null;
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 13 - Filename (null terminated, filled with junk)
        String filename = fm.readNullString(13);
        FieldValidator.checkFilename(filename);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        if (filename.equalsIgnoreCase(dnaFilename)) {
          dnaResource = resources[i];
        }
        else if (filename.equalsIgnoreCase(palFilename)) {
          palResource = resources[i];
        }

        TaskProgressManager.setValue(i);
      }

      calculateFileSizes(resources, arcSize);

      fm.close();

      if (dnaResource == null || palResource == null) {
        ErrorLogger.log("[Plugin_DAT_34]: Missing DNA or PAL resource");
        return null;
      }

      // Now read all the color palettes
      fm = new FileManipulator(monDatFile, false);
      fm.seek(palResource.getOffset());

      arcSize = (int) palResource.getLength();

      // 4 - Number of Color Palettes
      int numPalettes = fm.readInt();
      FieldValidator.checkNumFiles(numPalettes);

      palettes = new Palette[numPalettes];

      TaskProgressManager.setMaximum(numPalettes);

      // Loop through directory
      for (int i = 0; i < numPalettes; i++) {
        // 9 - Header (DLPalette)
        // 15 - Unknown
        fm.skip(24);

        // 1 - Animation Color Count
        int numAnimations = ByteConverter.unsign(fm.readByte());
        // 7 - Unknown
        fm.skip(7);

        // for each Animation Color
        // 2 - Animation Color
        fm.skip(numAnimations * 2);

        int[] colors = new int[256];
        for (int c = 0; c < 256; c++) {
          // 1 - Blue
          int b = ByteConverter.unsign(fm.readByte());
          // 1 - Green
          int g = ByteConverter.unsign(fm.readByte());
          // 1 - Red
          int r = ByteConverter.unsign(fm.readByte());
          // 1 - Padding
          fm.skip(1);
          colors[c] = (255 << 24 | r | g << 8 | b << 16);
        }

        palettes[i] = new Palette(colors);

        TaskProgressManager.setValue(i);
      }

      fm.close();

      // Now read the DNA to find all the tiles and map the color palettes to them
      fm = new FileManipulator(monDatFile, false);
      fm.seek(dnaResource.getOffset());

      arcSize = (int) dnaResource.getLength();

      // 4 - Number of Monsters
      int numMonsters = fm.readInt();
      FieldValidator.checkNumFiles(numMonsters);

      // Loop through directory
      int[] firstTileInParts = new int[numMonsters];
      int[] paletteNumbers = new int[numMonsters];
      for (int i = 0; i < numMonsters; i++) {
        //System.out.println("---------------------------" + fm.getOffset());
        // 4 - First Image ID?
        int firstTileInPart = fm.readInt();
        firstTileInParts[i] = firstTileInPart;

        // 1 - Number of Blocks
        int numBlocks = fm.readByte();
        // 1 - Unknown
        int unknownHeader = fm.readByte();
        // 2 - Palette Number
        short paletteNumber = fm.readShort();
        paletteNumbers[i] = paletteNumber;

        //System.out.println(firstTile + "\t" + numBlocks + "\t" + unknownHeader + "\t" + paletteNumber);

        for (int b = 0; b < numBlocks; b++) {

          // 2 - Number of Chunks
          int numChunks = fm.readShort();
          //System.out.println("  > ");

          for (int c = 0; c < numChunks; c++) {
            // 2 - Unknown ID
            // 2 - Unknown ID
            // 2 - Unknown (-1)
            // 2 - null
            // 1 - null
            fm.skip(9);
            //System.out.println("    > " + fm.readShort() + "\t" + fm.readShort() + "\t" + fm.readShort() + "\t" + fm.readShort() + "\t" + fm.readByte());
          }
        }

      }

      fm.close();

      // now work out how many tiles there are...
      //int numTiles = firstTileInParts[numParts - 1] + numTilesInParts[numParts - 1];
      int largestFirstNum = firstTileInParts[numMonsters - 1];
      int numTiles = firstTileInParts[numMonsters - 1] + 100; // guess
      FieldValidator.checkNumFiles(numTiles);

      Resource[] tileResources = new Resource[numTiles];

      TaskProgressManager.setMaximum(numTiles);

      // Find the first mon###.dat file that contains these tiles
      int sourceNumber = -1;
      int numRemainingInSource = 0;
      File sourceFile = null;
      int outPos = 0;

      // Loop through directory
      while (outPos < numTiles) {

        if (outPos > largestFirstNum) {
          // we've already read the last archive, so quit here
          break;
        }

        // find the next source file
        sourceNumber++;
        String sourceFilePath = sourcePath + File.separator + "mon" + sourceNumber + ".dat";
        sourceFile = new File(sourceFilePath);
        if (!sourceFile.exists()) {
          ErrorLogger.log("[Plugin_DAT_34]: Missing source file " + sourceFilePath);
          return null; // missing one of the source files
        }
        // Open the source file and find the number of tiles in there

        FileManipulator sourceFM = new FileManipulator(sourceFile, false);
        long sourceLength = sourceFM.getLength();

        sourceFM.skip(4);

        // 4 - File Offset
        long offset = sourceFM.readInt();
        FieldValidator.checkOffset(offset, sourceLength);
        sourceFM.seek(offset);

        // 2 - Part Count
        numRemainingInSource = sourceFM.readShort();
        FieldValidator.checkNumFiles(numRemainingInSource);

        sourceFM.skip(6);

        // 4 - Pixel Data Length
        long pixelLength = sourceFM.readInt();
        FieldValidator.checkLength(pixelLength, sourceLength);

        long pixelOffset = sourceFM.getOffset();

        sourceFM.skip(pixelLength);

        for (int t = 0; t < numRemainingInSource && outPos < numTiles; t++) {
          // 2 - Y Position
          int yPos = sourceFM.readShort();
          //if (yPos < 0) {
          yPos = 0 - yPos;
          //}

          // 2 - X Position
          int xPos = sourceFM.readShort();
          //if (xPos < 0) {
          xPos = 0 - xPos;
          //}

          // 2 - Height
          short height = sourceFM.readShort();
          //if (height < 0) {
          //  height = (short) (0 - height);
          //}
          //height -= yPos;
          height += yPos;
          FieldValidator.checkHeight(height + 1); // to allow 0 height

          // 2 - Width
          short width = sourceFM.readShort();
          //if (width < 0) {
          //  width = (short) (0 - width);
          //}
          //width -= xPos;
          width += xPos;
          FieldValidator.checkWidth(width + 1); // to allow 0 width

          // 4 - Pixel Data Offset (relative to the start of the pixel data above)
          long tileOffset = sourceFM.readInt() + pixelOffset;
          FieldValidator.checkLength(tileOffset, sourceLength);

          // 4 - Stencil Data Offset (relative to the start of the pixel data above)
          sourceFM.skip(4);

          tileResources[outPos] = new Resource_DAT_34(sourceFile, "Monster " + (outPos + 1) + ".tile", tileOffset, 0, null, width, height);
          tileResources[outPos].forceNotAdded(true);

          // set the length of the previous resource
          if (t != 0) {
            tileResources[outPos - 1].setLength(tileOffset - tileResources[outPos - 1].getOffset());
          }

          TaskProgressManager.setValue(outPos);
          outPos++;
        }

        // set the length of the last resource in this source file
        tileResources[outPos - 1].setLength(pixelLength - tileResources[outPos - 1].getOffset());

        sourceFM.close();

      }

      // Shrink the resources array to the right number of tiles
      if (outPos < numTiles) {
        Resource[] oldResources = tileResources;
        tileResources = new Resource[outPos];
        System.arraycopy(oldResources, 0, tileResources, 0, outPos);
        numTiles = outPos;
      }

      // Now go through and set the color palettes
      for (int i = 0; i < numMonsters - 1; i++) {
        int firstTileInPart = firstTileInParts[i];
        //int numTilesInPart = numTilesInParts[i];
        //int lastTileInPart = firstTileInPart + numTilesInPart;
        int lastTileInPart = firstTileInParts[i + 1];
        Palette palette = palettes[paletteNumbers[i]];

        for (int p = firstTileInPart; p < lastTileInPart; p++) {
          //System.out.println("Setting palette for resource " + p + " of " + numTiles);
          ((Resource_DAT_34) tileResources[p]).setPalette(palette);
        }
      }

      // The last one
      int firstTileInPart = firstTileInParts[numMonsters - 1];
      //int numTilesInPart = numTilesInParts[i];
      //int lastTileInPart = firstTileInPart + numTilesInPart;
      int lastTileInPart = numTiles;
      Palette palette = palettes[paletteNumbers[numMonsters - 1]];

      for (int p = firstTileInPart; p < lastTileInPart; p++) {
        //System.out.println("Setting palette for resource " + p + " of " + numTiles);
        ((Resource_DAT_34) tileResources[p]).setPalette(palette);
      }

      return tileResources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * Performs special reading of the tile###.dat archives and returns the list of images
   **********************************************************************************************
   **/
  public Resource[] processTileArchives(File path) {
    try {

      // Find the main tile.dat archive, which contains the TBL and PAL files in it
      String sourcePath = path.getParent();

      String tileDatPath = sourcePath + File.separator + "tile.dat";
      File tileDatFile = new File(tileDatPath);
      if (!tileDatFile.exists()) {
        ErrorLogger.log("[Plugin_DAT_34]: Missing tile.dat file");
        return null;
      }

      // Work out if we need the A, B, C, or raw Tile details
      String typePrefix = path.getName().substring(4, 5);
      if (typePrefix.equalsIgnoreCase("a") || typePrefix.equalsIgnoreCase("b") || typePrefix.equalsIgnoreCase("c")) {
        // thats OK
      }
      else {
        // ignore - grab the root type
        typePrefix = "";
      }

      String tblFilename = "tile" + typePrefix + ".tbl";
      String palFilename = "tile" + typePrefix + ".pal";

      // Read the tile.dat file to get the TBL and PAL files
      FileManipulator fm = new FileManipulator(tileDatFile, false);

      long arcSize = (int) fm.getLength();

      // 4 - Number Of Files
      int numFiles = fm.readInt() - 1;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      Resource tblResource = null;
      Resource palResource = null;
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 13 - Filename (null terminated, filled with junk)
        String filename = fm.readNullString(13);
        FieldValidator.checkFilename(filename);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        if (filename.equalsIgnoreCase(tblFilename)) {
          tblResource = resources[i];
        }
        else if (filename.equalsIgnoreCase(palFilename)) {
          palResource = resources[i];
        }

        TaskProgressManager.setValue(i);
      }

      calculateFileSizes(resources, arcSize);

      fm.close();

      if (tblResource == null || palResource == null) {
        ErrorLogger.log("[Plugin_DAT_34]: Missing TBL or PAL resource");
        return null;
      }

      // Now read all the color palettes
      fm = new FileManipulator(tileDatFile, false);
      fm.seek(palResource.getOffset());

      arcSize = (int) palResource.getLength();

      // 4 - Number of Color Palettes
      int numPalettes = fm.readInt();
      FieldValidator.checkNumFiles(numPalettes);

      palettes = new Palette[numPalettes];

      TaskProgressManager.setMaximum(numPalettes);

      // Loop through directory
      for (int i = 0; i < numPalettes; i++) {
        // 9 - Header (DLPalette)
        // 15 - Unknown
        fm.skip(24);

        // 1 - Animation Color Count
        int numAnimations = ByteConverter.unsign(fm.readByte());
        // 7 - Unknown
        fm.skip(7);

        // for each Animation Color
        // 2 - Animation Color
        fm.skip(numAnimations * 2);

        int[] colors = new int[256];
        for (int c = 0; c < 256; c++) {
          // 1 - Blue
          int b = ByteConverter.unsign(fm.readByte());
          // 1 - Green
          int g = ByteConverter.unsign(fm.readByte());
          // 1 - Red
          int r = ByteConverter.unsign(fm.readByte());
          // 1 - Padding
          fm.skip(1);
          colors[c] = (255 << 24 | r | g << 8 | b << 16);
        }

        palettes[i] = new Palette(colors);

        TaskProgressManager.setValue(i);
      }

      fm.close();

      // Now read the TBL to find all the tiles and map the color palettes to them
      fm = new FileManipulator(tileDatFile, false);
      fm.seek(tblResource.getOffset());

      arcSize = (int) tblResource.getLength();

      // 4 - Number of Tiles
      int numTiles = fm.readInt();
      FieldValidator.checkNumFiles(numTiles);

      Resource[] tileResources = new Resource[numTiles];

      TaskProgressManager.setMaximum(numTiles);

      // Find the first tile###.dat file that contains these tiles
      int sourceNumber = -1;
      int numRemainingInSource = 0;
      File sourceFile = null;
      long[] pixelOffsets = new long[0];
      long[] pixelLengths = new long[0];
      short[] widths = new short[0];
      short[] heights = new short[0];
      int readPixelPos = 0;

      // Loop through directory
      for (int i = 0; i < numTiles; i++) {
        // See if we need to load the next Source file
        if (numRemainingInSource <= 0) {
          // yep, find the next source file
          sourceNumber++;
          String sourceFilePath = sourcePath + File.separator + "tile" + typePrefix + sourceNumber + ".dat";
          sourceFile = new File(sourceFilePath);
          if (!sourceFile.exists()) {
            ErrorLogger.log("[Plugin_DAT_34]: Missing source file " + sourceFilePath);
            return null; // missing one of the source files
          }
          // Open the source file and find the number of tiles in there

          FileManipulator sourceFM = new FileManipulator(sourceFile, false);
          long sourceLength = sourceFM.getLength();

          sourceFM.skip(4);

          // 4 - File Offset
          long offset = sourceFM.readInt();
          FieldValidator.checkOffset(offset, sourceLength);
          sourceFM.seek(offset);

          // 2 - Tile Count
          numRemainingInSource = sourceFM.readShort();
          FieldValidator.checkNumFiles(numRemainingInSource);

          sourceFM.skip(6);

          // 4 - Pixel Data Length
          long pixelLength = sourceFM.readInt();
          FieldValidator.checkLength(pixelLength, sourceLength);

          long pixelOffset = sourceFM.getOffset();

          sourceFM.skip(pixelLength);

          pixelOffsets = new long[numRemainingInSource];
          pixelLengths = new long[numRemainingInSource];
          heights = new short[numRemainingInSource];
          widths = new short[numRemainingInSource];
          for (int t = 0; t < numRemainingInSource; t++) {
            // 2 - Y Position
            short yPos = sourceFM.readShort();

            // 2 - X Position
            short xPos = sourceFM.readShort();

            // 2 - Height
            short height = (short) (sourceFM.readShort() - yPos);
            FieldValidator.checkHeight(height + 1); // to allow 0 height
            heights[t] = height;

            // 2 - Width
            short width = (short) (sourceFM.readShort() - xPos);
            FieldValidator.checkWidth(width + 1); // to allow 0 width
            widths[t] = width;

            // 4 - Pixel Data Offset (relative to the start of the pixel data above)
            long tileOffset = sourceFM.readInt() + pixelOffset;
            FieldValidator.checkLength(tileOffset, sourceLength);
            pixelOffsets[t] = tileOffset;

            // 4 - Stencil Data Offset (relative to the start of the pixel data above)
            sourceFM.skip(4);
          }

          sourceFM.close();

          for (int t = 0; t < numRemainingInSource - 1; t++) {
            pixelLengths[t] = pixelOffsets[t + 1] - pixelOffsets[t];
          }
          pixelLengths[numRemainingInSource - 1] = pixelLength - pixelOffsets[numRemainingInSource - 1];

          readPixelPos = 0;
        }

        // 2 - Color Palette Number (need to exclude the top bit of the second byte ???)
        short paletteNumber = fm.readShort();
        if (paletteNumber < 0) {
          paletteNumber &= 32767;
        }
        FieldValidator.checkRange(paletteNumber, 0, numPalettes);

        tileResources[i] = new Resource_DAT_34(sourceFile, "Tile " + (i + 1) + ".tile", pixelOffsets[readPixelPos], pixelLengths[readPixelPos], palettes[paletteNumber], widths[readPixelPos], heights[readPixelPos]);
        tileResources[i].forceNotAdded(true);

        numRemainingInSource--;
        readPixelPos++;

        TaskProgressManager.setValue(i);
      }

      fm.close();

      return tileResources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
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

      //
      // Handle special archives from this game
      //
      String sourceName = path.getName().toLowerCase();
      if (FilenameSplitter.getExtension(sourceName).equals("dat") && sourceName.length() >= 7) {
        String name3 = sourceName.substring(0, 3);
        String name4 = sourceName.substring(0, 4);
        String name5 = sourceName.substring(0, 5);
        String name6 = sourceName.substring(0, 6);
        String name7 = sourceName.substring(0, 7);
        if (name4.equals("tile")) {
          Resource[] processedResources = processTileArchives(path); // TILE
          if (processedResources != null && processedResources.length > 0) {
            return processedResources;
          }
        }
        else if (/*name3.equals("all") || */name5.equals("arrow") || /*name4.equals("back") ||*/ name4.equals("body") || name3.equals("bow") || name4.equals("coat") || name7.equals("emotion") || name4.equals("face") || name7.equals("facedec") || name3.equals("fan") || name4.equals("hair") || name7.equals("hairdec") || name6.equals("helmet") || name6.equals("mantle") || name4.equals("neck") || name6.equals("shield") || name5.equals("shoes") || name5.equals("spear") || name5.equals("sword")) {
          Resource[] processedResources = processCharArchives(path); // CHAR ("all" and "back" are empty archives)
          if (processedResources != null && processedResources.length > 0) {
            return processedResources;
          }
        }
        else if (name3.equals("mon")) {
          Resource[] processedResources = processMonArchives(path); // MON
          if (processedResources != null && processedResources.length > 0) {
            return processedResources;
          }
        }
        else if (name3.equals("efx")) {
          Resource[] processedResources = processEfxArchives(path); // EFX
          if (processedResources != null && processedResources.length > 0) {
            return processedResources;
          }
        }
      }

      //
      // Otherwise handle as a generic archive of this type
      //
      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Number Of Files
      int numFiles = fm.readInt() - 1;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 13 - Filename (null terminated, filled with junk)
        String filename = fm.readNullString(13);
        FieldValidator.checkFilename(filename);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      calculateFileSizes(resources, arcSize);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
