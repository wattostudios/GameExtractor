/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.viewer;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;

import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_3DModel;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ShortConverter;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BIN_AllodsOnline_Geometry extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_BIN_AllodsOnline_Geometry() {
    super("BIN_AllodsOnline_Geometry", "Allods Online BIN Model");
    setExtensions("bin");

    setGames("Allods Online");
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

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      if (fm.getFile().getName().contains("(Geometry)")) {
        rating += 25;
      }
      else {
        rating = 0;
      }

      String header = fm.readString(1);
      if (header.equals("x")) {
        rating += 5;
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
  
  **********************************************************************************************
  **/
  @SuppressWarnings("unused")
  @Override
  public PreviewPanel read(FileManipulator fm) {
    try {

      int arcSize = (int) fm.getLength();
      File originalFile = fm.getFile();

      Exporter_ZLib_CompressedSizeOnly exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();
      exporter.open(fm, arcSize, arcSize);

      byte[] fileData = new byte[arcSize * 5]; // guess max 5* compression

      int decompWritePos = 0;
      while (exporter.available()) { // make sure we read the next bit of data, if required
        fileData[decompWritePos++] = (byte) exporter.read();
      }

      fm.close();

      // resize smaller (due to java heap space limit)
      byte[] realSizeFileData = new byte[decompWritePos];
      System.arraycopy(fileData, 0, realSizeFileData, 0, decompWritePos);
      fileData = realSizeFileData;

      fm = new FileManipulator(new ByteBuffer(fileData));

      // Set up the mesh
      TriangleMesh triangleMesh = new TriangleMesh();

      // VERTICES
      // 4 - Data Type Header (0 = Vertices)
      fm.skip(4);

      // 4 - Vertex Data Length
      int vertexDataLength = fm.readInt();

      int vertexSize = 32;
      if (vertexDataLength % 32 != 0) {
        vertexSize = 36;
        if (vertexDataLength % 36 != 0) {
          ErrorLogger.log("[Viewer_BIN_AllodsOnline_Geometry] Unknown vertex size for data length: " + vertexDataLength);
          return null;
        }
      }

      int numVertices = vertexDataLength / vertexSize;
      FieldValidator.checkNumVertices(numVertices);

      int numVertices2 = numVertices * 2;
      int numVertices3 = numVertices * 3;

      float[] vertices = new float[numVertices3];
      float[] texCoords = new float[numVertices2];

      float minX = 0f;
      float maxX = 0f;
      float minY = 0f;
      float maxY = 0f;
      float minZ = 0f;
      float maxZ = 0f;

      int skipSize = vertexSize - 20;

      for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {

        // 4 - Point X (float)
        // 4 - Point Y (float)
        // 4 - Point Z (float)
        float xPoint = fm.readFloat();
        float yPoint = fm.readFloat();
        float zPoint = fm.readFloat();

        vertices[j] = xPoint;
        vertices[j + 1] = yPoint;
        vertices[j + 2] = zPoint;

        // 4 - Texture U (float)
        // 4 - Texture V (float)
        float uPoint = fm.readFloat();
        float vPoint = fm.readFloat();

        texCoords[k] = uPoint;
        texCoords[k + 1] = vPoint;

        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // if (36) {
        //   4 - Unknown
        //   }
        fm.skip(skipSize);

        if (xPoint < minX) {
          minX = xPoint;
        }
        if (xPoint > maxX) {
          maxX = xPoint;
        }

        if (yPoint < minY) {
          minY = yPoint;
        }
        if (yPoint > maxY) {
          maxY = yPoint;
        }

        if (zPoint < minZ) {
          minZ = zPoint;
        }
        if (zPoint > maxZ) {
          maxZ = zPoint;
        }
      }

      // FACES
      // 4 - Data Type Header (1 = Faces)
      fm.skip(4);

      // 4 - Faces Data Length
      int facesDataLength = fm.readInt();
      int numFaces = facesDataLength / 2;
      FieldValidator.checkNumFaces(numFaces);

      int numFaces6 = numFaces * 2;

      int[] faces = new int[numFaces6];

      for (int i = 0, j = 0; i < numFaces; i += 3, j += 6) {

        // 2 - Point 1
        // 2 - Point 2
        // 2 - Point 3
        int face1 = ShortConverter.unsign(fm.readShort());
        int face2 = ShortConverter.unsign(fm.readShort());
        int face3 = ShortConverter.unsign(fm.readShort());

        // reverse face
        faces[j] = face3;
        faces[j + 1] = face2;
        faces[j + 2] = face1;

        // front face
        faces[j + 3] = face1;
        faces[j + 4] = face2;
        faces[j + 5] = face3;
      }

      if (faces != null && vertices != null && texCoords != null) {
        // we have a full mesh for a single object - add it to the model
        triangleMesh.getTexCoords().addAll(texCoords);

        triangleMesh.getPoints().addAll(vertices);
        triangleMesh.getFaces().addAll(faces);

        faces = null;
        vertices = null;

        texCoords = null;
      }

      // calculate the sizes and centers
      float diffX = (maxX - minX);
      float diffY = (maxY - minY);
      float diffZ = (maxZ - minZ);

      float centerX = minX + (diffX / 2);
      float centerY = minY + (diffY / 2);
      float centerZ = minZ + (diffZ / 2);

      Point3D sizes = new Point3D(diffX, diffY, diffZ);
      Point3D center = new Point3D(centerX, centerY, centerZ);

      PreviewPanel_3DModel preview = null;

      /*
      // See if we can find the texture file for this mesh
      try {
        String textureFilename = originalFile.getAbsolutePath();
        textureFilename = textureFilename.replaceAll("\\(Geometry\\)", "(Texture)");
        File textureFile = new File(textureFilename);
      
        if (textureFile.exists()) {
          javafx.scene.image.Image textureImage = loadTextureImage(textureFile);
      
          if (textureImage != null) {
            Material material = new PhongMaterial(Color.WHITE, textureImage, (javafx.scene.image.Image) null, (javafx.scene.image.Image) null, (javafx.scene.image.Image) null);
            MeshView view = new MeshView(triangleMesh);
            view.setMaterial(material);
      
            // generate the mesh with the texture attached
            preview = new PreviewPanel_3DModel(view, sizes, center);
          }
        }
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }
      */

      if (preview == null) { // just render as a plain mesh
        preview = new PreviewPanel_3DModel(triangleMesh, sizes, center);
      }

      return preview;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public javafx.scene.image.Image loadTextureImage(File textureFile) {
    try {

      ImageResource imageResourceObj = new Viewer_BIN_AllodsOnline_Texture().readThumbnail(new FileManipulator(textureFile, false));
      if (imageResourceObj == null) {
        return null;
      }

      java.awt.Image image = imageResourceObj.getImage();
      BufferedImage bufImage = null;
      if (image instanceof BufferedImage) {
        bufImage = (BufferedImage) image;
      }
      else {
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        bufImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bufImage.createGraphics();
        bGr.drawImage(image, 0, 0, null);
        bGr.dispose();
      }

      return SwingFXUtils.toFXImage(bufImage, null);
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
  public void write(PreviewPanel panel, FileManipulator fm) {

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ImageResource readThumbnail(FileManipulator source) {
    try {
      PreviewPanel preview = read(source);
      if (preview == null || !(preview instanceof PreviewPanel_3DModel)) {
        return null;
      }

      PreviewPanel_3DModel preview3D = (PreviewPanel_3DModel) preview;

      // generate a thumbnail-sized snapshot
      int thumbnailSize = 150; // bigger than ImageResource, so it is shrunk (and smoothed as a result)
      preview3D.generateSnapshot(thumbnailSize, thumbnailSize);

      Image image = preview3D.getImage();
      if (image != null) {
        ImageResource resource = new ImageResource(image, preview3D.getImageWidth(), preview3D.getImageHeight());
        preview3D.onCloseRequest(); // cleanup memory
        return resource;
      }

      return null;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

}