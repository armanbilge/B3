/*
 * MCMCMCRunner.java
 *
 * BEAST: Bayesian Evolutionary Analysis by Sampling Trees
 * Copyright (C) 2014 BEAST Developers
 *
 * BEAST is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * BEAST is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BEAST.  If not, see <http://www.gnu.org/licenses/>.
 */

package beast.inference.mcmcmc;

import beast.inference.markovchain.MarkovChain;

/**
 * @author rambaut
 *         Date: Jan 5, 2005
 *         Time: 5:05:59 PM
 */
public class MCMCMCRunner extends Thread {

    public MCMCMCRunner(MarkovChain markovChain, long length, long totalLength, boolean disableCoerce) {

        this.markovChain = markovChain;
        this.length = length;
        this.totalLength = totalLength;
        this.disableCoerce = disableCoerce;
    }

	public void run() {
        long i = 0;
        while (i < totalLength) {
            markovChain.runChain(length, disableCoerce/*, 0*/);

            i += length;

	        chainDone();

	        if (i < totalLength) {
		        while (isChainDone()) {
			        try {
				        synchronized(this) {
					        wait();
				        }
			        } catch (InterruptedException e) {
				        // continue...
			        }
		        }
	        }
        }
	}

	private synchronized void chainDone() {
		chainDone = true;
	}

	public synchronized boolean isChainDone() {
		return chainDone;
	}

    public synchronized void continueChain() {
        this.chainDone = false;
	    notify();
    }


	private final MarkovChain markovChain;
	private final long length;
    private final long totalLength;
    private final boolean disableCoerce;

	private boolean chainDone;
}

