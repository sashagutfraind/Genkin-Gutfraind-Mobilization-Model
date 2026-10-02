/*
 * Copyright (c) 2007-2010, Michael Genkin and Alexander Gutfraind and Cornell University.
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

import uchicago.src.reflector.ListPropertyDescriptor;
import uchicago.src.sim.analysis.Histogram;
import uchicago.src.sim.analysis.NetSequenceGraph;
import uchicago.src.sim.analysis.plot.OpenGraph;
import uchicago.src.sim.analysis.PlotModel;
import uchicago.src.sim.engine.*;
import uchicago.src.sim.gui.*;
//import uchicago.src.sim.network.NetUtilities;
//import uchicago.src.sim.analysis.StatisticUtilities;
import uchicago.src.sim.util.*;
//import uchicago.src.sim.network.NetworkRecorder;
import uchicago.src.sim.parameter.ParameterUtility;
//import uchicago.src.reflector.IntrospectPanel;

import java.io.*;
import java.awt.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.Vector;
import java.text.SimpleDateFormat;
import cern.jet.stat.Descriptive;

/* wishlist fixes
 * 1. pressing X in the control panel in the middle of a simulation causes it to freeze
 * 2. the node probeable properties should show which magnet this node is in (numTotalMagnets < 10)
 * 4. implement burn-in times in Python
 * 5. implement more fine tests of e.g. hopfield functionality
 * 6. test the metrics
 *
 */

import javax.swing.UIManager;

import cern.colt.list.DoubleArrayList;


public class NewModel extends SimModelImpl {
	public static void main(String[] args) {
		final String USAGE = "\nUsage:\n" + 
		"java -jar radicalization.jar [-b batch_parameter file]\n" +
		"e.g. \n" + 
		"java -jar radicalization.jar\n" + 
		"java -jar radicalization.jar -b batch1.pf     (for a batch file that scans a single parameter)\n" +
		"java -jar radicalization.jar -b batch1.pf -NS (for a batch file that runs a single simulation)\n";
		SimInit si     = new SimInit();
		NewModel model = new NewModel();

		//String s = System.getenv(name)
		try {
			if (args.length == 0) {
				try {
					String nativeLF = UIManager.getSystemLookAndFeelClassName();
					UIManager.setLookAndFeel(nativeLF);
				} catch (Exception e) {
					System.out.println("Couldn't load another look and feel.");
				}
				model.buildPropertyDisplay();
				si.loadModel(model, "", false);
			} else if (args[0].equals("-b")) {
				System.out.println("Initializing batch model. Make sure a GUI is still available (repast bug)");
				if (args.length >= 2) {
					String batchFilename = args[1];
					if (args.length >= 3 && args[2].equals("-NS")) {
						model.setSingleParamScan(false);
					}
					else {
						model.setSingleParamScan(true);
					}
					si.loadModel(model, batchFilename, true);
				} else {
					System.out.println("No batch file name specified!");
					System.out.println(USAGE);
					System.exit(1);
				}
			} else if (args[0].equals("-v")) {
				System.out.println(USAGE);
				System.exit(0);
			} else {
				System.out.println("Unrecognized parameters!");
				System.out.println(USAGE);
				System.exit(1);
			}
		} 
		catch (NoClassDefFoundError ex) {
			System.out.println("Simulation terminated abormally. Possible cause: $DISPLAY variable indicates a GUI execution despite batch mode");
			ex.printStackTrace();
			System.exit(1);
		}
		catch (Exception ex) {
			System.out.println("Simulation terminated abormally. Possible cause: lack of memory. Check also batch parameter file, if specified.");
			ex.printStackTrace();
			System.exit(1);
		}

	}

	// model variables
	private int population;
	private ArrayList <NewNode> agentList;
	private int updateEveryN;

	//private int initialSteps;
	private int numZealLevels;
	private double fractionInitPacifists;
	private double fractionInitRadicals;

	private double attritionRate;
	private double radicalsAttritionIncrement;
	private double diversity;

	private int numMagnetsN;
	private int numMagnetsR;
	private int numMagnetsP;
	private double  magneticEncounterRate;
	private double  fractionExposedToMagnets;

	private double  avgInitTieStrength;
	private double  transitiveFriendship;
	private double  avgDegree;
	//private double  nodeDegreeExponent;
	//repellingTies option is mostly supported by the software but ignored in the paper:
	private boolean repellingTies; 
	private double  mutualFriendFactor;
	private double  maxLinkAgeBonus;

	private int     numVarAttributes;
	private int     numFixedAttributes;
	private double  fixedAttribSalience;
	private double  zealSalience;
	private double  avgPressurability;
	private boolean safeNewFriends;

	//second-order parameters
	private double degreeSD;
	private double initTieStrengthSD;
	private double linkAgeBonusFactor;
	private double pressurabilitySD;
	private double strengthUpdateRate;
	private double switchRandomness;
	private double switchPressFactor;

	//statistical summaries
	//1. these should be members of the class rather than locals b/c we want to be able to retrieve them later
	//2. in principle we don't need arrays b/c we could just update avg estimator 
	//		(but not for all metrics exist sufficient scalar statistics)  
	private Hashtable <String, Vector <Double>> runningStats; 
	private Hashtable <String, Double> avgStats;
	private Hashtable <String, Double> initialStats;
	private Hashtable <String, Double> finalStats;

	private PrintStream rawStatsStream;
	private final String[] runtimeStatNames
	= {"R-fraction","R-homophilyPin","R-assortativity","R-avgCellSize","R-avgIndCellSize","R-clusteringSoffer","R-medianCellSize", 
			"R-avgTieStrength","R-E-I_index","R-numCells","R-numIsolatedCells",
			"medianNumRadRadNeighbors",
			"R-rawDyads","R-dyadRatio", 
			"R-clusteringWatts","R-clusteringWattsGlobal",
			"R-rawTriads","R-excessTriads",
			"zealCorr","energy","avgTieStrength","All-clusteringSoffer","All-clusteringWatts",};

	// implementation variables
	//private int nodeCounter;
	private int worldXSize;
	private int worldYSize;
	private String layoutType;
	private DisplaySurface surface;
	private Schedule schedule;
	private AbstractGraphLayout graphLayout;  //description of the visualization.  for the actual display, see Network2DDisplay below
	private Histogram degreeDist;
	//private boolean showHist;  //this feature was apparently never fully implemented
	private NetSequenceGraph graph; //plot of stats, not of the system itself
	private NetSequenceGraph graph2; //plot of stats, not of the system itself

	private boolean showNet;
	private boolean showEnergyPlot;
	private boolean showStatsPlot;
	private long    runLength;
	private String  SimName;
	private String  outputDirectory;
	private int     networkRecordTic;
	private String  lineSep;
	private String  fileSep;
	public  boolean isGui;
	public  boolean singleParamScan = true; //special

	/*
	 * the collection used here (ArrayList vs. TreeSet vs. HashSet) must tradeoff:
	 * 1. rapid insertion (new members join) - occasionally
	 * 3. rapid deletion (old change status or attrite away -> need to check every magnet!)
	 * 2. rapid random access (for pairing -> for each magnet and each member)
	 * 
	 * lookup probably occurs more frequently, so collection should be optimized to it
	 *  => HashSet, with convertion to array for random access
	 * wishlist: profile this 
	 */
	private ArrayList<HashSet<NewNode>> neutralMagnetMemberLists;
	private ArrayList<HashSet<NewNode>> radicalMagnetMemberLists;
	private ArrayList<HashSet<NewNode>> pacifistMagnetMemberLists;


	//private BasicAction initialAction;

	public NewModel() {
	}

