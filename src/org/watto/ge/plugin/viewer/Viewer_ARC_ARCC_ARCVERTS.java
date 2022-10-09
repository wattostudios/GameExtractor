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
import org.watto.ge.plugin.archive.Plugin_ARC_ARCC;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ShortConverter;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ARC_ARCC_ARCVERTS extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ARC_ARCC_ARCVERTS() {
    super("ARC_ARCC_ARCVERTS", "Street Racing Syndicate ARC_VERTS Model");
    setExtensions("arc_verts");

    setGames("Street Racing Syndicate");
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
      if (readPlugin instanceof Plugin_ARC_ARCC) {
        rating += 50;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      // 4 - Number of Vertexes
      if (FieldValidator.checkNumVertices(fm.readInt())) {
        rating += 5;
      }

      // 4 - Vertex Entry Length (36/24)
      int entryLength = fm.readInt();
      if (entryLength == 36 || entryLength == 24) {
        rating += 5;
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
  Extracts a ARC_FACES resource and then opens it
  **********************************************************************************************
  **/
  public int[] extractFaces(Resource facesResource) {
    try {
      ByteBuffer buffer = new ByteBuffer((int) facesResource.getLength());
      FileManipulator fm = new FileManipulator(buffer);
      facesResource.extract(fm);

      fm.seek(0); // back to the beginning of the byte array

      // 4 - Number of Face Indices
      int numFaces3 = fm.readInt();
      FieldValidator.checkNumFaces(numFaces3);

      int facesLength = (int) facesResource.getLength() - 4;
      int entrySize = (int) (facesLength / numFaces3);

      int numFaces = numFaces3 / 3;
      int numFaces6 = numFaces3 * 2;

      int[] faces = new int[numFaces6]; // need to store front and back faces

      if (entrySize == 4) {
        for (int i = 0, j = 0; i < numFaces; i++, j += 6) {
          // 4 - Point Index 1
          // 4 - Point Index 2
          // 4 - Point Index 3
          int facePoint1 = fm.readInt();
          int facePoint2 = fm.readInt();
          int facePoint3 = fm.readInt();

          // forward face first
          faces[j] = facePoint1;
          faces[j + 1] = facePoint2;
          faces[j + 2] = facePoint3;

          // reverse face second (so the light shines properly, for this model specifically)
          faces[j + 3] = facePoint3;
          faces[j + 4] = facePoint2;
          faces[j + 5] = facePoint1;

        }
      }
      else {// assume Shorts
        for (int i = 0, j = 0; i < numFaces; i++, j += 6) {
          // 2 - Point Index 1
          // 2 - Point Index 2
          // 2 - Point Index 3
          int facePoint1 = (ShortConverter.unsign(fm.readShort()));
          int facePoint2 = (ShortConverter.unsign(fm.readShort()));
          int facePoint3 = (ShortConverter.unsign(fm.readShort()));

          // forward face first
          faces[j] = facePoint1;
          faces[j + 1] = facePoint2;
          faces[j + 2] = facePoint3;

          // reverse face second (so the light shines properly, for this model specifically)
          faces[j + 3] = facePoint3;
          faces[j + 4] = facePoint2;
          faces[j + 5] = facePoint1;

        }
      }

      /*
      
      if (entrySize == 4) {
        for (int i = 0; i < numFaces; i++) {
          // 4 - Face Index
          faces[i] = fm.readInt();
        }
      }
      else { // assume Shorts
        for (int i = 0; i < numFaces; i++) {
          // 2 - Face Index
          faces[i] = ShortConverter.unsign(fm.readShort());
        }
      }
      */

      fm.close();

      return faces;
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
  public PreviewPanel read(FileManipulator fm) {

    try {

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

      // Find the matching ARC_FACES file
      Resource[] resources = Archive.getResources();
      int numResources = resources.length;
      for (int i = 0; i < numResources; i++) {
        Resource currentResource = resources[i];
        if (currentResource.getExtension().equalsIgnoreCase("arc_faces")) {
          // found the first ARC_FACES file - need to extract it and read it
          faces = extractFaces(resources[i]);
          break;
        }
      }

      if (faces == null) {
        return null;
      }

      // 4 - Number of Vertices
      int numVertices = fm.readInt();
      FieldValidator.checkNumVertices(numVertices);

      // 4 - Vertex Entry Length (36/24)
      int entryLength = fm.readInt();

      // 4 - Unknown (15/11)
      fm.skip(4);

      //
      //
      // VERTICES
      //
      //
      int numVertices3 = numVertices * 3;
      points = new float[numVertices3];
      normals = new float[numVertices3];

      int numPoints2 = numVertices * 2;
      texCoords = new float[numPoints2];

      for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
        // 4 - Position X (float)
        // 4 - Position Y (float)
        // 4 - Position Z (float)
        float xPoint = fm.readFloat();
        float yPoint = fm.readFloat();
        float zPoint = fm.readFloat();

        points[j] = xPoint;
        points[j + 1] = yPoint;
        points[j + 2] = zPoint;

        // 4 - Unknown
        // 4 - Tex Co-ord U (float)
        // 4 - Tex Co-ord V (float)
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(entryLength - 12); // caters for 24 and 36-length entries

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

      //
      //
      // FACES
      //
      //

      /*
      int numFaces3 = numFaces;
      FieldValidator.checkNumFaces(numFaces3);
      
      numFaces = numFaces3 / 3;
      int numFaces6 = numFaces3 * 2;
      
      faces = new int[numFaces6]; // need to store front and back faces
      
      for (int i = 0, j = 0; i < numFaces; i++, j += 6) {
      // 2 - Point Index 1
      // 2 - Point Index 2
      // 2 - Point Index 3
      int facePoint1 = (ShortConverter.unsign(fm.readShort())) + partOffset;
      int facePoint2 = (ShortConverter.unsign(fm.readShort())) + partOffset;
      int facePoint3 = (ShortConverter.unsign(fm.readShort())) + partOffset;
      
      // reverse face first (so the light shines properly, for this model specifically)
      faces[j] = facePoint3;
      faces[j + 1] = facePoint2;
      faces[j + 2] = facePoint1;
      
      // forward face second
      faces[j + 3] = facePoint1;
      faces[j + 4] = facePoint2;
      faces[j + 5] = facePoint3;
      
      }
      */

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