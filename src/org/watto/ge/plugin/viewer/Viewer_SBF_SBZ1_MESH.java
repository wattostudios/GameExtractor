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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_3DModel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_SBF_SBZ1;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ShortConverter;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Point3D;
//import java.awt.Image;
import javafx.scene.image.Image;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_SBF_SBZ1_MESH extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_SBF_SBZ1_MESH() {
    super("SBF_SBZ1_MESH", "SBF_SBZ1_MESH Model");
    setExtensions("mesh");

    setGames("SEGA Rally Revo");
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
      if (readPlugin instanceof Plugin_SBF_SBZ1) {
        rating += 50;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 20; // don't want to match anything unless it's part of this plugin
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

      long arcSize = fm.getLength();

      // Read in the model

      // Set up the mesh
      //TriangleMesh triangleMesh = new TriangleMesh();
      MeshView[] meshView = null; // we're using MeshView, as we have multiple parts

      float[] points = null;
      //float[] normals = null;
      float[] texCoords = null;
      int[] faces = null;

      minX = 20000f;
      maxX = -20000f;
      minY = 20000f;
      maxY = -20000f;
      minZ = 20000f;
      maxZ = -20000f;

      // BLOCK 1
      // for each entry in Block 1
      // 4 - Offset?

      // check the first entry is valid
      int offsetEntry = fm.readInt();
      FieldValidator.checkOffset(offsetEntry, arcSize);

      int firstEntry = offsetEntry;

      int numBlock2 = 0;

      // find where it changes back to an earlier entry
      while (fm.getOffset() < arcSize) {
        int nextOffsetEntry = fm.readInt();
        FieldValidator.checkOffset(nextOffsetEntry, arcSize);

        if (nextOffsetEntry < firstEntry) {
          // found the transition
          numBlock2 = nextOffsetEntry;
          break;
        }
        else {
          offsetEntry = nextOffsetEntry;
        }
      }

      // BLOCK 2
      // 4 - Number of Entries in Block 2

      // we already found this value above
      FieldValidator.checkNumFiles(numBlock2);

      // for each entry in Block 2
      // 4 - Offset?
      fm.skip(numBlock2 * 4);

      // PARTS
      long partsDirectoryOffset = fm.getOffset();

      int numParts = Archive.getMaxFiles();

      int[] entryLengths = new int[0];
      int[] numPartVertices = new int[0];
      int[] numPartFaces = new int[0];
      long[] vertexOffsets = new long[0];
      long[] faceOffsets = new long[0];

      for (int p = 0; p < numParts; p++) {
        // 4 - Vertex Entry Length (44/48/20/...)
        int entryLength = fm.readInt();
        FieldValidator.checkRange(entryLength, 12, 100); // guess max 100

        // 4 - null
        // 4 - Unknown
        // 8 - null
        fm.skip(16);

        // 4 - Number of Vertices
        int numVertices = fm.readInt();
        FieldValidator.checkNumVertices(numVertices);

        // 4 - null
        fm.skip(4);

        // 4 - Number of Face Indexes
        int numFaces = fm.readInt();
        FieldValidator.checkNumFaces(numFaces);

        // 4 - Unknown (8/1)
        // 8 - null
        fm.skip(12);

        // 4 - Entry Header Data Offset (relative to the start of the Parts directory)
        if (p == 0) {
          // we can use this field to work out how many parts we have
          numParts = fm.readInt() / 72;
          FieldValidator.checkNumFiles(numParts);

          entryLengths = new int[numParts];
          numPartVertices = new int[numParts];
          numPartFaces = new int[numParts];
          vertexOffsets = new long[numParts];
          faceOffsets = new long[numParts];
        }
        else {
          fm.skip(4);
        }

        // 4 - Vertex Data Offset (relative to the start of the Parts directory)
        long vertexOffset = fm.readInt() + partsDirectoryOffset;
        FieldValidator.checkOffset(vertexOffset, arcSize);

        // 4 - Face Index Data Offset (relative to the start of the Parts directory)
        long faceOffset = fm.readInt() + partsDirectoryOffset;
        FieldValidator.checkOffset(faceOffset, arcSize);

        // 16 - null
        fm.skip(16);

        entryLengths[p] = entryLength;
        numPartVertices[p] = numVertices;
        numPartFaces[p] = numFaces;
        vertexOffsets[p] = vertexOffset;
        faceOffsets[p] = faceOffset;

      }

      // Prepare the MeshView to hold all the parts
      meshView = new MeshView[numParts];

      // now go to each part, read it, and create the MeshView for it
      for (int p = 0; p < numParts; p++) {

        int entryLength = entryLengths[p];
        int numVertices = numPartVertices[p];
        int numFaces = numPartFaces[p];
        long vertexOffset = vertexOffsets[p];
        long faceOffset = faceOffsets[p];

        numFaces -= 2; // -2 because it's a triangle strip, and the first 2 points need to be read before we can form a triangle

        //
        //
        // VERTICES
        //
        //
        fm.seek(vertexOffset);

        int numVertices3 = numVertices * 3;
        points = new float[numVertices3];

        int numPoints2 = numVertices * 2;
        texCoords = new float[numPoints2];

        int extraSize = entryLength - 12;
        FieldValidator.checkPositive(extraSize);

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

          // X - Extra data
          fm.skip(extraSize);

          // Don't know where the texture co-ords are yet (could vary based on the size of the entries ?)
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

        fm.seek(faceOffset);

        int numFaces3 = numFaces * 3;
        FieldValidator.checkNumFaces(numFaces3);

        int numFaces6 = numFaces3 * 2;
        faces = new int[numFaces6]; // need to store front and back faces

        int firstPoint = (ShortConverter.unsign(fm.readShort()));
        int secondPoint = (ShortConverter.unsign(fm.readShort()));

        boolean swap = false; // in a triangle strip, every second triangle needs to swap points 2 and 3
        for (int i = 0, j = 0; i < numFaces; i++, j += 6) {
          // 2 - Point Index 1
          // 2 - Point Index 2
          // 2 - Point Index 3
          int facePoint1 = firstPoint;
          int facePoint2 = secondPoint;
          int facePoint3 = (ShortConverter.unsign(fm.readShort()));

          if (swap) {
            // reverse face first (so the light shines properly, for this model specifically)
            faces[j] = facePoint3;
            faces[j + 1] = facePoint2;
            faces[j + 2] = facePoint1;

            // forward face second
            faces[j + 3] = facePoint1;
            faces[j + 4] = facePoint2;
            faces[j + 5] = facePoint3;
          }
          else {
            // reverse face first (so the light shines properly, for this model specifically)
            faces[j] = facePoint2;
            faces[j + 1] = facePoint3;
            faces[j + 2] = facePoint1;

            // forward face second
            faces[j + 3] = facePoint1;
            faces[j + 4] = facePoint3;
            faces[j + 5] = facePoint2;
          }

          swap = !swap;

          // remember the last 2 points, as they form the first 2 points of the next triangle
          firstPoint = secondPoint;
          secondPoint = facePoint3;
        }

        // add the part to the model
        if (faces != null && points != null && texCoords != null) {
          // we have a full mesh for a single object - add it to the model
          TriangleMesh triangleMesh = new TriangleMesh();
          triangleMesh.getTexCoords().addAll(texCoords);

          triangleMesh.getPoints().addAll(points);
          triangleMesh.getFaces().addAll(faces);
          //triangleMesh.getNormals().addAll(normals);

          faces = null;
          points = null;
          //normals = null;
          texCoords = null;

          // Create the MeshView
          MeshView view = new MeshView(triangleMesh);

          /*
          // We don't actually load the textures, because they're stored in a different archive, so it's too hard to grab it
          String texture = "hamsterwheel.fsh.png";
          if (texture != null) {
            // set the texture
            Image image = loadTextureImage(texture);
            if (image != null) {
              Material material = new PhongMaterial(Color.WHITE, image, (Image) null, (Image) null, (Image) null);
              view.setMaterial(material);
            }
          }
          */

          meshView[p] = view;
        }

      } // now loop back and read the next part

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
    catch (

    Throwable t) {
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

      java.awt.Image image = preview3D.getImage();
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

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Image loadTextureImage(String textureFilename) {
    try {

      /*
      textureFilename = textureFilename.toLowerCase();
      
      // the filename here is .TGA, the filename in the archive is .PC TEXTURE, so we need to change it
      int dotPos = textureFilename.lastIndexOf('.');
      if (dotPos > 0) {
        textureFilename = textureFilename.substring(0, dotPos) + ".pc texture"; // note: lowercase
      }
      
      // now find the resource
      Resource[] resources = Archive.getResources();
      int numResources = resources.length;
      for (int i = 0; i < numResources; i++) {
        Resource currentResource = resources[i];
        if (currentResource.getName().toLowerCase().equals(textureFilename)) {
          // found the right resource
          return loadTextureImage(resources[i]);
        }
      }
      */
      File textureFile = new File(textureFilename);
      if (textureFile.exists() && textureFile.isFile()) {

        // path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(textureFile, textureFilename, 0, textureFile.length());
        resource.setExportedPath(textureFile);
        return loadTextureImage(resource);
      }

      // not found
      return null;
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
  public Image loadTextureImage(Resource imageResource) {
    try {

      // 1. Open the file
      ByteBuffer buffer = new ByteBuffer((int) imageResource.getLength());
      FileManipulator fm = new FileManipulator(buffer);
      imageResource.extract(fm);
      //fm.setFakeFile(new File(imageResource.getName())); // set a fake file here, so that the ViewerPlugins can check the file extension
      fm.setFakeFile(imageResource.getExportedPath());

      // 2. Get all the ViewerPlugins that can read this file type
      RatedPlugin[] plugins = PluginFinder.findPlugins(fm, ViewerPlugin.class); // NOTE: This closes the fm pointer!!!
      if (plugins == null || plugins.length == 0) {
        // no viewer plugins found that will accept this file
        return null;
      }

      Arrays.sort(plugins);

      // re-open the file - it was closed at the end of findPlugins();
      fm = new FileManipulator(buffer);
      fm.setFakeFile(imageResource.getExportedPath()); // set it again, so plugins like PNG can read the file

      // 3. Try each plugin until we find one that can render the file as an ImageResource
      PreviewPanel imagePreviewPanel = null;
      for (int i = 0; i < plugins.length; i++) {
        fm.seek(0); // go back to the start of the file
        imagePreviewPanel = ((ViewerPlugin) plugins[i].getPlugin()).read(fm);

        if (imagePreviewPanel != null) {
          // 4. We have found a plugin that was able to render the image
          break;
        }
      }

      fm.close();

      if (imagePreviewPanel == null || !(imagePreviewPanel instanceof PreviewPanel_Image)) {
        // no plugins were able to open this file successfully
        return null;
      }

      //
      //
      // If we're here, we have a rendered image
      //
      //

      //java.awt.Image image = ((PreviewPanel_Image) imagePreviewPanel).getImage();
      ImageResource imageResourceObj = ((PreviewPanel_Image) imagePreviewPanel).getImageResource();
      imageResourceObj = ImageFormatReader.flipVertically(imageResourceObj); // the previewer flips the image for this format (so the preview displays properly), we need to flip it back
      java.awt.Image image = imageResourceObj.getImage();
      BufferedImage bufImage = null;
      if (image instanceof BufferedImage) {
        bufImage = (BufferedImage) image;
      }
      else {
        bufImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);

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

}