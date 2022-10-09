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

package org.watto.ge.plugin.viewer;

import org.watto.ErrorLogger;
import org.watto.SingletonManager;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Palette;
import org.watto.datatype.PalettedImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_HQR;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_HQR_HQRTEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_HQR_HQRTEX() {
    super("HQR_HQRTEX", "Little Big Adventure 2 HQR_TEX Image");
    setExtensions("hqr_tex");

    setGames("Little Big Adventure 2");
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
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_HQR) {
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

      return rating;

    }
    catch (

    Throwable t) {
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
  Extracts a PALT resource and then gets the Palette from it
  **********************************************************************************************
  **/
  public int[] extractPalette(Resource paltResource) {
    try {
      ByteBuffer buffer = new ByteBuffer((int) paltResource.getLength());
      FileManipulator fm = new FileManipulator(buffer);
      paltResource.extract(fm);

      fm.seek(0); // back to the beginning of the byte array

      int numColors = (int) fm.getLength() / 3;
      int[] palette = new int[numColors];
      for (int i = 0; i < numColors; i++) {
        // 3 - RGB
        int rPixel = ByteConverter.unsign(fm.readByte());
        int gPixel = ByteConverter.unsign(fm.readByte());
        int bPixel = ByteConverter.unsign(fm.readByte());
        int aPixel = 255;

        palette[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
      }

      fm.close();

      return palette;
    }
    catch (Throwable t) {
      logError(t);
      return new int[0];
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

      long arcSize = fm.getLength();

      int width = 0;
      int height = 0;
      if (arcSize == 307200) {
        width = 640;
        height = 480;
      }
      else if (arcSize == 65536) {
        width = 256;
        height = 256;
      }
      else {
        return null;
      }

      // Get the Palette.
      int[] palette = null;

      // The correct palette for this image is the next Resource in the archive (SCREENS.HQR)
      Object resourceObject = SingletonManager.get("CurrentResource");
      if (resourceObject == null || !(resourceObject instanceof Resource)) {
        return null;
      }
      Resource resource = (Resource) resourceObject;

      Resource[] resources = Archive.getResources();
      int numResources = resources.length;

      try {
        // the palette file ID is hopefully stored on the Resource (SCREENS.HQR)
        int paletteID = Integer.parseInt(resource.getProperty("PaletteID"));
        palette = extractPalette(resources[paletteID]);
      }
      catch (Throwable t) {
      }

      boolean allowPaletteChoice = false;
      if (palette == null || palette.length <= 0) {
        // Find all the palettes and extract them
        if (PaletteManager.getNumPalettes() <= 0) {
          for (int i = 0; i < numResources; i++) {
            Resource currentResource = resources[i];
            if (currentResource.getDecompressedLength() == 768) {
              // found a palette
              palette = extractPalette(currentResource);
              PaletteManager.addPalette(new Palette(palette));
            }
          }
        }

        if (PaletteManager.getNumPalettes() <= 0) {
          // load the default color palette (from RESS.HQR file #1)
          palette = new int[] { -16777216, -8453921, -8453921, -8453921, -8453921, -8453921, -8453921, -8453921, -8453921, -8453921, -8453921, -8453921, -8453921, -8453921, -8453921, -1, -14477561, -13952249, -13426933, -12900593, -12375277, -11848937, -11323621, -10797277, -10271961, -9746645, -8958157, -8431813, -8168641, -7643321, -7380149, -7115953, -6590633, -6327461, -5801121, -5275801, -5011605, -4486289, -3959949, -3433605, -3168381, -2641013, -2112617, -1848413, -1320021, -1055817, -528445, -1073, -15263981, -14737637, -13948121, -13158609, -12369093, -11579581, -11053233, -10263717, -9474201, -8684685, -7368825, -5789793, -4473929, -2894901, -1579037, -1, -13697024, -12648448, -11599872, -10551296, -9502720, -8454144, -7665913, -6876401, -5825765, -5035225, -4244681, -3453109, -2660509, -1868937, -1075309, -19537, -13957369, -12645621, -11331821, -9756905, -8443105, -7128277, -5552333, -4237505, -2921653, -1343657, -27801, -284805, -279661, -274517, -270401, -267305, -14213357, -13423849, -12372193, -11320537, -10268885, -9217233, -8165577, -6588609, -5010617, -3433653, -1855661, -541865, -538769, -535669, -533597, -530497, -15260905, -14996709, -14470365, -13944025, -13416657, -12890317, -12101833, -11575489, -10787005, -9997493, -8944809, -7628953, -6575237, -5259381, -3943525, -2627665, -16310517, -16309489, -16045293, -15781097, -15516901, -15252705, -14988509, -14461141, -13670601, -12616889, -11563177, -10247317, -8931457, -7615597, -6036565, -4196413, -16770277, -16768221, -16766165, -16764113, -16303305, -16301249, -16036021, -15508653, -14980257, -14190745, -13138061, -12872837, -12343413, -11813989, -11284565, -10755141, -16772325, -16771293, -16769237, -16766157, -16764097, -16303289, -16300205, -16034977, -15769749, -15504521, -14976125, -13659237, -12079181, -10236981, -8133661, -5505025, -15525089, -15260889, -14996689, -14470345, -14206145, -13678773, -13152429, -12362913, -11835541, -11308169, -10780797, -9991281, -8937565, -7621705, -6043701, -4464669, -16313561, -15788237, -15784129, -15256757, -14991529, -14988441, -14723209, -14456953, -14452841, -14186585, -12873805, -11560001, -9984053, -8144937, -6305821, -4203537, -15001829, -14475485, -13949137, -13160649, -12634301, -12107957, -11319465, -10792093, -10002577, -9213061, -8423545, -7634025, -6054993, -4475961, -2895905, -1053701, -15791349, -15002861, -14214373, -13424857, -12636369, -11846853, -11057341, -10267825, -9215137, -8162449, -7109761, -6057073, -5003357, -3950669, -2634813, -1318953, -16777216, -1614080, -1081600, -547072, -12544, -256, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };
          PaletteManager.addPalette(new Palette(palette));
        }

        palette = PaletteManager.getCurrentPalette().getPalette();

        if (PaletteManager.getNumPalettes() > 1) {
          allowPaletteChoice = true;
        }
      }

      if (palette == null || palette.length <= 0) {
        ErrorLogger.log("[Viewer_HQX_HQXTEX] Couldn't find the corresponding palette file");
        return null;
      }

      ImageResource imageResource = null;

      if (allowPaletteChoice) {
        int numPixels = width * height;
        int[] pixels = new int[numPixels];
        for (int i = 0; i < numPixels; i++) {
          pixels[i] = ByteConverter.unsign(fm.readByte());
        }
        imageResource = new PalettedImageResource(pixels, width, height, palette);
      }
      else {
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
      }

      fm.close();

      return imageResource;

    }
    catch (

    Throwable t) {
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

}