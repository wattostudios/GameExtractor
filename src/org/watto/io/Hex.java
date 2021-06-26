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

package org.watto.io;

import java.io.UnsupportedEncodingException;
import java.lang.StringBuffer;
import java.util.Locale;


/***********************************************************************************************
A Hex <code>String</code>
@see String
***********************************************************************************************/
public class Hex {

  /** The hex String **/
  String hex;


  /***********************************************************************************************
  Creates a Hex <code>String</code> with the given <code>hex</code> value
  @param hex the value of the Hex <code>String</code>
  @see String
  ***********************************************************************************************/
  public Hex(String hex){
    this.hex = hex.toUpperCase();
  }


  /***********************************************************************************************
  Gets the <code>char</code> at the given <code>index</code> in the hex <code>String</code>
  @param index the index in the hex <code>String</code>
  @return the <code>char</code> at the <code>index</code>
  @see String
  ***********************************************************************************************/
  public char charAt(int index){
    return hex.charAt(index);
  }


  /***********************************************************************************************
  Compares the <code>hex</code> to another hex <code>String</code>. The comparison is case
  sensitive.
  @param anotherString the <code>String</code> to compare against
  @return 0 if the <code>String</code>s are equal<br />
          1 if <code>anotherString</code> is greater than the <code>hex</code> value<br />
         -1 if the <code>hex</code> value is greater than the <code>anotherString</code> value
  @see String
  ***********************************************************************************************/
  public int compareTo(String anotherString){
    return hex.compareTo(anotherString);
  }


  /***********************************************************************************************
  Compares the <code>hex</code> to another hex <code>String</code>, ignoring case differences.
  @param anotherString the <code>String</code> to compare against
  @return <b>0</b> if the <code>String</code>s are equal<br />
          <b>1</b> if <code>anotherString</code> is greater than the <code>hex</code> value<br />
         <b>-1</b> if the <code>hex</code> value is greater than the <code>anotherString</code> value
  @see String
  ***********************************************************************************************/
  public int compareToIgnoreCase(String anotherString){
    return hex.compareToIgnoreCase(anotherString);
  }


  /***********************************************************************************************
  Concatenates the <code>hex</code> value with <code>anotherString</code>
  @param anotherString the <code>String</code> to append to the end of the <code>hex</code> value
  @return the concatenated hex </code>String</code>
  @see String
  ***********************************************************************************************/
  public String concat(String anotherString){
    return hex.concat(anotherString);
  }


  /***********************************************************************************************
  Whether the <code>hex</code> value is equal to the value in the <code>StringBuffer</code>?
  @param buffer the <code>StringBuffer</code> to compare to
  @return <b>true</b> if the <code>hex</code> value is equal to the <code>StringBuffer</code><br />
          <b>false</b> if the values are not equal
  @see String
  ***********************************************************************************************/
  public boolean contentEquals(StringBuffer buffer){
    return hex.contentEquals(buffer);
  }


  /***********************************************************************************************
  Whether the <code>hex</code> value ends with the <code>suffix</code> value?
  @param suffix the <code>String</code> to compare to
  @return <b>true</b> if the <code>suffix</code> is at the end of the <code>hex</code> <code>String</code><br />
          <b>false</b> if the <code>suffix</code> is not at the end
  @see String
  ***********************************************************************************************/
  public boolean endsWith(String suffix){
    return hex.endsWith(suffix);
  }


  /***********************************************************************************************
  Whether the <code>hex</code> value is the same as <code>anotherObject</code>?
  @param anotherObject the <code>Object</code> to compare to
  @return <b>true</b> if the <code>hex</code> value is equal to the <code>Object</code><br />
          <b>false</b> if the values are not equal
  @see String
  ***********************************************************************************************/
  public boolean equals(Object anotherObject){
    return hex.equals(anotherObject);
  }


  /***********************************************************************************************
  Whether the <code>hex</code> value is the same as <code>anotherString</code>? This is not case
  sensitive.
  @param anotherString the <code>Object</code> to compare to
  @return <b>true</b> if the <code>hex</code> value is equal to the <code>String</code><br />
          <b>false</b> if the values are not equal
  @see String
  ***********************************************************************************************/
  public boolean equalsIgnoreCase(String anotherString){
    return hex.equalsIgnoreCase(anotherString);
  }


  /***********************************************************************************************
  Gets the <code>byte</code>s of the <code>hex</code> <code>String</code>
  @return the byte values of the <code>hex</code> <code>String</code>
  @see String
  ***********************************************************************************************/
  public byte[] getBytes(){
    return hex.getBytes();
  }


