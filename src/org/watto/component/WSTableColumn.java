////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       WATTO STUDIOS                                        //
//                             Java Code, Programs, and Software                              //
//                                    http://www.watto.org                                    //
//                                                                                            //
//                           Copyright (C) 2004-2010  WATTO Studios                           //
//                                                                                            //
// This program is free software; you can redistribute it and/or modify it under the terms of //
// the GNU General Public License published by the Free Software Foundation; either version 2 //
// of the License, or (at your option) any later versions. This program is distributed in the //
// hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties //
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License //
// at http://www.gnu.org for more details. For updates and information about this program, go //
// to the WATTO Studios website at http://www.watto.org or email watto@watto.org . Thanks! :) //
//                                                                                            //
////////////////////////////////////////////////////////////////////////////////////////////////

package org.watto.component;

import org.watto.Language;
import org.watto.Settings;

/***********************************************************************************************
A column in a <code>WSTable</code>
***********************************************************************************************/
public class WSTableColumn implements Comparable<WSTableColumn> {

  /** A character value representing this column, for faster sorting purposes **/
  char charCode; // for quicker sorting

  /** The code for the language and settings **/
  String code = "";

  /** The <code>Class</code> of <code>Object</code> in this table column **/
  @SuppressWarnings("rawtypes")
  Class type = String.class;

  /** Is the data in this column editable? **/
  boolean editable = false;

  /** Is the data in this column sortable? **/
  boolean sortable = true;

  /** The minimum width of this column **/
  int minWidth = 0;

  /** The maximum width of this column **/
  int maxWidth = -1;

  /***********************************************************************************************
  Constructor for extended classes only
  ***********************************************************************************************/
  public WSTableColumn() {
    super();
  }

  /***********************************************************************************************
  Creates a column for a <code>WSTable</code> which contains <code>String</code> data. It is not
  <code>editable</code> and not <code>sortable</code>
  @param code the <code>Language</code> and <code>Settings</code> code for the column
  @param charCode the character code for the column, for use when sorting
  ***********************************************************************************************/
  public WSTableColumn(String code, char charCode) {
    this(code, charCode, String.class, false, false);
  }

  /***********************************************************************************************
  Creates a column for a <code>WSTable</code>, which is not <code>editable</code> and not
  <code>sortable</code>
  @param code the <code>Language</code> and <code>Settings</code> code for the column
  @param charCode the character code for the column, for use when sorting
  @param type the <code>Class</code> of data in the column
  ***********************************************************************************************/
  @SuppressWarnings("rawtypes")
  public WSTableColumn(String code, char charCode, Class type) {
    this(code, charCode, type, false, false);
  }

  /***********************************************************************************************
  Creates a column for a <code>WSTable</code>
  @param code the <code>Language</code> and <code>Settings</code> code for the column
  @param charCode the character code for the column, for use when sorting
  @param type the <code>Class</code> of data in the column
  @param editable <b>true</b> if the data in this column is <code>editable</code><br />
                  <b>false</b> if the data in this column is not <code>editable</code>
  @param sortable <b>true</b> if the data in this column is <code>sortable</code><br />
                  <b>false</b> if the data in this column is not <code>sortable</code>
  ***********************************************************************************************/
  @SuppressWarnings("rawtypes")
  public WSTableColumn(String code, char charCode, Class type, boolean editable, boolean sortable) {
    this(code, charCode, type, editable, sortable, 0, -1);
  }

  /***********************************************************************************************
  Creates a column for a <code>WSTable</code>
  @param code the <code>Language</code> and <code>Settings</code> code for the column
  @param charCode the character code for the column, for use when sorting
  @param type the <code>Class</code> of data in the column
  @param editable <b>true</b> if the data in this column is <code>editable</code><br />
                  <b>false</b> if the data in this column is not <code>editable</code>
  @param sortable <b>true</b> if the data in this column is <code>sortable</code><br />
                  <b>false</b> if the data in this column is not <code>sortable</code>
  @param minWidth the minimum width of this column
  @param maxWidth the maximum width of this column
  ***********************************************************************************************/
  @SuppressWarnings("rawtypes")
  public WSTableColumn(String code, char charCode, Class type, boolean editable, boolean sortable, int minWidth, int maxWidth) {
    this.code = code;
    this.charCode = charCode;
    this.type = type;
    this.editable = editable;
    this.sortable = sortable;
    this.minWidth = minWidth;
    this.maxWidth = maxWidth;
  }

  /***********************************************************************************************
  Compares the <code>getName()</code> of this <code>WSTableColumn</code> to the <code>getName()</code>
  of another <code>WSTableColumn</code>
  @param otherColumn the <code>WSTableColumn</code> to compare to
  @return <b>0</b> if the <code>WSTableColumn</code>s are equal<br />
          <b>1</b> if the <code>otherColumn</code> comes after this <code>WSTableColumn</code><br />
          <b>-1</b> if the <code>otherColumn</code> comes before this <code>WSTableColumn</code>
  ***********************************************************************************************/
  @Override
  public int compareTo(WSTableColumn otherColumn) {
    return getName().compareTo((otherColumn).getName());
  }

  /***********************************************************************************************
  Gets the character code of this <code>WSTableColumn</code>
  @return the <code>charCode</code>
  ***********************************************************************************************/
  public char getCharCode() {
    return charCode;
  }

