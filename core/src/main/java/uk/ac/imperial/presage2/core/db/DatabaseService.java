/**
 * 	Copyright (C) 2011 Sam Macbeth <sm1106 [at] imperial [dot] ac [dot] uk>
 *
 * 	This file is part of Presage2.
 *
 *     Presage2 is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Presage2 is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser Public License
 *     along with Presage2.  If not, see <http://www.gnu.org/licenses/>.
 */

package uk.ac.imperial.presage2.core.db;

/**
 * This is a generic description of a Database service in presage. Anything we
 * want to use as a database should implement this so the platform can start and
 * stop it.
 * 
 * @author sm1106
 * 
 */
public interface DatabaseService {

	/**
	 * Start the database service. This generally will create a connection to a
	 * database.
	 * 
	 * @throws Exception
	 */
	void start() throws Exception;

	/**
	 * 
	 * @return true if {@link #start()} has be successfully called, false
	 *         otherwise.
	 */
	boolean isStarted();

	/**
	 * Stop this database service.
	 */
	void stop();

}
