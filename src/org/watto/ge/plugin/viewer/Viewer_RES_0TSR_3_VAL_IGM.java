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

import java.util.HashMap;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Table;
import org.watto.component.WSTableColumn;
import org.watto.datatype.Archive;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_RES_0TSR_3;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_RES_0TSR_3_VAL_IGM extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_RES_0TSR_3_VAL_IGM() {
    super("RES_0TSR_3_VAL_IGM", "Dirt To Daytona VAL Table");
    setExtensions("val");

    setGames("Dirt To Daytona");
    setPlatforms("PS2");
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
  public boolean canEdit(PreviewPanel panel) {
    if (panel instanceof PreviewPanel_Table) {
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
      if (plugin instanceof Plugin_RES_0TSR_3) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      // 4 - Header
      if (fm.readString(4).equals("!IGM")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      fm.skip(4);

      // 4 - Number of Keys
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  Writes out diagnostic information only
  **********************************************************************************************
  **/

  public void displayDiagnostics(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 4 - Header (!IGM)
      // 4 - Unknown (4235628)
      fm.skip(8);

      // 4 - Number of Keys
      int numKeys = fm.readInt();
      FieldValidator.checkNumFiles(numKeys);

      // 4 - null
      fm.skip(4);

      int[] valueCount = new int[numKeys];
      int[] offsets = new int[numKeys];
      String[] keys = new String[numKeys];

      for (int k = 0; k < numKeys; k++) {
        // 4 - Unknown (4235492)
        fm.skip(4);

        // 32 - Key Name (null terminated, filled with nulls)
        String keyName = fm.readNullString(32);
        FieldValidator.checkFilename(keyName);
        keys[k] = keyName;

        // 4 - Number of Values
        int numValuesForKey = fm.readInt();
        FieldValidator.checkRange(numValuesForKey, 1, 50); // guess
        valueCount[k] = numValuesForKey;

        // 4 - First Value Offset [+4]
        int valueOffset = fm.readInt() + 4;
        FieldValidator.checkOffset(valueOffset, arcSize);
        offsets[k] = valueOffset;

        // 36 - null
        fm.skip(36);
      }

      for (int k = 0; k < numKeys; k++) {
        fm.relativeSeek(offsets[k]);

        int numValues = valueCount[k];

        System.out.println("Child: " + keys[k] + " with " + numValues + " sub-nodes");

        int numValuesToRead = numValues * 2;
        int[] valueOffsets = new int[numValuesToRead];
        for (int v = 0; v < numValuesToRead; v++) {
          // 4 - Value Offset [+4]
          int offset = fm.readInt() + 4;
          FieldValidator.checkOffset(offset, arcSize);
          valueOffsets[v] = offset;
        }

        long thisOffset = fm.getOffset();

        for (int v = 0; v < numValuesToRead; v += 2) {
          int offset = valueOffsets[v];

          // X - Value Data
          // 1 - null Value Terminator
          fm.relativeSeek(offset);
          String value = fm.readNullString();
          int valueLength = value.length();

          fm.relativeSeek(offset);
          byte[] valueBytes = fm.readBytes(valueLength);
          fm.skip(1);

          String key = "";

          if (valueLength == 0) {
            key = "<empty>";
          }
          else {
            if (valueBytes[0] < 0) {
              // XOR'd with (byte)165
              for (int b = 0; b < valueLength; b++) {
                valueBytes[b] ^= (byte) 165;
              }
              value = new String(valueBytes);
              key = value;
            }
            else {
              // plain text
              key = value;
            }
          }

          offset = valueOffsets[v + 1];

          // X - Value Data
          // 1 - null Value Terminator
          fm.relativeSeek(offset);
          value = fm.readNullString();
          valueLength = value.length();

          fm.relativeSeek(offset);
          valueBytes = fm.readBytes(valueLength);
          fm.skip(1);

          String keyValue = "";

          if (valueLength == 0) {
            System.out.println("\t" + "<empty>");
            keyValue = "<empty>";
          }
          else {
            if (valueBytes[0] < 0) {
              // XOR'd with (byte)165
              for (int b = 0; b < valueLength; b++) {
                valueBytes[b] ^= (byte) 165;
              }
              value = new String(valueBytes);
              keyValue = value;
            }
            else {
              // plain text
              keyValue = value;
            }
          }

          System.out.println("\t" + key + " = " + keyValue);

        }

        fm.relativeSeek(thisOffset);
      }

    }
    catch (Throwable t) {
    }

    // back to the beginning, ready to read properly for the viewer
    fm.seek(0);

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

      displayDiagnostics(fm);

      /*
      // THIS LOADS INTO A TREE. WE'VE CHANGED TO JUST LOADING THE UNIQUE VALUES INTO A TABLE INSTEAD.
      long arcSize = fm.getLength();
      
      EditableTreeNode root = new EditableTreeNode(fm.getFile().getName());
      
      // 4 - Header (!IGM)
      // 4 - Unknown (4235628)
      fm.skip(8);
      
      // 4 - Number of Keys
      int numKeys = fm.readInt();
      FieldValidator.checkNumFiles(numKeys);
      
      // 4 - null
      fm.skip(4);
      
      int[] valueCount = new int[numKeys];
      int[] offsets = new int[numKeys];
      String[] keys = new String[numKeys];
      
      for (int k = 0; k < numKeys; k++) {
        // 4 - Unknown (4235492)
        fm.skip(4);
      
        // 32 - Key Name (null terminated, filled with nulls)
        String keyName = fm.readNullString(32);
        FieldValidator.checkFilename(keyName);
        keys[k] = keyName;
      
        // 4 - Number of Values
        int numValuesForKey = fm.readInt();
        FieldValidator.checkRange(numValuesForKey, 1, 50); // guess
        valueCount[k] = numValuesForKey;
      
        // 4 - First Value Offset [+4]
        int valueOffset = fm.readInt() + 4;
        FieldValidator.checkOffset(valueOffset, arcSize);
        offsets[k] = valueOffset;
      
        // 36 - null
        fm.skip(36);
      }
      
      for (int k = 0; k < numKeys; k++) {
        fm.relativeSeek(offsets[k]);
      
        int numValues = valueCount[k];
      
        EditableTreeNode keyNode = new EditableTreeNode(keys[k]);
        root.add(keyNode);
      
        int numValuesToRead = numValues * 2;
        int[] valueOffsets = new int[numValuesToRead];
        for (int v = 0; v < numValuesToRead; v++) {
          // 4 - Value Offset [+4]
          int offset = fm.readInt() + 4;
          FieldValidator.checkOffset(offset, arcSize);
          valueOffsets[v] = offset;
        }
      
        long thisOffset = fm.getOffset();
      
        for (int v = 0; v < numValuesToRead; v += 2) {
          int offset = valueOffsets[v];
      
          // X - Value Data
          // 1 - null Value Terminator
          fm.relativeSeek(offset);
          String value1 = fm.readNullString();
          fm.relativeSeek(offset);
          byte[] value1Bytes = fm.readBytes(value1.length());
          if (value1Bytes.length > 0) {
            if (value1Bytes[0] < 0) {
              // numeric, maybe
              value1 = HexConverter.convertLittle(value1Bytes).toString();
            }
            else {
              // text
            }
          }
      
          offset = valueOffsets[v + 1];
      
          // X - Value Data
          // 1 - null Value Terminator
          fm.relativeSeek(offset);
          String value2 = fm.readNullString();
          fm.relativeSeek(offset);
          byte[] value2Bytes = fm.readBytes(value2.length());
          if (value2Bytes.length > 0) {
            if (value2Bytes[0] < 0) {
              // numeric, maybe
              value2 = HexConverter.convertLittle(value2Bytes).toString();
            }
            else {
              // text
            }
          }
      
          EditableTreeNode pairNode = new EditableTreeNode("Value Pair");
      
          EditableTreeNode value1Node = new EditableTreeNode(value1, true);
          pairNode.add(value1Node);
          EditableTreeNode value2Node = new EditableTreeNode(value2, true);
          pairNode.add(value2Node);
      
          keyNode.add(pairNode);
        }
      
        fm.relativeSeek(thisOffset);
      }
      
      PreviewPanel_Tree preview = new PreviewPanel_Tree(root);
      
      return preview;
      */

      long arcSize = fm.getLength();

      // 4 - Header (!IGM)
      // 4 - Unknown (4235628)
      fm.skip(8);

      // 4 - Number of Keys
      int numKeys = fm.readInt();
      FieldValidator.checkNumFiles(numKeys);

      // 4 - null
      fm.skip(4);

      int totalValues = 0;
      for (int k = 0; k < numKeys; k++) {
        // 4 - Unknown (4235492)
        // 32 - Key Name (null terminated, filled with nulls)
        fm.skip(36);

        // 4 - Number of Values
        int numValuesForKey = fm.readInt();
        FieldValidator.checkRange(numValuesForKey, 1, 50); // guess
        totalValues += (numValuesForKey * 2);

        // 4 - First Value Offset [+4]
        // 36 - null
        fm.skip(40);
      }

      fm.skip(totalValues * 4);

      Object[][] data = new Object[totalValues][4];

      long offset = fm.getOffset();
      int realNumRows = 0;
      while (offset < arcSize - 4) {
        //System.out.println(offset);

        // X - Value Data
        // 1 - null Value Terminator
        fm.relativeSeek(offset);
        String value = fm.readNullString();
        int valueLength = value.length();

        fm.relativeSeek(offset);
        byte[] valueBytes = fm.readBytes(valueLength);
        fm.skip(1);

        if (valueLength == 0) {
          data[realNumRows] = new Object[] { (int) offset, false, "", "" };
        }
        else {
          if (valueBytes[0] < 0) {
            // XOR'd with (byte)165
            for (int b = 0; b < valueLength; b++) {
              valueBytes[b] ^= (byte) 165;
            }
            value = new String(valueBytes);
            data[realNumRows] = new Object[] { (int) offset, true, value, value };
          }
          else {
            // plain text
            data[realNumRows] = new Object[] { (int) offset, false, value, value };
          }
        }

        realNumRows++;
        offset = fm.getOffset();
      }

      if (realNumRows < totalValues) {
        Object[][] oldData = data;
        data = new Object[realNumRows][3];
        for (int i = 0; i < realNumRows; i++) {
          data[i] = oldData[i];
        }
      }

      WSTableColumn[] columns = new WSTableColumn[4];
      columns[0] = new WSTableColumn("Offset", 'n', Integer.class, false, true, 0, 0); // hidden column 0,0
      columns[1] = new WSTableColumn("Encrypted", 'e', Boolean.class, false, true, 0, 0); // hidden column 0,0
      columns[2] = new WSTableColumn("Original", 'o', String.class, false, true, 0, 0); // hidden column 0,0
      columns[3] = new WSTableColumn("Edited", 'e', String.class, true, true);

      PreviewPanel_Table preview = new PreviewPanel_Table(data, columns);

      return preview;

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
  public void edit(FileManipulator originalFM, PreviewPanel preview, FileManipulator fm) {
    // We can't write from scratch, but we can Edit the old file

    /*
    // THIS LOADS INTO A TREE. WE'VE CHANGED TO JUST LOADING THE UNIQUE VALUES INTO A TABLE INSTEAD.
    
    // should only be triggered from Tree panels
    if (!(preview instanceof PreviewPanel_Tree)) {
      return;
    }
    PreviewPanel_Tree previewPanel = (PreviewPanel_Tree) preview;
    
    // Get the tree nodes from the preview (which are the edited ones), so we know which ones have been changed, and can put that data
    // into the "fm" as we read through and replicate the "originalFM"
    TreeNode rootNode = previewPanel.getRootNode();
    
    long arcSize = originalFM.getLength();
    
    // 4 - Header (!IGM)
    // 4 - Unknown (4235628)
    fm.writeBytes(originalFM.readBytes(8));
    
    // 4 - Number of Keys
    int numKeys = originalFM.readInt();
    FieldValidator.checkNumFiles(numKeys);
    fm.writeInt(numKeys);
    
    // 4 - null
    fm.writeBytes(originalFM.readBytes(4));
    
    int[] valueCount = new int[numKeys];
    int[] offsets = new int[numKeys];
    
    int totalValues = 0;
    for (int k = 0; k < numKeys; k++) {
      // 4 - Unknown (4235492)
      // 32 - Key Name (null terminated, filled with nulls)
      fm.writeBytes(originalFM.readBytes(36));
    
      // 4 - Number of Values
      int numValuesForKey = originalFM.readInt();
      FieldValidator.checkRange(numValuesForKey, 1, 50); // guess
      valueCount[k] = numValuesForKey;
      fm.writeInt(numValuesForKey);
    
      totalValues += (numValuesForKey * 2);
    
      // 4 - First Value Offset [+4]
      int valueOffset = originalFM.readInt() + 4;
      FieldValidator.checkOffset(valueOffset, arcSize);
      offsets[k] = valueOffset;
      fm.writeInt(valueOffset - 4);
    
      // 36 - null
      fm.writeBytes(originalFM.readBytes(36));
    }
    
    // work out where we're writing to for the actual data
    long outOffset = 16 + (numKeys * 80) + (totalValues * 8);
    
    for (int k = 0; k < numKeys; k++) {
      originalFM.relativeSeek(offsets[k]);
    
      int numValues = valueCount[k];
    
      EditableTreeNode keyNode = new EditableTreeNode(keys[k]);
      root.add(keyNode);
    
      int numValuesToRead = numValues * 2;
      int[] valueOffsets = new int[numValuesToRead];
      for (int v = 0; v < numValuesToRead; v++) {
        // 4 - Value Offset [+4]
        int offset = originalFM.readInt() + 4;
        FieldValidator.checkOffset(offset, arcSize);
        valueOffsets[v] = offset;
      }
    
      long thisOffset = originalFM.getOffset();
    
      for (int v = 0; v < numValuesToRead; v += 2) {
        int offset = valueOffsets[v];
    
        // X - Value Data
        // 1 - null Value Terminator
        originalFM.relativeSeek(offset);
        String value1 = originalFM.readNullString();
        originalFM.relativeSeek(offset);
        byte[] value1Bytes = originalFM.readBytes(value1.length());
        if (value1Bytes.length > 0) {
          if (value1Bytes[0] < 0) {
            // numeric, maybe
            value1 = HexConverter.convertLittle(value1Bytes).toString();
          }
          else {
            // text
          }
        }
    
        offset = valueOffsets[v + 1];
    
        // X - Value Data
        // 1 - null Value Terminator
        originalFM.relativeSeek(offset);
        String value2 = originalFM.readNullString();
        originalFM.relativeSeek(offset);
        byte[] value2Bytes = originalFM.readBytes(value2.length());
        if (value2Bytes.length > 0) {
          if (value2Bytes[0] < 0) {
            // numeric, maybe
            value2 = HexConverter.convertLittle(value2Bytes).toString();
          }
          else {
            // text
          }
        }
    
        EditableTreeNode pairNode = new EditableTreeNode("Value Pair");
    
        EditableTreeNode value1Node = new EditableTreeNode(value1, true);
        pairNode.add(value1Node);
        EditableTreeNode value2Node = new EditableTreeNode(value2, true);
        pairNode.add(value2Node);
    
        keyNode.add(pairNode);
      }
    
      originalFM.relativeSeek(thisOffset);
    }*/

    try {
      // should only be triggered from Table panels
      if (!(preview instanceof PreviewPanel_Table)) {
        return;
      }
      PreviewPanel_Table previewPanel = (PreviewPanel_Table) preview;

      // Get the table data from the preview (which are the edited ones), so we know which ones have been changed, and can put that data
      // into the "fm" as we read through and replicate the "originalFM"
      Object[][] data = previewPanel.getData();
      if (data == null) {
        return;
      }

      int numRows = data.length;

      int offset = (int) data[0][0]; // the starting point

      // mapping the old offset values to new offset values (to allow resizing of the strings)
      HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
      for (int i = 0; i < numRows; i++) {
        Object[] row = data[i];

        map.put((int) row[0], offset);

        Object editedValue = row[3];
        offset += (((String) editedValue).length()) + 1; // +1 for null terminator

      }

      // 4 - Header (!IGM)
      // 4 - Unknown (4235628)
      fm.writeBytes(originalFM.readBytes(8));

      // 4 - Number of Keys
      int numKeys = originalFM.readInt();
      FieldValidator.checkNumFiles(numKeys);
      fm.writeInt(numKeys);

      // 4 - null
      fm.writeBytes(originalFM.readBytes(4));

      int totalValues = 0;
      for (int k = 0; k < numKeys; k++) {
        // 4 - Unknown (4235492)
        // 32 - Key Name (null terminated, filled with nulls)
        fm.writeBytes(originalFM.readBytes(36));

        // 4 - Number of Values
        int numValuesForKey = originalFM.readInt();
        FieldValidator.checkRange(numValuesForKey, 1, 50); // guess
        totalValues += (numValuesForKey * 2); // *2 because they're in pairs
        fm.writeInt(numValuesForKey);

        // 4 - First Value Offset [+4]
        // 36 - null
        fm.writeBytes(originalFM.readBytes(40));
      }

      for (int i = 0; i < totalValues; i++) {
        // 4 - Value Offset [+4]
        int oldOffset = originalFM.readInt() + 4;
        int newOffset = map.getOrDefault(oldOffset, oldOffset);
        fm.writeInt(newOffset - 4);
      }

      for (int i = 0; i < numRows; i++) {
        Object[] row = data[i];

        // X - Value Data

        Object encryptedObject = row[1];
        Object editedValue = row[3];

        boolean encrypted = false;
        if (encryptedObject instanceof Boolean) {
          encrypted = ((Boolean) encryptedObject).booleanValue();
        }

        if (encrypted) {
          // XOR'd with (byte)165

          byte[] editedBytes = ((String) editedValue).getBytes();
          int numBytes = editedBytes.length;

          for (int b = 0; b < numBytes; b++) {
            editedBytes[b] ^= (byte) 165;
          }

          fm.writeBytes(editedBytes);
        }
        else {
          // plain string
          fm.writeString((String) editedValue);
        }

        // 1 - null Value Terminator
        fm.writeByte(0);
      }

      // 4 - null
      fm.writeInt(0);

      fm.close();
      originalFM.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel panel, FileManipulator destination) {

  }

}