/*
 * Copyright (c) 2007-2010, Michael Genkin and Alexander Gutfraind, Cornell University.
 *    All rights reserved.
 *    BSD license.
 *
 * Neither the name of Cornell University nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * Derived in part from Repast project samples
 * Copyright (c) 1999, Trustees of the University of Chicago
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with 
 * or without modification, are permitted provided that the following 
 * conditions are met:
 *
 *	 Redistributions of source code must retain the above copyright notice,
 *	 this list of conditions and the following disclaimer.
 *
 *	 Redistributions in binary form must reproduce the above copyright notice,
 *	 this list of conditions and the following disclaimer in the documentation
 *	 and/or other materials provided with the distribution.
 *
 * Neither the name of the University of Chicago nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE TRUSTEES OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package edu.cornell.rad64;

import java.awt.Color;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.ListIterator;

import uchicago.src.sim.engine.*;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.network.DefaultDrawableNode;
import uchicago.src.sim.util.*;

public class NewNode extends DefaultDrawableNode implements CustomProbeable, Comparable {
	//statics
	private static double  	avgDegree = Double.NaN;
	private static double  	avgInitTieStrength = Double.NaN;
	private static double  	avgPressurability = Double.NaN;
	private static double   fixedAttribSalience = Double.NaN;
	private static double  	degreeSD = Double.NaN;
	private static double   diversity = Double.NaN;
	private static double   fractionInitPacifists = Double.NaN;
	private static double   fractionInitRadicals = Double.NaN;
	private static double   initTieStrengthSD = Double.NaN;
	private static double   linkAgeBonusFactor = Double.NaN;
	private static double   maxLinkAgeBonus = Double.NaN;
	private static NewModel model = null;
	//private static double   mutualFriendFactor = Double.NaN;
	private static int     	numFixAttributes = Integer.MIN_VALUE;
	private static int     	numVarAttributes = Integer.MIN_VALUE;
	private static int     	numZealLevels = Integer.MIN_VALUE;
	private static double  	pressurabilitySD = Double.NaN;
	private static boolean 	repellingTies;
	private static boolean 	safeNewFriends;
	private static double   strengthUpdateRate = Double.NaN;
	private static double  	transitiveFriendship = Double.NaN;
	private static double   switchPressFactor = Double.NaN;
	private static double   switchRandomness = Double.NaN;
	private static double   zealSalience = Double.NaN;

	//fields
	private int     	degreeCap;
	private double 		fixAttributes[];
	private double	  peerPressurability;
	private double 	  varAttributes[];
	private double  	zeal;

	//implementation variables
	private static int 		 labelCounter = 0;
	private static boolean staticParamsLoaded = false;
	private static double  maxPossibleAgreement;

	/*
	 * creates a new node without any edges
	 */
	public NewNode() {
		if(! staticParamsLoaded) {
			throw new RuntimeException("NewNode instance created without initializing the constants of this class");
		}

		//note: fix attributes includes ethnicity; var attributes DOES NOT include zeal; 
		fixAttributes 					= new double[numFixAttributes];
		varAttributes 					= new double[numVarAttributes];
		double diversityFrac = diversity/2;
		for(int i=0; i<numFixAttributes; ++i) {
			fixAttributes[i] 	= (Random.uniform.nextDouble()>diversityFrac)? -1: 1;
		}
		for(int i=0; i<numVarAttributes; ++i) {
			varAttributes[i] 	= (Random.uniform.nextDouble()>diversityFrac)? -1: 1;
		}

		double r = Random.uniform.nextDouble();
		r -= fractionInitRadicals;
		if ( r < 0) {
			zeal = 1.0;
		}
		else {
			r -= fractionInitPacifists;
			if (r < 0) {
				zeal = -1.0;
			} 
			else {
				if (numZealLevels % 2 == 0) {
					//throw new RuntimeException("New node is to be created with zeal=0.0, but moderates are not allowed with numZealLevel=even");
					System.out.println("RuntimeError: New node is to be created with zeal=0.0, but moderates are not allowed with numZealLevel=even");
					System.out.println("Creating a pacifist instead");
					//default:
					zeal = -1.0;
				}
				zeal = 0.0;
			}
		}

		setNodeLabel((new Integer(labelCounter+1)).toString());
		labelCounter ++;

		peerPressurability = Math.max(0, Random.normal.nextDouble()*pressurabilitySD + avgPressurability);

		int goalEdges  = (int)Math.round(Math.max(1, Random.normal.nextDouble()*degreeSD + avgDegree));
		//alternative: 
		//int goalEdges  = generateZipfNodeDegree(newAgent);
		degreeCap = goalEdges;

		recalcColor();
		item.setBorderColor(Color.BLACK);
		item.setBorderWidth(1);
	}

	/*
	 * add node d as a friend.  for performance reasons, agreement between with d is pre-computed
	 * WARNING: this method does not check the degree cap
	 * 		For a more safe add, consider exposeToNewAgent(d)
	 * returns true if the friend has been added (always happens)
	 */
	public boolean addNewFriend(NewNode d, double newAgreement){

		//double tieSign = (newAgreement < 0)? -1: 1;
		final double tieSign = 1.0;

		double tieStrength;
		if (!repellingTies) {
			tieStrength = Random.normal.nextDouble()*initTieStrengthSD + avgInitTieStrength*tieSign;
			tieStrength = Math.max( 0, Math.min(1, tieStrength));
		} else {
			tieStrength = Random.normal.nextDouble()*initTieStrengthSD + avgInitTieStrength*tieSign;
			tieStrength = Math.max(-1, Math.min(1, tieStrength));
		}

		NewEdge edge1 = new NewEdge(this, d, tieStrength, newAgreement);
		addOutEdge(edge1);
		d.addInEdge(edge1);
		NewEdge edge2 = new NewEdge(d, this, tieStrength, newAgreement);
		d.addOutEdge(edge2);
		addInEdge(edge2);

		//updateMutualFriends(d);
		return true;
	}

	/*
	 * updates the tie strengths with all friends based on agreement between the attributes
	 */
	private void applyHomophily() {
		ArrayList links = getOutEdges();
		for (int i=0; i<links.size(); ++i) {
			NewEdge link 	 = (NewEdge) links.get(i);
			NewNode friend = (NewNode) link.getTo();

			//note: despite the + in the formula, linkStrength will never be greater than gap:
			double newStrength = link.getStrength()*(1-strengthUpdateRate) 
			+ strengthUpdateRate*link.getAgreement()/maxPossibleAgreement;
			link.setStrength(newStrength);
		}
		//this could check if the tie has dropped below 0, and if so disconnect. 
		//pro: UI might show negative strength
		//con: updateTies() disconnects ties, and is run right after.
		if (model.isGui) {
			ProbeUtilities.updateProbePanel(this);
		}
	}

	/*
	 * updates the attributes based on agreement between this and its neighbors
	 */
	private void applyPeerPressure() {
		ArrayList links = getOutEdges();
		double peerAvg 						= 0.0;
		for (int i=0; i<links.size(); ++i) { //this should probably iterate over outnodes
			NewEdge link 		= (NewEdge) links.get(i);
			NewNode friend  = (NewNode) link.getTo();
			peerAvg		     += (friend.getZeal()-zeal)*link.getStrength();	
			//unlike with var attributes below, we take the difference
		}
		if (links.size() > 0 ) {
			peerAvg /= links.size();
		}

		boolean oldZealot   = isZealot();
		boolean oldPacifist = isPacifist();
		updateZeal(peerAvg);

		if (oldZealot && ! isZealot()) {
			model.delistFromRadicalMagnets(this);
		}
		if (! oldZealot && isZealot()) {
			model.listInRadicalMagnets(this);
		}
		if (oldPacifist && ! isPacifist()) {
			model.delistFromPacifistMagnets(this);
		}
		if (! oldPacifist && isPacifist()) {
			model.listInPacifistMagnets(this);
		}

		for (int i = 0; i<varAttributes.length; ++i) {
			peerAvg = 0.0;
			for (int j=0; j<links.size(); ++j) {
				NewEdge link 		= (NewEdge) links.get(j);
				NewNode friend  = (NewNode) link.getTo();
				peerAvg		     += friend.getVarAttributes()[i]*link.getStrength();	//it's i, not j index!
			}
			if (links.size() > 0 ) {
				peerAvg /= links.size();
			}
			updateAttribute(peerAvg, i);
		}

		fireAttributeUpdate();
	}

	public double avgNeighbZeal() {
		ArrayList neighbs = getOutNodes();
		ArrayList links   = getOutEdges();
		if (neighbs.size() == 0) {
			return Double.NaN;
		}
		double sumZ = 0;
		double sumS = 0;
		int numPositiveLinks = links.size();
		for (int i=0; i<neighbs.size(); ++i){
			double s = ((NewEdge)links.get(i)).getStrength();
			if (s <= 0) {
				--numPositiveLinks;
				//we dismiss negative ties from the count
				//pro: we do b/c we want to say that high radical neighbor zeal means 
				//that radicals are surrounded by fellow travelers ie. a nascent terrorist cell
				continue;
			}
			sumS += Math.abs(s);
			sumZ += ((NewNode)neighbs.get(i)).getZeal()*s;
		}
		if (numPositiveLinks == 0) {
			return Double.NaN;
		}
		return sumZ/sumS;
	}

	/*
	 * Comparator, which ensure that when sorting all radicals are together
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object node) {
		if (this == node) {
			return 0;
		} 
		if (! (node instanceof NewNode)) {
			return +1;
		}
		double nodesZeal = ((NewNode)node).zeal;
		if (nodesZeal < zeal) {
			return -1;
		} else if (nodesZeal > zeal) {
			return +1;
		}
		try{
			Integer lthis = new Integer(this.label);
			Integer lnode = new Integer(((NewNode)node).label);
			if (lthis < lnode) {
				return -1;
			} else {
				return +1;
			}
		} catch(Exception e) {
			//in case conversion to int fails
			return this.label.compareTo(((NewNode)node).label);
		}
	}

	/*
	 * randomly selects two neighbors and attempts to connect
	 * (even if connections to both are negative)
	 */
	private void connectFriends() {
		ArrayList neighbs = getOutNodes();
		if (neighbs.size() < 2) {
			return;
		}
		NewNode nodeA = (NewNode)neighbs.get(Random.uniform.nextIntFromTo(0, neighbs.size()-1));
		NewNode nodeB = (NewNode)neighbs.get(Random.uniform.nextIntFromTo(0, neighbs.size()-1));

		nodeA.exposeToNewAgent(nodeB, safeNewFriends);
	}

	/*
	 * disconnects the tie of smallest strength (does nothing if degree==0)
	 *	note that in the case of repelling ties, the disconnected tie would have the least strength in absolute terms
	 */
	public void disconnectWeakestTie() {
		ArrayList links   = getOutEdges();
		NewEdge   weakest = null;
		double    minStrength = 999;
		for (int i=0; i<links.size(); ++i) {
			NewEdge link = (NewEdge) links.get(i);
			if (! repellingTies && link.getStrength() < minStrength) {
				weakest = link;
				minStrength = link.getStrength();
			} else if (repellingTies && Math.abs(link.getStrength()) < minStrength) {
				weakest = link;
				minStrength = Math.abs(link.getStrength());
			}
		}
		if (weakest != null) {
			NewNode friend = (NewNode) weakest.getTo();
			friend.removeEdgesFrom(this);
			friend.removeEdgesTo(this);
			removeEdgesTo(friend);
			removeEdgesFrom(friend);
			//updateMutualFriends(friend);
		}
	}
	/*
	 * Correct drawing of node rectangles
	 * we override this method to work around a bug in Repast, simGraphs.drawString()
	 */
	public void draw(SimGraphics g) {
		float truScale = new Float(1.0);

		float oldXscale = g.getXScale();
		float oldYscale = g.getYScale();

		int oldWidth  = g.getCurWidth();
		int oldHeight = g.getCurHeight();

		item.calcSize(g);
		g.setFont(item.getFont());
		g.drawFastRect(item.getColor());
		g.setXScale(truScale);
		g.setYScale(truScale);

		g.setDrawingParameters(oldWidth, oldHeight, 0);
		g.drawString(label, item.getLabelColor());
		//item.draw(g);

		g.setXScale(oldXscale);
		g.setYScale(oldYscale);

		//the proper fix:
		/*		g2.setClip((int) (curX * getXScale()), 
				(int) (curY * getYScale()), 
				curWidth, 
				curHeight);
		drawInit(stringColor);

		float xCenter = curX * getXScale() + curWidth / 2;
		float yCenter = curY * getYScale() + curHeight / 2;
		 */
	}

	/*
	 * introduces this to possibleFriend
	 * 	forms a tie if there is space in the degree budget 
	 *  			or the new friend is better match than any existing friend
	 */
	public boolean exposeToNewAgent(NewNode possibleFriend, boolean safeActionOnly) {
		if (! newFriendAcceptable(possibleFriend) || ! possibleFriend.newFriendAcceptable(this)) {
			return false;
		}
		boolean iWillAccept  = false;
		boolean heWillAccept = false;

		boolean iFriendshipVoid  = outEdges.size() < degreeCap;
		boolean heFriendshipVoid = possibleFriend.outEdges.size() < possibleFriend.degreeCap;
		double newAgreement      = netAgreement(possibleFriend, null);  //this one is symmetric
		if (! safeActionOnly) {
			iWillAccept  = true;
			heWillAccept = true;
		} else {
			if ((newAgreement == 0) || (! repellingTies && newAgreement < 0)) {
				return false;
			}

			double iMinAgreement = Double.POSITIVE_INFINITY;
			if (! iFriendshipVoid) { //save recomputation if worstAgreement is not cached
				iMinAgreement = getWorstFriendAgreement();
			}
			iWillAccept = iFriendshipVoid
			|| (! repellingTies && (iMinAgreement < newAgreement))
			||( repellingTies && (Math.abs(iMinAgreement) < Math.abs(newAgreement)) );
			if (! iWillAccept) {
				return false;
			}

			double prospectsMinAgreement = Double.POSITIVE_INFINITY;
			if (! heFriendshipVoid) {
				prospectsMinAgreement = possibleFriend.getWorstFriendAgreement();
			}
			heWillAccept = heFriendshipVoid
			|| (! repellingTies && (prospectsMinAgreement < newAgreement) )
			||( repellingTies && (Math.abs(prospectsMinAgreement) < Math.abs(newAgreement)) ); 
			//+mutualFriendFactor is for the person being disconnected: if added as new, that person would have a mutual friend

		}
		if ( iWillAccept && heWillAccept) {
			//notice: here and below we must first disconnect and then try b/c otherwise the new
			//tie would be the one disconnected - it's the weakest.
			if (! iFriendshipVoid) {
				disconnectWeakestTie();
			}
			if (! heFriendshipVoid) {
				possibleFriend.disconnectWeakestTie();
			}
			return addNewFriend(possibleFriend, newAgreement);
		} else {
			return false;
		}
	}

	/*
	 * called when the attributes or zeal of the current node has been updated,
	 * 	to let friends know that their agreement level with this is no longer up-to-date.
	 */
	private void fireAttributeUpdate() {
		for (ListIterator it = getOutEdges().listIterator(); it.hasNext(); ) {
			((NewEdge)it.next()).agreementSetDirty();
		}
		for (ListIterator it = getInEdges().listIterator(); it.hasNext(); ) {
			((NewEdge)it.next()).agreementSetDirty();
		}
	}
	/*
	 * = a Zipf law-distributed variate - based on book of Devroye
	 * 
  public static int generateZipfDegree() {
  	//double power = cachedNodeDegreePower;
    double b = Math.pow(2, nodeDegreeExponent-1);
    int X = -1;
    int maxAttempts = 10000;
    boolean ok = false;
    for (int i=0; i < maxAttempts; ++i) {
        double V = Random.Uniform.getDouble();
        X = (int)Math.min(Integer.MAX_VALUE, Math.floor( Math.pow(node.getRandomUniform(), -1./(nodeDegreeExponent-1)) ) );
        double T = Math.pow(1+1/X, nodeDegreeExponent-1);
        if (V*X*(T-1)/(b-1) <= T/b) {
        	ok = true;
          break;
        }
    }
    if (! ok) {
       return 1;//'Unable to generate random number');
    }
    return X;
  }
	 */


	public double[] getFixAttributes() {
		return fixAttributes;
	}

	public String getFixAttributesStr() {
		String s = new String();
		for (int i=0; i<fixAttributes.length; ++i) {
			s = String.format("%2.0f  %s", fixAttributes[i], s);
		}
		return s;
	}

	public int getDegreeCap() {
		return degreeCap;
	}

	public String getLabel() {
		return label;
	}

	/*
  public int getNumMutualFriends(NewNode other) {
  	int ret = 0;
  	TreeSet <NewNode> myFriends = new TreeSet<NewNode>(getOutNodes());
		for (ListIterator it = other.getOutNodes().listIterator(); it.hasNext(); ) {
		  NewNode friend = (NewNode)it.next();
		  if (myFriends.contains(friend)) {
		  	++ret;
		  }
		}
  	return ret;
  }
	 */

	public String[] getProbedProperties() {
		//String[] p = {"label", "exposedToMagnets", "fixAttributesStr", "varAttributesStr", "TiesTo", "TieStrengths", "Zeal"};
		String[] p = {"label", "degreeCap", "fixAttributesStr", "varAttributesStr", "TiesTo", "TieStrengths", "Zeal"};
		return p;
	}

	public String getTiesTo() {
		String s = new String();
		ArrayList neighbs = this.getOutNodes();
		for (int i=0; i<neighbs.size(); ++i) {
			NewNode friend = (NewNode) neighbs.get(i);
			s = String.format("%s  %s", friend.label, s);
		}
		return s;
	}
	public String getTieStrengths() {
		String s = new String();
		ArrayList links = getOutEdges();
		for (int i=0; i<links.size(); ++i) {
			NewEdge link 		= (NewEdge) links.get(i);
			s = String.format("%2.3f  %s", link.getStrength(), s);
		}
		return s;
	}

	public String getTypeLabel() {
		if (isZealot()) return "R";
		if (isPacifist()) {
			return "P";
		} else {
			return "M";
		}  		
	}

	public double[] getVarAttributes() {
		return varAttributes;
	}

	public String getVarAttributesStr() {
		String s = new String();
		for (int i=0; i<varAttributes.length; ++i) {
			s = String.format("%2.0f  %s", varAttributes[i], s);
		}
		return s;
	}
	/*
	 * = the min agreement between all pairs of friends
	 *     when degree == 0 return is undefined
	 */
	public double getWorstFriendAgreement() {
		double worstFriendAgreement = Double.POSITIVE_INFINITY;
		for (ListIterator it = getOutEdges().listIterator(); it.hasNext(); ) {
			NewEdge link   = (NewEdge)it.next();
			worstFriendAgreement = Math.min(worstFriendAgreement, link.getAgreement());
		}
		return worstFriendAgreement;
	}
	public double getZeal(){
		return zeal;
	}
	public boolean isPacifist() {
		return zeal <= -0.5;
	}
	public boolean isZealot() {
		return zeal >= 0.5;
	}

	public double localEnergy() {
		ArrayList links = getOutEdges();
		double energy  = 0;
		for (int i=0; i<links.size(); ++i) {
			NewEdge link   = (NewEdge) links.get(i);
			NewNode friend = (NewNode) link.getTo();
			double  s      = link.getStrength();

			energy -= s*netAgreement(friend, link);
		}
		return energy;
	}

	public static void loadStaticParams(
			double avgDegree, 
			double avgInitTieStrength, 
			double avgPressurability,  
			double degreeSD,
			double diversity, 
			double fixedAttribSalience, 
			double fractionInitPacifists, 
			double fractionInitRadicals,
			double initTieStrengthSD,
			double linkAgeBonusFactor,
			double maxLinkAgeBonus, 
			NewModel model, 
			double mutualFriendFactor, 
			int numZealLevels, 
			int numFixAttributes, 
			int numVarAttributes, 
			double pressurabilitySD, 
			boolean repellingTies, 
			boolean safeNewFriends, 
			double strengthUpdateRate, 
			double switchRandomness, 
			double switchPressFactor, 
			double transitiveFriendship, 
			double zealSalience) {

		NewNode.avgDegree						 = avgDegree;
		NewNode.avgInitTieStrength   = avgInitTieStrength;
		NewNode.avgPressurability    = avgPressurability;
		NewNode.degreeSD					 	 = degreeSD;
		NewNode.fixedAttribSalience  = fixedAttribSalience;
		NewNode.fractionInitPacifists = fractionInitPacifists;
		NewNode.fractionInitRadicals  = fractionInitRadicals;
		NewNode.diversity 					 = diversity;
		NewNode.initTieStrengthSD    = initTieStrengthSD;
		NewNode.linkAgeBonusFactor   = linkAgeBonusFactor;
		NewNode.maxLinkAgeBonus      = maxLinkAgeBonus;
		NewNode.model      					 = model;
		//NewNode.mutualFriendFactor   = mutualFriendFactor;
		NewNode.numFixAttributes  	 = Math.max(0, numFixAttributes);
		NewNode.numVarAttributes  	 = Math.max(0, numVarAttributes);
		NewNode.numZealLevels 			 = numZealLevels;
		NewNode.pressurabilitySD     = pressurabilitySD;
		NewNode.repellingTies        = repellingTies;
		NewNode.safeNewFriends       = safeNewFriends;
		NewNode.strengthUpdateRate   = strengthUpdateRate;
		NewNode.switchPressFactor    = switchPressFactor;
		NewNode.switchRandomness     = switchRandomness;
		NewNode.transitiveFriendship = transitiveFriendship;
		NewNode.zealSalience         = zealSalience;

		maxPossibleAgreement = zealSalience + numFixAttributes*fixedAttribSalience + numVarAttributes + maxLinkAgeBonus;
		NewNode.staticParamsLoaded   = true;
	}

	/*
	 * = maximum possible number of triangles containing this node, based on its and its neighbor degrees (ref. Soffer and Vazquez)
	 * neighbDegrees = either the degrees in the full network, or the degrees in the radical subgraph
	 */
	public double maxTriangles(ArrayList <Integer> neighbDegrees){
		//the omega metric of Soffer et al.
		double triangles  = 0.;
		//ArrayList neighbs = getOutNodes();
		int numRadNeighbs    = neighbDegrees.size();

		if (numRadNeighbs < 2) {
			return 0.;
		}
		int[] freeEdges = new int[numRadNeighbs];
		for (int i=0; i<numRadNeighbs; ++i){
			int freeDegree = neighbDegrees.get(i) - 1; //1 for this node.
			freeEdges[i]   = Math.min(numRadNeighbs-1, freeDegree);  //fixme: this could be cached
		}
		Arrays.sort(freeEdges);
		int lastNonZero = 0;
		for (int i=numRadNeighbs-1; i>0; --i) {
			int j = lastNonZero;
			while (freeEdges[i] > 0 && j<i) {
				if (freeEdges[j] > 0) {
					freeEdges[j] --;
					freeEdges[i] --;
					triangles    ++;
				} else {
					lastNonZero = j+1;
				}
				++j;
			}
		}
		if (triangles > numRadNeighbs*(numRadNeighbs-1)/2) {
			//something is badly wrong!
			//double x = 1/0.;
			System.out.println("Incorrect local clustering calculation....");
			return Double.NaN;
		}
		return triangles;
	}
	/*
	 * computes the agreement with a friend based on his attributes and link's age
	 */
	public double netAgreement(NewNode friend, NewEdge link) {		
		//if applyExistingFriendCorrection, then link != null
		double[] friendFixAttributes = friend.getFixAttributes();
		double[] friendVarAttributes = friend.getVarAttributes();

		double agreement = 0;
		agreement   		+= zealSalience*(1 - Math.abs(friend.getZeal()-this.zeal)); //1216792867187, pop=3, std args.
		for (int j=0; j<fixAttributes.length; ++j) {
			agreement 		+= fixAttributes[j]*friendFixAttributes[j]*fixedAttribSalience;
		}
		for (int j=0; j<varAttributes.length; ++j) {
			agreement 		+= varAttributes[j]*friendVarAttributes[j];
		}
		if (link != null) {
			agreement += maxLinkAgeBonus * (link.getAge()) / ((double)link.getAge() + linkAgeBonusFactor);
		}
		//int mutFriends    = getNumMutualFriends(friend); //this cannot be negative unlike the others. maybe do numFriends - (numMutualFriends-1)
		//int maxMutFriends = Math.min(getOutDegree(), friend.getOutDegree()) - 1;
		//existing friend correction should be used when computing the agreement with an existing friend, 
		//and comparing it to a putative new friend (the latter computation should be done w/o the correction
		//this puts biases friendships to keep old friends.
		//if (applyExistingFriendCorrection) {
		//	mutFriends    += 1;
		//}

		//agreement += mutualFriendFactor*mutFriends;
		//return agreement;

		return agreement;
	}
	/*
	 * WARNING: only rudimendary check of acceptability.  no check of degree void or agreement level
	 */
	public boolean newFriendAcceptable(NewNode d) {
		if (d==this) {
			return false;
		} else if (getOutNodes().contains(d)) {
			return false;
		}
		return true;
	}

	public Color recalcColor(){
		//Color c = Color.getHSBColor(0.666f+0.333f*(new Float(zeal).floatValue()), 1.0f, 0.666f+0.333f*(new Float(status)).floatValue());
		Color c = new Color((new Float((1+zeal)/2)).floatValue(), 0.0f, (new Float(1-(1+zeal)/2)).floatValue());
		setColor(c);
		//  	draw();
		return c;
	}
	public void setDegreeCap(int d) {
		d = Math.max(0, d);
		degreeCap = d;
		while(getNumOutEdges() > degreeCap) {
			disconnectWeakestTie();
		}
		if (model.isGui) {
			ProbeUtilities.updateProbePanel(this);
		}
	}
	public void setFixAttributes(double[] e)  throws Exception{
		if(e.length != numFixAttributes) throw new Exception("argument overspecifies or underspecifies attributes");
		fixAttributes = e.clone(); 
		if (model.isGui) {
			ProbeUtilities.updateProbePanel(this);
		}
		fireAttributeUpdate();
	}

	public void setLabel(String s) {
		label = new String(s);
		item.setLabel(s);
		if (model.isGui) {
			ProbeUtilities.updateProbePanel(this);
		}
	}
	public void setPeerPressurability(double val) {
		peerPressurability = val;
	} 
	public void setVarAttributes(double[] e) throws Exception{
		if(e.length != numVarAttributes) throw new Exception("argument overspecifies or underspecifies attributes");
		varAttributes = e.clone();
		if (model.isGui) {
			ProbeUtilities.updateProbePanel(this);
		}
		fireAttributeUpdate();
	}
	public void setZeal(double z) {
		zeal = Math.min(1,Math.max(-1,z));
		recalcColor();
		if (model.isGui) {
			ProbeUtilities.updateProbePanel(this);
		}
		fireAttributeUpdate();
	}

	public void step(){
		applyPeerPressure();
		applyHomophily();
		if (transitiveFriendship > Random.uniform.nextDouble()) {
			connectFriends();
		}
		recalcColor();

		updateTies();
		if (model.isGui) {
			ProbeUtilities.updateProbePanel(this);
		}
	}

	/*
	 * reaps ties whose strength has become too small
	 */
	private void updateTies() {
		final double connectionThreshold = 0.01; 
		ArrayList links = getOutEdges();
		for (int i=0; i<links.size(); ++i) {
			NewEdge link = (NewEdge) links.get(i);
			if ((! repellingTies && link.getStrength()           <= connectionThreshold)
					||(repellingTies && Math.abs(link.getStrength()) <= connectionThreshold)) {
				NewNode friend = (NewNode) link.getTo();
				friend.removeEdgesFrom(this);
				friend.removeEdgesTo(this);
				removeEdgesTo(friend);
				removeEdgesFrom(friend);
				//updateMutualFriends(friend);
			} else {
				link.step();
			}
		}
		if (model.isGui) {
			ProbeUtilities.updateProbePanel(this);
		}
	}
	/*
	 * computes the state of a variable attribute i
	 */
	private void updateAttribute(double p, int attribNum) {
		double curState = varAttributes[attribNum];
		double newState = curState;
		double input;
		if (p > 0) {
			input = peerPressurability/(1+Math.exp(-switchPressFactor*p));
			if (input > 0.5 + (0.5-Random.uniform.nextDouble())*switchRandomness) {
				newState = 1;
			}
		} else {
			input = peerPressurability/(1+Math.exp(-switchPressFactor*(-p)));
			if (input > 0.5 + (0.5-Random.uniform.nextDouble())*switchRandomness) {
				newState = -1;
			}
		}
		varAttributes[attribNum] = newState;
	}

	/*
	 * computes the updated state of zeal
	 */
	private void updateZeal(double p) {
		//note: this algorithm leads to slower rate of change in the zeal than in other attributes, 
		//  when there are > 2 levels
		//note2: perhaps it would be appropriate to reduce the peer pressure applied to moderates.

		double curState = zeal;
		double newState = curState;
		double input;
		if (p > 0) {
			input = peerPressurability/(1 + Math.exp(-switchPressFactor*p));
			if (input > 0.5 + (0.5-Random.uniform.nextDouble())*switchRandomness) {
				newState = Math.min(+1.0, curState + 2.0/(numZealLevels-1));
			}
		} else if (p < 0) {
			input = peerPressurability/(1 + Math.exp(-switchPressFactor*(-p)));
			if (input > 0.5 + (0.5-Random.uniform.nextDouble())*switchRandomness) {
				newState = Math.max(-1.0, curState - 2.0/(numZealLevels-1));
			}
		} else { //p=0
		}
		zeal = newState;
	}
}
