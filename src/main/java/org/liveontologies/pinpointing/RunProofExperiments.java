package org.liveontologies.pinpointing;

/*-
 * #%L
 * Axiom Pinpointing Experiments
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2017 - 2020 Live Ontologies Project
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


/**
 * Class that run the experiments for pruning proofs strategies
 * 
 * @author Marouane Nadir
 *

 */


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.output.NullOutputStream;
import org.liveontologies.pinpointing.experiments.ExperimentException;
import org.liveontologies.pinpointing.proofs.experiments.ProofExperiment;
import org.liveontologies.puli.pinpointing.InterruptMonitor;
import org.liveontologies.puli.statistics.Stats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;



public class RunProofExperiments {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(RunProofExperiments.class);

	public static final String RECORD_OPT = "record";
	public static final String TIMEOUT_OPT = "t";
	public static final String GLOBAL_TIMEOUT_OPT = "g";
	public static final String WARMUP_TIMEOUT_OPT = "w";
	public static final String GC_OPT = "gc";
	public static final String ONE_JUST_OPT = "only1just";
	public static final String OPT_PROGRESS = "progress";
	public static final String RESET_INTERVAL_OPT = "ri";
	public static final String QUERIES_OPT = "queries";
	public static final String EXPERIMENT_OPT = "exp";
	public static final String EXPERIMENT_ARGS_OPT = "arg";
	public static final String ENUM_JUST = "j";

	public static class Options {
		@Arg(dest = RECORD_OPT)
		public File recordFile;
		@Arg(dest = TIMEOUT_OPT)
		public Long timeOutMillis;
		@Arg(dest = GLOBAL_TIMEOUT_OPT)
		public Long globalTimeOutMillis;
		@Arg(dest = WARMUP_TIMEOUT_OPT)
		public Long warmupTimeOut;
		@Arg(dest = GC_OPT)
		public boolean runGc;
		@Arg(dest = ONE_JUST_OPT)
		public boolean onlyOneJustification;
		@Arg(dest = OPT_PROGRESS)
		public boolean progress;
		@Arg(dest = RESET_INTERVAL_OPT)
		public Integer resetInterval;
		@Arg(dest = QUERIES_OPT)
		public File queryFile;
		@Arg(dest = EXPERIMENT_OPT)
		public String experimentClassName;
		@Arg(dest = ENUM_JUST)
		public String enumJust;
		@Arg(dest = EXPERIMENT_ARGS_OPT)
		public String[] experimentArgs;
	}

	public static final long TIMEOUT_DELAY_MILLIS = 10l;
	public static final double NANOS_IN_MILLIS = 1000000.0d;
	public static final double MILLIS_IN_SECOND = 1000.0d;