	/*
	 * called after play has been pressed: create a sim based on the parameters, and run it.(non-Javadoc)
	 * @see uchicago.src.sim.engine.SimModel#begin()
	 */
	public void begin() {
		try {
			buildModel();
			//recordInitStats();
			initializeStatsCollection();

			buildSchedule();

			if (isGui){
				if (showNet)  {
					buildDisplay();
					sortAgentListForDisplay();
					graphLayout.setList(agentList);
					graphLayout.updateLayout();
					surface.display();
					//surface.setSize(worldXSize, worldXSize);
				}

				//if (showHist) degreeDist.display();
				if (showEnergyPlot) graph. display();
				if (showStatsPlot)  graph2.display();

				IController c = getController();
				((AbstractGUIController)c).makeCurrentParamsDefault();
			}
			//if (! isGui) {
			//listen for batch ended event
			//((BatchController)c).
			//}
		} catch (Exception ex) {
			System.out.println("Simulation terminated abormally while initializing:");
			ex.printStackTrace();
			System.exit(1);
		}
	}
	public void buildDisplay() {
		if (layoutType.equals("KK")) {
			graphLayout = new KamadaGraphLayout(agentList, worldXSize, worldYSize, surface, updateEveryN);
		} else if (layoutType.equals("Fruch")) {
			graphLayout = new FruchGraphLayout (agentList, worldXSize, worldYSize, surface, updateEveryN);
			((FruchGraphLayout)graphLayout).setAnimateTransitions(false);
		} else if (layoutType.equals("Circle")) {
			graphLayout = new CircularGraphLayout(agentList, worldXSize, worldYSize);
		}

		// these four lines hook up the graph layouts to the stop, pause, and
		// exit buttons on the toolbar. When stop, pause, or exit is clicked
		// the graph layouts will interrupt their layout as soon as possible.
		if (isGui) {
			IController c = getController();
			((Controller) c).addStopListener(graphLayout);
			((Controller) c).addPauseListener(graphLayout);
			((Controller) c).addExitListener(graphLayout);
		}
		Network2DDisplay display = new Network2DDisplay(graphLayout);

		if (surface == null) {
			surface = new DisplaySurface(this, "Social Network Display");
			registerDisplaySurface("Society", surface);
		}
		surface.addDisplayableProbeable(display, "Network Display");

		// add the display as a Zoomable. This means we can "zoom" in on
		// various parts of the network.
		surface.addZoomable(display);
		surface.setBackground(java.awt.Color.white);
		addSimEventListener(surface);
	}
	public void buildModel() {
		Random.setSeed(getRngSeed());
		Random.createUniform();
		Random.createNormal(0.0, 1.0);

		neutralMagnetMemberLists  = new ArrayList <HashSet<NewNode>>();
		radicalMagnetMemberLists  = new ArrayList <HashSet<NewNode>>();
		pacifistMagnetMemberLists = new ArrayList <HashSet<NewNode>>();
		for (int i=0; i<numMagnetsN; ++i) {
			neutralMagnetMemberLists.add(new HashSet<NewNode>());
		}
		for (int i=0; i<numMagnetsR; ++i) {
			radicalMagnetMemberLists.add(new HashSet<NewNode>());
		}
		for (int i=0; i<numMagnetsP; ++i) {
			pacifistMagnetMemberLists.add(new HashSet<NewNode>());
		}

		//it is necessary to have a staged network creation (3 loops) in order to ensure that changing parameters such as
		//average degree or number of magnets has the least possible effect on node attributes or links.
		loadAgentStaticParams();
		for (int n = 0; n < population; n++) {
			NewNode node = new NewNode();
			agentList.add(node);
		}
		for (int n = 0; n < population; n++) {
			NewNode node = agentList.get(n);
			createNewNodeEdges(node);
		}
		for (int n = 0; n < population; n++) {
			NewNode node = agentList.get(n);
			listInNeutralMagnets(node);
			if (node.isZealot()) {
				listInRadicalMagnets(node);
			}
			if (node.isPacifist()) {
				listInPacifistMagnets(node);
			}
		}

		//if (showHist)  makeHistogram();
		if (showEnergyPlot)  makePlot();
		if (showStatsPlot) makePlot2();
	}
	//Eclipse IDE, designed to remove warning in descriptors.put()
	@SuppressWarnings("unchecked")
	private void buildPropertyDisplay() {
		Vector <String>vect = new Vector<String>();
		vect.add("Fruch");
		vect.add("KK");
		vect.add("Circle");
		ListPropertyDescriptor pd = new ListPropertyDescriptor("LayoutType", vect);
		descriptors.put("LayoutType", pd);
	}
	private void buildSchedule() {
		//initialAction = schedule.scheduleActionAt(1, this, "initialAction");
		//  removes the first call from the schedule at the end of "initial steps"
		//schedule.scheduleActionAt(initialSteps, this, "removeInitialAction", Schedule.LAST);
		schedule.scheduleActionBeginning(1, this, "mainAction");

		int interval = 1;
		schedule.scheduleActionAtInterval(interval, this, "statsAction");
		schedule.scheduleActionAtEnd(this, "recordSimulationStats");
	}
	public double calcAvgLinkStrength() {
		double sum = 0;
		int    num = 0;
		for (int i=0; i<agentList.size(); ++i) {
			ArrayList links = agentList.get(i).getOutEdges();
			for (int j=0; j<links.size(); ++j) {
				sum += ((NewEdge)links.get(j)).getStrength();
				num ++;
			}
		}
		if (num != 0) 
			return sum/num;
		else
			return 0;
	}
	/*
	 * = information about the connected components of the network
	 */
	public Hashtable <String, Double> calcComponents(boolean radicalSubgraph) {
		Hashtable <String, Double>	ret	= new Hashtable <String, Double>();
		DoubleArrayList compSizes       = new DoubleArrayList();
		TreeSet <NewNode> reachedNodes  = new TreeSet<NewNode>();
		int indCompSizes 						    = 0; //the size of the cell in which individual is sitting
		int isolatedRComponents      		= 0; //for radical run, the components of radicals with connections to non-radicals
		for (Iterator <NewNode>it = agentList.iterator(); it.hasNext(); ) {
			NewNode startNode = it.next();
			if ( (radicalSubgraph && ! startNode.isZealot()) || reachedNodes.contains(startNode)) { 
				continue; 
			}
			TreeSet <NewNode> curComp   = new TreeSet<NewNode>();
			LinkedList <NewNode> fringe = new LinkedList <NewNode>();
			fringe.add(startNode);
			boolean radicalsIsolated = true;
			while(! fringe.isEmpty()) {
				NewNode curNode = fringe.remove();
				if(radicalSubgraph && ! curNode.isZealot()) { 
					radicalsIsolated = false;
					continue; 
				}
				if(curComp.contains(curNode) ) { 
					continue; 
				}
				curComp.add(curNode);
				reachedNodes.add(curNode);
				ArrayList neighbs = curNode.getOutNodes();
				fringe.addAll(neighbs);
			}
			if (radicalSubgraph && radicalsIsolated) {
				isolatedRComponents ++;
			}
			int compSize = curComp.size();
			compSizes.add(compSize);
			indCompSizes += compSize*compSize;
		}
		if(compSizes.size() == 0) {
			ret.put("medianCellSize", 0.0);
			ret.put("avgCellSize", 0.0);
			ret.put("avgIndCellSize", 0.0);
			ret.put("numCells", 0.0);
		} else {
			compSizes.sort(); //median method only accepts sorted arrays!
			ret.put("medianCellSize", Descriptive.median(compSizes));
			ret.put("avgCellSize", Descriptive.mean(compSizes));
			ret.put("numCells", (double)compSizes.size());
			ret.put("avgIndCellSize", indCompSizes/((double)reachedNodes.size()));

		}
		if(radicalSubgraph) {
			ret.put("R-numIsolatedCells", (double)isolatedRComponents);
		}
		return ret;
	}
	public double calcAvgRadicalLinkStrength() {
		double sum = 0;
		int    num = 0;
		for (int i=0; i<agentList.size(); ++i) {
			NewNode node = agentList.get(i);
			if (! node.isZealot()) {
				continue;
			}
			ArrayList links = node.getOutEdges();
			for (int j=0; j<links.size(); ++j) {
				NewEdge edge = (NewEdge)links.get(j);

				//only consider ties to other radicals
				if (! ((NewNode)edge.getTo()).isZealot()) {
					continue;
				}
				sum += edge.getStrength();
				num ++;
			}
		}
		if (num != 0) 
			return sum/num;
		else
			return 0;
	}
	public double calcFractionRadicals() {
		double radicals = 0;
		for (int i=0; i<agentList.size(); ++i) {
			if (agentList.get(i).isZealot()) {
				radicals++;
			}
		}
		if (agentList.size() != 0) {
			return radicals/agentList.size();
		} else {
			return 0;
		}
	}
	/*
	 * = the avg size of the components within the radical subgraph 
	 */
	public double calcRAvgCellSize() {
		return calcComponents(true).get("avgCellSize");
	}
	/*
	 * = the avg size of the components within the radical subgraph 
	 */
	public double calcRIndAvgCellSize() {
		return calcComponents(true).get("avgIndCellSize");
	}
	/*
	 * = the median size of the components within the radical subgraph 
	 */
	public double calcRMedianCellSize() {
		return calcComponents(true).get("medianCellSize");
	}
	public double calcNetworkEnergy() {
		double energy = 0;
		for (int i=0; i<agentList.size(); ++i) {
			energy += agentList.get(i).localEnergy();
		}
		return energy/2;
	}
	public double calcRadicalNeighbZeal() {
		double sum      = 0;
		double radicals = 0;
		for (int i=0; i<agentList.size(); ++i) {
			NewNode node = (NewNode)agentList.get(i);
			if (! node.isZealot()) {
				continue;
			}
			double val  = node.avgNeighbZeal();
			if (! Double.isNaN(val)){
				sum      += val;
				radicals ++;
			} else {
				//do not increment - there are no neighbors
			}
		}
		if (radicals > 0)
			return sum/radicals;
		else
			return 0;
	}
	public double calcRadicalUnweightedClustering() {
		Hashtable <String,Double> radTriangles = calcTriangles(true);
		return radTriangles.get("unweightedClusteringSoffer");
	} 
	public double calcRadicalWeightedClustering() {
		Hashtable <String,Double> radTriangles = calcTriangles(true);
		return radTriangles.get("weightedClusteringSoffer");
	}
	public Hashtable <String, Double> calcRadicalPairsData() {
		double numRadicals  	   = 0.;
		//note that all of the folloowing tie counts are DIRECTED, to ensure fair treament of in-group and outgroup!
		double tiesFromRadicals  = 0.;   
		double tiesRadToRad      = 0.;
		double tiesNonradToNonradRaw= 0.;
		double tiesFromNonradRaw    = 0.;
		double tiesFromRadicalsRaw  = 0.;
		double tiesRadToRadRaw      = 0.;
		ArrayList <Integer> cellSizes    = new ArrayList<Integer>();

		HashMap <String,Double> cellDist = new HashMap<String,Double> ();
		cellDist.put("1", 0.);
		cellDist.put("2", 0.);
		cellDist.put("3", 0.);
		cellDist.put("4", 0.);
		cellDist.put("5", 0.);
		cellDist.put("6to10", 0.);
		cellDist.put("11on", 0.);

		for (int i=0; i<agentList.size(); ++i) {
			NewNode node = (NewNode)agentList.get(i);
			ArrayList myTies = node.getOutEdges();
			if (! node.isZealot()) {
				for (int j=0; j<myTies.size(); ++j) {
					NewNode neighbor = (NewNode) ((NewEdge)myTies.get(j)).getTo(); 			
					tiesFromNonradRaw ++;
					if (! neighbor.isZealot()) {
						tiesNonradToNonradRaw ++;
					}
				}
			} else {
				numRadicals += 1;	
				Integer radCellSize = new Integer(1);
				for (int j=0; j<myTies.size(); ++j) {
					NewEdge edge 		 = (NewEdge)myTies.get(j);
					NewNode neighbor = (NewNode)edge.getTo();

					double strength  = Math.max(0, edge.getStrength());
					tiesFromRadicals += strength;
					tiesFromRadicalsRaw += 1;
					if (! neighbor.isZealot()) {
						continue;
					}
					radCellSize  += 1;
					tiesRadToRadRaw += 1;
					tiesRadToRad += strength;
				}
				cellSizes.add(radCellSize);

				String key;
				if (radCellSize < 6) {
					key = radCellSize.toString();
				} else if (radCellSize < 11) {
					key = "6to10";
				} else {
					key = "11on";
				}
				cellDist.put(key, cellDist.get(key) + 1);
			}
		}
		Hashtable <String, Double> ret = new Hashtable <String, Double>();
		if (numRadicals == 0 || tiesFromRadicals == 0) {
			ret.put("medianNumRadRadNeighbors", new Double(0.));
			ret.put("R-homophilyPin", new Double(0.)); //an unbiased replacement of NaN
			ret.put("R-assortativity", new Double(0.)); //an unbiased replacement of NaN
			ret.put("distribution", Double.NaN); //reporting not implemented
			ret.put("R-rawDyads", new Double(0.));
			ret.put("R-dyadRatio", new Double(0.));
			ret.put("R-E-I_index", new Double(0.));
			return ret;
		}
		double fractionRadicals = numRadicals/agentList.size();
		double homophilyPin = 0.0;
		if (fractionRadicals < 1.0) {
			homophilyPin = new Double( (tiesRadToRad/tiesFromRadicals-fractionRadicals)/(1-fractionRadicals) );
		} 
		double radicalDyadRatio = 0.0;
		if (tiesFromRadicals > 0.0) {
			radicalDyadRatio = tiesRadToRadRaw/tiesFromRadicalsRaw;
		}
		//ret[1] = cellDist;
		double assortativity = 1.0;
		if (tiesFromRadicalsRaw > 0.0 && tiesNonradToNonradRaw > 0.0) {
			double totalTies = tiesFromRadicalsRaw + tiesFromNonradRaw;
			double trace = (tiesRadToRadRaw + tiesNonradToNonradRaw)/totalTies;
			double normSq = Math.pow(tiesFromRadicalsRaw/totalTies, 2.0) + Math.pow(tiesFromNonradRaw/totalTies, 2.0);
			assortativity = (trace - normSq)/(1-normSq);
		}
		ret.put("R-homophilyPin", new Double(homophilyPin));
		ret.put("R-assortativity", new Double(assortativity));
		ret.put("distribution", Double.NaN); //reporting not implemented
		ret.put("R-rawDyads", new Double(tiesRadToRadRaw/2.));
		ret.put("R-dyadRatio", new Double(radicalDyadRatio));

		double internalLinks = tiesRadToRadRaw/2.0; //due to double counting. Krackhardt uses undirected links (cf p.128) 
		double externalLinks = tiesFromRadicalsRaw - tiesRadToRadRaw;
		ret.put("R-E-I_index", new Double((externalLinks-internalLinks)/(externalLinks+internalLinks)));

		DoubleArrayList nbhoodSizes  = new DoubleArrayList();
		nbhoodSizes.addAllOf(cellSizes);
		nbhoodSizes.sort(); //required by median method!
		ret.put("medianNumRadRadNeighbors", new Double(Descriptive.median(nbhoodSizes)-1)); //-1 for the ego node
		return ret;
	}
	public double calcRadicalHomophily() {
		Hashtable <String, Double> pairsData = calcRadicalPairsData();
		return pairsData.get("R-homophilyPin");
	}
	public Hashtable <String, Double> calcTriangles(boolean radicalSubgraph) {
		double actualRawTriads = 0.0;
		double actualTriads = 0.0;
		double possible = 0.0;
		double possibleWattsGlobal = 0.0;  //based on the Watts-Strogatz measure
		double clusteringWatts = 0.0; //original Watts-Strogatz: uses average of local clustering

		int numNodes  = 0;
		for (int i=0; i<agentList.size(); ++i) {
			NewNode node = agentList.get(i);
			if ((radicalSubgraph && ! node.isZealot()) || node.getOutDegree() < 2) {
				continue;
			}
			numNodes += 1;
			ArrayList myNeighbs = node.getOutNodes();
			ArrayList myEdges = node.getOutEdges();
			HashMap <NewNode, Double> myNeighbsSet = new HashMap<NewNode, Double>();
			ArrayList <Integer> neighbDegrees   = new ArrayList<Integer> ();

			for (int j=0; j<myNeighbs.size(); ++j) { //cannot use iterators since we need indices later
				NewNode neighbor = (NewNode) myNeighbs.get(j);
				//only consider ties to other radicals
				if ((radicalSubgraph && ! neighbor.isZealot())) {
					continue;
				}
				myNeighbsSet.put(neighbor, ((NewEdge)myEdges.get(j)).getStrength());
			}
			double myRawTriads = 0.0;
			double myPossibleTriadsWatts = 0.0;
			for (int j=0; j<myNeighbs.size(); ++j) { //cannot use iterators since we need indices later
				NewNode neighbor = (NewNode) myNeighbs.get(j);
				//only consider ties to other radicals
				if ((radicalSubgraph && ! neighbor.isZealot()) || neighbor.getOutDegree() < 2) {
					continue;
				}
				ArrayList hisNeighbs = neighbor.getOutNodes();
				ArrayList hisEdges   = neighbor.getOutEdges();
				int numHisNbs 		 = 0;
				for (int k=0; k<hisNeighbs.size(); ++k) {
					NewNode w = (NewNode) hisNeighbs.get(k);
					//note 1. even though w might be non-radical, it contributes to our estimate of neighbor's degree budget
					//if (radicalSubgraph && ! w.isZealot()) {
					//	continue;
					//}
					//note 2. we do not need to comment-out the check for neighbor itself, since the number of Soffer-possible radical triads around node
					//			  is not effected if we add to node's neighborhood non-radical neighbors or neighbors of degree < 2
					//        (myPossibleTriadsWatts is OK b/c it considers just number of radical neighbors ignoring their degree)
					numHisNbs ++;
					if (myNeighbsSet.containsKey(w)) {
						myRawTriads  += 0.5;  //counted once from j and once from k
						actualTriads += 0.5 * 1/3. * (((NewEdge)myEdges.get(j)).getStrength() 
								+ ((NewEdge)hisEdges.get(k)).getStrength() 
								+ myNeighbsSet.get(w)); //1/3 for mean
					}
				}
				neighbDegrees.add(new Integer(numHisNbs));
			}
			actualRawTriads 			+= myRawTriads;
			possible      				+= node.maxTriangles(neighbDegrees);
			myPossibleTriadsWatts = myNeighbsSet.size()*(myNeighbsSet.size()-1)/2;
			possibleWattsGlobal   += myPossibleTriadsWatts;
			if (myPossibleTriadsWatts > 0) {
				clusteringWatts = (clusteringWatts*(numNodes-1) + myRawTriads/myPossibleTriadsWatts)/numNodes;
			}
		}
		actualRawTriads /= 3.; //all triads have been triple-counted
		actualTriads    /= 3.;
		possible        /= 3.;
		possibleWattsGlobal   /= 3.;
		//notice that we could have found the average over the nodes.
		Hashtable <String, Double> ret = new Hashtable <String, Double>();
		if (possible > 0) {
			ret.put("unweightedClusteringSoffer", new Double(actualRawTriads/possible));
			ret.put("weightedClusteringSoffer", new Double(actualTriads/possible));
			ret.put("rawTriads", new Double(actualRawTriads));

			double expectedTriads = possible/(numNodes*(numNodes-1)/2 ); //numNodes is guaranteed > 1
			ret.put("excessTriads", new Double(actualRawTriads/expectedTriads));
			ret.put("clusteringWatts", new Double(clusteringWatts));  //traditional measure
			ret.put("clusteringWattsGlobal", new Double(actualRawTriads/possibleWattsGlobal)); //a variant
		}	else {
			ret.put("unweightedClusteringSoffer", new Double(0));
			ret.put("weightedClusteringSoffer", new Double(0));
			ret.put("rawTriads", new Double(0));
			ret.put("excessTriads", new Double(0));
			ret.put("clusteringWatts", new Double(0));
			ret.put("clusteringWattsGlobal", new Double(0));
		}
		return ret;
	}
	/*
	 * = correlation coefficient between nodes and the average zeal of their neighbors
	 * (Warning: this metric cannot be computed just for the radical subgraph since then zeals (X) has no variance
	 */
	public double calcZealCorrelation() {
		DoubleArrayList zeals	         = new DoubleArrayList(agentList.size());
		DoubleArrayList neighbZeals	   = new DoubleArrayList(agentList.size());
		int numNodes = 0;
		for (int i=0; i<agentList.size(); ++i) {
			NewNode node = (NewNode)agentList.get(i);
			double nodeZeal   = node.getZeal();
			double nodeNbZeal = node.avgNeighbZeal();
			zeals.add(nodeNbZeal);
			neighbZeals.add(nodeZeal);
			numNodes++;
		}
		return calcZealCorrelationHelper(zeals, neighbZeals, 0.0);
	}
	public static double calcZealCorrelationHelper(DoubleArrayList zeals, DoubleArrayList neighbZeals, double alt) {
		int numNodes      = zeals.size();
		double meanZeal   = Descriptive.mean(zeals);
		double varZeal    = Descriptive.sampleVariance(zeals, meanZeal);
		double sdZeal     = Descriptive.sampleStandardDeviation(numNodes, varZeal);

		double meanNbZeal = Descriptive.mean(neighbZeals);
		double varNbZeal  = Descriptive.sampleVariance(neighbZeals, meanNbZeal);
		double sdNbZeal   = Descriptive.sampleStandardDeviation(numNodes, varNbZeal);

		if (Double.isNaN(varZeal) || Double.isNaN(varNbZeal)) {
			return alt;
		} else {
			return Descriptive.correlation(zeals, sdZeal, neighbZeals, sdNbZeal);
		}
	}

