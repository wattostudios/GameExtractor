/* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.ning.compress.lzf;

import java.io.*;

import com.ning.compress.lzf.util.LZFFileInputStream;
import com.ning.compress.lzf.util.LZFFileOutputStream;

/**
 * Simple command-line utility that can be used for testing LZF
 * compression, or as rudimentary command-line tool.
 * Arguments are the same as used by the "standard" lzf command line tool
 * 
 * @author Tatu Saloranta (tatu@ning.com)
 */
public class LZF
{
    public final static String SUFFIX = ".lzf";

    protected void process(String[] args) throws IOException
    {
        if (args.length == 2) {
            String oper = args[0];
            boolean compress = "-c".equals(oper);
            boolean toSystemOutput = !compress && "-o".equals(oper);
            if (compress || toSystemOutput || "-d".equals(oper)) {
                String filename = args[1];
                File src = new File(filename);
                if (!src.exists()) {
                    System.err.println("File '"+filename+"' does not exist.");
                    System.exit(1);
                }
                if (!compress && !filename.endsWith(SUFFIX)) {
                    System.err.println("File '"+filename+"' does end with expected suffix ('"+SUFFIX+"', won't decompress.");
                    System.exit(1);
                }

                if (compress) {
                    int inputLength = 0;
                    File resultFile = new File(filename+SUFFIX);
                    InputStream in = new FileInputStream(src);
                    OutputStream out = new LZFFileOutputStream(resultFile);
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer, 0, buffer.length)) != -1) { 
                        inputLength += bytesRead;
                        out.write(buffer, 0, bytesRead); 
                    }
                    in.close();
                    out.flush();
                    out.close();
                    System.out.printf("Compressed '%s' into '%s' (%d->%d bytes)\n",
                            src.getPath(), resultFile.getPath(),
                            inputLength, resultFile.length());
                } else {
                    OutputStream out;
                    LZFFileInputStream in = new LZFFileInputStream(src);
                    File resultFile = null;
                    if (toSystemOutput) {
                        out = System.out;
                    } else {
                        resultFile = new File(filename.substring(0, filename.length() - SUFFIX.length()));
                        out = new FileOutputStream(resultFile);
                    }
                    int uncompLen = in.readAndWrite(out);
                    in.close();
                    out.flush();
                    out.close();
                    if (resultFile != null) {
                        System.out.printf("Uncompressed '%s' into '%s' (%d->%d bytes)\n",
                                src.getPath(), resultFile.getPath(),
                                src.length(), uncompLen);
                    }
                }
                return;
            }
        }
        System.err.println("Usage: java "+getClass().getName()+" -c/-d/-o source-file");
        System.err.println(" -d parameter: decompress to file");
        System.err.println(" -c parameter: compress to file");
        System.err.println(" -o parameter: decompress to stdout");
        System.exit(1);
    }

    public static void main(String[] args) throws IOException {
        new LZF().process(args);
    }
}