	public static void main(final String[] args) {

		final ArgumentParser parser = ArgumentParsers
				.newArgumentParser(
						RunProofExperiments.class.getSimpleName())
				.description("Run justification experiments.");
		parser.addArgument(RECORD_OPT).type(File.class).help("record file");
		parser.addArgument("-" + TIMEOUT_OPT).type(Long.class)
				.help("timeout per query in milliseconds");
		parser.addArgument("-" + GLOBAL_TIMEOUT_OPT).type(Long.class)
				.help("global timeout in milliseconds");
		parser.addArgument("-" + WARMUP_TIMEOUT_OPT).type(Long.class)
				.help("how long should warm up in milliseconds");
		parser.addArgument("--" + GC_OPT).action(Arguments.storeTrue())
				.help("run garbage collector before every query");
		parser.addArgument("--" + ONE_JUST_OPT).action(Arguments.storeTrue())
				.help("compute only one justification");
		parser.addArgument("--" + OPT_PROGRESS).action(Arguments.storeTrue())
				.help("print progress to stdout");
		parser.addArgument("--" + RESET_INTERVAL_OPT).type(Integer.class)
				.help("after how many queries should the experiment be reset");
		parser.addArgument(QUERIES_OPT)
				.type(Arguments.fileType().verifyExists().verifyCanRead())
				.help("query file");
		parser.addArgument(EXPERIMENT_OPT).help("experiment class name");
		parser.addArgument("-"+ENUM_JUST).help("enumerition of justifications");
		parser.addArgument(EXPERIMENT_ARGS_OPT).nargs("*")
				.help("experiment arguments");

		BufferedReader queryReader = null;
		PrintWriter recordWriter = null;

		try {

			final Options opt = new Options();
			parser.parseArgs(args, opt);

			final File recordFile = opt.recordFile;
			if (recordFile.exists()) {
				Utils.recursiveDelete(recordFile);
			}
			LOGGER_.info("recordFile: {}", recordFile);
			final long timeOutMillis = opt.timeOutMillis == null ? 0l
					: opt.timeOutMillis;
			LOGGER_.info("timeOutMillis: {}", timeOutMillis);
			final long globalTimeOutMillis = opt.globalTimeOutMillis == null
					? 0l
					: opt.globalTimeOutMillis;
			LOGGER_.info("globalTimeOutMillis: {}", globalTimeOutMillis);
			final long warmupTimeOut = opt.warmupTimeOut == null ? 0l
					: opt.warmupTimeOut;
			LOGGER_.info("warmupTimeOut: {}", warmupTimeOut);
			final boolean runGc = opt.runGc;
			LOGGER_.info("runGc: {}", runGc);
			final boolean onlyOneJustification = opt.onlyOneJustification;
			LOGGER_.info("onlyOneJustification: {}", onlyOneJustification);
			final boolean progress = opt.progress;
			LOGGER_.info("progress: {}", progress);
			final int resetInterval = opt.resetInterval == null
					? Integer.MAX_VALUE
					: opt.resetInterval;
			LOGGER_.info("resetInterval: {}", resetInterval);
			final File queryFile = opt.queryFile;
			LOGGER_.info("queryFile: {}", queryFile);
			final String experimentClassName = opt.experimentClassName;
			LOGGER_.info("experimentClassName: {}", experimentClassName);
			final String enumJust = opt.enumJust;
			LOGGER_.info("enumerationJusts: {}", enumJust);
			final String[] experimentArgs = opt.experimentArgs;
			LOGGER_.info("experimentArgs: {}", Arrays.toString(experimentArgs));

			final ProofExperiment experiment = newExperiment(
					experimentClassName);

			recordWriter = new PrintWriter(recordFile);

			final PrintStream nullPrintStream = new PrintStream(
					new NullOutputStream());

			if (warmupTimeOut > 0) {
				LOGGER_.info("Warm Up");
				run(experiment, experimentArgs, queryFile, timeOutMillis,
						warmupTimeOut, 0, runGc, onlyOneJustification,
						resetInterval,enumJust, nullPrintStream, null);
			}

			LOGGER_.info("Actual Experiment Run");
			run(experiment, experimentArgs, queryFile, timeOutMillis,
					globalTimeOutMillis, 0, runGc, onlyOneJustification,
					resetInterval, enumJust,progress ? System.out : nullPrintStream,
					recordWriter);

		} catch (final ExperimentException e) {
			LOGGER_.error(e.getMessage(), e);
			System.exit(2);
		} catch (final FileNotFoundException e) {
			LOGGER_.error("File not found!", e);
			System.exit(2);
		} catch (final IOException e) {
			LOGGER_.error("Cannot read query!", e);
			System.exit(2);
		} catch (final ArgumentParserException e) {
			parser.handleError(e);
			System.exit(2);
		} finally {
			Utils.closeQuietly(queryReader);
			Utils.closeQuietly(recordWriter);
		}

	}

