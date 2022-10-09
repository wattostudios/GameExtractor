/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.helper;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import com.sun.jna.FunctionMapper;
import com.sun.jna.Library;
import com.sun.jna.NativeLibrary;

public class NativeOodleDecomp32FunctionMapper implements FunctionMapper {

  public String getFunctionName(NativeLibrary library, Method method) {
    if (method.getName().equalsIgnoreCase("OodleLZ_Compress")) {
      return "_OodleLZ_Compress@40";
    }
    else if (method.getName().equalsIgnoreCase("OodleLZ_Decompress")) {
      return "_OodleLZ_Decompress@56";
    }
    else {
      return method.getName();
    }
  }

  public static Map<String, Object> buildLibraryMap() {
    HashMap<String, Object> options = new HashMap<String, Object>();
    options.put(Library.OPTION_FUNCTION_MAPPER, new NativeOodleDecomp32FunctionMapper());
    return options;
  }

}