	public double computeAverage(Vector <Double> data) {
		//wishlist: considering adding a parameter for burn-in
		if (data == null || data.size() == 0) {
			return 0;
		}
		double avg = 0;
		for (Iterator <Double>it = data.iterator(); it.hasNext(); ) {
			avg += (it.next()).doubleValue();
		}
		return avg/data.size();
	}
	private void createNewNodeEdges(NewNode node) {
		int goalEdges = node.getDegreeCap();
		int collisions = 0; //we keep track of these, to avoid getting into an infinite loop
		if (agentList.size() < 2) {
			return;
		}
		for (int curEdges = node.getOutDegree(); curEdges < goalEdges && collisions < 100; ) {
			NewNode friend = agentList.get(Random.uniform.nextIntFromTo(0, agentList.size()-1));
			if (! node.exposeToNewAgent(friend, true) ) {
				++collisions;
				//for small graphs, many of the random draws will give nodes to which node is already connected
				//a new edge would not be added, and tryNewFriend will add nothing.
			} else {
				++curEdges;
			}
		} 
		collisions++;
	}

	public void delistFromNeutralMagnets(NewNode node) {
		for (int i = 0; i<numMagnetsN; ++i) {
			neutralMagnetMemberLists.get(i).remove(node);
		}
	}
	public void delistFromRadicalMagnets(NewNode node) {
		for (int i = 0; i<numMagnetsR; ++i) {
			radicalMagnetMemberLists.get(i).remove(node);
		}

	}
	public void delistFromPacifistMagnets(NewNode node) {
		for (int i = 0; i<numMagnetsP; ++i) {
			pacifistMagnetMemberLists.get(i).remove(node);
		}
	}
	public double getAttritionRate() {
		return attritionRate;
	}
	public double getAvgDegree() {
		return avgDegree;
	}
	public double getAvgStat(String statName) throws Exception {
		if (avgStats.containsKey(statName)) {
			return avgStats.get(statName);
		}
		throw new Exception ("Key " + statName + " was not found!");
	}
	public double getDegreeSD() {
		return degreeSD;
	}
	public double getDiversity() {
		return diversity;
	}
	public double getAvgInitTieStrength() {
		return avgInitTieStrength;
	}
	public double getAvgPressurability() {
		return avgPressurability;
	}
	public double getFractionInitPacifists() {
		return fractionInitPacifists;
	}
	public double getFractionInitRadicals() {
		return fractionInitRadicals;
	}
	public double getFixedAttribSalience() {
		return fixedAttribSalience;
	}
	//public boolean getDegreeHist() {
	//  return showHist;
	//}
	public double getFractionExposedToMagnets() {
		return fractionExposedToMagnets;
	}
	public String[] getInitParam() {
		String[] params = {//"worldXSize", "worldYSize",
				"LayoutType",
				//"DegreeHist", 
				"showNet", 
				"showEnergyPlot", 
				"showStatsPlot", 
				"runLength",
				"population",
				//"nodeDegreeExponent",
				"avgDegree",
				"fractionInitRadicals", 
				"fractionInitPacifists",
				"diversity",
				"avgPressurability",
				"numFixedAttributes", "numVarAttributes",
				"fixedAttribSalience", 
				"zealSalience",
				"numMagnetsN", "numMagnetsR", "numMagnetsP",
				"magneticEncounterRate",
				"fractionExposedToMagnets",
				"attritionRate", "radicalsAttritionIncrement",
				"avgInitTieStrength",
				"repellingTies",
				"safeNewFriends", 
				"transitiveFriendship",
				"degreeSD", 
				"initTieStrengthSD",
				//"mutualFriendFactor",
				"linkAgeBonusFactor",
				"maxLinkAgeBonus",
				"numZealLevels",
				"pressurabilitySD", 
				"strengthUpdateRate", 
				"switchRandomness", "switchPressFactor", 
				"SimName",
				"outputDirectory",
		"networkRecordTic"};
		return params;
	}
	public String getLayoutType() {
		return layoutType;
	}
	public double getLinkAgeBonusFactor() {
		return linkAgeBonusFactor;
	}
	public double getMagneticEncounterRate() {
		return magneticEncounterRate;
	}
	public double getMaxLinkAgeBonus() {
		return maxLinkAgeBonus;
	}
	public double getMutualFriendFactor() {
		return mutualFriendFactor;
	}
	/*public double getNodeDegreeExponent() {
  	return nodeDegreeExponent;
  }
	 */
	public String getName() {
		return "REM 3.24.3 - cell size upgrades";
	}
	public int getNetworkRecordTic() {
		return networkRecordTic;
	}
	public int getPopulation() {
		return population;
	}

