/*
 * OSType.java
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

/**
 * @author Walter Xie
 */
public enum OSType {
	WINDOWS,
    MAC,
    UNIX_LINUX;

	static OSType detect() {

		if (os.indexOf("mac") >= 0) {
			return MAC;
		}

		if (os.indexOf("win") >= 0) {
			return WINDOWS;
		}

		if (os.indexOf( "nix") >=0 || os.indexOf( "nux") >=0) {
			return UNIX_LINUX;
		}

		return null;
    }

    public static boolean isWindows(){
		//windows
	    return (os.indexOf( "win" ) >= 0);
	}

	public static boolean isMac(){
		//Mac
	    return (os.indexOf( "mac" ) >= 0);
	}

	public static boolean isUnixOrLinux(){
		//linux or unix
	    return (os.indexOf( "nix") >=0 || os.indexOf( "nux") >=0);
	}

    public String toString() {
        return os;
    }

    public String version() {
        return System.getProperty("os.version");
    }

    static final String os = System.getProperty("os.name").toLowerCase();
}
