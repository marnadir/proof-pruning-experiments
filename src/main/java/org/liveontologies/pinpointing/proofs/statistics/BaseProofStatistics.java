package org.liveontologies.pinpointing.proofs.statistics;

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

import org.liveontologies.pinpointing.Utils;
import org.liveontologies.pinpointing.experiments.ExperimentException;
import org.liveontologies.proofs.JustificationCompleteProof;
import org.liveontologies.proofs.ProofProvider;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.Proof;
import org.liveontologies.puli.statistics.NestedStats;
import org.liveontologies.puli.statistics.Stat;
import org.liveontologies.puli.statistics.Stats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

public abstract class BaseProofStatistics<O extends BaseProofStatistics.Options, C, I extends Inference<? extends C>, A>
		implements AbstractPrunedProofStatistics{

	private static final Logger LOGGER_ = LoggerFactory.getLogger(BaseProofStatistics.class);

	public static final String INDEX_FILE_NAME = "axiom_index";

	public static final String SAVE_OPT = "s";

	public static final String ONTOLOGY_OPT = "ontology";
	
	public static final String PRUNE_TYPE = "prune";

	

	public static class Options {
		@Arg(dest = SAVE_OPT)
		public File outputDir;
		@Arg(dest = ONTOLOGY_OPT)
		public File ontologyFile;
		
	}

	private File outputDir_;
	protected File ontologyFile_;
	private PrintWriter indexWriter_;

	Proof<? extends I> prunedProof;
	
	private ProofProvider<String, C, I, A> proofProvider_ = null;
	private JustificationCompleteProof<C, I, A> proof_;
	

	
	public static final double NANOS_IN_MILLIS = 1000000.0d;

	

	
	@Stat
	public int nEssential;
	
	@Stat
	public int nDerivEss;
	
	@Stat
	public int nInfCycl;
	
	

	
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
			

			if (outputDir_ == null) {
				this.indexWriter_ = null;
			} else {
				Utils.cleanDir(outputDir_);
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

	protected  abstract int computationEss(final Proof<? extends I> proof,C query)
		throws ExperimentException;
	
	protected  abstract int computationDerEss(final Proof<? extends I> proof,C query)
			throws ExperimentException;
	
	protected  abstract int computationInfCycl(final Proof<? extends I> proof,C query)
			throws ExperimentException;
	
	@Override
	public void before(final String query) throws ExperimentException {
		
		
		if (proofProvider_ != null) {
			Stats.resetStats(proofProvider_);
		}

		proof_ = proofProvider_.getProof(query);
			

	}



	@Override
	public void run() throws ExperimentException {
		
		nEssential=computationEss(proof_.getProof(), proof_.getQuery());
		nDerivEss=computationDerEss(proof_.getProof(), proof_.getQuery());
		nInfCycl=computationInfCycl(proof_.getProof(), proof_.getQuery());

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

	
}

