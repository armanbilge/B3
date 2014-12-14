/*
 * MessageLogHandler.java
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

import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class MessageLogHandler extends StreamHandler {

	public MessageLogHandler() {
		setOutputStream(System.out);
		setFormatter(new MessageLogFormatter());
	}


	public void publish(LogRecord record) {
		super.publish(record);
		flush();
	}

	public void close() {
		flush();
	}

	private class MessageLogFormatter extends Formatter {
				
		// Line separator string.  This is the value of the line.separator
		// property at the moment that the SimpleFormatter was created.
        private final String lineSeparator = System.getProperty("line.separator");

        // AR - is there a reason why this was used? It causes warnings at compile
//        private final String lineSeparator = (String) java.security.AccessController.doPrivileged(
//                new sun.security.action.GetPropertyAction("line.separator"));

		/**
		 * Format the given LogRecord.
		 * @param record the log record to be formatted.
		 * @return a formatted log record
		 */
		public synchronized String format(LogRecord record) {
			final StringBuffer sb = new StringBuffer();
            sb.append(formatMessage(record));
			sb.append(lineSeparator);
			return sb.toString();
		}
	}
}

