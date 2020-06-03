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

/***********************************************************************************************
Defines a <code>Class</code> that can be dynamically loaded by a <code>WSPluginManager</code>.
Allows plugin <code>class</code>es to be developed separately to a program, and included without
needing to change the code of the program
***********************************************************************************************/
public interface WSPlugin extends WSComparable {


  /***********************************************************************************************
  Gets the text code for this <code>WSPlugin</code>, which is used for <code>Language</code>s
  and other functionality
  @return the text code for this <code>WSPlugin</code>
  ***********************************************************************************************/
  public String getCode();


  /***********************************************************************************************
  Gets the <code>Language</code> description of this <code>WSPlugin</code>
  @return the <code>Language</code> description
  ***********************************************************************************************/
  public String getDescription();


  /***********************************************************************************************
  Gets the <code>Language</code> name of this <code>WSPlugin</code>
  @return the <code>Language</code> name
  ***********************************************************************************************/
  public String getName();


  /***********************************************************************************************
  Gets the type of this <code>WSPlugin</code>
  @return the <code>WSPlugin</code> type
  ***********************************************************************************************/
  public String getType();


  /***********************************************************************************************
  Whether this <code>WSPlugin</code> is enabled or not?
  @return <b>true</b> if this <code>WSPlugin</code> is enabled<br />
          <b>false</b> if this <code>WSPlugin</code> is not enabled.
  ***********************************************************************************************/
  public boolean isEnabled();


  /***********************************************************************************************
  Sets the text <code>code</code> for this <code>WSPlugin</code>
  @param code the text code
  ***********************************************************************************************/
  public void setCode(String code);


  /***********************************************************************************************
  Sets the <code>Language</code> description of this <code>WSPlugin</code>
  @param description the <code>Language</code> description
  ***********************************************************************************************/
  public void setDescription(String description);


  /***********************************************************************************************
  Whether this <code>WSPlugin</code> is enabled or not?
  @param enabled <b>true</b> if this <code>WSPlugin</code> is enabled<br />
                 <b>false</b> if this <code>WSPlugin</code> is not enabled.
  ***********************************************************************************************/
  public void setEnabled(boolean enabled);


  /***********************************************************************************************
  Sets the <code>Language</code> name of this <code>WSPlugin</code>
  @param name the <code>Language</code> name
  ***********************************************************************************************/
  public void setName(String name);


  /***********************************************************************************************
  Sets the type of this <code>WSPlugin</code>
  @param type the <code>WSPlugin</code> type
  ***********************************************************************************************/
  public void setType(String type);


  /***********************************************************************************************
  Gets the <code>Language</code> name of this <code>WSPlugin</code>
  @return the <code>Language</code> name
  ***********************************************************************************************/
  public String toString();
}