  /***********************************************************************************************
  Gets a number of <code>byte</code>s from the <code>hex</code> <code>String</code> into the <code>destination</code>
  @param sourceBegin the byte number to start reading from in the <code>hex</code> <code>String</code>
  @param sourceEnd the byte number to stop reading from in the <code>hex</code> <code>String</code>
  @param destination the <code>byte</code> array to store the <code>byte</code> values
  @param destinationBegin the index in the <code>destination</code> array to start storing data
  @see String
  ***********************************************************************************************/
  public void getBytes(int sourceBegin,int sourceEnd,byte[] destination,int destinationBegin){
  //hex.getBytes(sourceBegin,sourceEnd,destination,destinationBegin);
  }


  /***********************************************************************************************
  Gets the <code>byte</code>s of the <code>hex</code> <code>String</code>, in the given <code>charset</code>
  @return the byte values of the <code>hex</code> <code>String</code>
  @see String
  ***********************************************************************************************/
  public byte[] getBytes(String charset) throws UnsupportedEncodingException{
    return hex.getBytes(charset);
  }


  /***********************************************************************************************
  Gets a number of <code>char</code>s from the <code>hex</code> <code>String</code> into the <code>destination</code>
  @param sourceBegin the char number to start reading from in the <code>hex</code> <code>String</code>
  @param sourceEnd the char number to stop reading from in the <code>hex</code> <code>String</code>
  @param destination the <code>char</code> array to store the <code>char</code> values
  @param destinationBegin the index in the <code>destination</code> array to start storing data
  @see String
  ***********************************************************************************************/
  public void getChars(int srcBegin,int srcEnd,char[] dst,int dstBegin){
    hex.getChars(srcBegin,srcEnd,dst,dstBegin);
  }


  /***********************************************************************************************
  Gets the hash code of the <code>hex</code> <code>String</code>
  @return the hash code
  @see String
  ***********************************************************************************************/
  public int hashCode(){
    return hex.hashCode();
  }


  /***********************************************************************************************
  Gets the first index of the <code>character</code> in the <code>hex</code> <code>String</code>
  @param character the character to search for
  @return the index where the <code>character</code> was found, or -1 if the <code>character</code>
   wasn't found.
   @see String
  ***********************************************************************************************/
  public int indexOf(int character){
    return hex.indexOf(character);
  }


  /***********************************************************************************************
  Gets the first index of the <code>character</code> in the <code>hex</code> <code>String</code>,
  starting at the <code>fromIndex</code>
  @param character the character to search for
  @param fromIndex the index in the <code>hex</code> value to start looking for the <code>character</code>
  @return the index where the <code>character</code> was found, or -1 if the <code>character</code>
   wasn't found.
   @see String
  ***********************************************************************************************/
  public int indexOf(int character,int fromIndex){
    return hex.indexOf(character,fromIndex);
  }


  /***********************************************************************************************
  Gets the first index of the <code>String</code> in the <code>hex</code> <code>String</code>
  @param string the <code>String</code> to search for
  @return the index where the <code>string</code> was found, or -1 if the <code>string</code>
   wasn't found.
   @see String
  ***********************************************************************************************/
  public int indexOf(String string){
    return hex.indexOf(string);
  }


  /***********************************************************************************************
  Gets the first index of the <code>String</code> in the <code>hex</code> <code>String</code>,
  starting at the <code>fromIndex</code>
  @param string the <code>String</code> to search for
  @param fromIndex the index in the <code>hex</code> value to start looking for the <code>character</code>
  @return the index where the <code>string</code> was found, or -1 if the <code>string</code>
   wasn't found.
   @see String
  ***********************************************************************************************/
  public int indexOf(String string,int fromIndex){
    return hex.indexOf(string,fromIndex);
  }


  /***********************************************************************************************
  Gets the internal representation of the <code>hex</code> <code>String</code>
  @return the canonical representation
  @see String
  ***********************************************************************************************/
  public String intern(){
    return hex.intern();
  }


  /***********************************************************************************************
  Gets the last index of the <code>character</code> in the <code>hex</code> <code>String</code>
  @param character the character to search for
  @return the index where the <code>character</code> was found, or -1 if the <code>character</code>
   wasn't found.
   @see String
  ***********************************************************************************************/
  public int lastIndexOf(int character){
    return hex.lastIndexOf(character);
  }


  /***********************************************************************************************
  Gets the last index of the <code>character</code> in the <code>hex</code> <code>String</code>,
  starting backwards from the <code>fromIndex</code>
  @param character the character to search for
  @param fromIndex the index in the <code>hex</code> value to start looking backwards for the
  <code>character</code>
  @return the index where the <code>character</code> was found, or -1 if the <code>character</code>
   wasn't found.
   @see String
  ***********************************************************************************************/
  public int lastIndexOf(int character,int fromIndex){
    return hex.lastIndexOf(character,fromIndex);
  }


