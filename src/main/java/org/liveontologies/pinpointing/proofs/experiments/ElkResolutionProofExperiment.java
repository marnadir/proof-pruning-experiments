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

import java.util.Set;

import org.liveontologies.pinpointing.experiments.ExperimentException;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.InferenceJustifier;
import org.liveontologies.puli.Proof;
import org.liveontologies.puli.pinpointing.InterruptMonitor;
import org.liveontologies.puli.pinpointing.MinimalSubsetEnumerator;
import org.liveontologies.puli.pinpointing.PrunedProofComputation;
import org.liveontologies.puli.pinpointing.ResolutionJustificationComputation;
import org.semanticweb.elk.owl.interfaces.ElkAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.inf.ArgumentParser;

public class ElkResolutionProofExperiment extends
		ElkProofExperiment<ElkResolutionProofExperiment.Options> {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(ElkResolutionProofExperiment.class);

	public static final String PRUNE_TYPE = "prune";

	
	public static class Options extends ElkProofExperiment.Options {
		@Arg(dest = PRUNE_TYPE)
		public PrunedProofComputation.PruneType pruneType;
	}

	private PrunedProofComputation.PruneType pruneType;

	@Override
	protected Options newOptions() {
		return new Options();
	}


	@Override
	protected void addArguments(final ArgumentParser parser) {
		super.addArguments(parser);
		parser.description(
				"Experiment using Resolutionun Justification Computation and OWL API proofs from ELK.");
		parser.addArgument(PRUNE_TYPE)
		.type(PrunedProofComputation.PruneType.class)
		.help("prune type");
	}

	@Override
	protected void init(final Options options) throws ExperimentException {
		super.init(options);
		LOGGER_.info("pruneType: {}", options.pruneType);
		this.pruneType=options.pruneType;
	}


	@Override
	protected Proof<? extends Inference<Object>> computationPruning(Proof<? extends Inference<Object>> proof,
			InterruptMonitor monitor, Object query, Set<Object> just) throws ExperimentException {
		return new PrunedProofComputation<Object, Inference<Object>>(proof, monitor, pruneType, query,just).computePrune();
	}
	
	@Override
	protected MinimalSubsetEnumerator.Factory<Object, ElkAxiom> newComputation(
			final Proof<? extends Inference<Object>> proof,
			final InferenceJustifier<? super Inference<Object>, ? extends Set<? extends ElkAxiom>> justifier,
			final InterruptMonitor monitor) throws ExperimentException {
		return ResolutionJustificationComputation
				.<Object, Inference<Object>, ElkAxiom> getFactory()
				.create(proof, justifier, monitor);
	}





}