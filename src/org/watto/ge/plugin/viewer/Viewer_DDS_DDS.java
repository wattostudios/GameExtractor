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
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageFormatWriter;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_DDS_DDS extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_DDS_DDS() {
    super("DDS_DDS", "DirectX DDS Image");
    setExtensions("dds");
    setStandardFileFormat(true);
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

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      // header
      if (fm.readString(4).equals("DDS ")) {
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

  @SuppressWarnings("unused")
  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      // 4 - Header (DDS )
      String header = fm.readString(4);

      // 4 - Header 1 Length (124)
      int size = fm.readInt();

      // 4 - Flags
      int flags = fm.readInt();

      // 4 - Height
      int height = fm.readInt();

      // 4 - Width
      int width = fm.readInt();

      // 4 - Linear Size
      int linearSize = fm.readInt();

      // 4 - Depth
      int depth = fm.readInt();

      // 4 - Number Of MipMaps
      int mipMapCount = fm.readInt();

      // 4 - Alpha Bit Depth
      int dwAlphaBitDepth = fm.readInt();

      // 40 - Unknown
      fm.skip(40);

      // 4 - Header 2 Length (32)
      int size2 = fm.readInt();

      // 4 - Flags 2
      int flags2 = fm.readInt();

      // 4 - Format Code (DXT1 - DXT5)
      String fourCC = fm.readString(4);

      // 4 - Color Bit Count
      int rgbBitCount = fm.readInt();

      // 4 - Red Bit Mask
      int rBitMask = fm.readInt();

      // 4 - Green Bit Mask
      int gBitMask = fm.readInt();

      // 4 - Blue Bit Mask
      int bBitMask = fm.readInt();

      // 4 - Alpha Bit Mask
      int rgbAlphaBitMask = fm.readInt();

      // 16 - DDCAPS2
      int caps1 = fm.readInt();
      int caps2 = fm.readInt();
      int caps3 = fm.readInt();
      int caps4 = fm.readInt();

      // 4 - Texture Stage
      int dwTextureStage = fm.readInt();

      int dxgiFormat = 0;
      if (fourCC.equals("DX10")) {
        // DX10 Extended Header comes here

        // 4 - dxgiFormat
        dxgiFormat = fm.readInt();

        // 4 - resourceDimension;
        // 4 - miscFlag
        // 4 - arraySize
        // 4 - miscFlags2
        fm.skip(16);
      }

      // Go to the start of the texture data
      //fm.seek(128); // We should already be here!

      ImageResource imageResource = null;

      if (dxgiFormat != 0) {
        // DX10 format codes - much easier!

        if (dxgiFormat == 1) {
          // DXGI_FORMAT_R32G32B32A32_TYPELESS
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R32G32B32A32_TYPELESS");
          throw new Exception();
        }
        else if (dxgiFormat == 2) {
          // DXGI_FORMAT_R32G32B32A32_FLOAT
          imageResource = ImageFormatReader.read32F32F32F32F_ABGR(fm, width, height);
          imageResource.addProperty("ImageFormat", "32F32F32F32F_ABGR");
        }
        else if (dxgiFormat == 3) {
          // DXGI_FORMAT_R32G32B32A32_UINT
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R32G32B32A32_UINT");
          throw new Exception();
        }
        else if (dxgiFormat == 4) {
          // DXGI_FORMAT_R32G32B32A32_SINT
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R32G32B32A32_SINT");
          throw new Exception();
        }
        else if (dxgiFormat == 5) {
          // DXGI_FORMAT_R32G32B32_TYPELESS
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R32G32B32_TYPELESS");
          throw new Exception();
        }
        else if (dxgiFormat == 6) {
          // DXGI_FORMAT_R32G32B32_FLOAT
          imageResource = ImageFormatReader.read32F32F32F_RGB(fm, width, height);
          imageResource.addProperty("ImageFormat", "32F32F32F_RGB");
        }
        else if (dxgiFormat == 7) {
          // DXGI_FORMAT_R32G32B32_UINT
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R32G32B32_UINT");
          throw new Exception();
        }
        else if (dxgiFormat == 8) {
          // DXGI_FORMAT_R32G32B32_SINT
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R32G32B32_SINT");
          throw new Exception();
        }
        else if (dxgiFormat == 9) {
          // DXGI_FORMAT_R16G16B16A16_TYPELESS
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R16G16B16A16_TYPELESS");
          throw new Exception();
        }
        else if (dxgiFormat == 10) {
          // DXGI_FORMAT_R16G16B16A16_FLOAT
          imageResource = ImageFormatReader.read16F16F16F16F_ABGR(fm, width, height);
          imageResource.addProperty("ImageFormat", "16F16F16F16F_ABGR");
        }
        else if (dxgiFormat == 11) {
          // DXGI_FORMAT_R16G16B16A16_UNORM
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R16G16B16A16_UNORM");
          throw new Exception();
        }
        else if (dxgiFormat == 12) {
          // DXGI_FORMAT_R16G16B16A16_UINT
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R16G16B16A16_UINT");
          throw new Exception();
        }
        else if (dxgiFormat == 13) {
          // DXGI_FORMAT_R16G16B16A16_SNORM
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R16G16B16A16_SNORM");
          throw new Exception();
        }
        else if (dxgiFormat == 14) {
          // DXGI_FORMAT_R16G16B16A16_SINT
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R16G16B16A16_SINT");
          throw new Exception();
        }
        else if (dxgiFormat == 15) {
          // DXGI_FORMAT_R32G32_TYPELESS
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R32G32_TYPELESS");
          throw new Exception();
        }
        else if (dxgiFormat == 16) {
          // DXGI_FORMAT_R32G32_FLOAT
          imageResource = ImageFormatReader.read32F32F_RG(fm, width, height);
          imageResource.addProperty("ImageFormat", "32F32F_RG");
        }
        else if (dxgiFormat == 17) {
          // DXGI_FORMAT_R32G32_UINT
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R32G32_UINT");
          throw new Exception();
        }
        else if (dxgiFormat == 18) {
          // DXGI_FORMAT_R32G32_SINT
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R32G32_SINT");
          throw new Exception();
        }
        else if (dxgiFormat == 19) {
          // DXGI_FORMAT_R32G8X24_TYPELESS
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R32G8X24_TYPELESS");
          throw new Exception();
        }
        else if (dxgiFormat == 20) {
          // DXGI_FORMAT_D32_FLOAT_S8X24_UINT
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_D32_FLOAT_S8X24_UINT");
          throw new Exception();
        }
        else if (dxgiFormat == 21) {
          // DXGI_FORMAT_R32_FLOAT_X8X24_TYPELESS
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R32_FLOAT_X8X24_TYPELESS");
          throw new Exception();
        }
        else if (dxgiFormat == 22) {
          // DXGI_FORMAT_X32_TYPELESS_G8X24_UINT
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_X32_TYPELESS_G8X24_UINT");
          throw new Exception();
        }
        else if (dxgiFormat == 23) {
          // DXGI_FORMAT_R10G10B10A2_TYPELESS
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R10G10B10A2_TYPELESS");
          throw new Exception();
        }
        else if (dxgiFormat == 24) {
          // DXGI_FORMAT_R10G10B10A2_UNORM
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R10G10B10A2_UNORM");
          throw new Exception();
        }
        else if (dxgiFormat == 25) {
          // DXGI_FORMAT_R10G10B10A2_UINT
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R10G10B10A2_UINT");
          throw new Exception();
        }
        else if (dxgiFormat == 26) {
          // DXGI_FORMAT_R11G11B10_FLOAT
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R11G11B10_FLOAT");
          throw new Exception();
        }
        else if (dxgiFormat == 27) {
          // DXGI_FORMAT_R8G8B8A8_TYPELESS
          imageResource = ImageFormatReader.readBGRA(fm, width, height);
          imageResource.addProperty("ImageFormat", "BGRA");
        }
        else if (dxgiFormat == 28) {
          // DXGI_FORMAT_R8G8B8A8_UNORM
          imageResource = ImageFormatReader.readBGRA(fm, width, height);
          imageResource.addProperty("ImageFormat", "BGRA");
        }
        else if (dxgiFormat == 29) {
          // DXGI_FORMAT_R8G8B8A8_UNORM_SRGB
          imageResource = ImageFormatReader.readBGRA(fm, width, height);
          imageResource.addProperty("ImageFormat", "BGRA");
        }
        else if (dxgiFormat == 30) {
          // DXGI_FORMAT_R8G8B8A8_UINT
          imageResource = ImageFormatReader.readBGRA(fm, width, height);
          imageResource.addProperty("ImageFormat", "BGRA");
        }
        else if (dxgiFormat == 31) {
          // DXGI_FORMAT_R8G8B8A8_SNORM
          imageResource = ImageFormatReader.readBGRA(fm, width, height);
          imageResource.addProperty("ImageFormat", "BGRA");
        }
        else if (dxgiFormat == 32) {
          // DXGI_FORMAT_R8G8B8A8_SINT
          imageResource = ImageFormatReader.readBGRA(fm, width, height);
          imageResource.addProperty("ImageFormat", "BGRA");
        }
        else if (dxgiFormat == 33) {
          // DXGI_FORMAT_R16G16_TYPELESS
          imageResource = ImageFormatReader.readR16G16(fm, width, height);
          imageResource.addProperty("ImageFormat", "R16G16");
        }
        else if (dxgiFormat == 34) {
          // DXGI_FORMAT_R16G16_FLOAT
          imageResource = ImageFormatReader.read16F16F_RG(fm, width, height);
          imageResource.addProperty("ImageFormat", "16F16F_RG");
        }
        else if (dxgiFormat == 35) {
          // DXGI_FORMAT_R16G16_UNORM
          imageResource = ImageFormatReader.readR16G16(fm, width, height);
          imageResource.addProperty("ImageFormat", "R16G16");
        }
        else if (dxgiFormat == 36) {
          // DXGI_FORMAT_R16G16_UINT
          imageResource = ImageFormatReader.readR16G16(fm, width, height);
          imageResource.addProperty("ImageFormat", "R16G16");
        }
        else if (dxgiFormat == 37) {
          // DXGI_FORMAT_R16G16_SNORM
          imageResource = ImageFormatReader.readR16G16(fm, width, height);
          imageResource.addProperty("ImageFormat", "R16G16");
        }
        else if (dxgiFormat == 38) {
          // DXGI_FORMAT_R16G16_SINT
          imageResource = ImageFormatReader.readR16G16(fm, width, height);
          imageResource.addProperty("ImageFormat", "R16G16");
        }
        else if (dxgiFormat == 39) {
          // DXGI_FORMAT_R32_TYPELESS
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R32_TYPELESS");
          throw new Exception();
        }
        else if (dxgiFormat == 40) {
          // DXGI_FORMAT_D32_FLOAT
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_D32_FLOAT");
          throw new Exception();
        }
        else if (dxgiFormat == 41) {
          // DXGI_FORMAT_R32_FLOAT
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R32_FLOAT");
          throw new Exception();
        }
        else if (dxgiFormat == 42) {
          // DXGI_FORMAT_R32_UINT
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R32_UINT");
          throw new Exception();
        }
        else if (dxgiFormat == 43) {
          // DXGI_FORMAT_R32_SINT
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R32_SINT");
          throw new Exception();
        }
        else if (dxgiFormat == 44) {
          // DXGI_FORMAT_R24G8_TYPELESS
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R24G8_TYPELESS");
          throw new Exception();
        }
        else if (dxgiFormat == 45) {
          // DXGI_FORMAT_D24_UNORM_S8_UINT
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_D24_UNORM_S8_UINT");
          throw new Exception();
        }
        else if (dxgiFormat == 46) {
          // DXGI_FORMAT_R24_UNORM_X8_TYPELESS
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R24_UNORM_X8_TYPELESS");
          throw new Exception();
        }
        else if (dxgiFormat == 47) {
          // DXGI_FORMAT_X24_TYPELESS_G8_UINT
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_X24_TYPELESS_G8_UINT");
          throw new Exception();
        }
        else if (dxgiFormat == 48) {
          // DXGI_FORMAT_R8G8_TYPELESS
          imageResource = ImageFormatReader.readRG(fm, width, height);
          imageResource.addProperty("ImageFormat", "RG");
        }
        else if (dxgiFormat == 49) {
          // DXGI_FORMAT_R8G8_UNORM
          imageResource = ImageFormatReader.readRG(fm, width, height);
          imageResource.addProperty("ImageFormat", "RG");
        }
        else if (dxgiFormat == 50) {
          // DXGI_FORMAT_R8G8_UINT
          imageResource = ImageFormatReader.readRG(fm, width, height);
          imageResource.addProperty("ImageFormat", "RG");
        }
        else if (dxgiFormat == 51) {
          // DXGI_FORMAT_R8G8_SNORM
          imageResource = ImageFormatReader.readRG(fm, width, height);
          imageResource.addProperty("ImageFormat", "RG");
        }
        else if (dxgiFormat == 52) {
          // DXGI_FORMAT_R8G8_SINT
          imageResource = ImageFormatReader.readRG(fm, width, height);
          imageResource.addProperty("ImageFormat", "RG");
        }
        else if (dxgiFormat == 53) {
          // DXGI_FORMAT_R16_TYPELESS
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R16_TYPELESS");
          throw new Exception();
        }
        else if (dxgiFormat == 54) {
          // DXGI_FORMAT_R16_FLOAT
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R16_FLOAT");
          throw new Exception();
        }
        else if (dxgiFormat == 55) {
          // DXGI_FORMAT_D16_UNORM
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_D16_UNORM");
          throw new Exception();
        }
        else if (dxgiFormat == 56) {
          // DXGI_FORMAT_R16_UNORM
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R16_UNORM");
          throw new Exception();
        }
        else if (dxgiFormat == 57) {
          // DXGI_FORMAT_R16_UINT
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R16_UINT");
          throw new Exception();
        }
        else if (dxgiFormat == 58) {
          // DXGI_FORMAT_R16_SNORM
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R16_SNORM");
          throw new Exception();
        }
        else if (dxgiFormat == 59) {
          // DXGI_FORMAT_R16_SINT
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R16_SINT");
          throw new Exception();
        }
        else if (dxgiFormat == 60) {
          // DXGI_FORMAT_R8_TYPELESS
          imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
          imageResource.addProperty("ImageFormat", "8BitPaletted");
        }
        else if (dxgiFormat == 61) {
          // DXGI_FORMAT_R8_UNORM
          imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
          imageResource.addProperty("ImageFormat", "8BitPaletted");
        }
        else if (dxgiFormat == 62) {
          // DXGI_FORMAT_R8_UINT
          imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
          imageResource.addProperty("ImageFormat", "8BitPaletted");
        }
        else if (dxgiFormat == 63) {
          // DXGI_FORMAT_R8_SNORM
          imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
          imageResource.addProperty("ImageFormat", "8BitPaletted");
        }
        else if (dxgiFormat == 64) {
          // DXGI_FORMAT_R8_SINT
          imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
          imageResource.addProperty("ImageFormat", "8BitPaletted");
        }
        else if (dxgiFormat == 65) {
          // DXGI_FORMAT_A8_UNORM
          imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
          imageResource.addProperty("ImageFormat", "8BitPaletted");
        }
        else if (dxgiFormat == 66) {
          // DXGI_FORMAT_R1_UNORM
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R1_UNORM");
          throw new Exception();
        }
        else if (dxgiFormat == 67) {
          // DXGI_FORMAT_R9G9B9E5_SHAREDEXP
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R9G9B9E5_SHAREDEXP");
          throw new Exception();
        }
        else if (dxgiFormat == 68) {
          // DXGI_FORMAT_R8G8_B8G8_UNORM
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R8G8_B8G8_UNORM");
          throw new Exception();
        }
        else if (dxgiFormat == 69) {
          // DXGI_FORMAT_G8R8_G8B8_UNORM
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_G8R8_G8B8_UNORM");
          throw new Exception();
        }
        else if (dxgiFormat == 70) {
          // DXGI_FORMAT_BC1_TYPELESS
          imageResource = ImageFormatReader.readBC1(fm, width, height);
          imageResource.addProperty("ImageFormat", "BC1");
        }
        else if (dxgiFormat == 71) {
          // DXGI_FORMAT_BC1_UNORM
          imageResource = ImageFormatReader.readBC1(fm, width, height);
          imageResource.addProperty("ImageFormat", "BC1");
        }
        else if (dxgiFormat == 72) {
          // DXGI_FORMAT_BC1_UNORM_SRGB
          imageResource = ImageFormatReader.readBC1(fm, width, height);
          imageResource.addProperty("ImageFormat", "BC1");
        }
        else if (dxgiFormat == 73) {
          // DXGI_FORMAT_BC2_TYPELESS
          imageResource = ImageFormatReader.readBC2(fm, width, height);
          imageResource.addProperty("ImageFormat", "BC2");
        }
        else if (dxgiFormat == 74) {
          // DXGI_FORMAT_BC2_UNORM
          imageResource = ImageFormatReader.readBC2(fm, width, height);
          imageResource.addProperty("ImageFormat", "BC2");
        }
        else if (dxgiFormat == 75) {
          // DXGI_FORMAT_BC2_UNORM_SRGB
          imageResource = ImageFormatReader.readBC2(fm, width, height);
          imageResource.addProperty("ImageFormat", "BC2");
        }
        else if (dxgiFormat == 76) {
          // DXGI_FORMAT_BC3_TYPELESS
          imageResource = ImageFormatReader.readBC3(fm, width, height);
          imageResource.addProperty("ImageFormat", "BC3");
        }
        else if (dxgiFormat == 77) {
          // DXGI_FORMAT_BC3_UNORM
          imageResource = ImageFormatReader.readBC3(fm, width, height);
          imageResource.addProperty("ImageFormat", "BC3");
        }
        else if (dxgiFormat == 78) {
          // DXGI_FORMAT_BC3_UNORM_SRGB
          imageResource = ImageFormatReader.readBC3(fm, width, height);
          imageResource.addProperty("ImageFormat", "BC3");
        }
        else if (dxgiFormat == 79) {
          // DXGI_FORMAT_BC4_TYPELESS
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_BC4_TYPELESS");
          throw new Exception();
        }
        else if (dxgiFormat == 80) {
          // DXGI_FORMAT_BC4_UNORM
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_BC4_UNORM");
          throw new Exception();
        }
        else if (dxgiFormat == 81) {
          // DXGI_FORMAT_BC4_SNORM
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_BC4_SNORM");
          throw new Exception();
        }
        else if (dxgiFormat == 82) {
          // DXGI_FORMAT_BC5_TYPELESS
          imageResource = ImageFormatReader.readBC5(fm, width, height);
          imageResource.addProperty("ImageFormat", "BC5");
        }
        else if (dxgiFormat == 83) {
          // DXGI_FORMAT_BC5_UNORM
          imageResource = ImageFormatReader.readBC5(fm, width, height);
          imageResource.addProperty("ImageFormat", "BC5");
        }
        else if (dxgiFormat == 84) {
          // DXGI_FORMAT_BC5_SNORM
          imageResource = ImageFormatReader.readBC5(fm, width, height);
          imageResource.addProperty("ImageFormat", "BC5");
        }
        else if (dxgiFormat == 85) {
          // DXGI_FORMAT_B5G6R5_UNORM
          imageResource = ImageFormatReader.readBGR565(fm, width, height);
          imageResource.addProperty("ImageFormat", "BGR565");
        }
        else if (dxgiFormat == 86) {
          // DXGI_FORMAT_B5G5R5A1_UNORM
          imageResource = ImageFormatReader.readBGRA5551(fm, width, height);
          imageResource.addProperty("ImageFormat", "BGRA5551");
        }
        else if (dxgiFormat == 87) {
          // DXGI_FORMAT_B8G8R8A8_UNORM
          imageResource = ImageFormatReader.readBGRA(fm, width, height);
          imageResource.addProperty("ImageFormat", "BGRA");
        }
        else if (dxgiFormat == 88) {
          // DXGI_FORMAT_B8G8R8X8_UNORM
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_B8G8R8X8_UNORM");
          throw new Exception();
        }
        else if (dxgiFormat == 89) {
          // DXGI_FORMAT_R10G10B10_XR_BIAS_A2_UNORM
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_R10G10B10_XR_BIAS_A2_UNORM");
          throw new Exception();
        }
        else if (dxgiFormat == 90) {
          // DXGI_FORMAT_B8G8R8A8_TYPELESS
          imageResource = ImageFormatReader.readBGRA(fm, width, height);
          imageResource.addProperty("ImageFormat", "BGRA");
        }
        else if (dxgiFormat == 91) {
          // DXGI_FORMAT_B8G8R8A8_UNORM_SRGB
          imageResource = ImageFormatReader.readBGRA(fm, width, height);
          imageResource.addProperty("ImageFormat", "BGRA");
        }
        else if (dxgiFormat == 92) {
          // DXGI_FORMAT_B8G8R8X8_TYPELESS
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_B8G8R8X8_TYPELESS");
          throw new Exception();
        }
        else if (dxgiFormat == 93) {
          // DXGI_FORMAT_B8G8R8X8_UNORM_SRGB
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_B8G8R8X8_UNORM_SRGB");
          throw new Exception();
        }
        else if (dxgiFormat == 94) {
          // DXGI_FORMAT_BC6H_TYPELESS
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_BC6H_TYPELESS");
          throw new Exception();
        }
        else if (dxgiFormat == 95) {
          // DXGI_FORMAT_BC6H_UF16
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_BC6H_UF16");
          throw new Exception();
        }
        else if (dxgiFormat == 96) {
          // DXGI_FORMAT_BC6H_SF16
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_BC6H_SF16");
          throw new Exception();
        }
        else if (dxgiFormat == 97) {
          // DXGI_FORMAT_BC7_TYPELESS
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_BC7_TYPELESS");
          throw new Exception();
        }
        else if (dxgiFormat == 98) {
          // DXGI_FORMAT_BC7_UNORM
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_BC7_UNORM");
          throw new Exception();
        }
        else if (dxgiFormat == 99) {
          // DXGI_FORMAT_BC7_UNORM_SRGB
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_BC7_UNORM_SRGB");
          throw new Exception();
        }
        else if (dxgiFormat == 100) {
          // DXGI_FORMAT_AYUV
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_AYUV");
          throw new Exception();
        }
        else if (dxgiFormat == 101) {
          // DXGI_FORMAT_Y410
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_Y410");
          throw new Exception();
        }
        else if (dxgiFormat == 102) {
          // DXGI_FORMAT_Y416
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_Y416");
          throw new Exception();
        }
        else if (dxgiFormat == 103) {
          // DXGI_FORMAT_NV12
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_NV12");
          throw new Exception();
        }
        else if (dxgiFormat == 104) {
          // DXGI_FORMAT_P010
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_P010");
          throw new Exception();
        }
        else if (dxgiFormat == 105) {
          // DXGI_FORMAT_P016
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_P016");
          throw new Exception();
        }
        else if (dxgiFormat == 106) {
          // DXGI_FORMAT_420_OPAQUE
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_420_OPAQUE");
          throw new Exception();
        }
        else if (dxgiFormat == 107) {
          // DXGI_FORMAT_YUY2
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_YUY2");
          throw new Exception();
        }
        else if (dxgiFormat == 108) {
          // DXGI_FORMAT_Y210
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_Y210");
          throw new Exception();
        }
        else if (dxgiFormat == 109) {
          // DXGI_FORMAT_Y216
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_Y216");
          throw new Exception();
        }
        else if (dxgiFormat == 110) {
          // DXGI_FORMAT_NV11
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_NV11");
          throw new Exception();
        }
        else if (dxgiFormat == 111) {
          // DXGI_FORMAT_AI44
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_AI44");
          throw new Exception();
        }
        else if (dxgiFormat == 112) {
          // DXGI_FORMAT_IA44
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_IA44");
          throw new Exception();
        }
        else if (dxgiFormat == 113) {
          // DXGI_FORMAT_P8
          imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
          imageResource.addProperty("ImageFormat", "8BitPaletted");
        }
        else if (dxgiFormat == 114) {
          // DXGI_FORMAT_A8P8
          imageResource = ImageFormatReader.readA8L8(fm, width, height);
          imageResource.addProperty("ImageFormat", "A8L8");
        }
        else if (dxgiFormat == 115) {
          // DXGI_FORMAT_B4G4R4A4_UNORM
          imageResource = ImageFormatReader.readBGRA4444(fm, width, height);
          imageResource.addProperty("ImageFormat", "BGRA4444");
        }
        else if (dxgiFormat == 130) {
          // DXGI_FORMAT_P208
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_P208");
          throw new Exception();
        }
        else if (dxgiFormat == 131) {
          // DXGI_FORMAT_V208
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_V208");
          throw new Exception();
        }
        else if (dxgiFormat == 132) {
          // DXGI_FORMAT_V408
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: DXGI_FORMAT_V408");
          throw new Exception();
        }
        else {
          // Unsupported format
          ErrorLogger.log("[Viewer_DDS]: Unsupported Format: DX10: " + dxgiFormat);
          throw new Exception();
        }

      }
      else if (fourCC.equals("DXT1")) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
        imageResource.addProperty("ImageFormat", "DXT1");
      }
      else if (fourCC.equals("DX1A")) {
        imageResource = ImageFormatReader.readDX1A(fm, width, height);
        imageResource.addProperty("ImageFormat", "DX1A");
      }
      else if (fourCC.equals("DXT3") || fourCC.equals("DXT2")) {
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
        imageResource.addProperty("ImageFormat", "DXT3");
      }
      else if (fourCC.equals("DXT5") || fourCC.equals("DXT4")) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
        imageResource.addProperty("ImageFormat", "DXT5");
      }
      else if (fourCC.equals("ATI2")) {
        imageResource = ImageFormatReader.readBC5(fm, width, height);
        imageResource.addProperty("ImageFormat", "BC5");
      }
      else if (fourCC.trim().equals("q")) {
        // A16:B16:G16:R16 FLOAT (DXGI_FORMAT_R16G16B16A16_FLOAT or D3DFMT_A16B16G16R16F)
        imageResource = ImageFormatReader.read16F16F16F16F_ABGR(fm, width, height);
        imageResource.addProperty("ImageFormat", "16F16F16F16F_ABGR");
      }
      else if (fourCC.trim().equals("r")) {
        // R32 FLOAT (D3DFMT_R32F or DXGI_FORMAT_R32_FLOAT)
        imageResource = ImageFormatReader.read32F_R(fm, width, height);
        imageResource.addProperty("ImageFormat", "32F_R");
      }
      else if (fourCC.trim().equals("t")) {
        // A32:B32:G32:R32 FLOAT (DXGI_FORMAT_R32G32B32A32_FLOAT or D3DFMT_A32B32G32R32F)
        imageResource = ImageFormatReader.read32F32F32F32F_ABGR(fm, width, height);
        imageResource.addProperty("ImageFormat", "32F32F32F32F_ABGR");
      }
      else {
        if (/*flags2 == 32 &&*/ rgbBitCount == 8) {
          // Indexed pixel data (eg grayscale paletted)
          imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
          imageResource.addProperty("ImageFormat", "8BitPaletted");
        }
        else if (rgbBitCount == 16 && rBitMask == 31744 && gBitMask == 992 && bBitMask == 31 && rgbAlphaBitMask == 32768) {
          // ARGB 1555 format
          imageResource = ImageFormatReader.readARGB1555(fm, width, height);
          imageResource.addProperty("ImageFormat", "ARGB1555");
        }
        else if (rgbBitCount == 16 && rBitMask != 0 && gBitMask != 0 && bBitMask != 0 && rgbAlphaBitMask != 0) {
          // A4R4G4B4 format
          imageResource = ImageFormatReader.readARGB4444(fm, width, height);
          imageResource.addProperty("ImageFormat", "ARGB4444");
        }
        else if (rgbBitCount == 16 && rBitMask == 63488 && gBitMask == 2016 && bBitMask == 31 && rgbAlphaBitMask == 0) {
          // RGB565 format
          imageResource = ImageFormatReader.readRGB565(fm, width, height);
          imageResource.addProperty("ImageFormat", "RGB565");
        }
        else if (rgbBitCount == 16 && rBitMask == 255 && rgbAlphaBitMask == 65280) {
          // A8L8 format
          imageResource = ImageFormatReader.readA8L8(fm, width, height);
          imageResource.addProperty("ImageFormat", "A8L8");
        }
        else if ((flags2 == 32 || flags2 == 64) && rgbBitCount == 24) {
          // uncompressed RGB data
          imageResource = ImageFormatReader.readRGB(fm, width, height);
          imageResource.addProperty("ImageFormat", "RGB");
        }
        else if (rgbBitCount == 32) {
          // uncompressed RGBA data
          imageResource = ImageFormatReader.readBGRA(fm, width, height);
          imageResource.addProperty("ImageFormat", "BGRA");
        }
        else if (fourCC.equals("BC4U")) {
          // BC4U
          imageResource = ImageFormatReader.readBC4(fm, width, height);
          imageResource.addProperty("ImageFormat", "BC4");
        }
        else if (fourCC.equals("BC5U")) {
          // BC5U
          imageResource = ImageFormatReader.readBC5(fm, width, height);
          imageResource.addProperty("ImageFormat", "BC5");
        }
        else {

          int fourCCint = IntConverter.convertLittle(fourCC.getBytes());
          if (fourCCint == 15) {
            imageResource = ImageFormatReader.readDXT5(fm, width, height);
            imageResource.addProperty("ImageFormat", "DXT5");
          }
          else {
            // Unsupported format
            ErrorLogger.log("[Viewer_DDS]: Unsupported Format: " + fourCC);
            throw new Exception();
          }
        }
      }

      imageResource.addProperty("MipmapCount", "" + mipMapCount);

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
    try {

      if (!(preview instanceof PreviewPanel_Image)) {
        return;
      }

      ImageManipulator im = new ImageManipulator((PreviewPanel_Image) preview);

      int imageWidth = im.getWidth();
      int imageHeight = im.getHeight();

      if (imageWidth == -1 || imageHeight == -1) {
        return;
      }

      // Generate all the mipmaps of the image
      ImageResource[] mipmaps = im.generateMipmaps();
      int mipmapCount = mipmaps.length;

      // Set some property defaults in case we're doing a conversion (and thus there probably isn't any properties set)
      String imageFormat = "DXT5";

      // Now try to get the property values from the ImageResource, if they exist
      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();

      if (imageResource != null) {
        mipmapCount = imageResource.getProperty("MipmapCount", mipmapCount);
        imageFormat = imageResource.getProperty("ImageFormat", "DXT3");
      }

      if (mipmapCount > mipmaps.length) {
        mipmapCount = mipmaps.length;
      }
      if (!(imageFormat.equals("DXT1") || imageFormat.equals("DXT3") || imageFormat.equals("DXT5") || imageFormat.equals("ARGB"))) {
        // a different image format not allowed in this image - change to DXT5
        imageFormat = "DXT5";
      }

      int DDSD_CAPS = 0x0001;
      int DDSD_HEIGHT = 0x0002;
      int DDSD_WIDTH = 0x0004;
      int DDSD_PIXELFORMAT = 0x1000;
      int DDSD_MIPMAPCOUNT = 0x20000;
      int DDSD_LINEARSIZE = 0x80000;

      // Write the header

      // 4 - Header (DDS )
      fm.writeString("DDS ");

      // 4 - Header 1 Length (124)
      fm.writeInt(124);

      // 4 - Flags
      int flag = DDSD_CAPS | DDSD_HEIGHT | DDSD_WIDTH | DDSD_PIXELFORMAT | DDSD_MIPMAPCOUNT | DDSD_LINEARSIZE;
      fm.writeInt(flag);

      // 4 - Height
      fm.writeInt(imageHeight);

      // 4 - Width
      fm.writeInt(imageWidth);

      // 4 - Linear Size
      fm.writeInt(imageWidth * imageHeight / 2);

      // 4 - Depth
      fm.writeInt(0);

      // 4 - Number Of MipMaps
      fm.writeInt(mipmapCount);

      // 4 - Alpha Bit Depth
      fm.writeInt(0);

      // 40 - Unknown
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);
      fm.writeInt(0);

      // 4 - Header 2 Length (32)
      fm.writeInt(32);

      // 4 - Flags 2
      fm.writeInt(0x0004);

      // 4 - Format Code (DXT1 - DXT5)
      if (imageFormat.equals("DXT1") || imageFormat.equals("DXT3") || imageFormat.equals("DXT5")) {
        fm.writeString(imageFormat);
      }
      else {
        fm.writeInt(0);
      }

      // 4 - Color Bit Count
      // 4 - Red Bit Mask
      // 4 - Green Bit Mask
      // 4 - Blue Bit Mask
      // 4 - Alpha Bit Mask
      if (imageFormat.equals("ARGB")) {
        fm.writeInt(32);

        // Red
        fm.writeByte(0);
        fm.writeByte(0);
        fm.writeByte(255);
        fm.writeByte(0);

        // Green
        fm.writeByte(0);
        fm.writeByte(255);
        fm.writeByte(0);
        fm.writeByte(0);

        // Blue
        fm.writeByte(255);
        fm.writeByte(0);
        fm.writeByte(0);
        fm.writeByte(0);

        // Alpha
        fm.writeByte(0);
        fm.writeByte(0);
        fm.writeByte(0);
        fm.writeByte(255);
      }
      else {
        fm.writeInt(0);
        fm.writeInt(0);
        fm.writeInt(0);
        fm.writeInt(0);
        fm.writeInt(0);
      }

      // 16 - DDCAPS2
      // 4 - Texture Stage
      // X - Unknown
      fm.writeInt(0x1000);
      fm.writeInt(0);
      fm.seek(128);

      // X - Mipmaps
      for (int i = 0; i < mipmapCount; i++) {
        ImageResource mipmap = mipmaps[i];

        // X - Pixels
        if (imageFormat.equals("DXT1")) {
          ImageFormatWriter.writeDXT1(fm, mipmap);
        }
        else if (imageFormat.equals("DXT3")) {
          ImageFormatWriter.writeDXT3(fm, mipmap);
        }
        else if (imageFormat.equals("DXT5")) {
          ImageFormatWriter.writeDXT5(fm, mipmap);
        }
        else {
          ImageFormatWriter.writeARGB(fm, mipmap);
        }
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}