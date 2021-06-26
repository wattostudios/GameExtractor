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

package org.watto.ge.plugin.exporter;

import java.io.ByteArrayOutputStream;
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.resource.Resource_RAR_RAR;
import org.watto.io.FileManipulator;
import com.github.junrar.RarArchive;
import com.github.junrar.rarfile.FileHeader;

public class Exporter_RAR_RAR extends ExporterPlugin {

  static Exporter_RAR_RAR instance = new Exporter_RAR_RAR();

  static byte[] readSource;

  static long readLength = 0;
  static int readPos = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_RAR_RAR getInstance() {
    return instance;
  }

  RarArchive rarArchive = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_RAR_RAR() {
    setName("RAR Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_RAR_RAR(RarArchive rarArchive) {
    super();
    this.rarArchive = rarArchive;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      if (readPos < readLength) {
        return true;
      }
      return false;
    }
    catch (Throwable t) {
    }
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    readSource = null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {
    return "This exporter allows files to be exported from RAR 2.0 archives.\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {

      if (!(source instanceof Resource_RAR_RAR)) {
        readLength = 0;
        readPos = 0;
        return;
      }

      FileHeader fileHeader = ((Resource_RAR_RAR) source).getFileHeader();

      readLength = fileHeader.getFullUnpackSize();
      readSource = new byte[(int) readLength];
      /*
      ByteBuffer byteBuffer = new ByteBuffer(readSource);
      
      ManipulatorOutputStream mos = new ManipulatorOutputStream(new FileManipulator(byteBuffer));
      rarArchive.extractFile(fileHeader, mos);
      mos.close();
      
      // Get the byte[] array, as for some reason, the original array wasn't updated.
      readSource = byteBuffer.getBuffer();
      */

      ByteArrayOutputStream baos = new ByteArrayOutputStream((int) readLength);

      /*
      if (fileHeader.isSolid()) {
        // need to effectively decompress all files up to this one
      
        List<FileHeader> fileHeaders = rarArchive.getFileHeaders();
        int numFiles = fileHeaders.size();
        int realNumFiles = 0;
        for (int i = 0; i < numFiles; i++) {
          FileHeader intermediateHeader = fileHeaders.get(i);
          if (intermediateHeader.isDirectory()) {
            continue;
          }
          if (intermediateHeader == fileHeader) {
            // found it
            break;
          }
          else {
            // We need to decompress all the files up to this one.
            // So, if we've gotta decompress it anyway, lets keep the decompressed file in /temp/ so we don't need
            // to re-decompress in the future.

            // So lets find the Resource for the FileHeader

            // Shortcut - hopefully it matches
            Resource resource = org.watto.datatype.Archive.getResource(realNumFiles);
            if (resource instanceof Resource_RAR_RAR){
              if (((Resource_RAR_RAR)resource).getFileHeader() == intermediateHeader){
                // excellent - a match
              }
            }

            realNumFiles++;

            // fake decompress the file (TODO We might as well decompress all the files to real files in /temp/ so we can have faster access next time?)
            rarArchive.extractFile(fileHeader, new NullOutputStream());
          }
        }
      
      }
      */

      rarArchive.extractFile(fileHeader, baos);
      baos.close();

      // Get the byte[] array, as for some reason, the original array wasn't updated.
      readSource = baos.toByteArray();

      readPos = 0;

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    // not supported
    /*
     * try { DeflaterOutputStream outputStream = new DeflaterOutputStream(new
     * FileManipulatorOutputStream(destination));
     *
     * ExporterPlugin exporter = source.getExporter(); exporter.open(source);
     *
     * while (exporter.available()){ outputStream.write(exporter.read()); }
     *
     * exporter.close();
     *
     * outputStream.finish();
     *
     * } catch (Throwable t){ logError(t); }
     */
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      byte value = readSource[readPos];
      readPos++;
      return value;
    }
    catch (Throwable t) {
      //t.printStackTrace();
      readLength = 0;
      return 0;
    }
  }

}