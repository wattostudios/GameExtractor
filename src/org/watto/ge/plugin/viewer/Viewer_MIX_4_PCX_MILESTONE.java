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

import java.io.File;

import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageFormatWriter;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_MIX_4;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_MIX_4_PCX_MILESTONE extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_MIX_4_PCX_MILESTONE() {
    super("MIX_4_PCX_MILESTONE", "MIX_4_PCX_MILESTONE Image");
    setExtensions("pcx");
    setStandardFileFormat(false);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
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
      if (plugin instanceof Plugin_MIX_4) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FilenameSplitter.getExtension(fm.getFile()).equalsIgnoreCase(extensions[0])) {
        rating += 25;
      }

      // 9 - Header (Milestone)
      if (fm.readString(9).equals("Milestone")) {
        rating += 5;
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
  
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(File path) {
    return read(new FileManipulator(path, false));
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

  @SuppressWarnings("unused")
  public ImageResource readThumbnail(FileManipulator fm) {
    try {
      long arcSize = fm.getLength();

      // 9 - Header (Milestone)
      // 4 - Image Data Length
      fm.skip(13);

      // X - PCX Image

      int numColors = 256;
      int[] palette = new int[0];

      // 1 - Manufacturer (10)
      fm.skip(1);

      // 1 - Version (0/2/3/5)
      int version = ByteConverter.unsign(fm.readByte());

      // 1 - Encoding (1)
      fm.skip(1);

      // 1 - Bits per pixel
      int bitCount = ByteConverter.unsign(fm.readByte());

      // 2 - Xmin
      short xMin = fm.readShort();

      // 2 - Ymin
      short yMin = fm.readShort();

      // 2 - Xmax
      int width = fm.readShort() - xMin + 1;
      FieldValidator.checkWidth(width);

      // 2 - Ymax
      int height = fm.readShort() - yMin + 1;
      FieldValidator.checkHeight(height);

      // 2 - Horizontal Resolution
      // 2 - Vertical Resolution
      fm.skip(4);

      // 48 - Colormap (16 colors * 3x RGB)
      if (version < 5) {
        numColors = 16;

        palette = new int[numColors];
        for (int i = 0; i < numColors; i++) {
          // 1 - Red
          // 1 - Green
          // 1 - Blue
          int r = ByteConverter.unsign(fm.readByte());
          int g = ByteConverter.unsign(fm.readByte());
          int b = ByteConverter.unsign(fm.readByte());

          palette[i] = ((255 << 24) | (b << 16) | (g << 8) | (r));
        }
      }
      else {
        fm.skip(48);
      }

      // 1 - null
      fm.skip(1);

      // 1 - Number of color planes
      int numPlanes = ByteConverter.unsign(fm.readByte());

      // 2 - Number of bytes per scan line per color plane
      int numScanBytes = fm.readShort();

      // 2 - Palette Type (1 = color/BW, 2 = grayscale)
      // 58 - Padding to 128 bytes
      fm.skip(60);

      if (version == 5 && numPlanes == 1) {
        // IN THIS SPECIAL PCX VERSION, THE COLOR PALETTE IS STORED HERE, NOT AT THE END OF THE FILE

        numColors = 256;

        // X - Palette
        palette = new int[numColors];
        for (int i = 0; i < numColors; i++) {
          // 1 - Blue
          // 1 - Green
          // 1 - Red
          int b = ByteConverter.unsign(fm.readByte());
          int g = ByteConverter.unsign(fm.readByte());
          int r = ByteConverter.unsign(fm.readByte());

          palette[i] = ((255 << 24) | (b << 16) | (g << 8) | (r));
        }

        if (width % 2 == 1) {
          width++;
        }

        // X - Pixels
        ImageResource image = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
        fm.close();
        return image;
      }
      else if (version == 5 && numPlanes == 3) {

        // STILL NEED TO SKIP OVER A COLOR PALETTE
        fm.skip(768);

        // cater for odd widths
        if (width % 2 == 1) {
          width++;
        }

        // Now read the image
        // X - Pixels

        int numPixels = width * height;
        int[] pixels = new int[numPixels];

        for (int h = 0; h < height; h++) {
          // RED
          byte[] r = fm.readBytes(width);
          // GREEN
          byte[] g = fm.readBytes(width);
          // BLUE
          byte[] b = fm.readBytes(width);

          int startPos = h * width;
          for (int w = 0; w < width; w++) {
            pixels[startPos + w] = ((255 << 24) | (ByteConverter.unsign(r[w]) << 16) | (ByteConverter.unsign(g[w]) << 8) | (ByteConverter.unsign(b[w])));
          }
        }

        /*
        
        
        int totalBytes = numPlanes * numScanBytes;
        int[] scanline = new int[totalBytes];
        
        int overshoot = 0;
        int overshootPixel = 0;
        
        for (int y = 0; y < height; y++) {
        
          //ErrorLogger.log("Reading line " + y + " of  " + height);
        
          int x = 0;
        
          // apply any overshoot from the previous line, if any (even though it goes against the official standard)
          for (int o = 0; o < overshoot; o++) {
            scanline[x + o] = overshootPixel;
          }
          overshoot = 0;
        
          // build the scan line
          while (x < totalBytes) {
            if (fm.getRemainingLength() <= 0) {
              break;
            }
        
            //ErrorLogger.log("  X = " + x);
            int value = ByteConverter.unsign(fm.readByte());
            if ((value & 192) == 192) {
              int count = (value & 63);
              //ErrorLogger.log("  " + count + " repeats");
        
              value = ByteConverter.unsign(fm.readByte());
        
              overshoot = x + count - totalBytes;
              if (overshoot > 0) {
                count -= overshoot;
                overshootPixel = value;
              }
        
              for (int i = 0; i < count; i++) {
                //pixels[y*width+x+i] = value;
                scanline[x + i] = value;
              }
              x += count;
            }
            else {
        
              //ErrorLogger.log("  single byte");
        
              //pixels[y*width+x] = value;
              //ErrorLogger.log("    writing 1 pixel");
              scanline[x] = value;
              x++;
            }
          }
        
          // convert into RGB
          if (numPlanes == 3) {
            for (int i = 0, j = numScanBytes, k = numScanBytes * 2; i < numScanBytes; i++, j++, k++) {
              scanline[i] = ((255 << 24) | (scanline[i] << 16) | (scanline[j] << 8) | (scanline[k]));
            }
          }
        
          // copy to the pixel array
          System.arraycopy(scanline, 0, pixels, y * width, width);
        }
        */

        return new ImageResource(pixels, width, height);
      }

    }
    catch (Throwable t) {
      logError(t);
    }
    return null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel preview, FileManipulator fm) {
    try {

      // NOTE: No compression is done here!

      int imageWidth = -1;
      int imageHeight = -1;

      if (preview instanceof PreviewPanel_Image) {
        PreviewPanel_Image ivp = (PreviewPanel_Image) preview;
        imageWidth = ivp.getImageWidth();
        imageHeight = ivp.getImageHeight();
      }
      else {
        return;
      }

      if (imageWidth == -1 || imageHeight == -1) {
        return;
      }

      // 9 - Header
      fm.writeString("Milestone");

      // 4 - Image Data Length
      fm.writeInt(imageWidth * imageHeight);

      // 1 - Manufacturer (10)
      fm.writeByte(10);

      // 1 - Version (0/2/3/5)
      fm.writeByte(5);

      // 1 - Encoding (1)
      fm.writeByte(1);

      // 1 - Bits per pixel
      fm.writeByte(8);

      // 2 - Xmin
      fm.writeShort((short) 0);

      // 2 - Ymin
      fm.writeShort((short) 0);

      // 2 - Xmax
      fm.writeShort((short) (imageWidth - 1));

      // 2 - Ymax
      fm.writeShort((short) (imageHeight - 1));

      // 2 - Horizontal Resolution
      fm.writeShort((short) 96);

      // 2 - Vertical Resolution
      fm.writeShort((short) 96);

      // 48 - Colormap (16 colors * 3x RGB)
      for (int i = 0; i < 255; i += 16) {
        // 1 - Blue
        // 1 - Green
        // 1 - Red
        fm.writeByte(i);
        fm.writeByte(i);
        fm.writeByte(i);
      }

      // 1 - null
      fm.writeByte(0);

      // 1 - Number of color planes
      fm.writeByte(1);

      int scanWidth = imageWidth;
      if (scanWidth % 2 == 1) {
        scanWidth++;
      }
      int scanHeight = imageHeight;
      if (scanHeight % 2 == 1) {
        scanHeight++;
      }

      // 2 - Number of bytes per scan line per color plane
      fm.writeShort((short) scanWidth);

      // 2 - Palette Type (1 = color/BW, 2 = grayscale)
      fm.writeShort((short) 1);

      // 58 - Padding to 128 bytes
      fm.writeBytes(new byte[58]);

      // X - Palette
      ImageManipulator im = new ImageManipulator((PreviewPanel_Image) preview);
      im.convertToPaletted();
      im.changeColorCount(256);
      int[] palette = im.getPalette();
      ImageFormatWriter.writePaletteRGB(fm, palette);

      // X - Pixels
      if (imageWidth % 2 == 0) {
        // even width
        byte[] pixels = im.getPixelBytes();
        fm.writeBytes(pixels);
      }
      else {
        // odd width has an extra byte at the end of each row
        byte[] pixels = im.getPixelBytes();

        int startPos = 0;
        for (int h = 0; h < imageHeight; h++) {
          // X - Line
          for (int w = 0; w < imageWidth; w++) {
            fm.writeByte(pixels[startPos + w]);
          }
          // 1 - padding byte
          fm.writeByte(0);

          startPos += imageWidth;
        }

        fm.writeBytes(pixels);
      }

      // 8 - null
      fm.writeLong(0);

      /*
      // X - Pixels
      BufferedImage bufImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
      Graphics g = bufImage.getGraphics();
      g.drawImage(image, 0, 0, null);
      
      int[] pixels = bufImage.getRGB(0, 0, imageWidth, imageHeight, null, 0, imageWidth);
      
      for (int y = 0; y < imageHeight; y++) {
        ColorSplit[] colors = new ColorSplit[scanWidth];
      
        ErrorLogger.log("Y = " + y);
      
        // fill the pixels for this line
        for (int x = 0; x < imageWidth; x++) {
          colors[x] = new ColorSplit(pixels[y * imageWidth + x]);
        }
        // fill the padding pixels
        for (int x = imageWidth; x < scanWidth; x++) {
          colors[x] = new ColorSplit(0);
        }
      
        // red pixels for this scan line
        for (int x = 0; x < scanWidth; x++) {
          // 1 - Red
          int red = colors[x].getRed();
          if (red >= 192) {
            // need to set the 1-byte code before the pixel - set count to "1"
            fm.writeByte(193);
          }
          fm.writeByte(red);
        }
        // green pixels for this scan line
        for (int x = 0; x < scanWidth; x++) {
          // 1 - Green
          int green = colors[x].getGreen();
          if (green >= 192) {
            // need to set the 1-byte code before the pixel - set count to "1"
            fm.writeByte(193);
          }
          fm.writeByte(green);
        }
        // blue pixels for this scan line
        for (int x = 0; x < scanWidth; x++) {
          // 1 - Blue
          int blue = colors[x].getBlue();
          if (blue >= 192) {
            // need to set the 1-byte code before the pixel - set count to "1"
            fm.writeByte(193);
          }
          fm.writeByte(blue);
        }
      }
      
      // height padding scan lines at the end
      fm.writeBytes(new byte[(scanHeight - imageHeight) * scanWidth]);
      */

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}