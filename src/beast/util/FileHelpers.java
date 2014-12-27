/*
 * FileHelpers.java
 *
 * BEAST: Bayesian Evolutionary Analysis by Sampling Trees
 * Copyright (C) 2014 BEAST Developers
 *
 * BEAST is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * BEAST is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BEAST.  If not, see <http://www.gnu.org/licenses/>.
 */

package beast.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;

/**
 * @author Joseph Heled
 *         Date: 15/04/2008
 */
public class FileHelpers {

    public static final String FILE_NAME = "fileName";

    /**
     * @param file the file to read the line numbers from
     * @return Number of lines in file
     * @throws IOException low level file error
     */
    public static int numberOfLines(File file) throws IOException {
        RandomAccessFile randFile = new RandomAccessFile(file, "r");
        long lastRec = randFile.length();
        randFile.close();
        FileReader fileRead = new FileReader(file);
        LineNumberReader lineRead = new LineNumberReader(fileRead);
        lineRead.skip(lastRec);
        int count = lineRead.getLineNumber() - 1;
        fileRead.close();
        lineRead.close();
        return count;
    }

    /**
     * Resolve file from name.
     * <p/>
     * Keep A fully qualified (i.e. absolute path) as is. A name starting with a "./" is
     * relative to the master directory (set by FileHelpers.setMasterDir).
     * Any other name is stripped of any directory
     * component and placed in the "user.dir" directory.
     *
     * @param fileName an absolute or relative file name
     * @return a File object resolved from provided file name
     */
    public static File getFile(String fileName, String prefix) {
        final boolean localFile = fileName.startsWith("./");
        final boolean relative = masterDirectory != null && localFile;
        if (localFile) {
            fileName = fileName.substring(2);
        }

        if (prefix != null) {
            fileName = prefix + fileName;
        }

        final File file = new File(fileName);
        final String name = file.getName();
        String parent = file.getParent();

        if (!file.isAbsolute()) {
            String p;
            if (relative) {
                p = masterDirectory.getAbsolutePath();
            } else {
                p = System.getProperty("user.dir");
            }
            if (parent != null && parent.length() > 0) {
                parent = p + '/' + parent;
            } else {
                parent = p;
            }
        }
        return new File(parent, name);
    }

    public static String readLastLine(final File file) throws IOException {
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = null;
        String nextLine = reader.readLine();
        while (nextLine != null) {
            line = nextLine;
            nextLine = reader.readLine();
        }
        reader.close();
        return line;
    }

    public static void deleteLastLine(final File file) throws IOException {
        final RandomAccessFile f = new RandomAccessFile(file, "rw");
        long length = f.length() - 1;
        byte b;
        do {
            length -= 1;
            f.seek(length);
            b = f.readByte();
        } while(b != '\n');
        f.setLength(length+1);
    }

    public static File getFile(String fileName) {
        return getFile(fileName, null);
    }

    // directory where beast xml file resides
    private static File masterDirectory = null;

    public static void setMasterDir(File fileName) {
        masterDirectory = fileName;
    }
}
