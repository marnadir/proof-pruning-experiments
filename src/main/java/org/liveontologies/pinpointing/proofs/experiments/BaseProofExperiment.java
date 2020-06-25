package org.liveontologies.pinpointing.proofs.experiments;

/*-
 * #%L
 * Axiom Pinpointing Experiments
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2017 - 2018 Live Ontologies Project
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.liveontologies.pinpointing.Utils;
import org.liveontologies.pinpointing.experiments.ExperimentException;
import org.liveontologies.pinpointing.experiments.AbstractJustificationExperiment;
import org.liveontologies.proofs.JustificationCompleteProof;
import org.liveontologies.proofs.ProofProvider;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Proof;
import org.liveontologies.puli.Proofs;
import org.liveontologies.puli.pinpointing.InterruptMonitor;
import org.liveontologies.puli.pinpointing.MinimalSubsetEnumerator;
import org.liveontologies.puli.pinpointing.PrunedProofComputation.PruneType;
import org.liveontologies.puli.statistics.NestedStats;
import org.liveontologies.puli.statistics.Stat;
import org.liveontologies.puli.statistics.Stats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

public abstract class BaseProofExperiment<O extends BaseProofExperiment.Options, C, I extends Inference<? extends C>, A>
		extends AbstractJustificationExperiment implements ProofExperiment{

	private static final Logger LOGGER_ = LoggerFactory.getLogger(BaseProofExperiment.class);

	public static final String INDEX_FILE_NAME = "axiom_index";

	public static final String SAVE_OPT = "s";

	public static final String ONTOLOGY_OPT = "ontology";
	
	public static final String PRUNE_TYPE = "prune";

	

	public static class Options {
		@Arg(dest = SAVE_OPT)
		public File outputDir;
		@Arg(dest = ONTOLOGY_OPT)
		public File ontologyFile;
		@Arg(dest = PRUNE_TYPE)
		public PruneType pruneType;
	}

	private File outputDir_;
	protected File ontologyFile_;
	private PrintWriter indexWriter_;
	private String pruneType_;
	Proof<? extends I> prunedProof;
	
	private MinimalSubsetEnumerator.Factory<C, A> computation_ = null;
	private ProofProvider<String, C, I, A> proofProvider_ = null;
	private JustificationCompleteProof<C, I, A> proof_;
	
	protected JustificationCounter justificationListener_;

	
	public static final double NANOS_IN_MILLIS = 1000000.0d;

	
	@Stat
	public int nInferences;
	
	@Stat
	public long timeProof;

	
	@Override
	public final void init(final String[] args) throws ExperimentException {

		final ArgumentParser parser = ArgumentParsers.newArgumentParser(getClass().getSimpleName());
		parser.addArgument("-" + SAVE_OPT).type(File.class)
				.help("if provided, save justification into specified directory");

		addArguments(parser);

		try {

			final O options = newOptions();
			parser.parseArgs(args, options);

			LOGGER_.info("outputDir: {}", options.outputDir);
			this.outputDir_ = options.outputDir;
			this.outputDir_ = new File("/home/nadir/Desktop/OutputDir");
			this.ontologyFile_ = options.ontologyFile;
			this.pruneType_=options.pruneType.name();

			if (outputDir_ == null) {
				this.justificationListener_ = new JustificationCounter();
				this.indexWriter_ = null;
			} else {
				Utils.cleanDir(outputDir_);
				this.justificationListener_ = new JustificationCollector();
				this.indexWriter_ = new PrintWriter(new FileWriter(new File(outputDir_, INDEX_FILE_NAME), true));
			}
			init(options);
			proofProvider_ = newProofProvider();

		} catch (final SecurityException e) {
			throw new ExperimentException(e);
		} catch (final IOException e) {
			throw new ExperimentException(e);
		} catch (final ArgumentParserException e) {
			parser.handleError(e);
			System.exit(2);
		}

	}
	



	/**
	 * Adds arguments to the provided parser that populate fields required by
	 * {@link #init(Options)}.
	 * 
	 * @param parser
	 */
	protected abstract void addArguments(ArgumentParser parser);

	/**
	 * @return New instance of a subclass of {@link Options} that adds fields
	 *         required by {@link #init(Options)}.
	 */
	protected abstract O newOptions();

	/**
	 * @param options The object instantiated with {@link #newOptions()} and
	 *                populated by
	 *                {@link ArgumentParser#parseArgs(String[], Object)} called on
	 *                the parser passed to {@link #addArguments(ArgumentParser)}.
	 * @throws ExperimentException
	 */
	protected abstract void init(O options) throws ExperimentException;

	/**
	 * Called after {@link #init(Options)}.
	 * 
	 * @return The proof provider used for the experiments.
	 * @throws ExperimentException
	 */
	protected abstract ProofProvider<String, C, I, A> newProofProvider() throws ExperimentException;

	protected  abstract Proof<? extends I> computationPruning(final Proof<? extends I> proof,
			InterruptMonitor monitor,C query,Set<Object> just)
		throws ExperimentException;
	

	/**
	 * Called after {@link #init(Options)}.
	 * 
	 * @param proof
	 * @param justifier
	 * @param monitor
	 * @return The computation used for the experiments.
	 * @throws ExperimentException
	 */
	protected abstract MinimalSubsetEnumerator.Factory<C, A> newComputation(Proof<? extends I> proof,
			InferenceJustifier<? super I, ? extends Set<? extends A>> justifier, InterruptMonitor monitor)
			throws ExperimentException;
	
	@Override
	public void before(final String query) throws ExperimentException {
		
		if (computation_ != null) {
			Stats.resetStats(computation_);
		}
		if (proofProvider_ != null) {
			Stats.resetStats(proofProvider_);
		}

		proof_ = proofProvider_.getProof(query);
			

	}

	@Override
	public void computeProofs(String query, InterruptMonitor monitor) throws ExperimentException {
		
		prunedProof=proof_.getProof();
		if(pruneType_.equals("JUST_PRUNE")) {
			run(monitor);
		}
		long startTime = System.currentTimeMillis();
		prunedProof=computationPruning(proof_.getProof(), monitor, proof_.getQuery(),
		(Set<Object>) justificationListener_.getUnionJustif());
		long stopTime = System.currentTimeMillis();
		
		timeProof = (long) ((stopTime - startTime)/NANOS_IN_MILLIS);

		nInferences = Proofs.countInferences(prunedProof, proof_.getQuery());			
	}
	
	
	
	@Override
	public void run(final InterruptMonitor monitor) throws ExperimentException {
		computation_ = newComputation(prunedProof, proof_.getJustifier(), monitor);
		computation_.newEnumerator(proof_.getQuery()).enumerate(justificationListener_);
	}

	double RoundTo2Decimals(double val) {
		DecimalFormat df2 = new DecimalFormat("###.##");
		return Double.valueOf(df2.format(val));
	}

	@Override
	public void after() throws ExperimentException {
	
		
	}

	@Override
	public void dispose() {
		Utils.closeQuietly(indexWriter_);
		if (proofProvider_ != null) {
			proofProvider_.dispose();
		}
	}



	@NestedStats(name = "proofProvider")
	public ProofProvider<String, C, I, A> getProofProvider() {
		return proofProvider_;
	}

	private class JustificationCounter implements MinimalSubsetEnumerator.Listener<A> {

		private final List<Integer> justSizes_ = new ArrayList<>();
		private final List<Long> justTimes_ = new ArrayList<>();

		@Override
		public void newMinimalSubset(final Set<A> justification) {
			fireNewJustification();
			justTimes_.add(System.nanoTime());
			justSizes_.add(justification.size());
		}

		public Collection<Set<A>> getJustifications() {
			return null;
		}

		public Set<A> getUnionJustif() {
			return null;
		}

		public List<Integer> getSizes() {
			return justSizes_;
		}

		public List<Long> getTimes() {
			return justTimes_;
		}

		public void reset() {
			justSizes_.clear();
			justTimes_.clear();
		}

	}

	private class JustificationCollector extends JustificationCounter {

		private final Collection<Set<A>> justifications_ = new ArrayList<>();

		@Override
		public void newMinimalSubset(final Set<A> justification) {
			super.newMinimalSubset(justification);
			justifications_.add(justification);
		}

		@Override
		public Collection<Set<A>> getJustifications() {
			return justifications_;
		}
		
		
		public Set<A> getUnionJustif() {
			Set<A> union = new HashSet<>();
			for (Set<A> justif : justifications_) {
				union.addAll(justif);
			}

			return union;
		}

		@Override
		public void reset() {
			super.reset();
			justifications_.clear();
		}

	}

}