  /***********************************************************************************************
  Gets the last index of the <code>string</code> in the <code>hex</code> <code>String</code>
  @param string the <code>String</code> to search for
  @return the index where the <code>string</code> was found, or -1 if the <code>string</code>
   wasn't found.
   @see String
  **********************************************************************************************/
  public int lastIndexOf(String string){
    return hex.lastIndexOf(string);
  }


  /***********************************************************************************************
  Gets the last index of the <code>string</code> in the <code>hex</code> <code>String</code>,
  starting backwards from the <code>fromIndex</code>
  @param string the <code>String</code> to search for
  @param fromIndex the index in the <code>hex</code> value to start looking backwards for the
  <code>character</code>
  @return the index where the <code>string</code> was found, or -1 if the <code>string</code>
   wasn't found.
   @see String
  ***********************************************************************************************/
  public int lastIndexOf(String string,int fromIndex){
    return hex.lastIndexOf(string,fromIndex);
  }


  /***********************************************************************************************
  Gets the length of the <code>hex</code> <code>String</code>
  @return the length of the <code>hex</code> <code>String</code>
  @see String
  **********************************************************************************************/
  public int length(){
    return hex.length();
  }


  /***********************************************************************************************
  Whether this <code>hex</code> value satisfies the <code>regex</code> expression
  @param regex the regular expression used for comparison
  @return <b>true</b> if the <code>hex</code> value satisfies the <code>regex</code> expression<br />
          <b>false</b> if the match does not exist
  @see String
  **********************************************************************************************/
  public boolean matches(String regex){
    return hex.matches(regex);
  }


  /***********************************************************************************************
  Whether part of the <code>hex</code> value equals part of the <code>otherString</code>? 
  @param ignoreCase whether to ignore case differences or not
  @param thisOffset the offset to start the comparison from in the <code>hex</code> value
  @param otherString the <code>String</code> to compare to
  @param otherOffset the offset to start the comparison from in the <code>otherString</code> value
  @param length the number of characters to check
  @return <b>true</b> if the <code>String</code>s match in the given region
          <b>false</b> if the <code>String</code>s don't match
  @see String
  **********************************************************************************************/
  public boolean regionMatches(boolean ignoreCase,int thisOffset,String otherString,int otherOffset,int length){
    return hex.regionMatches(ignoreCase,thisOffset,otherString,otherOffset,length);
  }


  /***********************************************************************************************
  Whether part of the <code>hex</code> value equals part of the <code>otherString</code>? 
  @param thisOffset the offset to start the comparison from in the <code>hex</code> value
  @param otherString the <code>String</code> to compare to
  @param otherOffset the offset to start the comparison from in the <code>otherString</code> value
  @param length the number of characters to check
  @return <b>true</b> if the <code>String</code>s match in the given region
          <b>false</b> if the <code>String</code>s don't match
  @see String
  **********************************************************************************************/
  public boolean regionMatches(int thisOffset,String otherString,int otherOffset,int length){
    return hex.regionMatches(thisOffset,otherString,otherOffset,length);
  }


  /***********************************************************************************************
  Replace all occurrences of <code>oldChar</code> with <code>newChar</code> in the <code>hex</code> <code>String</code>
  @param oldChar the old character to be replaces
  @param newChar the new character to replace the old characters with
  @return the replaced String
  @see String
  **********************************************************************************************/
  public String replace(char oldChar,char newChar){
    return hex.replace(oldChar,newChar);
  }


  /***********************************************************************************************
  Replace all occurrences of the <code>regex</code> with <code>replacement</code> in the <code>hex</code> <code>String</code>
  @param regex the regular expression to match against
  @param replacement the String to replace the old characters with
  @return the replaced String
  @see String
  **********************************************************************************************/
  public String replaceAll(String regex,String replacement){
    return hex.replaceAll(regex,replacement);
  }


  /***********************************************************************************************
  Replace the first occurrence of the <code>regex</code> with <code>replacement</code> in the <code>hex</code> <code>String</code>
  @param regex the regular expression to match against
  @param replacement the String to replace the old characters with
  @return the replaced String
  @see String
  **********************************************************************************************/
  public String replaceFirst(String regex,String replacement){
    return hex.replaceFirst(regex,replacement);
  }


  /***********************************************************************************************
  Splits the <code>hex</code> value into an array of values, based on the <code>regex</code>
  @param regex the regular expression to match against
  @return an array of <code>String</code>s that were split due to the <code>regex</code>
  @see String 
  **********************************************************************************************/
  public String[] split(String regex){
    return hex.split(regex);
  }