	private static ProofExperiment newExperiment(
			final String experimentClassName) throws ExperimentException {

		try {
			final Class<?> experimentClass = RunProofExperiments.class
					.getClassLoader().loadClass(experimentClassName);
			final Constructor<?> constructor = experimentClass.getConstructor();
			final Object object = constructor.newInstance();
			if (!(object instanceof ProofExperiment)) {
				throw new ExperimentException(
						"The specified experiment class is not a subclass of "
								+ ProofExperiment.class.getName());
			}
			return (ProofExperiment) object;
		} catch (final ClassNotFoundException e) {
			throw new ExperimentException(
					"The specified experiment class could not be found!", e);
		} catch (final NoSuchMethodException e) {
			throw new ExperimentException(
					"The specified experiment class does not define required constructor!",
					e);
		} catch (final SecurityException e) {
			throw new ExperimentException(
					"The specified experiment could not be instantiated!", e);
		} catch (final InstantiationException e) {
			throw new ExperimentException(
					"The specified experiment could not be instantiated!", e);
		} catch (final IllegalAccessException e) {
			throw new ExperimentException(
					"The specified experiment could not be instantiated!", e);
		} catch (final InvocationTargetException e) {
			throw new ExperimentException(
					"The specified experiment could not be instantiated!", e);
		}

	}

	private static void run(final ProofExperiment experiment,
			final String[] experimentArgs, final File queryFile,
			final long timeOutMillis, final long globalTimeOutMillis,
			final int maxIterations, final boolean runGc,
			final boolean onlyOneJustification, final int resetInterval,
			final String enumJust,
			final PrintStream progressOut, final PrintWriter recordWriter)
			throws IOException, ExperimentException {

		experiment.init(experimentArgs);
		final boolean computeJust=enumJust.contentEquals("enumJust");

		Progress progress = null;

		BufferedReader queryReader = null;

		try {

			queryReader = new BufferedReader(new FileReader(queryFile));
			int queryCount = 0;
			while (queryReader.readLine() != null) {
				queryCount++;
			}
			queryReader.close();
			final int total = maxIterations <= 0 ? queryCount
					: Math.min(maxIterations, queryCount);
			progress = new Progress(progressOut, total);

			queryReader = new BufferedReader(new FileReader(queryFile));

			final Recorder recorder = new Recorder(recordWriter);

			final long globalStartTimeMillis = System.currentTimeMillis();
			long globalStopTimeMillis = globalTimeOutMillis > 0
					? globalStartTimeMillis + globalTimeOutMillis
					: Long.MAX_VALUE;

			boolean didSomeExperimentRun = false;
			for (int nIter = 0; nIter < maxIterations
					|| maxIterations <= 0; nIter++) {
				final String query = queryReader.readLine();
				if (query == null) {
					break;
				}

				if (maxIterations > 0) {
					LOGGER_.info("Run number {} of {}", nIter + 1,
							maxIterations);
				} else {
					LOGGER_.info("Run number {}", nIter + 1);
				}

				if (globalTimeOutMillis > 0) {
					final long globalTimeLeftMillis = globalStopTimeMillis
							- System.currentTimeMillis();
					LOGGER_.info("{}s left until global timeout",
							globalTimeLeftMillis / MILLIS_IN_SECOND);
					if (globalTimeLeftMillis <= 0l) {
						break;
					}
				}

				if (nIter % resetInterval == resetInterval - 1) {
					experiment.dispose();
					experiment.init(experimentArgs);
				}

				if (runGc) {
					System.gc();
				}

				
				//compute profs
				long localStartTimeMillis = System.currentTimeMillis();
			    long localStopTimeMillis = timeOutMillis > 0
						? localStartTimeMillis + timeOutMillis
						: Long.MAX_VALUE;

				long stopTimeMillis = localStopTimeMillis;
				final TimeOutMonitor monitorProofs = new TimeOutMonitor(
						stopTimeMillis, onlyOneJustification);
		
				experiment.addJustificationListener(monitorProofs);
				
				experiment.before(query);
				long startTimeNanos = System.nanoTime();
				experiment.computeProofs(query, monitorProofs);
				long runTimeNanos = System.nanoTime() - startTimeNanos;
				double timeProofs = runTimeNanos/NANOS_IN_MILLIS;

				experiment.removeJustificationListener(monitorProofs);

				
				//compute proofs
				localStartTimeMillis = System.currentTimeMillis();
				localStopTimeMillis = timeOutMillis > 0
						? localStartTimeMillis + timeOutMillis
						: Long.MAX_VALUE;
	
				stopTimeMillis = localStopTimeMillis;
				final TimeOutMonitor monitorJust = new TimeOutMonitor(
						stopTimeMillis, onlyOneJustification);
				experiment.addJustificationListener(monitorJust);


				final Recorder.RecordBuilder record = recorder.newRecord();
				record.put("query", query);
				if (didSomeExperimentRun) {
					recorder.flush();
				}


				final Runnable runnable = new Runnable() {
					@Override
					public void run() {
						try { 
							if(computeJust) {
								experiment.run(monitorJust);
							}
						}catch (final ExperimentException e) {
							throw new RuntimeException(e);
						}
					}
				};
				final Thread worker = new Thread(runnable);
				startTimeNanos = System.nanoTime();
				worker.start();
				// wait for timeout
				try {
					worker.join(timeOutMillis > 0
							? timeOutMillis + TIMEOUT_DELAY_MILLIS
							: 0);
				} catch (final InterruptedException e) {
					LOGGER_.warn("Waiting for the worker thread interruptet!",
							e);
				}
				runTimeNanos = System.nanoTime() - startTimeNanos;
				experiment.removeJustificationListener(monitorJust);
				final int nJust = monitorJust.getJustificationCount();
				didSomeExperimentRun = true;
				killIfAlive(worker);

			
				
				
				final boolean didTimeOut = localStartTimeMillis
						+ (runTimeNanos / NANOS_IN_MILLIS) > stopTimeMillis;
				record.put("didTimeOut", didTimeOut);
				if(computeJust) {
					record.put("time", runTimeNanos / (NANOS_IN_MILLIS));
					record.put("nJust", nJust);
				}
				record.put("timePtoofs", timeProofs);

				experiment.after();

				final Map<String, Object> stats = Stats.copyIntoMap(experiment,
						new TreeMap<String, Object>());
				for (final Map.Entry<String, Object> entry : stats.entrySet()) {
					record.put(entry.getKey(), entry.getValue());
				}
				
				
				
				recorder.flush();

				progress.update();

			}

		} finally {
			Utils.closeQuietly(queryReader);
			experiment.dispose();
			if (progress != null) {
				progress.stop();
			}
		}

	}