	public int getNumFixedAttributes() {
		return numFixedAttributes;
	}
	public int getNumZealLevels() {
		return numZealLevels;
	}
	public int getNumMagnetsN() {
		return numMagnetsN;
	}
	public int getNumMagnetsP() {
		return numMagnetsP;
	}
	public int getNumMagnetsR() {
		return numMagnetsR;
	}
	public int getNumVarAttributes() {
		return numVarAttributes;
	}
	public String getOutputDirectory() {
		return outputDirectory;
	}
	public String getSimName() {
		return SimName;
	}
	public boolean getShowNet() {
		return showNet;
	}
	public double getRadicalsAttritionIncrement() {
		return radicalsAttritionIncrement;
	}
	public boolean getShowEnergyPlot() {
		return showEnergyPlot;
	}
	public boolean getShowStatsPlot() {
		return showStatsPlot;
	}
	public boolean getRepellingTies() {
		return repellingTies;
	}
	public long getRunLength() {
		return runLength;
	}
	public double getPressurabilitySD() {
		return pressurabilitySD;
	}
	public boolean getSafeNewFriends() {
		return safeNewFriends;
	}
	public Schedule getSchedule() {
		return schedule;
	}
	public Hashtable<String,Double> getStatsInitial() {
		return initialStats;
	}
	public Hashtable<String,Double> getStatsAvg() {
		return avgStats;
	}
	public Hashtable<String,Double> getStatsFinal() {
		return finalStats;
	}
	public double getStrengthUpdateRate() {
		return strengthUpdateRate;
	}
	public double getSwitchRandomness() {
		return switchRandomness;
	}
	public double getSwitchPressFactor() {
		return switchPressFactor;
	}
	public double getInitTieStrengthSD() {
		return initTieStrengthSD;
	}
	public double getTransitiveFriendship() {
		return transitiveFriendship;
	}
	public int getUpdateEveryN() {
		return updateEveryN;
	}
	public double getZealSalience() {
		return zealSalience;
	}
	/*
  public void initialAction() {
  	if (showNet)  {
  		sortAgentListForDisplay();
  		graphLayout.updateLayout();
      surface.updateDisplay();
  	}
  	//if (showHist) degreeDist.step();
    if (showEnergyPlot) graph. step();
    if (showStatsPlot) graph2.step();
  }
	 */

