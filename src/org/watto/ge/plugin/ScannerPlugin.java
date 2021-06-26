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

package org.watto.ge.plugin;

import org.watto.Language;
import org.watto.component.WSObjectPlugin;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.io.FileManipulator;

public abstract class ScannerPlugin extends WSObjectPlugin {

  static FieldValidator check = new FieldValidator();

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ScannerPlugin(String code, String name) {
    setCode(code);
    setName(name);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {
    return toString() + "\n\n" + Language.get("Description_ScannerPlugin");
  }

  /**
  **********************************************************************************************
  Tests the input byte b, and if it is recognized by this scanner then it can read onwards using
  the fm FileManipulator. If an invalid input occurs while reading from the fm, it returns null,
  otherwise it returns the new Resource with the information about the found file.
  **********************************************************************************************
  **/
  public abstract Resource scan(int b, FileManipulator fm);

}