	/**
	 * If the specified thread is alive, calls {@link Thread#stop()} on it.
	 * <strong>This breaks any synchronization with the thread.</strong>
	 * 
	 * @param thread
	 */
	@SuppressWarnings("deprecation")
	private static void killIfAlive(final Thread thread) {
		if (thread.isAlive()) {
			LOGGER_.info("killing the thread {}", thread.getName());
			thread.stop();
		}
	}

	/**
	 * Interrupts when the global or local timeout expires. The global timeout
	 * is counted from the passed global start time and the local from the
	 * creation of this object.
	 * 
	 * @author Peter Skocovsky
	 */
	private static class TimeOutMonitor implements InterruptMonitor,ProofExperiment.Listener {

		private final long stopTimeMillis_;
		private final boolean onlyOneJustification_;

		private int count_ = 0;

		private volatile boolean cancelled = false;

		public TimeOutMonitor(final long stopTimeMillis, final boolean onlyOneJustification) {
			this.stopTimeMillis_ = stopTimeMillis;
			this.onlyOneJustification_ = onlyOneJustification;
		}

		@Override
		public boolean isInterrupted() {
			if (stopTimeMillis_ < System.currentTimeMillis()) {
				cancelled = true;
			}
			return cancelled;
		}

		@Override
		public void newJustification() {
			count_++;
			if (onlyOneJustification_) {
				cancelled = true;
			}
		}

		public int getJustificationCount() {
			return count_;
		}

	}
}
