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

import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_3DModel;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_PPR_2RPX;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ShortConverter;

import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_PPR_2RPX_FUBI extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_PPR_2RPX_FUBI() {
    super("PPR_2RPX_FUBI", "PPR_2RPX_FUBI");
    setExtensions("fubi");

    setGames("Ms. Splosion Man");
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

      ArchivePlugin readPlugin = Archive.getReadPlugin();
      if (readPlugin instanceof Plugin_PPR_2RPX) {
        rating += 50;
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

  float minX = 20000f;

  float maxX = -20000f;

  float minY = 20000f;

  float maxY = -20000f;

  float minZ = 20000f;

  float maxZ = -20000f;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {

    try {

      // First, find the FUBV file, which contains all the vertices
      String filename = fm.getFile().getName();
      int dotPos = filename.indexOf(".ib.FUBI");
      if (dotPos <= 0) {
        return null;
      }
      filename = filename.substring(0, dotPos);
      dotPos = filename.lastIndexOf('.');
      if (dotPos <= 0) {
        return null;
      }
      filename = filename.substring(0, dotPos) + ".vb.FUBV";

      Resource[] resources = Archive.getResources();
      int numResources = resources.length;

      Resource fubvResource = null;
      for (int i = 0; i < numResources; i++) {
        Resource currentResource = resources[i];
        if (currentResource.getName().equals(filename)) {
          // found the FUBV file
          fubvResource = currentResource;
          break;
        }
      }

      if (fubvResource == null) {
        return null;
      }

      // Read in the model

      // Set up the mesh
      TriangleMesh triangleMesh = new TriangleMesh();

      float[] points = null;
      float[] normals = null;
      float[] texCoords = null;
      int[] faces = null;

      minX = 20000f;
      maxX = -20000f;
      minY = 20000f;
      maxY = -20000f;
      minZ = 20000f;
      maxZ = -20000f;

      //
      //
      // VERTICES (from the FUBV file)
      //
      //

      int fubvLength = (int) fubvResource.getLength();
      ByteBuffer buffer = new ByteBuffer(fubvLength);
      FileManipulator fubvFM = new FileManipulator(buffer);
      fubvResource.extract(fubvFM);

      fubvFM.seek(0); // back to the beginning of the byte array

      int numVertices = fubvLength / 72;
      int numVertices3 = numVertices * 3;
      points = new float[numVertices3];
      normals = new float[numVertices3];

      int numPoints2 = numVertices * 2;
      texCoords = new float[numPoints2];

      for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
        // 4 - Vertex X
        // 4 - Vertex Y
        // 4 - Vertex Z
        float xPoint = fubvFM.readFloat();
        float yPoint = fubvFM.readFloat();
        float zPoint = fubvFM.readFloat();

        points[j] = xPoint;
        points[j + 1] = yPoint;
        points[j + 2] = zPoint;

        // 4 - Unknown
        // 4 - Unknown
        fubvFM.skip(8);

        // 4 - Normal X
        // 4 - Normal Y
        // 4 - Normal Z
        float xNormal = fubvFM.readFloat();
        float yNormal = fubvFM.readFloat();
        float zNormal = fubvFM.readFloat();

        normals[j] = xNormal;
        normals[j + 1] = yNormal;
        normals[j + 2] = zNormal;

        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        fubvFM.skip(24);

        // 4 - Texture U (Float)
        // 4 - Texture V (Float)
        float uTexture = fubvFM.readFloat();
        float vTexture = fubvFM.readFloat();

        texCoords[k] = uTexture;
        texCoords[k + 1] = vTexture;

        // 4 - Unknown
        // 4 - Unknown
        fubvFM.skip(8);

        // Calculate the size of the object
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

      fubvFM.close();

      //
      //
      // FACES (from the FUBI file (which is this file))
      //
      //

      int numFaces3 = ((int) fm.getLength()) / 2;
      FieldValidator.checkNumFaces(numFaces3);

      int numFaces = numFaces3 / 3;
      int numFaces6 = numFaces3 * 2;

      faces = new int[numFaces6]; // need to store front and back faces

      for (int i = 0, j = 0; i < numFaces; i++, j += 6) {
        // 2 - Point Index 1
        // 2 - Point Index 2
        // 2 - Point Index 3
        int facePoint1 = (ShortConverter.unsign(fm.readShort()));
        int facePoint2 = (ShortConverter.unsign(fm.readShort()));
        int facePoint3 = (ShortConverter.unsign(fm.readShort()));

        // reverse face first (so the light shines properly, for this model specifically)
        faces[j] = facePoint3;
        faces[j + 1] = facePoint2;
        faces[j + 2] = facePoint1;

        // forward face second
        faces[j + 3] = facePoint1;
        faces[j + 4] = facePoint2;
        faces[j + 5] = facePoint3;

      }

      // add the part to the model
      if (faces != null && points != null && normals != null && texCoords != null) {
        // we have a full mesh for a single object - add it to the model
        triangleMesh.getTexCoords().addAll(texCoords);

        triangleMesh.getPoints().addAll(points);
        triangleMesh.getFaces().addAll(faces);
        triangleMesh.getNormals().addAll(normals);

        faces = null;
        points = null;
        normals = null;
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

      PreviewPanel_3DModel preview = new PreviewPanel_3DModel(triangleMesh, sizes, center);

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
  @Override
  public void write(PreviewPanel panel, FileManipulator destination) {
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

      preview3D.onCloseRequest(); // cleanup memory

      return null;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

}