	private void initializeStatsCollection() {
		String rawStatsFileDir = "";
		String rawStatsFileName = "";
		try {
			//ParameterUtility.createInstance();
			Hashtable params = ParameterUtility.getInstance().getModelProperties(this);
			ArrayList dynamicParams = ParameterUtility.getInstance().getDynamicParameterNames();
			dynamicParams.remove("rngseed"); //if present
			if(! isGui && singleParamScan) {
				if (dynamicParams.size() != 1) {
					System.out.println("WARNING: running in single parameter scan mode, but:");
					System.out.println("Multiple test parameters are not supported by data analysis software!");
					System.out.println("(did you forget to append the -NS option?)");
					System.out.println("Attempting to scan just the first parameter...");
				}
				String currentParamName  = (String) dynamicParams.get(0);
				String currentParamValue;
				for (Enumeration keys=params.keys(); keys.hasMoreElements(); ) {
					String name = (String)keys.nextElement();
					if (name.compareToIgnoreCase(currentParamName) == 0) {
						currentParamName = name;
						break;
					}
				}
				currentParamValue = params.get(currentParamName).toString();
				rawStatsFileDir = outputDirectory+fileSep+
				currentParamName+"="+currentParamValue+fileSep;
			} else {
				rawStatsFileDir = outputDirectory+fileSep;
			}
			File file = new java.io.File(rawStatsFileDir);
			if (! file.exists()) {
				file.mkdirs();
			}
			SimpleDateFormat formatter = new SimpleDateFormat ("yyyy.MM.dd--hh.mm.ss");
			Date currentTime = new Date();
			rawStatsFileName = rawStatsFileDir+formatter.format(currentTime)+".raw.csv";
			rawStatsStream = new PrintStream(rawStatsFileName); 
			//}

			//line 1: the name of the simulation
			rawStatsStream.printf("%s,,,,"+lineSep, SimName);

			//line 2&3: param names and values
			String paramNames = new String();
			String paramVals  = new String();
			for (Enumeration keys=params.keys(); keys.hasMoreElements(); ) {
				String paramName = (String)keys.nextElement();
				paramNames += paramName + ",";
				paramVals  += params.get(paramName) + ",";
			}
			paramNames += "randomSeed,"+lineSep;
			paramVals  += getRngSeed() +","+lineSep;

			rawStatsStream.printf(paramNames);
			rawStatsStream.printf(paramVals);

			//line 4: blank
			rawStatsStream.printf(lineSep);

			//line 5: header of the stats
			for (int i=0; i<runtimeStatNames.length; ++i) {
				rawStatsStream.printf("%s,",runtimeStatNames[i]);
			}
			rawStatsStream.printf(lineSep);

		} catch (Exception ex) {
			System.out.println("Failed to initialize output streams:");
			System.out.println("Raw output dir: " + rawStatsFileDir);
			System.out.println("Raw output filename: " + rawStatsFileName);
			ex.printStackTrace();
			System.exit(42);    	
			//rawStatsStream.close();
		} 

		runningStats = new Hashtable<String, Vector<Double>>();
		for (int i=0; i<runtimeStatNames.length; ++i) {
			runningStats.put(runtimeStatNames[i], new Vector<Double>());
		}
		for (Iterator it = runningStats.values().iterator(); it.hasNext(); ) {
			Vector<Double> ar = (Vector<Double>) it.next();
			ar.ensureCapacity((int)runLength+1);
		}
	}

	public void listInNeutralMagnets(NewNode node) {
		for (int i = 0; i<numMagnetsN; ++i) {
			if (Random.uniform.nextDouble() < fractionExposedToMagnets) {
				neutralMagnetMemberLists.get(i).add(node);
			}
		}
	}
	public void listInRadicalMagnets(NewNode node) {
		for (int i = 0; i<numMagnetsR; ++i) {
			if (node.isZealot() && Random.uniform.nextDouble() < fractionExposedToMagnets) {
				radicalMagnetMemberLists.get(i).add(node);
			}
		}

	}
	public void listInPacifistMagnets(NewNode node) {
		for (int i = 0; i<numMagnetsP; ++i) {
			if (node.isPacifist() && Random.uniform.nextDouble() < fractionExposedToMagnets) {
				pacifistMagnetMemberLists.get(i).add(node);
			}
		}
	}

	/*
	 * updates the static fields of the NewNode class
	 */
	private void loadAgentStaticParams() {  	
		NewNode.loadStaticParams(
				avgDegree, 
				avgInitTieStrength, 
				avgPressurability,  
				degreeSD,
				diversity, 
				fixedAttribSalience, 
				fractionInitPacifists, 
				fractionInitRadicals,
				initTieStrengthSD,
				linkAgeBonusFactor,
				maxLinkAgeBonus, 
				this, 
				mutualFriendFactor, 
				numZealLevels, 
				numFixedAttributes, 
				numVarAttributes, 
				pressurabilitySD, 
				repellingTies, 
				safeNewFriends, 
				strengthUpdateRate, 
				switchRandomness, 
				switchPressFactor, 
				transitiveFriendship, 
				zealSalience);
	}

	public void mainAction() {
		try {
			if (getTickCount() >= runLength) {
				super.stop();
				return;
			}
			int toll = reapNodes();

			for (int j=0; j < toll; ++j) {
				NewNode node = new NewNode();
				createNewNodeEdges(node);
				agentList.add(node);
				listInNeutralMagnets(node);
				if (node.isZealot()) {
					listInRadicalMagnets(node);
				}
				if (node.isPacifist()) {
					listInPacifistMagnets(node);
				}
			}

			magnetizeNewFriends();

			for (int i=0; i<agentList.size(); ++i) {
				((NewNode)agentList.get(i)).step();
			}

			if (showNet && isGui) {
				sortAgentListForDisplay();	    	

				graphLayout.setList(agentList);
				graphLayout.updateLayout();

				surface.updateDisplay();
			}
		} catch (Exception ex) {
			System.out.println("Simulation terminated abormally during state update:");
			ex.printStackTrace();
			System.exit(1);
		}
	}
	/*
	 * form new ties through magnets
	 *  HashSet implementation
	 */
	private void magnetizeNewFriends() {
		for (int i = 0; i < neutralMagnetMemberLists.size(); i++) {
			Object[] magnetMembers = neutralMagnetMemberLists.get(i).toArray();
			for (int numOfNodeA=0; numOfNodeA<magnetMembers.length; ++numOfNodeA) {
				NewNode nodeA = (NewNode)magnetMembers[numOfNodeA];
				if (Random.uniform.nextDouble() > magneticEncounterRate) {
					continue;
				}
				int numOfNodeB = Random.uniform.nextIntFromTo(0, magnetMembers.length-1);
				nodeA.exposeToNewAgent((NewNode)magnetMembers[numOfNodeB], safeNewFriends);
			}
		}
		for (int i = 0; i < radicalMagnetMemberLists.size(); i++) {
			Object[] magnetMembers = radicalMagnetMemberLists.get(i).toArray();
			for (int numOfNodeA=0; numOfNodeA<magnetMembers.length; ++numOfNodeA) {
				NewNode nodeA = (NewNode)magnetMembers[numOfNodeA];
				if (Random.uniform.nextDouble() > magneticEncounterRate) {
					continue;
				}
				int numOfNodeB = Random.uniform.nextIntFromTo(0, magnetMembers.length-1);
				nodeA.exposeToNewAgent((NewNode)magnetMembers[numOfNodeB], safeNewFriends);
			}
		}
		for (int i = 0; i < pacifistMagnetMemberLists.size(); i++) {
			Object[] magnetMembers = pacifistMagnetMemberLists.get(i).toArray();
			for (int numOfNodeA=0; numOfNodeA<magnetMembers.length; ++numOfNodeA) {
				NewNode nodeA = (NewNode)magnetMembers[numOfNodeA];
				if (Random.uniform.nextDouble() > magneticEncounterRate) {
					continue;
				}
				int numOfNodeB = Random.uniform.nextIntFromTo(0, magnetMembers.length-1);
				nodeA.exposeToNewAgent((NewNode)magnetMembers[numOfNodeB], safeNewFriends);
			}
		}
	}
	/*
	 * form new ties through magnets
	 * 	ArrayList implementation
	 * 
  private void magnetizeNewFriends() {
  	for (int i = 0; i < neutralMagnetMemberLists.size(); i++) {
  		ArrayList <NewNode> magnet = neutralMagnetMemberLists.get(i);
    	for (Iterator <NewNode>itA = magnet.iterator(); itA.hasNext(); ) {
    		NewNode nodeA = itA.next();
    		if (Random.uniform.nextDouble() > magneticEncounterRate) {
      		continue;
      	}
        int numOfNodeB = Random.uniform.nextIntFromTo(0, magnet.size()-1);
    		nodeA.exposeToNewAgent(magnet.get(numOfNodeB), safeNewFriends);
    	}
    }
  	for (int i = 0; i < radicalMagnetMemberLists.size(); i++) {
  		ArrayList <NewNode> magnet = radicalMagnetMemberLists.get(i);
    	for (Iterator <NewNode>itA = magnet.iterator(); itA.hasNext(); ) {
    		NewNode nodeA = itA.next();
    		if (Random.uniform.nextDouble() > magneticEncounterRate) {
      		continue;
      	}
        int numOfNodeB = Random.uniform.nextIntFromTo(0, magnet.size()-1);
    		nodeA.exposeToNewAgent(magnet.get(numOfNodeB), safeNewFriends);
    	}
    }
  	for (int i = 0; i < pacifistMagnetMemberLists.size(); i++) {
  		ArrayList <NewNode> magnet = pacifistMagnetMemberLists.get(i);
    	for (Iterator <NewNode>itA = magnet.iterator(); itA.hasNext(); ) {
    		NewNode nodeA = itA.next();
    		if (Random.uniform.nextDouble() > magneticEncounterRate) {
      		continue;
      	}
        int numOfNodeB = Random.uniform.nextIntFromTo(0, magnet.size()-1);
    		nodeA.exposeToNewAgent(magnet.get(numOfNodeB), safeNewFriends);
    	}
    }
	}
	 */

