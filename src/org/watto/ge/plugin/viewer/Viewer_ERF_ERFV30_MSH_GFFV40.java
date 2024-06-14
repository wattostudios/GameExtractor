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

package org.watto.ge.plugin.viewer;

import java.awt.Image;
import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_3DModel;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_ERF_ERFV30;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ERF_ERFV30_MSH_GFFV40 extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ERF_ERFV30_MSH_GFFV40() {
    super("ERF_ERFV30_MSH_GFFV40", "ERF_ERFV30_MSH_GFFV40 Model");
    setExtensions("msh");

    setGames("Dragon Age 2");
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
      if (readPlugin instanceof Plugin_ERF_ERFV30) {
        rating += 50;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      if (fm.readString(8).equals("GFF V4.0")) {
        rating += 25;
      }

      if (fm.readString(12).equals("PC  MESHV1.0")) {
        rating += 25;
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

      long arcSize = fm.getLength();

      // Read in the model

      // Set up the mesh
      TriangleMesh triangleMesh = new TriangleMesh();

      float[] points = null;
      float[] texCoords = null;
      int[] faces = null;

      minX = 20000f;
      maxX = -20000f;
      minY = 20000f;
      maxY = -20000f;
      minZ = 20000f;
      maxZ = -20000f;

      // 20 - Header ("GFF V4.0PC  MESHV1.0")
      // 4 - Number of Chunks
      fm.skip(24);

      // 4 - Mesh Directory Offset (relative to the start of the archive)
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);
      fm.seek(dirOffset);

      // 4 - Mesh Header Size (including these header fields)
      // 4 - null
      // 4 - Mesh Header Size (not including these header fields)
      fm.skip(12);

      // 4 - Vertices Directory Offset (relative to the start of the Mesh Directory)
      int vertOffset = fm.readInt() + dirOffset;
      FieldValidator.checkOffset(vertOffset, arcSize);

      // 4 - Faces Directory Offset (relative to the start of the Mesh Directory)
      int faceOffset = fm.readInt() + dirOffset;
      FieldValidator.checkOffset(faceOffset, arcSize);

      fm.skip(64);

      // 4 - Vertex Size
      int vertexSize = fm.readInt();
      if (vertexSize < 12 || vertexSize > 64) {
        ErrorLogger.log("[Viewer_ERF_ERFV30_MSH_GFFV40] Unsupported Vertex Size: " + vertexSize);
        vertexSize = 28; // fallback
      }

      int vertexSizeSkip = vertexSize - 12;

      //
      //
      // FACES
      //
      //
      fm.relativeSeek(faceOffset);

      // 4 -  Face Data Length
      int numFaces = fm.readInt() / 6;
      FieldValidator.checkNumFaces(numFaces);

      int numFaces3 = numFaces * 3;
      FieldValidator.checkNumFaces(numFaces3);

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

      //
      //
      // VERTICES
      //
      //
      fm.seek(vertOffset);

      // 4 - Vertices Data Length
      int numVertices = fm.readInt() / vertexSize;
      FieldValidator.checkNumVertices(numVertices);

      int numVertices3 = numVertices * 3;
      points = new float[numVertices3];

      int numPoints2 = numVertices * 2;
      texCoords = new float[numPoints2];

      for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
        // 4 - Vertex X
        // 4 - Vertex Y
        // 4 - Vertex Z
        float xPoint = fm.readFloat();
        float yPoint = fm.readFloat();
        float zPoint = fm.readFloat();

        points[j] = xPoint;
        points[j + 1] = yPoint;
        points[j + 2] = zPoint;

        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(vertexSizeSkip);

        // Don't know where the texture co-ords are yet
        float xTexture = 0;
        float yTexture = 0;

        texCoords[k] = xTexture;
        texCoords[k + 1] = yTexture;

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

      // add the part to the model
      if (faces != null && points != null && texCoords != null) {
        // we have a full mesh for a single object - add it to the model
        triangleMesh.getTexCoords().addAll(texCoords);

        triangleMesh.getPoints().addAll(points);
        triangleMesh.getFaces().addAll(faces);

        faces = null;
        points = null;

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