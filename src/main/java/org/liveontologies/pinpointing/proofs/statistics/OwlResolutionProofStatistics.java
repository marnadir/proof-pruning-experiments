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


import org.liveontologies.pinpointing.experiments.ExperimentException;
import org.liveontologies.puli.Inference;
import org.liveontologies.puli.Proof;
import org.liveontologies.puli.statistics.PrunedProofStats;
import org.semanticweb.owlapi.model.OWLAxiom;

import net.sourceforge.argparse4j.inf.ArgumentParser;

public class OwlResolutionProofStatistics extends
		OwlProofStatistics<OwlResolutionProofStatistics.Options> {


	
	public static class Options extends BaseProofStatistics.Options {
		
	}
	

	@Override
	protected Options newOptions() {
		return new Options();
	}

	@Override
	protected void addArguments(final ArgumentParser parser) {
		super.addArguments(parser);
		parser.description(
				"Experiment using Resolutionun Justification Computation and OWL API proofs from ELK.");

	}

	@Override
	protected void init(final Options options) throws ExperimentException {
		super.init(options);
	}

	@Override
	protected int computationEss(
			Proof<? extends Inference<OWLAxiom>> proof,OWLAxiom query) throws ExperimentException {
		return  new PrunedProofStats<OWLAxiom, Inference<OWLAxiom>>(proof, query).computeEss();
	}

	@Override
	protected int computationDerEss(Proof<? extends Inference<OWLAxiom>> proof, OWLAxiom query)
			throws ExperimentException {
		return  new PrunedProofStats<OWLAxiom, Inference<OWLAxiom>>(proof, query).computeDerivEss();

	}

	@Override
	protected int computationInfCycl(Proof<? extends Inference<OWLAxiom>> proof, OWLAxiom query)
			throws ExperimentException {
		return  new PrunedProofStats<OWLAxiom, Inference<OWLAxiom>>(proof, query).computeInfCycl();

	}




	

}