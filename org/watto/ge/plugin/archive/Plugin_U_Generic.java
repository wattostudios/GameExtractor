
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.archive.datatype.UnrealProperty;
import org.watto.ge.plugin.exporter.Exporter_Custom_U_Palette_Generic;
import org.watto.ge.plugin.exporter.Exporter_Custom_U_Texture_Generic;
import org.watto.ge.plugin.resource.Resource_Unreal;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_U_Generic extends PluginGroup_U {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_U_Generic() {
    super("U_Generic", "Unreal Engine (Generic)");

    setExtensions("u", "uax", "ukx", "umx", "upx", "usx", "utx", "uvx");
    setGames(
        "Adventure Pinball: Forgotten Island",
        "Brother Bear",
        "Clive Barker's Undying",
        "Deus Ex",
        "Harry Potter And The Chamber Of Secrets",
        "Land Of The Dead: Road To Fiddlers Green",
        "Lemony Snicket's A Series of Unfortunate Events",
        "Medal Of Honor: Airborne",
        "Mobile Forces",
        "Nerf ArenaBlast",
        "Postal 2",
        "Rainbow Six 3: Raven Shield",
        "Redneck Kentucky and the Next Generation Chickens",
        "RoboBlitz",
        "Rune",
        "Rune Classic",
        "Shadow Ops: Red Mercury",
        "Splinter Cell",
        "Splinter Cell: Pandora Tomorrow",
        "Star Trek: Deep Space Nine: The Fallen: Maximum Warp",
        "Star Trek: The Next Generation: Klingon Honor Guard",
        "Star Wars: Republic Commando",
        "SWAT 4",
        "Thief 3: Deadly Shadows",
        "Tribes Vengeance",
        "Unreal",
        "Unreal Gold",
        "Unreal 2: The Awakening",
        "Unreal Tournament",
        "Unreal Tournament 2003",
        "Unreal Tournament 2004",
        "Virtual Reality Notre Dame: A Real Time Construction",
        "Wheel Of Time",
        "X-Com Enforcer",
        "XIII");
    setPlatforms("PC", "XBox");

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    return super.getMatchRating(fm, -1);
  }

  /**
   **********************************************************************************************
   * Basic Format
   **********************************************************************************************
   **/
  @Override
  public Resource[] readGeneric(File path, FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 4 - Header
      // 2 - Version
      // ALREADY KNOW THESE 2 FIELDS FROM read()

      // 2 - License Mode
      // 4 - Package Flags
      fm.skip(6);

      // 4 - Number Of Names
      int numNames = fm.readInt();
      FieldValidator.checkNumFiles(numNames);

      // 4 - Name Directory Offset
      long nameOffset = fm.readInt();
      FieldValidator.checkOffset(nameOffset, arcSize);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - File Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Number Of Types
      int numTypes = fm.readInt();
      FieldValidator.checkNumFiles(numTypes);

      // 4 - Type Directory Offset
      long typesOffset = fm.readInt();
      FieldValidator.checkOffset(typesOffset, arcSize);

      // if (version < 68){
      //   4 - Number Of Generations
      //   4 - Generation Directory Offset
      //   for each generation
      //     16 - GUID Hash
      //   }
      // else {
      //   16 - GUID Hash
      //   4 - Number Of Generations
      //   for each generation
      //     4 - Number Of Names
      //     4 - Number Of Files
      //   }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // read the names directory
      fm.seek(nameOffset);

      names = new String[numNames];

      // Loop through directory
      if (version < 64) {
        for (int i = 0; i < numNames; i++) {
          // X - Name
          // 1 - null Name Terminator
          String name = fm.readNullString();
          FieldValidator.checkFilename(name);
          names[i] = name;

          // 4 - Flags
          fm.skip(4);
        }
      }
      else {
        for (int i = 0; i < numNames; i++) {
          // 1 - Name Length (including null)
          int nameLength = ByteConverter.unsign(fm.readByte()) - 1;

          // X - Name
          names[i] = fm.readString(nameLength);

          // 1 - null Name Terminator
          // 4 - Flags
          fm.skip(5);
        }
      }

      // read the types directory
      fm.seek(typesOffset);

      String[] types = new String[numTypes];

      // Loop through directory
      for (int i = 0; i < numTypes; i++) {
        // 1-5 - Package Name ID
        readIndex(fm);

        // 1-5 - Format Name ID
        readIndex(fm);

        // 4 - Package Object ID
        fm.skip(4);

        // 1-5 - Object Name ID
        int objectID = (int) readIndex(fm);
        types[i] = names[objectID];
      }

      // read the files directory
      fm.seek(dirOffset);

      String[] parentNames = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 1-5 - Type Object ID
        int typeID = (int) readIndex(fm);
        String type = "";

        if (typeID > 0) {
          typeID--;
          FieldValidator.checkLength(typeID, numNames); // check for the name
          type = names[typeID];
        }
        else if (typeID == 0) {
          type = names[0];
        }
        else {
          typeID = (0 - typeID) - 1;
          FieldValidator.checkLength(typeID, numTypes); // check for the name
          type = types[typeID];
        }

        // 1-5 - Parent Object ID
        readIndex(fm);

        // 4 - Package Object ID [-1]
        int parentID = fm.readInt();
        if (parentID > 0) {
          parentID--;
          FieldValidator.checkLength(parentID, numFiles);
        }
        else if (parentID == 0) {
          //parentID = -1; // don't want to look this entry up in the names table
        }

        // 1-5 - Object Name ID
        int nameID = (int) readIndex(fm);
        FieldValidator.checkLength(nameID, numNames); // checks the ID is within the names list

        // 4 - Flags
        fm.skip(4);

        // 1-5 - File Length
        long length = readIndex(fm);
        FieldValidator.checkLength(length, arcSize);

        // 1-5 - File Offset
        long offset = 0;
        if (length > 0) {
          offset = readIndex(fm);
          FieldValidator.checkOffset(offset - 1, arcSize); // to trap offset=0
        }

        // put the parent IDs before the filename, in a directory structure.
        String filename = names[nameID];
        if (parentID >= 0) {
          String parentName = parentNames[parentID];
          if (parentName != null) {
            filename = parentName + "\\" + filename;
          }
        }
        parentNames[i] = filename;

        // append the type name
        filename += "." + type;

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource_Unreal(path, filename, offset, length);

        if (type.equals("Texture")) {
          resources[i].setExporter(Exporter_Custom_U_Texture_Generic.getInstance());
        }
        else if (type.equals("Palette")) {
          resources[i].setExporter(Exporter_Custom_U_Palette_Generic.getInstance());
        }

        TaskProgressManager.setValue(i);
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * Basic Format
   **********************************************************************************************
   **/
  @Override
  public Resource[] readGenericUE3(File path, FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 4 - Header
      // 2 - Version
      // ALREADY KNOW THESE 2 FIELDS FROM read()

      // 2 - License Mode
      // 4 - First File Offset
      fm.skip(6);

      // 4 - Base Name Length (including null) (eg "bg", "none", etc)
      int baseNameLength = fm.readInt();
      FieldValidator.checkFilenameLength(baseNameLength);

      // X - Base Name
      // 1 - null Base Name Terminator
      fm.skip(baseNameLength);

      // 4 - Package Flags
      fm.skip(4);

      // 4 - Number Of Names
      int numNames = fm.readInt();
      FieldValidator.checkNumFiles(numNames);

      // 4 - Name Directory Offset
      long nameOffset = fm.readInt();
      FieldValidator.checkOffset(nameOffset, arcSize);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - File Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Number Of Types
      int numTypes = fm.readInt();
      FieldValidator.checkNumFiles(numTypes);

      // 4 - Type Directory Offset
      long typesOffset = fm.readInt();
      FieldValidator.checkOffset(typesOffset, arcSize);

      // 4 - Unknown
      // 16 - GUID Hash
      fm.skip(20);

      // 4 - Generation Count
      int numGenerations = fm.readInt();
      FieldValidator.checkNumFiles(numGenerations);

      // for each generation
      // 4 - Number Of Files
      // 4 - Number Of Names
      fm.skip(numGenerations * 8);

      // 4 - Unknown
      // 4 - Unknown (2859)
      // 4 - Unknown (38)
      fm.skip(12);

      // 4 - Compression Type? (0=none/2=archives)
      int compression = fm.readInt();

      // 4 - Number Of Archives (0 if field above is 0);
      int numArchives = fm.readInt();

      if (compression == 2) {
        return readGenericUE3Collection(path, fm, numArchives);
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // read the names directory
      fm.seek(nameOffset);

      names = new String[numNames];

      // Loop through directory
      for (int i = 0; i < numNames; i++) {
        // 4 - Name Length (including null)
        int nameLength = fm.readInt() - 1;
        FieldValidator.checkFilenameLength(nameLength);

        // X - Name
        names[i] = fm.readString(nameLength);

        // 1 - null Name Terminator
        // 4 - null
        // 4 - Flags
        fm.skip(9);
      }

      // read the types directory
      fm.seek(typesOffset);

      String[] types = new String[numTypes];

      // Loop through directory
      for (int i = 0; i < numTypes; i++) {
        // 8 - Package Name ID
        // 8 - Format Name ID
        // 4 - Package Object ID
        fm.skip(20);

        // 8 - Object Name ID
        int objectID = (int) fm.readLong();
        types[i] = names[objectID];
      }

      // read the files directory
      fm.seek(dirOffset);

      String[] parentNames = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Type Object ID
        int typeID = fm.readInt();
        String type = "";

        if (typeID > 0) {
          typeID--;
          FieldValidator.checkLength(typeID, numNames); // check for the name
          type = names[typeID];
        }
        else if (typeID == 0) {
          type = names[0];
        }
        else {
          typeID = (0 - typeID) - 1;
          FieldValidator.checkLength(typeID, numTypes); // check for the name
          type = types[typeID];
        }

        // 4 - Parent Object ID
        fm.skip(4);

        // 4 - Package Object ID [-1]
        int parentID = fm.readInt();
        if (parentID > 0) {
          parentID--;
          FieldValidator.checkLength(parentID, numFiles);
        }
        else if (parentID == 0) {
          //parentID = -1; // don't want to look this entry up in the names table
        }

        // 8 - Object Name ID
        int nameID = (int) fm.readLong();
        FieldValidator.checkLength(nameID, numNames); // checks the ID is within the names list

        // 8 - null
        // 4 - Flags
        fm.skip(12);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Count
        int count = fm.readInt();
        // for each count
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(count * 12);

        // 24 - null
        fm.skip(24);

        // put the parent IDs before the filename, in a directory structure.
        String filename = names[nameID];
        if (parentID >= 0) {
          String parentName = parentNames[parentID];
          if (parentName != null) {
            filename = parentName + "\\" + filename;
          }
        }
        parentNames[i] = filename;

        // append the type name
        filename += "." + type;

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource_Unreal(path, filename, offset, length);

        //if (type.equals("Texture")){
        //  resources[i].setExporter(Exporter_Custom_U_Texture_Generic.getInstance());
        //  }
        //else if (type.equals("Palette")){
        //  resources[i].setExporter(Exporter_Custom_U_Palette_Generic.getInstance());
        //  }

        TaskProgressManager.setValue(i);
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * Basic Format
   **********************************************************************************************
   **/
  public Resource[] readGenericUE3Collection(File path, FileManipulator fm, int numFiles) {
    try {

      long arcSize = fm.getLength();

      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      for (int i = 0; i < numFiles; i++) {

        // 4 - Unknown
        fm.skip(4);

        // 4 - Decompressed Length?
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Archive Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i) + ".u";

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource_Unreal(path, filename, offset, length, decompLength);

        TaskProgressManager.setValue(i);
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * Reads a single Property
   **********************************************************************************************
   **/
  @Override
  public UnrealProperty readProperty(FileManipulator fm) throws Exception {

    long nameID = readIndex(fm);

    try {
      if (names[(int) nameID].equals("None")) {
        return null;
      }
    }
    catch (Throwable t) {
      return null;
    }

    int infoByte = ByteConverter.unsign(fm.readByte());

    boolean array = (infoByte >= 128);
    int size = (infoByte >> 4) & 7;
    int type = (infoByte & 15);

    // convert sizes
    if (size == 0) {
      size = 1;
    }
    else if (size == 1) {
      size = 2;
    }
    else if (size == 2) {
      size = 4;
    }
    else if (size == 3) {
      size = 12;
    }
    else if (size == 4) {
      size = 16;
    }
    else if (size == 5) {
      size = ByteConverter.unsign(fm.readByte());
    }
    else if (size == 6) {
      size = fm.readShort();
    }
    else if (size == 7) {
      size = fm.readInt();
    }

    // read array index
    int arrayIndex = 0;
    if (type == 3) {
      // boolean type - not an array
      if (array) {
        arrayIndex = 1;
      }
    }
    else if (array) {
      // array - need to read index
      arrayIndex = readArrayIndex(fm);
    }

    //System.out.println(fm.getOffset() + "\t" + arrayIndex + "\t" + size + "\t" + type + "\t" + nameID + "\t" + names[(int)nameID]);

    UnrealProperty property = new UnrealProperty(names[(int) nameID], nameID, arrayIndex, size, type);

    // analyse property data
    if (type == 0) {
      return readProperty0(property, fm);
    }
    else if (type == 1) {
      return readProperty1(property, fm);
    }
    else if (type == 2) {
      return readProperty2(property, fm);
    }
    else if (type == 3) {
      return readProperty3(property, fm);
    }
    else if (type == 4) {
      return readProperty4(property, fm);
    }
    else if (type == 5) {
      return readProperty5(property, fm);
    }
    else if (type == 6) {
      return readProperty6(property, fm);
    }
    else if (type == 7) {
      return readProperty7(property, fm);
    }
    else if (type == 8) {
      return readProperty8(property, fm);
    }
    else if (type == 9) {
      return readProperty9(property, fm);
    }
    else if (type == 10) {
      return readProperty10(property, fm);
    }
    else if (type == 11) {
      return readProperty11(property, fm);
    }
    else if (type == 12) {
      return readProperty12(property, fm);
    }
    else if (type == 13) {
      return readProperty13(property, fm);
    }
    else if (type == 14) {
      return readProperty14(property, fm);
    }
    else if (type == 15) {
      return readProperty15(property, fm);
    }

    return property;

  }

  /**
   **********************************************************************************************
   * Overwritten - slight change
   **********************************************************************************************
   **/
  @Override
  public UnrealProperty readProperty10(UnrealProperty property, FileManipulator fm) {
    // Structure

    String name = names[(int) property.getNameID()];

    if (name.equals("MipZero") || name.equals("MaxColor")) {
      property.setValue(fm.readBytes(5));
    }
    else {
      // normal
      property.setValue(fm.readBytes((int) property.getLength()));
    }
    return property;
  }

}