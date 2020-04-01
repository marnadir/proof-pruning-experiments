package org.liveontologies.pinpointing.proofs.experiments;

import org.liveontologies.pinpointing.experiments.ExperimentException;
import org.liveontologies.pinpointing.experiments.JustificationExperiment;
import org.liveontologies.puli.pinpointing.InterruptMonitor;

public interface ProofExperiment extends JustificationExperiment{
	public void computeProofs(String query, InterruptMonitor monitor) throws ExperimentException;
}