  /***********************************************************************************************
  Gets the <code>Class</code> name of this <code>WSTableColumn</code>
  @return the <code>Class</code> name
  ***********************************************************************************************/
  public String getClassName() {
    String name = getClass().getName();
    String pack = getClass().getPackage().getName();
    name = name.substring(pack.length() + 1);
    return name;
  }

  /***********************************************************************************************
  Gets the text code for this <code>WSTableColumn</code>, which is used for <code>Language</code>s
  and other functionality
  @return the text code for this <code>WSTableColumn</code>
  ***********************************************************************************************/
  public String getCode() {
    return code;
  }

  /***********************************************************************************************
  Gets the maximum width of this <code>WSTableColumn</code>
  @return the <code>maxWidth</code>
  ***********************************************************************************************/
  public int getMaxWidth() {
    return maxWidth;
  }

  /***********************************************************************************************
  Gets the minimum width of this <code>WSTableColumn</code>
  @return the <code>minWidth</code>
  ***********************************************************************************************/
  public int getMinWidth() {
    return minWidth;
  }

  /***********************************************************************************************
  Gets the heading name of this <code>WSTableColumn</code>
  @return the <code>heading name</code>
  ***********************************************************************************************/
  public String getName() {
    if (code == null) {
      return "";
    }
    String langCode = getClassName() + "_" + code + "_Text";
    if (Language.has(langCode)) {
      return Language.get(langCode);
    }
    return code;
  }

  /***********************************************************************************************
  Gets the <code>Class</code> of data in this <code>WSTableColumn</code>
  @return the <code>type</code> <code>Class</code>
  ***********************************************************************************************/
  @SuppressWarnings("rawtypes")
  public Class getType() {
    return type;
  }

  /***********************************************************************************************
  Gets the width of this <code>WSTableColumn</code>, from the <code>Settings</code>
  @return the current <code>width</code>
  ***********************************************************************************************/
  public int getWidth() {
    String colCode = getClassName() + "_" + code + "_Width";
    int width = Settings.getInt(colCode);

    if (width < 0) {
      width = 100;
    }

    if (width > maxWidth && maxWidth > 0) {
      width = maxWidth;
    }
    if (width < minWidth) {
      width = minWidth;
    }

    return width;
  }

  /***********************************************************************************************
  Whether this <code>WSTableColumn</code> is <code>editable</code> or not
  @return <b>true</b> if the data in this column is <code>editable</code><br />
          <b>false</b> if the data in this column is not <code>editable</code>
  ***********************************************************************************************/
  public boolean isEditable() {
    return editable;
  }

  /***********************************************************************************************
  Whether this <code>WSTableColumn</code> is <code>sortable</code> or not
  @return <b>true</b> if the data in this column is <code>sortable</code><br />
          <b>false</b> if the data in this column is not <code>sortable</code>
  ***********************************************************************************************/
  public boolean isSortable() {
    return sortable;
  }

  /***********************************************************************************************
  Sets whether the data in this <code>WSTableColumn</code> is <code>editable</code> or not
  @param editable <b>true</b> if the data in this column is <code>editable</code><br />
                  <b>false</b> if the data in this column is not <code>editable</code>
  ***********************************************************************************************/
  public void setEditable(boolean editable) {
    this.editable = editable;
  }

  /***********************************************************************************************
  Sets the maximum width of this <code>WSTableColumn</code>
  @param maxWidth the maximum width
  ***********************************************************************************************/
  public void setMaxWidth(int maxWidth) {
    this.maxWidth = maxWidth;
  }

  /***********************************************************************************************
  Hides/Shows the column
  ***********************************************************************************************/
  public void setVisible(boolean visible) {
    if (visible) {
      setMinWidth(0);
      setMaxWidth(-1);
    }
    else {
      setMinWidth(0);
      setMaxWidth(0);
    }
  }

  /***********************************************************************************************
  Sets the minimum width of this <code>WSTableColumn</code>
  @param minWidth the minimum width
  ***********************************************************************************************/
  public void setMinWidth(int minWidth) {
    this.minWidth = minWidth;
  }

  /***********************************************************************************************
  Sets whether the data in this <code>WSTableColumn</code> is <code>sortable</code> or not
  @param sortable <b>true</b> if the data in this column is <code>sortable</code><br />
                  <b>false</b> if the data in this column is not <code>sortable</code>
  ***********************************************************************************************/
  public void setSortable(boolean sortable) {
    this.sortable = sortable;
  }

  /***********************************************************************************************
  Sets the <code>Class</code> of data in this <code>WSTableColumn</code>
  @param type the <code>Class</code> type
  ***********************************************************************************************/
  @SuppressWarnings("rawtypes")
  public void setType(Class type) {
    this.type = type;
  }

  /***********************************************************************************************
  Sets the current width of this <code>WSTableColumn</code>
  @param width the current width
  ***********************************************************************************************/
  public void setWidth(int width) {

    if (width == 0) {
      return;
    }

    if (width > maxWidth && maxWidth > -1) {
      width = maxWidth;
    }
    if (width < minWidth) {
      width = minWidth;
    }

    String colCode = getClassName() + "_" + code + "_Width";

    Settings.set(colCode, width);
  }

  /***********************************************************************************************
  Gets the heading name of this <code>WSTableColumn</code>
  @return the <code>heading name</code>
  ***********************************************************************************************/
  @Override
  public String toString() {
    return getName();
  }
}