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
import org.watto.SingletonManager;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_3DModel;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_ZBD;
import org.watto.io.FileManipulator;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ZBD_ZBDMDL extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ZBD_ZBDMDL() {
    super("ZBD_ZBDMDL", "Recoil ZBD_MDL Model");
    setExtensions("zbd_mdl");

    setGames("Recoil");
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
      if (readPlugin instanceof Plugin_ZBD) {
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

      // Get the numFaces and numVertices (stored as properties on the image)
      Object resourceObject = SingletonManager.get("CurrentResource");
      if (resourceObject == null || !(resourceObject instanceof Resource)) {
        return null;
      }
      Resource resource = (Resource) resourceObject;

      int numFaces = -1;
      int numVertices = -1;
      int numNormals = -1;
      int numNormals2 = -1;
      int numBlock76 = -1;
      try {
        numFaces = Integer.parseInt(resource.getProperty("FaceCount"));
        numVertices = Integer.parseInt(resource.getProperty("VertexCount"));
        numNormals = Integer.parseInt(resource.getProperty("NormalCount"));
        numNormals2 = Integer.parseInt(resource.getProperty("Normal2Count"));
        numBlock76 = Integer.parseInt(resource.getProperty("Block76Count"));
      }
      catch (Throwable t) {
        //
      }

      if (numFaces == -1 || numVertices == -1 || numNormals == -1 || numNormals2 == -1 || numBlock76 == -1) {
        ErrorLogger.log("[Viewer_ZBD_ZBDMDL] NumFaces or NumVertices or NumNormals missing");
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
      // VERTICES
      //
      //

      int numVertices3 = numVertices * 3;
      points = new float[numVertices3];
      normals = new float[numVertices3];

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

        // Use empty tex-coords for now
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
      // NORMALS ETC
      //
      //

      // Just skip them
      fm.skip(numNormals * 12);

      fm.skip(numNormals2 * 12);

      fm.skip(numBlock76 * 76 + numBlock76 * 12);

      //
      //
      // FACES
      //
      //
      int[] numPointsInFaces = new int[numFaces];
      int[] numExtraInFaces = new int[numFaces];
      boolean[] faceHasCoords = new boolean[numFaces];
      int totalTriangles = 0;
      for (int i = 0; i < numFaces; i++) {
        // 1 - Number of Points
        // 1 - Normals Flag (0=? 1=? 2=Normals included in this point)
        // 2 - null
        int numPointsInFace = fm.readByte();
        int numExtraInFace = fm.readByte();
        fm.skip(2);
        numPointsInFaces[i] = numPointsInFace;
        numExtraInFaces[i] = numExtraInFace;

        int numTrianglesForFace = 1 + (numPointsInFace - 3);
        /*
        int numTrianglesForFace = 1;
        if (numPointsInFace == 3) {
          // OK already
        }
        else if (numPointsInFace == 4 || numPointsInFace == 6) {
          numTrianglesForFace = 2; // either 2 triangles, or 1 polygon (which we'll do as 2 triangles)
        }
        else if (numPointsInFace == 5) {
          numTrianglesForFace = 3; // 1 polygon which we'll do as 3 triangles
        }
        else if (numPointsInFace == 8) {
          numTrianglesForFace = 4; // 2 polygons, which we'll do as 4 triangles
        }
        else {
          ErrorLogger.log("[Viewer_ZBD_ZBDMDL] Can't handle NumPointsInFace=" + numPointsInFace);
        }
        */
        totalTriangles += numTrianglesForFace;

        // 4 - null
        // 4 - Unknown
        // 4 - null
        fm.skip(12);

        // 4 - TexCoords Flag? (0 = no TexCoords for this point)
        boolean faceHasCoordsFlag = (fm.readInt() != 0);
        faceHasCoords[i] = faceHasCoordsFlag;

        // 4 - Unknown ID/Count
        // 2 - Unknown (1)
        // 2 - Unknown (-1)
        fm.skip(8);
      }

      int numFaces3 = totalTriangles * 3;
      FieldValidator.checkNumFaces(numFaces3);

      //numFaces = numFaces3 / 3;
      int numFaces6 = numFaces3 * 2;

      faces = new int[numFaces6]; // need to store front and back faces

      for (int i = 0, j = 0; i < numFaces; i++) {
        int numPointsInFace = numPointsInFaces[i];

        // for each point in each face
        //   4 - Point ID
        int[] facePoints = new int[numPointsInFace];
        for (int p = 0; p < numPointsInFace; p++) {
          facePoints[p] = fm.readInt();
        }

        if (faceHasCoords[i]) {
          // for each point in each face
          //   4 - Float X
          //   4 - Float Y
          fm.skip(numPointsInFace * 8);
        }

        for (int t = 3, z = 0; t <= numPointsInFace; t++, z++) {

          // reverse face first (so the light shines properly)
          faces[j] = facePoints[2 + z];
          faces[j + 1] = facePoints[1 + z];
          faces[j + 2] = facePoints[0];

          // forward face second
          faces[j + 3] = facePoints[0];
          faces[j + 4] = facePoints[1 + z];
          faces[j + 5] = facePoints[2 + z];

          j += 6;

        }

        /*
        // Now make triangles from the polygons
        if (numPointsInFace == 3) {
          // single triangle
        
          // reverse face first (so the light shines properly, for this model specifically)
          faces[j] = facePoints[2];
          faces[j + 1] = facePoints[1];
          faces[j + 2] = facePoints[0];
        
          // forward face second
          faces[j + 3] = facePoints[0];
          faces[j + 4] = facePoints[1];
          faces[j + 5] = facePoints[2];
        
          j += 6;
        }
        else if (numPointsInFace == 4) {
          // single polygon (convert to 2 triangles)
        
          // reverse face first (so the light shines properly, for this model specifically)
          faces[j] = facePoints[2];
          faces[j + 1] = facePoints[1];
          faces[j + 2] = facePoints[0];
        
          // forward face second
          faces[j + 3] = facePoints[0];
          faces[j + 4] = facePoints[1];
          faces[j + 5] = facePoints[2];
        
          j += 6;
        
          // reverse face first (so the light shines properly, for this model specifically)
          faces[j] = facePoints[3];
          faces[j + 1] = facePoints[2];
          faces[j + 2] = facePoints[0];
        
          // forward face second
          faces[j + 3] = facePoints[0];
          faces[j + 4] = facePoints[2];
          faces[j + 5] = facePoints[3];
        
          j += 6;
        }
        else if (numPointsInFace == 5) {
          // 1 polygon which we'll do as 3 triangles
        
          // reverse face first (so the light shines properly, for this model specifically)
          faces[j] = facePoints[2];
          faces[j + 1] = facePoints[1];
          faces[j + 2] = facePoints[0];
        
          // forward face second
          faces[j + 3] = facePoints[0];
          faces[j + 4] = facePoints[1];
          faces[j + 5] = facePoints[2];
        
          j += 6;
        
          // reverse face first (so the light shines properly, for this model specifically)
          faces[j] = facePoints[3];
          faces[j + 1] = facePoints[2];
          faces[j + 2] = facePoints[0];
        
          // forward face second
          faces[j + 3] = facePoints[0];
          faces[j + 4] = facePoints[2];
          faces[j + 5] = facePoints[3];
        
          j += 6;
        
          // reverse face first (so the light shines properly, for this model specifically)
          faces[j] = facePoints[4];
          faces[j + 1] = facePoints[3];
          faces[j + 2] = facePoints[0];
        
          // forward face second
          faces[j + 3] = facePoints[0];
          faces[j + 4] = facePoints[3];
          faces[j + 5] = facePoints[4];
        
          j += 6;
        }
        else if (numPointsInFace == 6) {
          // 2 triangles
        
          // reverse face first (so the light shines properly, for this model specifically)
          faces[j] = facePoints[2];
          faces[j + 1] = facePoints[1];
          faces[j + 2] = facePoints[0];
        
          // forward face second
          faces[j + 3] = facePoints[0];
          faces[j + 4] = facePoints[1];
          faces[j + 5] = facePoints[2];
        
          j += 6;
        
          // reverse face first (so the light shines properly, for this model specifically)
          faces[j] = facePoints[5];
          faces[j + 1] = facePoints[4];
          faces[j + 2] = facePoints[3];
        
          // forward face second
          faces[j + 3] = facePoints[3];
          faces[j + 4] = facePoints[4];
          faces[j + 5] = facePoints[5];
        
          j += 6;
        }
        else if (numPointsInFace == 8) {
          // 2 polygons (convert to 2 triangles each)
        
          // polygon 1
        
          // reverse face first (so the light shines properly, for this model specifically)
          faces[j] = facePoints[2];
          faces[j + 1] = facePoints[1];
          faces[j + 2] = facePoints[0];
        
          // forward face second
          faces[j + 3] = facePoints[0];
          faces[j + 4] = facePoints[1];
          faces[j + 5] = facePoints[2];
        
          j += 6;
        
          // reverse face first (so the light shines properly, for this model specifically)
          faces[j] = facePoints[3];
          faces[j + 1] = facePoints[2];
          faces[j + 2] = facePoints[0];
        
          // forward face second
          faces[j + 3] = facePoints[0];
          faces[j + 4] = facePoints[2];
          faces[j + 5] = facePoints[3];
        
          j += 6;
        
          // polygon 2
        
          // reverse face first (so the light shines properly, for this model specifically)
          faces[j] = facePoints[6];
          faces[j + 1] = facePoints[5];
          faces[j + 2] = facePoints[4];
        
          // forward face second
          faces[j + 3] = facePoints[4];
          faces[j + 4] = facePoints[5];
          faces[j + 5] = facePoints[6];
        
          j += 6;
        
          // reverse face first (so the light shines properly, for this model specifically)
          faces[j] = facePoints[7];
          faces[j + 1] = facePoints[6];
          faces[j + 2] = facePoints[4];
        
          // forward face second
          faces[j + 3] = facePoints[4];
          faces[j + 4] = facePoints[6];
          faces[j + 5] = facePoints[7];
        
          j += 6;
        }
        */

        if (numExtraInFaces[i] == 2) {
          fm.skip(numPointsInFace * 4);
        }

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