  /***********************************************************************************************
  Splits the <code>hex</code> value into an array of values, based on the <code>regex</code> and
  the <code>matchLimit</code> limit
  @param regex the regular expression to match against
  @param matchLimit the maximum number of splits to obtain
  @return an array of <code>String</code>s that were split due to the <code>regex</code>
  @see String 
  **********************************************************************************************/
  public String[] split(String regex,int matchLimit){
    return hex.split(regex,matchLimit);
  }


  /***********************************************************************************************
  Whether the <code>hex</code> value starts with the <code>prefix</code> value?
  @param prefix the <code>String</code> to compare to
  @return <b>true</b> if the <code>prefix</code> is at the start of the <code>hex</code> <code>String</code><br />
          <b>false</b> if the <code>prefix</code> is not at the start
  @see String
  ***********************************************************************************************/
  public boolean startsWith(String prefix){
    return hex.startsWith(prefix);
  }


  /***********************************************************************************************
  Whether the <code>hex</code> value starts with the <code>prefix</code> value, from the <code>fromIndex</code>
  @param prefix the <code>String</code> to compare to
  @param fromIndex the index to start searching from in the <code>hex</code> <code>String</code>
  @return <b>true</b> if the <code>prefix</code> is at the start of the <code>hex</code> <code>String</code><br />
          <b>false</b> if the <code>prefix</code> is not at the start
  @see String
  ***********************************************************************************************/
  public boolean startsWith(String prefix,int fromIndex){
    return hex.startsWith(prefix,fromIndex);
  }


  /***********************************************************************************************
  Gets the <code>CharSequence</code> for the characters between <code>beginIndex</code> and
  <code>endIndex</code>
  @param beginIndex the first character to be retrieved
  @param endIndex the last character to be retrieved
  @return the characters as a <code>CharSequence</code>
  @see String
  ***********************************************************************************************/
  public CharSequence subSequence(int beginIndex,int endIndex){
    return hex.subSequence(beginIndex,endIndex);
  }


  /***********************************************************************************************
  Gets a <code>String</code> of the characters between <code>beginIndex</code> and
  the end of the <code>hex</code> <code>String</code>
  @param beginIndex the first character to be retrieved
  @return the characters as a <code>String</code>
  @see String
  ***********************************************************************************************/
  public String substring(int beginIndex){
    return hex.substring(beginIndex);
  }


  /***********************************************************************************************
  Gets a <code>String</code> of the characters between <code>beginIndex</code> and
  <code>endIndex</code>
  @param beginIndex the first character to be retrieved
  @param endIndex the last character to be retrieved
  @return the characters as a <code>String</code>
  @see String
  ***********************************************************************************************/
  public String substring(int beginIndex,int endIndex){
    return hex.substring(beginIndex,endIndex);
  }


  /***********************************************************************************************
  Gets the <code>char</code>s from the <code>hex</code> <code>String</code>
  @return the array of <code>char</code>s for the <code>hex</code> value
  @see String
  ***********************************************************************************************/
  public char[] toCharArray(){
    return hex.toCharArray();
  }


  /***********************************************************************************************
  Converts the <code>hex</code> value into all lower case characters
  @return the converted <code>hex</code> <code>String</code>
  @see String
  ***********************************************************************************************/
  public String toLowerCase(){
    return hex.toLowerCase();
  }


  /***********************************************************************************************
  Converts the <code>hex</code> value into all lower case characters, for the given <code>locale</code>
  @param local the locale used for conversion
  @return the converted <code>hex</code> <code>String</code>
  @see String
  ***********************************************************************************************/
  public String toLowerCase(Locale locale){
    return hex.toLowerCase(locale);
  }


  /***********************************************************************************************
  Gets the <code>hex</code> value
  @return the <code>hex</code> <code>String</code>
  @see String
  ***********************************************************************************************/
  public String toString(){
    return hex.toString();
  }


  /***********************************************************************************************
  Converts the <code>hex</code> value into all upper case characters
  @return the converted <code>hex</code> <code>String</code>
  @see String
  ***********************************************************************************************/
  public String toUpperCase(){
    return hex.toUpperCase();
  }


  /***********************************************************************************************
  Converts the <code>hex</code> value into all upper case characters, for the given <code>locale</code>
  @param local the locale used for conversion
  @return the converted <code>hex</code> <code>String</code>
  @see String
  ***********************************************************************************************/
  public String toUpperCase(Locale locale){
    return hex.toUpperCase(locale);
  }


  /***********************************************************************************************
  Removes all whitespace characters from around the <code>hex</code> value
  @return the trimmed <code>hex</code> <code>String</code>
  @see String
  ***********************************************************************************************/
  public String trim(){
    return hex.trim();
  }
}