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
package uk.ac.imperial.presage2.core.simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import uk.ac.imperial.presage2.core.Time;
import uk.ac.imperial.presage2.core.TimeDriven;
import uk.ac.imperial.presage2.core.participant.Participant;
import uk.ac.imperial.presage2.core.plugin.Plugin;

import com.google.inject.Inject;

/**
 * @author Sam Macbeth
 *
 */
public class MultiThreadedSimulator extends Simulator {

	private final Logger logger = Logger.getLogger(MultiThreadedSimulator.class);
	
	private final Time time;
	
	private final int threads;
	
	private final ExecutorService threadPool;
	
	/**
	 * Create a multi threaded simulator for a given {@link Scenario}.
	 * @param scenario	{@link Scenario} to simulate
	 * @param t			{@link Time} start time
	 * @param threads	Number of threads to use.
	 */
	@Inject
	public MultiThreadedSimulator(Scenario scenario, Time t, @Threads int threads) {
		super(scenario);
		this.time = t;
		this.threads = threads;
		this.threadPool = Executors.newFixedThreadPool(this.threads);
	}

	/**
	 * <p>Wrapper for a call to {@link Participant#initialise()} as a {@link Runnable} for
	 * use with the {@link ExecutorService}.</p>
	 */
	private class ParticipantInitialisor implements Runnable {

		private final Participant p;
		
		public ParticipantInitialisor(Participant p) {
			this.p = p;
		}
		
		@Override
		public void run() {
			try {
				p.initialise();
			} catch(Exception e) {
				logger.warn("Exception thrown by participant "+p.getName()+" on initialisation.", e);
			}
		}
		
	}
	
	/**
	 * <p>Wrapper for a call to {@link Plugin#initialise()} as a {@link Runnable} for
	 * use with the {@link ExecutorService}.</p>
	 */
	private class PluginInitialisor implements Runnable {

		private final Plugin p;
		
		public PluginInitialisor(Plugin p) {
			this.p = p;
		}
		
		@Override
		public void run() {
			try {
				p.initialise();
			} catch(Exception e) {
				logger.warn("Exception thrown by plugin "+p+" on initialisation.", e);
			}
		}
	}
	
	@Override
	public void initialise() {
		
		final List<Future<?>> futures = new ArrayList<Future<?>>();
		// init Participants
		logger.info("Initialising Participants..");
		for(Participant p : this.scenario.getParticipants()) {
			futures.add(threadPool.submit(new ParticipantInitialisor(p)));
		}
		// init Plugins
		logger.info("Initialising Plugins..");
		for(Plugin pl : this.scenario.getPlugins()) {
			futures.add(threadPool.submit(new PluginInitialisor(pl)));
		}
		// wait for threads to complete
		for(Future<?> f : futures) {
			try {
				f.get();
			} catch (InterruptedException e) {
				logger.warn("Unexpected InterruptedException on sim initialisation.", e);
			} catch (ExecutionException e) {
				logger.warn("Unexpected ExecutionException on sim initialisation.", e);
			}
		}
		
	}
	
	/**
	 * <p>Wrapper for a call to {@link TimeDriven#incrementTime()} as a {@link Runnable} for
	 * use with the {@link ExecutorService}.</p>
	 */
	private class TimeIncrementor implements Runnable {

		private final TimeDriven t;
		
		public TimeIncrementor(TimeDriven t) {
			this.t = t;
		}
		
		@Override
		public void run() {
			t.incrementTime();
		}
	}

	/**
	 * Runs the simulation. We split the simulation cycle into two parts. We add all
	 * the participants to the thread pool, wait for them to finish executing and then
	 * run other {@link TimeDriven} elements and {@link Plugin}s.
	 */
	@Override
	public void run() {
		
		while(this.scenario.getFinishTime().greaterThan(time)) {
			
			List<Future<?>> futures = new ArrayList<Future<?>>();
			
			logger.info("Time: "+ time.toString());
			
			logger.debug("Executing Participants...");
			for(Participant p : this.scenario.getParticipants()) {
				try {
					futures.add(threadPool.submit(new TimeIncrementor(p)));
				} catch(Exception e) {
					logger.warn("Exception thrown by participant "+p.getName()+" on execution.", e);
				}
			}
			
			// wait for Participants to finish
			for(Future<?> f : futures) {
				try {
					f.get();
				} catch (InterruptedException e) {
					logger.warn("Unexpected InterruptedException on sim initialisation.", e);
				} catch (ExecutionException e) {
					logger.warn("Unexpected ExecutionException on sim initialisation.", e);
				}
			}
			futures.clear();
			
			logger.debug("Executing TimeDriven...");
			for(TimeDriven t : this.scenario.getTimeDriven()) {
				try {
					futures.add(threadPool.submit(new TimeIncrementor(t)));
				} catch(Exception e) {
					logger.warn("Exception thrown by TimeDriven "+t+" on execution.", e);
				}
			}
			
			logger.debug("Executing Plugins...");
			for(Plugin pl : this.scenario.getPlugins()) {
				try {
					futures.add(threadPool.submit(new TimeIncrementor(pl)));
				} catch(Exception e) {
					logger.warn("Exception thrown by Plugin "+pl+" on execution.", e);
				}
			}
			
			for(Future<?> f : futures) {
				try {
					f.get();
				} catch (InterruptedException e) {
					logger.warn("Unexpected InterruptedException on sim initialisation.", e);
				} catch (ExecutionException e) {
					logger.warn("Unexpected ExecutionException on sim initialisation.", e);
				}
			}
			futures.clear();
			time.increment();
			
		}
		
		threadPool.shutdown();
	}
	
	@Override
	public void complete() {
		for(Plugin pl : this.scenario.getPlugins()) {
			try {
				pl.onSimulationComplete();
			} catch(Exception e) {
				logger.warn("Exception thrown by Plugin "+pl+" on simulation completion.", e);
			}
		}
	}

}