	/*
  private void makeHistogram() {
    degreeDist = new Histogram("Degree Distribution", 10, 0, 10, this);
    degreeDist.createHistogramItem("Degree Distribution", agentList, "getOutDegree");
  }
	 */
	private void makePlot() {
		//this undershoots the actual min due to the existence of fixed traits.
		//it is hard to find the exact number b/c it depends on the fraction of pop with a given fixed allele.
		int avgDegree = 5;
		double minEnergy = -0.5 * avgDegree * population * (numVarAttributes + numFixedAttributes + 1);

		graph = new NetSequenceGraph("Energy Stats", this, outputDirectory+fileSep+"netEnergy.txt", PlotModel.CSV, agentList);
		graph.setAxisTitles("Time", "Statistic Value");
		graph.setXRange(0, 50);
		graph.setYRange(minEnergy/3, 1); 
		graph.setLocation(600, 570);
		graph.createSequence("Energy", Color.black,	OpenGraph.FILLED_CIRCLE, this, "calcNetworkEnergy");
	}
	private void makePlot2() {
		graph2 = new NetSequenceGraph("Network Stats", this, outputDirectory+fileSep+"netStats.txt", PlotModel.CSV, agentList);
		graph2.setAxisTitles("Time", "Statistic Value");
		graph2.setXRange(0, 50);
		graph2.setYRange(-1.2, 1.2);
		graph2.setLocation(600, 80);
		//debug: schedule call to isMultiplexed() every 20 ticks, to make sure the graph is OK.

		graph2.createSequence("R-fraction", 	  		Color.orange, OpenGraph.FILLED_CIRCLE, this, "calcFractionRadicals");
		graph2.createSequence("R-homophilyPin",			Color.blue, 	OpenGraph.FILLED_CIRCLE, this, "calcRadicalHomophily");
		graph2.createSequence("R-clusteringSoffer",	Color.red,	  OpenGraph.FILLED_CIRCLE, this, "calcRadicalUnweightedClustering");
		graph2.createSequence("R-MedCellSize",			Color.black,  OpenGraph.FILLED_CIRCLE, this, "calcRMedianCellSize");

		//graph2.createSequence("AvgRadTieStrength",Color.cyan, 		OpenGraph.FILLED_CIRCLE, this, "calcAvgRadicalLinkStrength");
		//graph2.createSequence("AvgTieStrength", 	Color.gray,	 	OpenGraph.SEQUENCE, this, "calcAvgLinkStrength");

	}

	private int reapNodes() {
		ArrayList <NewNode> removedNodes = new ArrayList<NewNode>();
		for (int i=0; i<agentList.size(); ++i){
			NewNode node = agentList.get(i); 
			double fate = Random.uniform.nextDouble();
			boolean zealot = node.isZealot();
			if (   (! zealot && fate < attritionRate)
					|| ( zealot && fate < attritionRate + radicalsAttritionIncrement)){
				removedNodes.add(node);
				ArrayList neighbs = node.getOutNodes();
				for (int j=0; j<neighbs.size(); ++j) {
					NewNode neighb = (NewNode) neighbs.get(j);
					neighb.removeEdgesFrom(node);
					neighb.removeEdgesTo(node);
				}
				node.clearInEdges();
				node.clearOutEdges();

				delistFromNeutralMagnets(node);
				delistFromRadicalMagnets(node);
				delistFromPacifistMagnets(node);
			}
		}
		agentList.removeAll(removedNodes);
		return removedNodes.size();
	}

	/*
  public void recordInitStats() throws Exception {
  	initialStats = new Hashtable<String, Double>();
	  for (int i=0; i<runtimeStatNames.length; ++i) {
	  	initialStats.put(runtimeStatNames[i], Double.MAX_VALUE); //error trap
	  }
	  initialStats.put("zealCorr", calcZealCorrelation(false));
	  initialStats.put("energy", calcNetworkEnergy());
	  initialStats.put("avgTieStrength", calcAvgLinkStrength());
	  initialStats.put("R-avgTieStrength", calcAvgRadicalLinkStrength());
	  initialStats.put("R-fraction", calcFractionRadicals());

    Hashtable <String,Double> pairsData = calcRadicalPairsData();
    initialStats.put("R-homophilyPin", pairsData.get("R-homophilyPin")); 
    initialStats.put("R-assortativity", pairsData.get("R-assortativity")); 
		//wishlist: reporting the distribution
    initialStats.put("medianNumRadRadNeighbors", pairsData.get("medianNumRadRadNeighbors"));
    initialStats.put("R-rawDyads", pairsData.get("R-rawDyads"));
    initialStats.put("R-dyadRatio", pairsData.get("R-dyadRatio"));
    initialStats.put("R-E-I_index", pairsData.get("R-E-I_index"));

		Hashtable <String,Double> radicalTriangles = calcTriangles(true);
		initialStats.put("R-weightedClusteringSoffer", radicalTriangles.get("weightedClusteringSoffer"));
		initialStats.put("R-clusteringSoffer", radicalTriangles.get("unweightedClusteringSoffer"));
		initialStats.put("R-rawTriads", radicalTriangles.get("rawTriads"));
		initialStats.put("R-excessTriads", radicalTriangles.get("excessTriads"));
		initialStats.put("R-clusteringWatts", radicalTriangles.get("clusteringWatts"));
		initialStats.put("R-clusteringWattsGlobal", radicalTriangles.get("clusteringWattsGlobal"));

  	Hashtable <String,Double> triangles = calcTriangles(false);
  	initialStats.put("All-clusteringSoffer", triangles.get("unweightedClusteringSoffer"));
  	initialStats.put("All-clusteringWatts", triangles.get("clusteringWatts"));

  	initialStats.put("R-medianCellSize", calcRMedianCellSize());
  	initialStats.put("R-avgCellSize", calcRAvgCellSize());
  	initialStats.put("R-avgIndCellSize", calcRAvgIndCellSize());

	  for (int i=0; i<runtimeStatNames.length; ++i) {
	  	if(initialStats.get(runtimeStatNames[i]) == Double.MAX_VALUE) {
	  		throw new Exception("Programming error: runtime statistic "+ runtimeStatNames[i] + " is not computed!");
	  	}
	  }
  }
	 */

	/*
	 * Records the network described by the Nodes in nodeList. 
	 *   Repast's own recorder is an option
	 * @param n/a
	 */
	public void recordNetwork() {
		SimpleDateFormat formatter = new SimpleDateFormat ("yyyy.MM.dd--hh.mm.ss");
		Date currentTime = new Date();
		String networkFileName   = outputDirectory+fileSep+"full_network_"+
		formatter.format(currentTime)+".DL";
		String outString = "";
		outString += "labels embedded" + lineSep;
		//netStream.println("labels:");

		outString += "data:" + lineSep;
		for (int i=0; i<agentList.size(); ++i) {
			NewNode node = agentList.get(i);
			ArrayList links = agentList.get(i).getOutEdges();
			for (int j=0; j<links.size(); ++j) {
				NewNode neighb = (NewNode)((NewEdge)links.get(j)).getTo();
				outString += node.getTypeLabel() + node.getLabel() + " " +
				neighb.getTypeLabel() +  neighb.getLabel() + " " +
				((NewEdge)links.get(j)).getStrength() + lineSep;
			}
			if (links.size() == 0) { //print node by itself to stop UCInet complaining
				outString += node.getLabel() + "r" + lineSep;
			}
		}
		outString = "dl nr="+agentList.size()+", nc="+agentList.size()+" format = edgelist" + lineSep
		+ outString;
		try {
			PrintStream netStream    = new PrintStream(networkFileName); 

			netStream.println(outString);
			if (netStream != null) {
				netStream.flush();
				netStream.close();
			}
		} catch (Exception ex){
			System.out.println("Error opening file to record network:");
			ex.printStackTrace();
		}
	}

	/*
	 * Records the network described by the Nodes in nodeList. 
	 * Only the radicals and their radical neighbors are reported.
	 * Currently not called
	 * 
	 * @param n/a
	 */
	public void recordNetworkRadicals () {
		SimpleDateFormat formatter = new SimpleDateFormat ("yyyy.MM.dd--hh.mm.ss");
		Date currentTime = new Date();
		String networkFileName   = outputDirectory+fileSep+"radicals_network_"+
		formatter.format(currentTime)+".DL";
		String outString = "";
		outString += "labels embedded" + lineSep;
		//netStream.println("labels:");
		int numRadicals = 0;
		//int numRadRadLinks = 0;
		for (int i=0; i<agentList.size(); ++i) {
			NewNode node = agentList.get(i);
			if (node.isZealot()) {
				numRadicals += 1;
				//netStream.print(node.getLabel() + "r,");  //"r" for radical
			}
		}
		//netStream.println();

		outString += "data:" + lineSep;
		for (int i=0; i<agentList.size(); ++i) {
			NewNode node = agentList.get(i);
			if(! node.isZealot()) {
				continue;
			}
			ArrayList links = agentList.get(i).getOutEdges();
			int numRadNbs = 0;
			for (int j=0; j<links.size(); ++j) {
				NewNode neighb = (NewNode)((NewEdge)links.get(j)).getTo();
				if(! neighb.isZealot()) {
					continue;
				}
				numRadNbs += 1;
				//numRadRadLinks += 1;
				outString += node.getLabel() + "r " 
				+ neighb.getLabel() + "r " 
				+ ((NewEdge)links.get(j)).getStrength() + lineSep;
			}
			if (numRadNbs == 0) { //print node by itself to stop UCInet complaining
				outString += node.getLabel() + "r" + lineSep;
			}
		}
		outString = "dl nr="+numRadicals+", nc="+numRadicals+" format = edgelist" + lineSep
		+ outString;
		try {
			PrintStream netStream    = new PrintStream(networkFileName); 

			netStream.println(outString);
			if (netStream != null) {
				netStream.flush();
				netStream.close();
			}
		} catch (Exception ex){
			System.out.println("Error opening file to record network:");
			ex.printStackTrace();
		}
	}

