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

/***********************************************************************************************
 * Defines a <code>Class</code> that can be dynamically loaded by a <code>WSPluginManager</code>.
 * Allows plugin <code>class</code>es to be developed separately to a program, and included
 * without needing to change the code of the program
 ***********************************************************************************************/
public abstract class AbstractPlugin implements WSPlugin {

  /** The unique code of this plugin **/
  protected String code = "AbstractPlugin";
  /** A description of this plugin **/
  protected String description = null;
  /** The name of this plugin **/
  protected String name = null;
  /** The type of this plugin **/
  protected String type = "AbstractPlugin";
  /** Whether this plugin is enabled or not **/
  boolean enabled = true;

  /***********************************************************************************************
   * Compares the <code>getName()</code> of this <code>WSPlugin</code> to the
   * <code>getName()</code> of another <code>WSPlugin</code>
   * @param otherPlugin the <code>WSPlugin</code> to compare to
   * @return <b>0</b> if the <code>WSPlugin</code>s are equal<br />
   *         <b>1</b> if the <code>otherPlugin</code> comes after this
   *         <code>WSPlugin</code><br />
   *         <b>-1</b> if the <code>otherPlugin</code> comes before this <code>WSPlugin</code>
   ***********************************************************************************************/
  public int compareTo(WSPlugin otherPlugin) {
    return getName().compareToIgnoreCase(otherPlugin.getName());
  }

  /***********************************************************************************************
   * Gets the text code for this <code>WSPlugin</code>, which is used for <code>Language</code>s
   * and other functionality
   * @return the text code for this <code>WSPlugin</code>
   ***********************************************************************************************/
  @Override
  public String getCode() {
    return code;
  }

  /***********************************************************************************************
   * Gets the <code>Language</code> description of this <code>WSPlugin</code>
   * @return the <code>Language</code> description
   ***********************************************************************************************/
  @Override
  public String getDescription() {
    if (description == null) {
      String descriptionCode = code + "_Description";
      if (Language.has(descriptionCode)) {
        return Language.get(descriptionCode);
      }
      else {
        return code;
      }
    }
    return description;
  }

  /***********************************************************************************************
   * Gets the <code>Language</code> name of this <code>WSPlugin</code>
   * @return the <code>Language</code> name
   ***********************************************************************************************/
  @Override
  public String getName() {
    if (name == null) {
      String nameCode = code + "_Name";
      if (Language.has(nameCode)) {
        return Language.get(nameCode);
      }
      else {
        return code;
      }
    }
    return name;
  }

  /***********************************************************************************************
   * Gets the type of this <code>WSPlugin</code>
   * @return the <code>WSPlugin</code> type
   ***********************************************************************************************/
  @Override
  public String getType() {
    if (type == null) {
      return getClass().getName();
    }
    else {
      return type;
    }
  }

  /***********************************************************************************************
   * Whether this <code>WSPlugin</code> is enabled or not?
   * @return <b>true</b> if this <code>WSPlugin</code> is enabled<br />
   *         <b>false</b> if this <code>WSPlugin</code> is not enabled.
   ***********************************************************************************************/
  @Override
  public boolean isEnabled() {
    return enabled;
  }

  /***********************************************************************************************
   * Sets the text <code>code</code> for this <code>WSPlugin</code>
   * @param code the text code
   ***********************************************************************************************/
  @Override
  public void setCode(String code) {
    this.code = code;
  }

  /***********************************************************************************************
   * Sets the <code>Language</code> description of this <code>WSPlugin</code>
   * @param description the <code>Language</code> description
   ***********************************************************************************************/
  @Override
  public void setDescription(String description) {
    this.description = description;
  }

  /***********************************************************************************************
   * Whether this <code>WSPlugin</code> is enabled or not?
   * @param enabled <b>true</b> if this <code>WSPlugin</code> is enabled<br />
   *        <b>false</b> if this <code>WSPlugin</code> is not enabled.
   ***********************************************************************************************/
  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /***********************************************************************************************
   * Sets the <code>Language</code> name of this <code>WSPlugin</code>
   * @param name the <code>Language</code> name
   ***********************************************************************************************/
  @Override
  public void setName(String name) {
    this.name = name;
  }

  /***********************************************************************************************
   * Sets the type of this <code>WSPlugin</code>
   * @param type the <code>WSPlugin</code> type
   ***********************************************************************************************/
  @Override
  public void setType(String type) {
    this.type = type;
  }

  /***********************************************************************************************
   * Gets the <code>Language</code> code of this <code>WSPlugin</code>
   * @return the <code>Language</code> name
   ***********************************************************************************************/
  @Override
  public String toString() {
    return getName();
  }
}