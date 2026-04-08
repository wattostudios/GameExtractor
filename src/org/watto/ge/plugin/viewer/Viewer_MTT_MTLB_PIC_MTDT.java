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

package org.watto.ge.plugin.viewer;

import java.awt.Image;
import java.io.File;

import org.watto.Settings;
import org.watto.SingletonManager;
import org.watto.component.ComponentRepository;
import org.watto.component.FileListPanel;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.component.WSFileListPanelHolder;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Palette;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageFormatWriter;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_MTT_MTLB;
import org.watto.ge.plugin.resource.Resource_Property;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_MTT_MTLB_PIC_MTDT extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_MTT_MTLB_PIC_MTDT() {
    super("MTT_MTLB_PIC_MTDT", "MTT_MTLB_PIC_MTDT Image");
    setExtensions("pic");

    setGames("Superbike World Championship");
    setPlatforms("PC");
    setStandardFileFormat(false);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canReplace(PreviewPanel panel) {
    if (panel instanceof PreviewPanel_Image) {
      return true;
    }
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_MTT_MTLB) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      // 4 - Header
      if (fm.readString(4).equals("MTDT")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  Reads a resource from the FileManipulator, and generates a PreviewPanel for it. The FileManipulator
  is an extracted temp file, not the original archive!
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {
    try {

      ImageResource imageResource = readThumbnail(fm);

      if (imageResource == null) {
        return null;
      }

      PreviewPanel_Image preview = new PreviewPanel_Image(imageResource);

      return preview;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  Reads a resource from the FileManipulator, and generates a Thumbnail for it (generally, only
  an Image ViewerPlugin will do this, but others can do it if they want). The FileManipulator is
  an extracted temp file, not the original archive!
  **********************************************************************************************
  **/

  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      // 4 - Header (MTDT)
      // 4 - File Length [+12]
      // 4 - Unknown (-1)
      fm.skip(12);

      // Find the palette and image dimensions
      int height = 0;
      int width = 0;
      int paletteID = -1;

      // get the width/height from the properties of the image resource, which were read by the ArchivePlugin
      Object resourceObject = SingletonManager.get("CurrentResource");
      if (resourceObject == null || !(resourceObject instanceof Resource)) {
        return null;
      }
      Resource resource = (Resource) resourceObject;

      try {
        height = Integer.parseInt(resource.getProperty("Height"));
        width = Integer.parseInt(resource.getProperty("Width"));
        paletteID = Integer.parseInt(resource.getProperty("PaletteID"));
      }
      catch (Throwable t) {
        //
      }

      if (width == 0 || height == 0) {
        return null;
      }

      // X - Pixels
      ImageResource imageResource = null;

      if (paletteID == -1) {
        // RGBA pixels
        //System.out.println("true color");
        imageResource = ImageFormatReader.readBGRA(fm, width, height);
      }
      else {
        // 8-bit paletted
        int numPalettes = PaletteManager.getNumPalettes();
        if (paletteID < 0 || paletteID >= numPalettes) {
          return null; // invalid palette number
        }
        int[] palette = PaletteManager.getPalette(paletteID).getPalette();
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
      }

      fm.close();

      return imageResource;

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
  @Override
  public void write(PreviewPanel preview, FileManipulator fm) {
  }

  /**
  **********************************************************************************************
  We can't WRITE these files from scratch, but we can REPLACE some of the images with new content  
  **********************************************************************************************
  **/
  public void replace(Resource resourceBeingReplaced, PreviewPanel preview, FileManipulator fm) {
    try {

      if (!(preview instanceof PreviewPanel_Image)) {
        return;
      }

      PreviewPanel_Image ivp = (PreviewPanel_Image) preview;
      Image image = ivp.getImage();
      int width = ivp.getImageWidth();
      int height = ivp.getImageHeight();

      if (width == -1 || height == -1) {
        return;
      }

      // see if the image was true-color or paletted. Try to build the new image to be the same.
      int paletteID = Integer.parseInt(resourceBeingReplaced.getProperty("PaletteID"));

      // Try to get the existing ImageResource (if it was stored), otherwise build a new one
      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();
      if (imageResource == null) {
        imageResource = new ImageResource(image, width, height);
      }

      if (paletteID == -1) {
        // RGBA, no palette

        int pixelLength = width * height * 4;

        // 4 - Header (MTDT)
        fm.writeString("MTDT");

        // 4 - File Length [+12]
        fm.writeInt(pixelLength);

        // 4 - Unknown (-1)
        fm.writeInt(-1);

        // X - Pixels (BGRA)
        ImageFormatWriter.writeBGRA(fm, imageResource);

        fm.close();
      }
      else {
        // store the image as a paletted image.
        // after creating the new image, we will create a new palette file so that we don't overwrite the existing ones (which might be shared with multiple PICs)

        int pixelLength = width * height;

        // 4 - Header (MTDT)
        fm.writeString("MTDT");

        // 4 - File Length [+12]
        fm.writeInt(pixelLength);

        // 4 - Unknown (-1)
        fm.writeInt(-1);

        // X - Pixels (paletted)
        ImageManipulator im = new ImageManipulator(imageResource);
        im.convertToPaletted(); // NOTE: This adds the palette to the PaletteManager automatically
        im.changeColorCount(256);

        byte[] pixels = im.getPixelBytes();

        fm.writeBytes(pixels);

        fm.close();

        // Now create the new palette file
        int[] palette = im.getPalette();

        // work out the new palette filename
        paletteID = PaletteManager.getNumPalettes() - 1; // need to -1 because the palette was already automatically added by convertToPaletted();
        String paletteFilename = "pal" + paletteID;

        // Write out the temporary file
        File newPaletteFile = new File(Settings.get("TempDirectory") + File.separatorChar + "new_palettes" + File.separatorChar + paletteFilename + "." + System.currentTimeMillis());
        fm = new FileManipulator(newPaletteFile, true);

        // 4 - Header (MTDT)
        fm.writeString("MTDT");

        // 4 - File Length [+12]
        fm.writeInt(540);

        // 4 - Unknown (-1)
        fm.writeInt(-1);

        // 4 - Number of Palettes? (1)
        fm.writeInt(1);

        // 4 - Max Number of Colors? (256)
        fm.writeInt(256);

        // 4 - Unknown
        fm.writeInt(29618188);

        // 8 - null
        fm.writeInt(0);
        fm.writeInt(0);

        // 4 - Number of Bytes used to store the actual number of colors in the palette
        fm.writeInt(256 * 2);

        // 4 - null
        fm.writeInt(0);

        // X - Palette (RGB565)
        //ImageFormatWriter.writeRGB565(fm, palette);
        for (int i = 0; i < 256; i++) {
          // INPUT = ARGB
          int pixel = palette[i];

          // 5bits - Blue
          // 6bits - Green
          // 5bits - Red
          int r = (((((pixel >> 16) & 255) / 8) << 3) & 248);
          int g1 = (((((pixel >> 8) & 255) / 4) >> 3) & 7);
          int byte2 = g1 | r;

          int g2 = (((((pixel >> 8) & 255) / 4) << 5) & 224);
          int b = ((((pixel) & 255) / 8) & 31);
          int byte1 = g2 | b;

          // OUTPUT = RGB565
          fm.writeByte(byte1);
          fm.writeByte(byte2);
        }

        fm.close();

        // finally, add the palette to the PaletteManager (it was already added by convertToPaletted() above, so need to *replace* this palette with the new one)
        PaletteManager.replacePalette(paletteID, new Palette(palette));

        // Now add a Resource for the Palette, to the end of the Archive
        //path,name,offset,length,decompLength,exporter
        Archive.addResource(new Resource(newPaletteFile, paletteFilename + ".plt", 0, 552));

        ((FileListPanel) ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getCurrentPanel()).reload(); // so the palette file is added to the end of the file list display

      }

      // Now update the properties on the PIC
      resourceBeingReplaced.setProperty("Width", "" + width);
      resourceBeingReplaced.setProperty("Height", "" + height);
      resourceBeingReplaced.setProperty("PaletteID", "" + paletteID);

      // NOW WE NEED TO UPDATE ALL THE MTLs (that reference the PIC) TO HAVE THE NEW VALUES.
      String thisPicName = resourceBeingReplaced.getName();

      // Get all the MTL resource numbers
      Resource_Property[] properties = resourceBeingReplaced.getProperties();
      int numProperties = properties.length;
      for (int i = 0; i < numProperties; i++) {
        Resource_Property property = properties[i];
        if (property.getCode().equals("MTLResource")) {
          int mtlID = Integer.parseInt(property.getValue());

          Resource mtlResource = Archive.getResource(mtlID);

          //System.out.println("Updating MTL file " + mtlResource.getName());

          // Read the existing MTL file into a buffer (it's small)
          byte[] mtlBytes = new byte[(int) mtlResource.getDecompressedLength()];
          FileManipulator mtlIn = new FileManipulator(new ByteBuffer(mtlBytes));
          mtlResource.extract(mtlIn);
          mtlIn.seek(0);

          // Build a new temporary MTL file for holding the edited file
          File replacedMTLFile = new File(Settings.get("TempDirectory") + File.separatorChar + "mtl_replaced" + File.separatorChar + mtlResource.getName() + "." + System.currentTimeMillis());

          // Open the temporary file, and update anything that references this PIC
          FileManipulator mtlFM = new FileManipulator(replacedMTLFile, true);

          // 4 - Header (MTDT)
          // 4 - File Length [+12]
          // 4 - Unknown (-1)
          // 32 - Name 1 (null terminated, filled with nulls)
          // 4 - Unknown
          // 32 - Name 2 (null terminated, filled with nulls)
          // 4 - Unknown
          // 4 - Unknown (4)
          // 100 - Unknown
          mtlFM.writeBytes(mtlIn.readBytes(188));

          // 4 - Number of Pictures
          int numPicsInMaterial = mtlIn.readInt();
          FieldValidator.checkRange(numPicsInMaterial, 0, 1000); // guess max
          mtlFM.writeInt(numPicsInMaterial);

          // 92 - Unknown
          mtlFM.writeBytes(mtlIn.readBytes(92));

          for (int p = 0; p < numPicsInMaterial; p++) {
            // 32 - Picture Filename (null terminated, filled with nulls)
            String picFilename = mtlIn.readNullString(32) + ".pic";
            mtlIn.relativeSeek(mtlIn.getOffset() - 32);
            mtlFM.writeBytes(mtlIn.readBytes(32));

            if (picFilename.equals(thisPicName)) {
              // found a PIC to update in the MTL
              //System.out.println("  Updating PIC file " + picFilename);

              // 4 - Unknown
              mtlFM.writeBytes(mtlIn.readBytes(4));

              // 4 - Image Width
              mtlFM.writeInt(width);
              mtlIn.skip(4);

              // 4 - Image Height
              mtlFM.writeInt(height);
              mtlIn.skip(4);

              // 4 - Bits Per Pixel? (8)
              // 40 - Unknown
              mtlFM.writeBytes(mtlIn.readBytes(44));

              // 4 - Palette Number? (-1 for true color)
              mtlFM.writeInt(paletteID);
              mtlIn.skip(4);

              // 16 - null
              mtlFM.writeBytes(mtlIn.readBytes(16));
            }
            else {
              // a different PIC - just skip over the details, no change needed
              //System.out.println("  Skipping PIC file " + picFilename);

              // 4 - Unknown
              // 4 - Image Width
              // 4 - Image Height
              // 4 - Bits Per Pixel? (8)
              // 40 - Unknown
              // 4 - Palette Number?
              // 16 - null
              mtlFM.writeBytes(mtlIn.readBytes(76));
            }

          }

          mtlIn.close();
          mtlFM.close();

          // Now update the resource to point to the temporary file we just built
          mtlResource.setExportedPath(replacedMTLFile);
          mtlResource.updatePropertiesFromExportFile();

        }
      }

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}