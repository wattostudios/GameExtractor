/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.datatype.FileType;
import org.watto.datatype.Palette;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ZBD extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ZBD() {

    super("ZBD", "Zipper Interactive ZBD");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("zbd");
    setGames("Recoil",
        "Mech Warrior 3");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("zbd_tex", "Texture Image", FileType.TYPE_IMAGE),
        new FileType("zbd_mdl", "Model", FileType.TYPE_MODEL));

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
      if (fm.readInt() == 0) {
        rating += 15;
      }

      // Version
      int version = fm.readInt();
      if (version == 1 || version == 7 || version == 15) {
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);
      // 4 - null
      fm.skip(4);

      // 4 - Version
      int version = fm.readInt();
      //System.out.println(version);

      Resource[] success = new Resource[0];
      if (version == 1) {
        success = read1(fm, path);
      }
      else if (version == 2) {
        //success = read2(fm,path);
      }
      else if (version == 7) {
        success = read7(fm, path);
      }
      else if (version == 15) {
        success = read15(fm, path);
      }
      else if (version == 28) {
        //success = read28(fm,path);
      }

      fm.close();

      return success;

    }
    catch (Throwable t) {
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource[] read1(FileManipulator fm, File path) throws Exception {

    // 4 - Number of Color Palettes
    int numPalettes = fm.readInt();

    // 4 - Number of Files
    int numFiles = fm.readInt();
    FieldValidator.checkNumFiles(numFiles);

    // 8 - null
    fm.skip(8);

    long arcSize = fm.getLength();

    Resource[] resources = new Resource[numFiles];
    TaskProgressManager.setMaximum(numFiles);

    for (int i = 0; i < numFiles; i++) {

      // 32 - Filename (null)
      String filename = fm.readNullString(32) + ".zbd_tex";
      FieldValidator.checkFilename(filename);

      // 4 - File Offset
      int offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);

      // 4 - Color Palette ID
      int colorPaletteID = fm.readInt();

      //path,id,name,offset,length,decompLength,exporter
      Resource resource = new Resource(path, filename, offset);
      resource.addProperty("ColorPaletteID", colorPaletteID);
      resources[i] = resource;

      TaskProgressManager.setValue(i);
    }

    // color palettes
    PaletteManager.clear();

    for (int i = 0; i < numPalettes; i++) {
      // 512 - Color Palette (256 colors * RGB565 format)
      Palette palette = new Palette(ImageFormatReader.readRGB565(fm, 256, 1).getPixels());
      PaletteManager.addPalette(palette);
    }

    fm.close();

    calculateFileSizes(resources, arcSize);

    return resources;

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource[] read15(FileManipulator fm, File path) throws Exception {

    long arcSize = path.length();

    // 4 - Number of Entries in Directory 1
    // 4 - Directory 1 Offset (36)
    // 4 - Directory 2 Offset
    fm.skip(12);

    // 4 - Directory 3 Offset
    int dir3Offset = fm.readInt();
    FieldValidator.checkOffset(dir3Offset, arcSize);

    // 4 - Number of Entries in Directory 4 (including Padding)
    fm.skip(4);

    // 4 - Number of Entries in Directory 4 (not including Padding)
    int num4Files = fm.readInt();
    FieldValidator.checkNumFiles(num4Files);

    // 4 - Directory 4 Offset
    int dir4Offset = fm.readInt();
    FieldValidator.checkOffset(dir4Offset, arcSize);

    fm.seek(dir3Offset);

    // 4 - Number of Entries in Directory 3 (including Padding)
    fm.skip(4);

    // 4 - Number of Entries in Directory 3 (not including Padding)
    int num3Files = fm.readInt();
    FieldValidator.checkNumFiles(num3Files);

    // 4 - Number of Entries in Directory 3 (not including Padding)
    fm.skip(4);

    int numFilesTotal = num3Files + num4Files;
    int realNumFiles = 0;

    Resource[] resources = new Resource[numFilesTotal];
    TaskProgressManager.setMaximum(numFilesTotal);

    for (int i = 0; i < num3Files; i++) {
      // 4 - null
      // 4 - Unknown (2)
      // 4 - Unknown (12)
      fm.skip(12);

      // 4 - Number of Faces?
      int numFaces = fm.readInt();
      //FieldValidator.checkNumFaces(numFaces);

      // 4 - Number of Points?
      int numVertices = fm.readInt();
      //FieldValidator.checkNumVertices(numVertices);

      // 4 - Number of Normals? (can be null)
      int numNormals = fm.readInt();

      // 4 - Another Number of Normals? (can be null)
      int numNormals2 = fm.readInt();

      // 4 - Blocks of 76 + 12 (can be null)
      int block76 = fm.readInt();

      // 16 - null
      // 8 - Unknown
      // 12 - null
      // 8 - Unknown
      // 4 - null
      // 4 - Unknown
      fm.skip(52);

      // 4 - File Offset
      int offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);

      String filename = Resource.generateFilename(realNumFiles) + ".zbd_mdl";

      //path,id,name,offset,length,decompLength,exporter
      Resource resource = new Resource(path, filename, offset);
      resource.addProperty("VertexCount", numVertices);
      resource.addProperty("FaceCount", numFaces);
      resource.addProperty("NormalCount", numNormals);
      resource.addProperty("Normal2Count", numNormals2);
      resource.addProperty("Block76Count", block76);
      resources[realNumFiles] = resource;
      realNumFiles++;

      TaskProgressManager.setValue(realNumFiles);
    }

    // set file sizes for dir3 files
    for (int i = 0; i < num3Files - 1; i++) {
      Resource currentResource = resources[i];
      int length = (int) (resources[i + 1].getOffset() - currentResource.getOffset());
      currentResource.setLength(length);
      currentResource.setDecompressedLength(length);
    }
    int lastLength = (int) (dir4Offset - resources[num3Files - 1].getOffset());
    resources[num3Files - 1].setLength(lastLength);
    resources[num3Files - 1].setDecompressedLength(lastLength);

    fm.seek(dir4Offset);

    for (int i = 0; i < num4Files; i++) {
      // 36 - Filename (null terminated, filled with nulls and junk)
      String filename = fm.readNullString(36);

      if (filename == null || filename.length() <= 0) {
        filename = Resource.generateFilename(realNumFiles);
      }

      // 4 - Unknown
      // 4 - null
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown (-1)
      // 4 - null
      // 4 - Unknown (1)
      // 4 - null
      // 4 - Unknown (-1)
      // 4 - Unknown (-1)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 16 - null
      // 24 - Unknown
      // 24 - null
      // 16 - Unknown
      // 4 - null
      fm.skip(156);

      // 4 - File Offset
      int offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);

      //path,id,name,offset,length,decompLength,exporter
      resources[realNumFiles] = new Resource(path, filename, offset);
      realNumFiles++;

      TaskProgressManager.setValue(realNumFiles);
    }

    // set file sizes for dir4 files
    for (int i = num3Files; i < numFilesTotal - 1; i++) {
      Resource currentResource = resources[i];
      int length = (int) (resources[i + 1].getOffset() - currentResource.getOffset());
      currentResource.setLength(length);
      currentResource.setDecompressedLength(length);
    }
    lastLength = (int) (arcSize - resources[numFilesTotal - 1].getOffset());
    resources[numFilesTotal - 1].setLength(lastLength);
    resources[numFilesTotal - 1].setDecompressedLength(lastLength);

    fm.close();

    return resources;

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource[] read7(FileManipulator fm, File path) throws Exception {

    // 4 - numFiles
    int numFiles = fm.readInt();
    FieldValidator.checkNumFiles(numFiles);

    Resource[] resources = new Resource[numFiles];

    long offset = 12 + (128 * numFiles);
    for (int i = 0; i < numFiles; i++) {
      // 120 - Filename (null)
      String filename = fm.readNullString(120);

      // 4 - Unknown
      fm.skip(4);

      // 4 - File Length
      long length = fm.readInt();

      //path,id,name,offset,length,decompLength,exporter
      resources[i] = new Resource(path, filename, offset, length);

      TaskProgressManager.setValue(offset);

      offset += length;
    }

    fm.close();

    return resources;

  }

}