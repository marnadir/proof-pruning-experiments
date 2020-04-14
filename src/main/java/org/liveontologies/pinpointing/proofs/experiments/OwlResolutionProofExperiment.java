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
import org.liveontologies.puli.pinpointing.PrunedProofComputation;
import org.liveontologies.puli.pinpointing.ResolutionJustificationComputation;
import org.liveontologies.puli.pinpointing.MinimalSubsetEnumerator.Factory;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.inf.ArgumentParser;

public class OwlResolutionProofExperiment extends
		OwlProofExperiment<OwlResolutionProofExperiment.Options> {

	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(OwlResolutionProofExperiment.class);

	public static final String PRUNE_TYPE = "prune";

	
	public static class Options extends BaseProofExperiment.Options {
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
	protected Proof<? extends Inference<OWLAxiom>>computationPruning(
			Proof<? extends Inference<OWLAxiom>> proof, InterruptMonitor monitor, OWLAxiom query,Set<Object> justUnion)
			throws ExperimentException {
		
		return new PrunedProofComputation<Object, Inference<OWLAxiom>>(proof, monitor, pruneType, query,justUnion).computePrune();
	}

	@Override
	protected Factory<OWLAxiom, OWLAxiom> newComputation(
			final Proof<? extends Inference<OWLAxiom>> proof,
			final InferenceJustifier<? super Inference<OWLAxiom>, ? extends Set<? extends OWLAxiom>> justifier,
			final InterruptMonitor monitor) throws ExperimentException {
		return ResolutionJustificationComputation.<OWLAxiom, Inference<OWLAxiom>, OWLAxiom> getFactory().create(proof, justifier, monitor);
	}

	

}