	public void recordSimulationStats() {
		try {
			//rather than selecting some metrics, it might be better to automatically report all init,avg & final
			initialStats = new Hashtable <String, Double>();
			for (int i=0; i<runtimeStatNames.length; ++i) {
				initialStats.put(runtimeStatNames[i], runningStats.get(runtimeStatNames[i]).firstElement());
			}

			finalStats = new Hashtable <String, Double>();
			for (int i=0; i<runtimeStatNames.length; ++i) {
				finalStats.put(runtimeStatNames[i], runningStats.get(runtimeStatNames[i]).lastElement());
			}

			//the AVG refers to time-average (the underlying quantities are network-averages)
			//ignoring burn-in
			avgStats = new Hashtable <String, Double>();
			for (int i=0; i<runtimeStatNames.length; ++i) {
				avgStats.put(runtimeStatNames[i], computeAverage(runningStats.get(runtimeStatNames[i])));
			}

			PrintStream outStream = null;
			if (outStream == null) {
				SimpleDateFormat formatter = new SimpleDateFormat ("yyyy.MM.dd--hh.mm.ss");
				Date currentTime = new Date();
				outStream = new PrintStream(outputDirectory+fileSep+formatter.format(currentTime)+".csv"); 
			}

			reportSummaryToStream(System.out);
			System.out.printf(lineSep);

			//write run summaries
			//note: there may be better ways of producing output, and separating variable parameters from fixed parameters.
			//I have such code elsewhere
			//ParameterUtility.createInstance(); <- done by the controller

			//
			//beginning of trouble code: somehow, when running in Eclipse this code leads to freeze, intermittently
			//	when clicking X in the middle of simulation
			Hashtable params = ParameterUtility.getInstance().getModelProperties(this);
			IController c = getController();
			if (c.getRunCount()==1) {
				outStream.printf("%s,,,,"+lineSep, SimName);
				for (Enumeration keys=params.keys(); keys.hasMoreElements(); ) {
					outStream.printf("%s,", keys.nextElement());
				}
			}
			for (Enumeration keys=params.keys(); keys.hasMoreElements(); ) {
				outStream.printf("%s,", params.get(keys.nextElement()));
			}
			//reportSummaryToStream(outStream);
			if(networkRecordTic==-1) {
				recordNetwork();
			}
			//end of trouble code
			//

			if (rawStatsStream != null) {
				rawStatsStream.flush();
				rawStatsStream.close();
			}
			if (outStream != null) {
				outStream.flush();
				outStream.close();
			}
		} catch (Exception ex) {
			System.out.printf("Cannot produce output due to an error:"+lineSep);
			ex.printStackTrace();
		}
	}

	private void reportSummaryToStream(PrintStream stream) {
		stream.printf("%s: %d," + lineSep, "seed", getRngSeed());
		stream.printf("\t Init\t\t AVG(fullTime)\t\t Final" + lineSep);
		for (int i=0; i<runtimeStatNames.length; ++i) {
			String statName = runtimeStatNames[i];
			stream.printf("%s: %f\t%f\t%f" + lineSep, statName, 
					initialStats.get(statName), avgStats.get(statName), finalStats.get(statName));
		}
	}
	/*
  public void removeInitialAction() {
    schedule.removeAction(initialAction);
  }
	 */

	/*
	 * external setup of agent list - used for testing
	 */
	public void setAgentList(ArrayList <NewNode> val) {
		agentList = val;
	}
	public void setAttritionRate(double val) {
		val = Math.max(val, 0.0);
		val = Math.min(val, 1.0);
		attritionRate = val;
		if (radicalsAttritionIncrement + attritionRate > 1.0) {
			setRadicalsAttritionIncrement(1.0-attritionRate);
		}
		updatePanelIfGui();
	}

	public void setAvgDegree(double val) {
		val = Math.max(0, val);
		avgDegree = val;
		updatePanelIfGui();
		loadAgentStaticParams();
	}
	public void setAvgInitTieStrength(double val) {
		avgInitTieStrength = val;
		updatePanelIfGui();
		loadAgentStaticParams();
	}
	public void setDegreeSD(double val) {
		val = Math.max(0, val);
		degreeSD = val;
		updatePanelIfGui();
		loadAgentStaticParams(); 
	}
	public void setDiversity(double val) {
		val = Math.max(0, val);
		diversity = val;
		updatePanelIfGui();
		loadAgentStaticParams();
	}
	public void setFractionInitPacifists(double val) {
		val = Math.min(1.0, val);
		val = Math.max(0.0, val);
		fractionInitPacifists = val;

		//reset the radicals
		if ((fractionInitPacifists + fractionInitRadicals > 1.0) || (numZealLevels % 2 == 0)) {
			fractionInitRadicals = (1 - fractionInitPacifists);
		}
		updatePanelIfGui();
		loadAgentStaticParams();
	}
	public void setFractionInitRadicals(double val) {
		val = Math.min(1.0, val);
		val = Math.max(0.0, val);
		fractionInitRadicals = val;

		//reset the pacifists
		if ((fractionInitPacifists + fractionInitRadicals > 1.0) || (numZealLevels % 2 == 0)) {
			fractionInitPacifists = (1 - fractionInitRadicals);
		}
		updatePanelIfGui();
		loadAgentStaticParams();
	}

	//public void setDegreeHist(boolean val) {
	//  showHist = val;
	//}

	public void setFractionExposedToMagnets(double val) {
		val = Math.max(0.0, val);
		fractionExposedToMagnets = val;
		updatePanelIfGui();
	}
	public void setFixedAttribSalience(double val) {
		val = Math.min(10.0, val);
		val = Math.max(0.0, val);
		fixedAttribSalience = val;
		loadAgentStaticParams();
	}
	public void setInitTieStrengthSD(double val) {
		val = Math.max(0, val);
		val = Math.min(10.0, val);
		initTieStrengthSD = val;
		updatePanelIfGui();
		loadAgentStaticParams();
	}
	public void setLayoutType(String type) {
		layoutType = type;
		updatePanelIfGui();
	}
	public void setLinkAgeBonusFactor(double val) {
		val = Math.max(0.0, val);
		linkAgeBonusFactor = val;
		loadAgentStaticParams();
	}
	public void setMagneticEncounterRate(double val) {
		val = Math.max(0.0, val);
		magneticEncounterRate = val;
		updatePanelIfGui();
	}
	public void setMaxLinkAgeBonus(double val) {
		val = Math.max(0.0, val);
		maxLinkAgeBonus = val;
		updatePanelIfGui();
		loadAgentStaticParams();
	}

	public void setMutualFriendFactor(double val) {
		val = Math.max(0, val);
		mutualFriendFactor = val;
		updatePanelIfGui();
		loadAgentStaticParams();
	}
	public void setNetworkRecordTic(int val) {
		val = Math.max(-10, val);
		networkRecordTic = val;
		updatePanelIfGui();
	}
	public void setPopulation(int val) {
		val = Math.max(1, val);
		population = val;
		updatePanelIfGui();
	}
	public void setNumFixedAttributes(int val) {
		numFixedAttributes = val;
		numFixedAttributes = Math.max(0, numFixedAttributes);
		updatePanelIfGui();
		loadAgentStaticParams();
	}

	public void setNumZealLevels(int n) {
		n = Math.max(2, n);
		numZealLevels = n;
		if (numZealLevels % 2 == 0 && isGui) {
			fractionInitPacifists = (1 - fractionInitRadicals);
			SimUtilities.showMessage(
					"When numZealLevels is even, true moderates with zeal=0 cannot be represented.  The fraction of pacifists has been changed to 1 - fraction of radicals.");
		}
		updatePanelIfGui();
		loadAgentStaticParams();
	}

	public void setNumMagnetsN(int val) {
		val = Math.max(0, val);
		numMagnetsN = val;
		updatePanelIfGui();
	}

	public void setNumMagnetsP(int val) {
		val = Math.max(0, val);
		numMagnetsP = val;
		updatePanelIfGui();
	}

	public void setNumMagnetsR(int val) {
		val = Math.max(0, val);
		numMagnetsR = val;
		updatePanelIfGui();
	}
	public void setNumVarAttributes(int val) {
		numVarAttributes = val;
		numVarAttributes = Math.max(0, numVarAttributes);
		updatePanelIfGui();
		loadAgentStaticParams();
	}

	public void setSimName(String s) {
		SimName = s;
		updatePanelIfGui();
	}
	public void setOutputDirectory(String s) {
		outputDirectory = s;
		File outDir = new File(outputDirectory);
		if (! outDir.exists()) {
			outDir.mkdirs();
		}
		updatePanelIfGui();
	}
	public void setAvgPressurability(double val) {
		val = Math.min(1.0, val);
		//val = Math.max(0.0, val); 
		//we allow negative number so as to make it possible to disable pressure despite pressurabilitySD
		avgPressurability = val;
		updatePanelIfGui();
		loadAgentStaticParams();
	}

	public void setShowNet(boolean val) {
		showNet = val;
		updatePanelIfGui();
	}

	public void setShowEnergyPlot(boolean val) {
		showEnergyPlot = val;
		updatePanelIfGui();
	}

