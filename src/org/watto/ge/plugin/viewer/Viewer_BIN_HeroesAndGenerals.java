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

import java.awt.Image;

import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_3DModel;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.ShortConverter;

import javafx.geometry.Point3D;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BIN_HeroesAndGenerals extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_BIN_HeroesAndGenerals() {
    super("BIN_HeroesAndGenerals", "BIN_HeroesAndGenerals Model");
    setExtensions("bin");

    setGames("Heroes and Generals WWII");
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
      if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      if (fm.readByte() == 120) {
        rating += 5;
      }
      else {
        rating = 0;
      }

      if (fm.getFilePath().contains("ZPrimMesh")) {
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
  
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {

    try {

      long arcSize = fm.getLength();

      // Need to decompress the file first

      int decompLength = (int) arcSize * 20; // guess max

      // X - Pixels (ZLib Compression)
      byte[] decompBytes = new byte[decompLength];

      Exporter_ZLib_CompressedSizeOnly exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();
      exporter.open(fm, (int) arcSize, decompLength);

      for (int b = 0; b < decompLength; b++) {
        if (exporter.available()) { // make sure we read the next bit of data, if required
          decompBytes[b] = (byte) exporter.read();
        }
        else { // End Of Compressed Data
          arcSize = b;
          break;
        }
      }

      //FileManipulator temp = new FileManipulator(new File("C:\\" + fm.getFile().getName() + ".out"), true);
      //temp.writeBytes(decompBytes);
      //temp.close();

      // open the decompressed file data for processing
      fm.close();

      fm = new FileManipulator(new ByteBuffer(decompBytes));
      FileManipulator decompFM = fm; // so we can keep going back to this file, to read the next part (if it has multiple parts)

      // Read in the model

      // Set up the mesh
      //TriangleMesh triangleMesh = new TriangleMesh();

      float[] points = null;
      float[] texCoords = null;
      int[] faces = null;

      minX = 20000f;
      maxX = -20000f;
      minY = 20000f;
      maxY = -20000f;
      minZ = 20000f;
      maxZ = -20000f;

      // 2 - Unknown (1)
      // 4 - Number of Floats? (3)
      // 4 - Unknown Float (1.0)
      // 4 - Unknown Float (1.0)
      // 4 - Unknown Float (1.0)
      fm.skip(18);

      // 4 - Number of Parts
      int numParts = fm.readInt();

      MeshView[] meshView = new MeshView[numParts];

      for (int p = 0; p < numParts; p++) {
        //System.out.println(fm.getOffset());

        // 4 - Vertex Data Length
        int vertexDataLength = fm.readInt();
        FieldValidator.checkLength(vertexDataLength, arcSize);

        // 4 - Face Data Length
        int faceDataLength = fm.readInt();
        FieldValidator.checkLength(faceDataLength, arcSize);

        // 4 - Unknown Data Length (often null)
        int unknownLength = fm.readInt();

        // VERTICES
        byte[] vertexData = fm.readBytes(vertexDataLength);

        // FACES
        byte[] faceData = fm.readBytes(faceDataLength);

        // UNKNOWN DATA
        fm.skip(unknownLength);

        // 2 - Vertex Block Size? (24)
        int vertexBlockSize = fm.readShort();
        FieldValidator.checkRange(vertexBlockSize, 12, 100); // guess

        // 2 - Face Block Size? (2)
        int faceBlockSize = fm.readShort();
        FieldValidator.checkRange(faceBlockSize, 2, 4); // guess

        // for (12)
        // 4 - Unknown Float
        fm.skip(48);

        // 1 - Filename Length (including null terminator)
        int filenameLength = ByteConverter.unsign(fm.readByte());

        // X - Filename
        // 1 - null Filename Terminator
        fm.skip(filenameLength);

        // 4 - null
        fm.skip(4);

        // THIS IS THE RIGHT SPOT IN decompFM FOR READING THE NEXT PART

        //
        //
        // VERTICES
        //
        //
        //fm.close(); // don't want to close the decompFM, as we need to use it again for the next part
        fm = new FileManipulator(new ByteBuffer(vertexData));

        int numVertices = vertexDataLength / vertexBlockSize;

        int numVertices3 = numVertices * 3;
        points = new float[numVertices3];

        int numPoints2 = numVertices * 2;
        texCoords = new float[numPoints2];

        int skipSize = vertexBlockSize - 12;
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

          // X - Unknown Data
          fm.skip(skipSize);

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
        fm.close();
        fm = new FileManipulator(new ByteBuffer(faceData));

        int numFaces = faceDataLength / faceBlockSize;

        int numFaces3 = numFaces;
        FieldValidator.checkNumFaces(numFaces3);

        numFaces = numFaces3 / 3;
        int numFaces6 = numFaces3 * 2;

        faces = new int[numFaces6]; // need to store front and back faces

        if (faceBlockSize == 2) {
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
        }
        else if (faceBlockSize == 4) { // guess, do we have this?
          for (int i = 0, j = 0; i < numFaces; i++, j += 6) {
            // 4 - Point Index 1
            // 4 - Point Index 2
            // 4 - Point Index 3
            int facePoint1 = fm.readInt();
            int facePoint2 = fm.readInt();
            int facePoint3 = fm.readInt();

            // reverse face first (so the light shines properly, for this model specifically)
            faces[j] = facePoint3;
            faces[j + 1] = facePoint2;
            faces[j + 2] = facePoint1;

            // forward face second
            faces[j + 3] = facePoint1;
            faces[j + 4] = facePoint2;
            faces[j + 5] = facePoint3;
          }
        }

        fm.close();

        // add the part to the model
        if (faces != null && points != null && texCoords != null) {
          // we have a full mesh for a single object - add it to the model
          TriangleMesh triangleMesh = new TriangleMesh();
          triangleMesh.getTexCoords().addAll(texCoords);

          triangleMesh.getPoints().addAll(points);
          triangleMesh.getFaces().addAll(faces);

          // Create the MeshView
          MeshView view = new MeshView(triangleMesh);
          meshView[p] = view;

          faces = null;
          points = null;
          texCoords = null;
        }

        // put the decompFM back again, ready for reading the next part
        fm = decompFM;

      }

      // now we've finished reading, close close decompFM;
      decompFM.close();

      // calculate the sizes and centers
      float diffX = (maxX - minX);
      float diffY = (maxY - minY);
      float diffZ = (maxZ - minZ);

      float centerX = minX + (diffX / 2);
      float centerY = minY + (diffY / 2);
      float centerZ = minZ + (diffZ / 2);

      Point3D sizes = new Point3D(diffX, diffY, diffZ);
      Point3D center = new Point3D(centerX, centerY, centerZ);

      PreviewPanel_3DModel preview = new PreviewPanel_3DModel(meshView, sizes, center);

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