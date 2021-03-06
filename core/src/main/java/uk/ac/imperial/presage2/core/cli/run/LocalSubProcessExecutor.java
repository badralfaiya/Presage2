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
package uk.ac.imperial.presage2.core.cli.run;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import uk.ac.imperial.presage2.core.cli.Presage2CLI;

import com.google.inject.Singleton;

/**
 * A {@link SubProcessExecutor} which runs each simulation process on the local
 * machine.
 * 
 * @author Sam Macbeth
 * 
 */
@Singleton
public class LocalSubProcessExecutor extends SubProcessExecutor implements
		SimulationExecutor {

	public LocalSubProcessExecutor() {
		this(1);
	}

	public LocalSubProcessExecutor(int mAX_PROCESSES) {
		super(mAX_PROCESSES);
	}

	public LocalSubProcessExecutor(int max_processes, String xms, String xmx,
			int gcThreads) {
		super(max_processes, xms, xmx, gcThreads);
	}

	public LocalSubProcessExecutor(int max_processes, String... customArgs) {
		super(max_processes, customArgs);
	}

	@Override
	protected ProcessBuilder createProcess(long simId)
			throws InsufficientResourcesException {
		// set up processbuilder
		// see
		// http://stackoverflow.com/questions/636367/java-executing-a-java-application-in-a-separate-process/723914#723914
		String javaHome = System.getProperty("java.home");
		String javaBin = javaHome + File.separator + "bin" + File.separator
				+ "java";
		String classpath = getClasspath();
		String className = Presage2CLI.class.getCanonicalName();

		// build program args
		List<String> args = new LinkedList<String>();
		args.add(javaBin);

		// jvm args
		args.addAll(getJvmArgs());

		// classpath and program args
		args.add("-cp");
		args.add(classpath);
		args.add(className);
		args.add("run");
		args.add(Long.toString(simId));

		return new ProcessBuilder(args);
	}

	@Override
	public String toString() {
		return "SubProcessExecutor @ localhost";
	}
}