	public void setShowStatsPlot(boolean val) {
		showStatsPlot = val;
		updatePanelIfGui();
	}
	public void setSingleParamScan(boolean val) {
		singleParamScan = val;
	}
	public void setPressurabilitySD(double val) {
		val = Math.max(0.0, val);
		pressurabilitySD = val;
		updatePanelIfGui();
		loadAgentStaticParams();
	}
	public void setRepellingTies(boolean val) {
		repellingTies = val;
		if (! repellingTies && avgInitTieStrength < 0 && isGui) {
			SimUtilities.showMessage(
					"Warning: RepellingTies are disabled, but avgInitTieStrength was set to negative values. " +
			"A lot of ties will be disconnected on first tick.");
		}
		updatePanelIfGui();
		loadAgentStaticParams();
	}
	public void setRadicalsAttritionIncrement(double val) {
		val = Math.max(val, -1.0);
		val = Math.min(val,  1.0);
		radicalsAttritionIncrement = val;
		if (radicalsAttritionIncrement + attritionRate > 1.0){
			radicalsAttritionIncrement = (1.0 - attritionRate);
		}
		updatePanelIfGui();
	}
	public void setRunLength(long val) {
		val = Math.max(val, 1);
		runLength = (long)val; //do we need this cast?
		updatePanelIfGui();
	}

	public void setSafeNewFriends(boolean val) {
		safeNewFriends = val;
		updatePanelIfGui();
		loadAgentStaticParams();
	}

	public void setStrengthUpdateRate(double val) {
		val = Math.max(0, val);
		val = Math.min(1.0, val);
		strengthUpdateRate = val;
		updatePanelIfGui();
		loadAgentStaticParams();
	}
	public void setSwitchRandomness(double val) {
		val = Math.max(0, val);
		val = Math.min(1.0, val);
		switchRandomness = val;
		updatePanelIfGui();
		loadAgentStaticParams();
	}
	public void setSwitchPressFactor(double val) {
		val = Math.max(0, val);
		val = Math.min(1000, val);
		switchPressFactor = val;
		updatePanelIfGui();
		loadAgentStaticParams();
	}
	public void setTransitiveFriendship(double val) {
		val = Math.max(0, val);
		val = Math.min(1, val);
		transitiveFriendship = val;
		updatePanelIfGui();
		loadAgentStaticParams();
	}
	public void setZealSalience(double val) {
		val = Math.min(10.0, val);
		val = Math.max(0.0, val);
		zealSalience = val;
		loadAgentStaticParams();
	}
	//called when loading the model
	public void setup() {
		if (surface != null) surface.dispose();
		if (degreeDist != null) degreeDist.dispose();
		if (graph != null)  graph. dispose();
		if (graph2!= null)  graph2.dispose();

		surface = null;
		schedule = null;
		degreeDist = null;
		graph  = null;
		graph2 = null;

		System.gc();    //garbage collection!

		if (showNet) {
			surface = new DisplaySurface(this, "Social Network Display");
			registerDisplaySurface("Society", surface);
		}
		schedule = new Schedule();
		agentList  = new ArrayList <NewNode>();
		worldXSize = 350;
		worldYSize = 350;

		numVarAttributes   = 7;
		numFixedAttributes = 5;

		fixedAttribSalience = 2.0;
		zealSalience        = 2.0;


		numMagnetsN = 100;
		numMagnetsP = 0;
		numMagnetsR = 0;
		transitiveFriendship = 0.5;
		safeNewFriends       = true;
		mutualFriendFactor   = 0.0; //whether this works is questionable. =1.0;
		maxLinkAgeBonus      = 1.0;

		numZealLevels         = 3;
		fractionInitRadicals  = 0.05;
		fractionInitPacifists = 0.05;
		diversity             = 1.0;

		attritionRate 						 = 2E-3;
		radicalsAttritionIncrement = 0.00;

		avgPressurability 			 = 0.33;
		magneticEncounterRate    = 0.10;
		fractionExposedToMagnets = 0.03;

		population = 50;
		avgDegree  = 5;
		//nodeDegreeExponent = 2.25; //gives a mean of 5

		degreeSD 			     = 1.0;
		pressurabilitySD 	 = 0.1;	  

		initTieStrengthSD  = 0.05;
		linkAgeBonusFactor = 100.0;
		strengthUpdateRate = 0.2;
		switchRandomness   = 0.1;
		switchPressFactor  = 10.;

		runLength    			 = 1000;

		avgInitTieStrength  = 0.10;
		repellingTies 			= false;

		updateEveryN = 1;
		//initialSteps = 1;

		SimName         = "myName";
		fileSep					= System.getProperty("file.separator");
		lineSep					= System.getProperty("line.separator");
		outputDirectory = System.getProperty("java.io.tmpdir");
		networkRecordTic = -1;

		File outDir = new File(outputDirectory);
		if (! outDir.exists()) {
			outDir.mkdirs();
		}

		layoutType = "Circle";
		//showHist   = false;
		showNet    = false;
		showEnergyPlot   = false;
		showStatsPlot  = false;

		isGui = getController().isGUI();
		if (isGui){
			showNet   = true;
			showEnergyPlot  = true;
			showStatsPlot = true;
			AbstractGUIController.ALPHA_ORDER = false;
		} else {
			//fixme: this is a useful feature, but unfortunately, it crashes on the second run!  Maybe it didn't crash back in April26 version.
			((BatchController)getController()).setAutoRecording(false);
			//    	recorder = new DataRecorder("c:/temp/simdata.cvs", this);
		}

		//this should not assign a value, since a value might have been make in the pre-setup() call
		//singleParamScan = true;
	}

	public void setUpdateEveryN(int val) {
		//not exposed
		//val = Math.max(0, val);
		updateEveryN = val;
	}

	public void sortAgentListForDisplay() {
		if (agentList.size() > 400 || ! layoutType.equals("Circle")) {
			return;
		}
		NewNode sortedAgents [] = new NewNode[agentList.size()];
		for (int i=0; i<agentList.size(); ++i) {
			sortedAgents[i]=(NewNode)agentList.get(i);
		}
		Arrays.sort(sortedAgents);
		agentList.clear();
		for (int i=0; i<sortedAgents.length; ++i) {
			agentList.add((NewNode)sortedAgents[i]);
		}
	}

	public void statsAction(){
		try{
			if (getTickCount() > runLength) {
				return; //prevents extra recording of state. wishlist: we should rather update the schedule
			}
			//if (showHist && isGui) degreeDist.  step();
			if (showEnergyPlot && isGui) graph. step();
			if (showStatsPlot && isGui)  graph2.step();

			Hashtable <String,Double> currentData = new Hashtable <String,Double>();
			currentData.put("zealCorr", calcZealCorrelation());
			currentData.put("energy", calcNetworkEnergy());
			currentData.put("avgTieStrength", calcAvgLinkStrength());
			currentData.put("R-avgTieStrength", calcAvgRadicalLinkStrength());
			currentData.put("R-fraction", calcFractionRadicals());

			Hashtable <String,Double> pairsData = calcRadicalPairsData();
			currentData.put("R-homophilyPin", pairsData.get("R-homophilyPin")); 
			currentData.put("R-assortativity", pairsData.get("R-assortativity")); 
			//wishlist: reporting the distribution
			currentData.put("medianNumRadRadNeighbors", pairsData.get("medianNumRadRadNeighbors"));
			currentData.put("R-rawDyads", pairsData.get("R-rawDyads"));
			currentData.put("R-dyadRatio", pairsData.get("R-dyadRatio"));
			currentData.put("R-E-I_index", pairsData.get("R-E-I_index"));

			Hashtable <String,Double> radicalTriangles = calcTriangles(true);
			currentData.put("R-weightedClusteringSoffer", radicalTriangles.get("weightedClusteringSoffer"));
			currentData.put("R-clusteringSoffer", radicalTriangles.get("unweightedClusteringSoffer"));
			currentData.put("R-rawTriads", radicalTriangles.get("rawTriads"));
			currentData.put("R-excessTriads", radicalTriangles.get("excessTriads"));
			currentData.put("R-clusteringWatts", radicalTriangles.get("clusteringWatts"));
			currentData.put("R-clusteringWattsGlobal", radicalTriangles.get("clusteringWattsGlobal"));

			Hashtable <String,Double> triangles = calcTriangles(false);
			currentData.put("All-clusteringSoffer", triangles.get("unweightedClusteringSoffer"));
			currentData.put("All-clusteringWatts", triangles.get("clusteringWatts"));

			Hashtable <String,Double> radicalComponents = calcComponents(true);
			currentData.put("R-avgCellSize", radicalComponents.get("avgCellSize"));
			currentData.put("R-avgIndCellSize", radicalComponents.get("avgIndCellSize"));
			currentData.put("R-medianCellSize", radicalComponents.get("medianCellSize"));
			currentData.put("R-numCells", radicalComponents.get("numCells"));
			currentData.put("R-numIsolatedCells", radicalComponents.get("R-numIsolatedCells"));

			//some of these operations could be optimized by grouping them together, esp. the ones that look at radicals
			for (int i=0; i<runtimeStatNames.length; ++i) {
				rawStatsStream.printf("%f,",currentData.get(runtimeStatNames[i]));
			}
			rawStatsStream.printf(lineSep);

			for (Enumeration <String> en = runningStats.keys(); en.hasMoreElements();) {
				String statName = en.nextElement();
				runningStats.get(statName).add(currentData.get(statName));
			}

			if (getTickCount() == networkRecordTic) {
				recordNetwork();
			}
		} catch (Exception ex) {
			System.out.println("Simulation terminated abormally while computing statistics:");
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private void updatePanelIfGui() {
		IController c = getController();
		if (c.isGUI()) {
			ProbeUtilities.updateModelProbePanel();
			((AbstractGUIController)c).makeCurrentParamsDefault();
		}
	}
}
