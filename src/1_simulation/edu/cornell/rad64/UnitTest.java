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

import java.util.ArrayList;
import java.util.Hashtable;

import cern.colt.list.DoubleArrayList;

import uchicago.src.sim.util.Random;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/* 
 * resources on junit
 * 1. http://www.laliluna.de/eclipse-junit-testing-tutorial.html
 * 2. http://supportweb.cs.bham.ac.uk/documentation/tutorials/docsystem/build/tutorials/junit/junit.html
 */

public class UnitTest extends TestCase {

	public static Test suite(){
		TestSuite suite = new TestSuite();

		suite.addTest(new UnitTest("testComponentMetrics"));
		suite.addTest(new UnitTest("testDyadicMetrics"));
		suite.addTest(new UnitTest("testFriendshipActions"));
		suite.addTest(new UnitTest("testGeneralMetrics"));
		suite.addTest(new UnitTest("testNodeAction"));
		suite.addTest(new UnitTest("testTriadicMetrics"));


		return suite;
	}

	public UnitTest(String name) {
		super(name);
		System.out.println("Executing UnitTest:"+name);
	}

	/*
	 * test computation of network components
	 */
	public static void testComponentMetrics() throws Exception {
		NewModel model = new NewModel();

		//warning: those setting might interfere with later simulations, if the later are poorly written...
		//avgDegree must be high so new friends are not rejected
		NewNode.loadStaticParams(10, 0.5, 0.3, 1, 1.0, 1.0, 0.1, 0.1, 0.1, 10, 1.0, model, 0, 
				3, 3, 3, 0.1, false, true, 1.0, 1.0, 1.0, 1.0, 1.0);

		Random.setSeed(0);
		Random.createUniform();
		Random.createNormal(0.0, 1.0);

		ArrayList <NewNode> agentList = new ArrayList <NewNode> ();
		NewNode node1, node2, node3, node4, node5, node6, node7;
		node1 = new NewNode();
		node2 = new NewNode();
		node3 = new NewNode();
		node4 = new NewNode();
		node5 = new NewNode();
		node6 = new NewNode();
		node7 = new NewNode();

		//network: R1-R2-R3 and M1-R4 and M2-R5
		node1.setZeal(1.0);
		node2.setZeal(1.0);
		node3.setZeal(1.0);
		node4.setZeal(0.0);
		node5.setZeal(1.0);
		node6.setZeal(0.0);
		node7.setZeal(1.0);

		node1.addNewFriend(node2, 0.1);
		node2.addNewFriend(node3, 0.1);
		node4.addNewFriend(node5, 0.1);
		node6.addNewFriend(node7, 0.1);

		agentList.add(node1);
		agentList.add(node2);
		agentList.add(node3);
		agentList.add(node4);
		agentList.add(node5);
		agentList.add(node6);
		agentList.add(node7);

		model.setAgentList(agentList);    
		{
			Hashtable <String, Double> data = model.calcComponents(false);
			assertTrue(Math.abs(data.get("numCells") - 3) < 0.01);
			assertTrue(Math.abs(data.get("medianCellSize") - 2) < 0.01);
			assertTrue(Math.abs(data.get("avgCellSize") - 7/3.) < 0.01);
			assertTrue(Math.abs(data.get("avgIndCellSize") - (3+3+3+2+2+2+2)/7.) < 0.01);
		}
		{
			Hashtable <String, Double> data = model.calcComponents(true);
			assertTrue(Math.abs(data.get("numCells") - 3) < 0.01);
			assertTrue(Math.abs(data.get("R-numIsolatedCells") - 1) < 0.01);
			assertTrue(Math.abs(data.get("medianCellSize") - 1) < 0.01);
			assertTrue(Math.abs(data.get("avgCellSize") - 5/3.) < 0.01);
			assertTrue(Math.abs(data.get("avgIndCellSize") - (3+3+3+1+1)/5.) < 0.01);
		}   
	}
	/*
	 * test computation of e.g. isolation
	 */
	public static void testDyadicMetrics() throws Exception {
		NewModel model = new NewModel();

		//warning: those setting might interfere with later simulations, if the later are poorly written...
		//avgDegree must be high so new friends are not rejected
		double avgInitTieStrength = 0.4;
		NewNode.loadStaticParams(10, avgInitTieStrength, 0.3, 1, 1.0, 1.0, 0.1, 0.1, 0.01, 10, 1.0, model, 0, 
				3, 3, 3, 0.1, false, true, 1.0, 1.0, 1.0, 1.0, 1.0);

		Random.setSeed(0);
		Random.createUniform();
		Random.createNormal(0.0, 1.0);

		ArrayList <NewNode> agentList = new ArrayList <NewNode> ();
		NewNode node1, node2, node3, node4;
		node1 = new NewNode();
		node2 = new NewNode();
		node3 = new NewNode();
		node4 = new NewNode();

		//initial tie strengths were set to 1.0 with SD 0.01
		node1.addNewFriend(node2, 0.1);
		node2.addNewFriend(node3, 0.1);
		node3.addNewFriend(node4, 0.1);
		node4.addNewFriend(node1, 0.1);

		agentList.add(node1);
		agentList.add(node2);
		agentList.add(node3);
		agentList.add(node4);

		node1.setZeal(1.0);
		node2.setZeal(1.0);
		node3.setZeal(0.0);
		node4.setZeal(0.0);

		model.setAgentList(agentList);

		{
			Hashtable <String, Double> data = model.calcRadicalPairsData();
			assertTrue(Math.abs(data.get("R-homophilyPin")-0.0)<0.05);
			//(inTies/allTies -density) / (1-density) = ([1+1]/[2+2]-0.5)/(1-0.5) = (0.5-0.5)/0.5 = 0
			assertTrue(Math.abs(data.get("R-dyadRatio")-0.5)<0.05);
			assertTrue(Math.abs(data.get("R-rawDyads")-1.0)<0.05);
			assertTrue(Math.abs(model.calcAvgRadicalLinkStrength()-avgInitTieStrength)<0.05);
			assertTrue(Math.abs(data.get("medianNumRadRadNeighbors")-1)<0.05);
			assertTrue(Math.abs(data.get("R-E-I_index")- 0.333)<0.01);
			//(2.-1.)/3.
			assertTrue(Math.abs(data.get("R-assortativity")- 0.0)<0.02);
			//((2+2)/8. - ((4/8.)**2+(4/8.)**2))/(1 - ((4/8.)**2+(4/8.)**2))    
		}

		node1.setZeal(1.0);
		node2.setZeal(0.0);
		node3.setZeal(0.0);
		node4.setZeal(0.0);

		model.setAgentList(agentList);

		{
			Hashtable <String, Double> data2 = model.calcRadicalPairsData();
			assertTrue(Math.abs(data2.get("R-homophilyPin") + 0.333) < 0.01);
			assertTrue(Math.abs(data2.get("R-dyadRatio")-0.0)<0.05);
			//(inTies/allTies -density) / (1-density) = ([0]/[2]-0.25)/(1-0.25) = (-0.25)/0.75 = -0.333
			assertTrue(Math.abs(data2.get("R-rawDyads")-0.0)<0.05);
			assertTrue(Math.abs(model.calcAvgRadicalLinkStrength()-0.0)<0.05);
			assertTrue(Math.abs(data2.get("medianNumRadRadNeighbors")-0)<0.05);
			assertTrue(Math.abs(data2.get("R-E-I_index")- 1.0)<0.05);
			//(2.-0.)/2.
			assertTrue(Math.abs(data2.get("R-assortativity")- -0.333)<0.02);
			//((0+4)/8. - ((2/8.)**2+(6/8.)**2))/(1 - ((2/8.)**2+(6/8.)**2)) = -0.333
		}
	}

	/*
	 * test handling of new friends (agreement threshold, degree cap)
	 */
	public static void testFriendshipActions() throws Exception {
		NewModel model = new NewModel();

		//warning: those setting might interfere with later simulations, if the later are poorly written...
		double zealSalience = 1;
		int numFixAttributes = 3;
		double fixedAttribSalience = 1;
		int numVarAttributes = 3;
		double linkAgeBonusFactor = 1.0;
		double maxLinkAgeBonus = 1.0;
		NewNode.loadStaticParams(10, 1.0, 0.3, 1, 1.0, fixedAttribSalience, 0.1, 0.1, 0.01, 
				linkAgeBonusFactor, maxLinkAgeBonus, model, 0, 
				3, numFixAttributes, numVarAttributes, 0.1, false, true, 
				1.0, 1.0, 1.0, 1.0, zealSalience);
		Random.setSeed(0);
		Random.createUniform();
		Random.createNormal(0.0, 1.0);

		ArrayList <NewNode> agentList = new ArrayList <NewNode> ();
		NewNode node1, node2, node3, node4;
		node1 = new NewNode();
		node2 = new NewNode();
		node3 = new NewNode();
		node4 = new NewNode();

		double [] defaultFixAttrib = {1.0, 1.0, 1.0};
		double [] defaultVarAttrib = {1.0, 1.0, 1.0};
		double defaultZeal = 1.0;
		node1.setFixAttributes(defaultFixAttrib);
		node1.setVarAttributes(defaultVarAttrib);
		node1.setZeal(defaultZeal);

		node2.setFixAttributes(node1.getFixAttributes());
		node2.setVarAttributes(node1.getFixAttributes());
		node2.setZeal(node1.getZeal()*-1);

		//degree cap
		node1.setDegreeCap(0);    
		node2.setDegreeCap(1);
		assertTrue(! node1.exposeToNewAgent(node2, true));
		assertTrue(! node1.hasEdgeTo(node2));
		assertTrue(node1.getNumOutEdges()==0);
		assertTrue(node1.getNumInEdges()==0);
		node1.setDegreeCap(1); 
		assertTrue(node1.exposeToNewAgent(node2, true));
		assertTrue(node1.hasEdgeTo(node2));
		assertTrue(node1.getNumOutEdges()==1);
		assertTrue(node1.getNumInEdges()==1);
		double expectedAgreement12 = 
			(-1*zealSalience + numFixAttributes*fixedAttribSalience + numVarAttributes + 0*maxLinkAgeBonus);
		assertTrue(Math.abs(node1.getWorstFriendAgreement() - expectedAgreement12) <0.001);

		//degree cap of friend
		node1.setDegreeCap(0); //kills all ties    
		node1.setDegreeCap(1);    
		node2.setDegreeCap(0);
		assertTrue(! node1.exposeToNewAgent(node2, true));
		assertTrue(! node1.hasEdgeTo(node2));
		assertTrue(node1.getNumOutEdges()==0);
		assertTrue(node1.getNumInEdges()==0);

		//better agreement
		node1.setDegreeCap(0); //kills all ties    
		node1.setDegreeCap(1);    
		node3.setFixAttributes(node1.getFixAttributes());
		node3.setVarAttributes(node1.getFixAttributes());
		node3.setZeal(node1.getZeal());
		assertTrue(node1.exposeToNewAgent(node3, true));
		assertTrue(node1.hasEdgeTo(node3));
		assertTrue(! node1.hasEdgeTo(node2));
		assertTrue(node1.getNumOutEdges()==1);
		assertTrue(node1.getNumInEdges()==1);
		double expectedAgreement13 = 
			(1*zealSalience + numFixAttributes*fixedAttribSalience + numVarAttributes + 0*maxLinkAgeBonus);
		assertTrue(Math.abs(node1.getWorstFriendAgreement() - expectedAgreement13)<0.001);

		((NewEdge)node1.getOutEdges().get(0)).step();
		((NewEdge)node1.getOutEdges().get(0)).step();
		((NewEdge)node1.getOutEdges().get(0)).step();
		((NewEdge)node1.getOutEdges().get(0)).step();
		double expectedAgreement13upd = 
			(1*zealSalience + numFixAttributes*fixedAttribSalience + numVarAttributes + 4.0/(linkAgeBonusFactor+4.0)*maxLinkAgeBonus);
		assertTrue(Math.abs(node1.getWorstFriendAgreement() - expectedAgreement13upd)<0.00001);

		//higher cap -> node2 now ok friend
		node1.setDegreeCap(0); //kills all ties    
		node1.setDegreeCap(2); 
		node2.setDegreeCap(1); 
		assertTrue(node1.exposeToNewAgent(node2, true));
		assertTrue(node1.exposeToNewAgent(node3, true));
		assertTrue(node1.getNumOutEdges()==2);
		assertTrue(node1.getNumInEdges()==2);
		assertTrue(Math.abs(node1.getWorstFriendAgreement() - expectedAgreement12) <0.001);
	}

	/*
	 * test computation of metrics such as energy, R-fraction
	 */
	public static void testGeneralMetrics() throws Exception {
		NewModel model = new NewModel();

		double avgDegreeCap       = 10;
		double avgInitTieStrength = 0.5;
		double initTieStrengthSD  = 0.0001;     //avgDegree must be high so new friends are not rejected
		double fixedAttribSalience = 5;
		double linkAgeBonusFactor = 1.0;
		double maxLinkAgeBonus = 1.0;
		int 	 numVarAttributes = 3;
		int 	 numFixAttributes = 3;
		double zealSalience = 10;

		//warning: those setting might interfere with later simulations, if the later are poorly written...
		NewNode.loadStaticParams(avgDegreeCap, avgInitTieStrength, 0.3, 1, 1.0, fixedAttribSalience, 
				0.1, 0.1, initTieStrengthSD, 
				linkAgeBonusFactor, maxLinkAgeBonus, model, 0, 
				3, numFixAttributes, numVarAttributes, 0.1, false, true, 
				1.0, 1.0, 1.0, 1.0, zealSalience);

		Random.setSeed(0);
		Random.createUniform();
		Random.createNormal(0.0, 1.0);

		ArrayList <NewNode> agentList = new ArrayList <NewNode> ();
		NewNode node1, node2, node3, node4, node5, node6, node7;
		node1 = new NewNode();
		node2 = new NewNode();
		node3 = new NewNode();
		node4 = new NewNode();
		node5 = new NewNode();
		node6 = new NewNode();
		node7 = new NewNode();

		//network: R1-R2-R3 and M1-R4 and M2-R5
		node1.setZeal(1.0);
		node2.setZeal(1.0);
		node3.setZeal(1.0);
		node4.setZeal(0.0);
		node5.setZeal(1.0);
		node6.setZeal(0.0);
		node7.setZeal(1.0);

		node1.addNewFriend(node2, 0.1);
		node2.addNewFriend(node3, 0.1);
		node4.addNewFriend(node5, 0.1);
		node6.addNewFriend(node7, 0.1);

		agentList.add(node1);
		agentList.add(node2);
		agentList.add(node3);
		agentList.add(node4);
		agentList.add(node5);
		agentList.add(node6);
		agentList.add(node7);

		model.setAgentList(agentList);  
		assertTrue(Math.abs(model.calcFractionRadicals() - 5.0/7.0) < 0.01);

		double [] zeals          = {1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0};
		double [] neighbAvgZeals = {1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0};
		assertTrue(Math.abs(model.calcZealCorrelation() 
				- NewModel.calcZealCorrelationHelper(new DoubleArrayList(zeals),new DoubleArrayList(neighbAvgZeals),1.0)) < 0.01);

		assertTrue(Math.abs(model.calcAvgLinkStrength() - avgInitTieStrength) < 0.01);
		assertTrue(Math.abs(model.calcAvgRadicalLinkStrength() - avgInitTieStrength) < 0.01);

		//energy test
		double [] defaultFixAttrib = {1.0, 1.0, 1.0};
		double [] defaultVarAttrib = {1.0, 1.0, 1.0};
		node1.setFixAttributes(defaultFixAttrib);
		node1.setVarAttributes(defaultVarAttrib);
		node2.setFixAttributes(node1.getFixAttributes());
		node2.setVarAttributes(node1.getVarAttributes());
		node3.setFixAttributes(node1.getFixAttributes());
		node3.setVarAttributes(node1.getVarAttributes());

		double expectedEnergy2=-2*avgInitTieStrength*(zealSalience + numFixAttributes*fixedAttribSalience + numVarAttributes + 0*maxLinkAgeBonus);
		assertTrue(Math.abs(node2.localEnergy() - expectedEnergy2) < 0.05);

		double [] modedFixAttrib = {1.0, 1.0, -1.0};
		node2.setFixAttributes(modedFixAttrib);
		assertTrue(Math.abs(node2.localEnergy() - 
				-2*avgInitTieStrength*(zealSalience + (numFixAttributes-2)*fixedAttribSalience + numVarAttributes + 0*maxLinkAgeBonus)) < 0.05);

		node4.setFixAttributes(defaultFixAttrib);
		node4.setVarAttributes(defaultVarAttrib);
		node5.setFixAttributes(defaultFixAttrib);
		node5.setVarAttributes(defaultVarAttrib);
		assertTrue(Math.abs(node4.localEnergy() - 
				-avgInitTieStrength*(0*zealSalience + numFixAttributes*fixedAttribSalience + numVarAttributes + 0*maxLinkAgeBonus)) < 0.05);

		node6.setFixAttributes(defaultFixAttrib);
		node6.setVarAttributes(defaultVarAttrib);
		node7.setFixAttributes(defaultFixAttrib);
		node7.setVarAttributes(defaultVarAttrib);
		((NewEdge)node6.getOutEdges().get(0)).step();
		((NewEdge)node6.getOutEdges().get(0)).step();
		double expected6 = -avgInitTieStrength*(0*zealSalience + numFixAttributes*fixedAttribSalience + numVarAttributes + 2./(2.+linkAgeBonusFactor)*maxLinkAgeBonus);
		assertTrue(Math.abs(node6.localEnergy() - expected6) < 0.05);
	}
	/*
	 * test functionality of nodes: 
	 * 
	 */
	public static void testNodeAction() throws Exception {
		NewModel model = new NewModel();

		double avgDegreeCap       = 10;   //avgDegree must be high so new friends are not rejected
		double avgPressurability  = 0.0;  //disabled for now
		double avgInitTieStrength = 0.5;
		double initTieStrengthSD  = 0.0001;
		double fixedAttribSalience = 5;
		double linkAgeBonusFactor = 1.0;
		double maxLinkAgeBonus = 1.0;
		int 	 numVarAttributes = 3;
		int 	 numFixAttributes = 3;
		double pressurabilitySD = 0.001;
		double switchPressFactor = 10.;  //reasonable to make it overcome hump
		double switchRandomness = 0.01;
		double strengthUpdateRate = 0.5; //pretty high so we reach max quickly
		double transitiveFriendship = 1.0;
		double zealSalience = 10;

		//warning: those setting might interfere with later simulations, if the later are poorly written...
		NewNode.loadStaticParams(avgDegreeCap, avgInitTieStrength, avgPressurability, 1, 1.0, fixedAttribSalience, 
				0.1, 0.1, initTieStrengthSD, 
				linkAgeBonusFactor, maxLinkAgeBonus, model, 0, 
				3, numFixAttributes, numVarAttributes, pressurabilitySD, false, true, 
				strengthUpdateRate, transitiveFriendship, switchPressFactor, switchRandomness, zealSalience);

		Random.setSeed(0);
		Random.createUniform();
		Random.createNormal(0.0, 1.0);

		ArrayList <NewNode> agentList = new ArrayList <NewNode> ();
		NewNode node1, node2, node3, node4, node5, node6, node7, node8, node9;
		node1 = new NewNode();
		node2 = new NewNode();
		node3 = new NewNode();
		node4 = new NewNode();
		node5 = new NewNode();
		node6 = new NewNode();
		node7 = new NewNode();
		node8 = new NewNode();
		node9 = new NewNode();

		//network: R1-R2-R3 and M1-R4-M2-M1 and P1-P2-P3-P1
		node1.setZeal(1.0);
		node2.setZeal(1.0);
		node3.setZeal(1.0);
		node4.setZeal(0.0);
		node5.setZeal(1.0);
		node6.setZeal(0.0);
		node7.setZeal(-1.0);
		node8.setZeal(-1.0);
		node9.setZeal(-1.0);

		double [] defaultFixAttrib = {1.0, 1.0, 1.0};
		double [] defaultVarAttrib = {1.0, 1.0, 1.0};
		node1.setFixAttributes(defaultFixAttrib);
		node1.setVarAttributes(defaultVarAttrib);
		node2.setFixAttributes(node1.getFixAttributes());
		node2.setVarAttributes(node1.getVarAttributes());
		node3.setFixAttributes(node1.getFixAttributes());
		node3.setVarAttributes(node1.getVarAttributes());

		node1.addNewFriend(node2, 0.1);
		node2.addNewFriend(node3, 0.1);

		node4.addNewFriend(node5, 0.1);
		node5.addNewFriend(node6, 0.1);
		node6.addNewFriend(node4, 0.1);

		node7.addNewFriend(node8, 0.1);
		node8.addNewFriend(node9, 0.1);
		node9.addNewFriend(node7, 0.1);

		agentList.add(node1);
		agentList.add(node2);
		agentList.add(node3);
		agentList.add(node4);
		agentList.add(node5);
		agentList.add(node6);
		agentList.add(node7);

		//increase in strength
		NewEdge edge12 = (NewEdge)node1.getOutEdges().get(0);
		assertTrue(Math.abs(edge12.getStrength() - avgInitTieStrength) < 0.01);

		node1.step();
		node1.step();
		node1.step();
		assertTrue(edge12.getStrength() > 0.7);

		//transitive friendship
		assertTrue(! node1.getOutNodes().contains(node3));
		node2.step();
		assertTrue(node2.getOutNodes().contains(node3));


		//pressure on zeal
		node5.setPeerPressurability(1.0);
		assertTrue(Math.abs(node5.getZeal() - 1.0) < 0.01);
		node4.step();
		node4.step();
		node4.step();
		node5.step();
		node5.step();
		node5.step();
		node6.step();
		node6.step();
		node6.step();
		assertTrue(Math.abs(node5.getZeal() - 0.0) < 0.1);

		//pressure on var attributes
		node7.setFixAttributes(defaultFixAttrib);
		node7.setVarAttributes(defaultVarAttrib);
		node9.setFixAttributes(defaultFixAttrib);
		node9.setVarAttributes(defaultVarAttrib);
		double [] modedVarAttrib = defaultVarAttrib.clone();
		modedVarAttrib[0] = -1;
		node8.setFixAttributes(defaultFixAttrib);
		node8.setVarAttributes(modedVarAttrib);

		assertTrue(Math.abs(node8.getVarAttributes()[0] - -1.0) < 0.01);
		node8.setPeerPressurability(0.8); //at 1.0 it becomes unstable
		node7.step();
		node7.step();
		node7.step();
		node7.step();
		node7.step();
		node8.step();
		node8.step();
		node8.step();
		node8.step();
		node8.step();
		node9.step();
		node9.step();
		node9.step();
		node9.step();
		node9.step();
		assertTrue(Math.abs(node8.getVarAttributes()[0] - 1.0) < 0.01);
	}
	/*
	 * test computation of clustering based on Soffer figure 2c
	 */
	public static void testTriadicMetrics() throws Exception {
		NewModel model = new NewModel();

		double avgInitTieStrength = 0.5;
		double initTieStrengthSD  = 0.001;
		//warning: those setting might interfere with later simulations, if the later are poorly written...
		//avgDegree must be high so new friends are not rejected
		NewNode.loadStaticParams(10, avgInitTieStrength, 0.3, 1, 1.0, 1.0, 0.1, 0.1, initTieStrengthSD, 10, 1.0, model, 0, 
				3, 3, 3, 0.1, false, true, 1.0, 1.0, 1.0, 1.0, 1.0);

		Random.setSeed(0);
		Random.createUniform();
		Random.createNormal(0.0, 1.0);

		ArrayList <NewNode> agentList = new ArrayList <NewNode> ();
		NewNode nodeBottom, nodeTop, node2, node3, node4, node5;
		nodeBottom = new NewNode();
		nodeTop = new NewNode();
		node2 = new NewNode();
		node3 = new NewNode();
		node4 = new NewNode();
		node5 = new NewNode();

		nodeBottom.setZeal(0.0);
		nodeTop.setZeal(0.0);
		node2.setZeal(0.0);
		node3.setZeal(0.0);
		node4.setZeal(0.0);
		node5.setZeal(0.0);

		//network: b-t, b-n2-t, b-n3-t, b-n4-t, b-n5-t 
		nodeBottom.addNewFriend(nodeTop, 0.1);

		nodeBottom.addNewFriend(node2, 0.1);
		nodeBottom.addNewFriend(node3, 0.1);
		nodeBottom.addNewFriend(node4, 0.1);
		nodeBottom.addNewFriend(node5, 0.1);

		nodeTop.addNewFriend(node2, 0.1);
		nodeTop.addNewFriend(node3, 0.1);
		nodeTop.addNewFriend(node4, 0.1);
		nodeTop.addNewFriend(node5, 0.1);

		agentList.add(nodeBottom);
		agentList.add(nodeTop);
		agentList.add(node2);
		agentList.add(node3);
		agentList.add(node4);
		agentList.add(node5);

		//all nodes nonradicals
		model.setAgentList(agentList);    
		{
			Hashtable <String, Double> data = model.calcTriangles(false);
			assertTrue(Math.abs(data.get("rawTriads") - 4.0) < 0.01);
			assertTrue(Math.abs(data.get("excessTriads") - 4.0 * 15 / 4) < 0.01);
			assertTrue(Math.abs(data.get("weightedClusteringSoffer") - 1.0*avgInitTieStrength) < 0.01);
			assertTrue(Math.abs(data.get("unweightedClusteringSoffer") - 1.0) < 0.01);
			assertTrue(Math.abs(data.get("clusteringWatts") - 0.8) < 0.01);
			//average of: (4./10, 4./10, 1, 1, 1, 1) = 0.8
			assertTrue(Math.abs(data.get("clusteringWattsGlobal") - 0.5) < 0.01);
			//4 triangles exist x 3 = 12.  
			//neighbors of Botton can have max of C(5,2)=10;  + 10 for neighbors of nodeTop; +1 for node 2,3,5,6 
			// = 10+10+4 = 24 possible
			//Watts = 12/24 = 0.5 
		}

		//all nodes Radicals
		nodeBottom.setZeal(1.0);
		nodeTop.setZeal(1.0);
		node2.setZeal(1.0);
		node3.setZeal(1.0);
		node4.setZeal(1.0);
		node5.setZeal(1.0);    
		{
			Hashtable <String, Double> data2 = model.calcTriangles(true);
			assertTrue(Math.abs(data2.get("rawTriads") - 4.0) < 0.01);
			assertTrue(Math.abs(data2.get("excessTriads") - 4.0 * 15 / 4) < 0.01);
			assertTrue(Math.abs(data2.get("weightedClusteringSoffer") - 1.0*avgInitTieStrength) < 0.01);
			assertTrue(Math.abs(data2.get("unweightedClusteringSoffer") - 1.0) < 0.01);
			assertTrue(Math.abs(data2.get("clusteringWatts") - 0.8) < 0.01);
			assertTrue(Math.abs(data2.get("clusteringWattsGlobal") - 0.5) < 0.01);
		}

		//extra degrees but not connected to other radicals
		NewNode nodeX1 = new NewNode();
		NewNode nodeX2 = new NewNode();
		nodeX1.setZeal(0.0);
		nodeX2.setZeal(0.0);

		node2.addNewFriend(nodeX1, 0.1);
		node3.addNewFriend(nodeX2, 0.1);

		agentList.add(nodeX1);
		agentList.add(nodeX2);

		{
			Hashtable <String, Double> data3 = model.calcTriangles(true);
			assertTrue(Math.abs(data3.get("weightedClusteringSoffer") -   (4./(4+0.666666))*avgInitTieStrength) < 0.01);
			assertTrue(Math.abs(data3.get("unweightedClusteringSoffer") - (4./(4+0.666666))) < 0.01);
			//note: X1 contributes to possible degree of node2 when viewed from nodeTop and nodeBottom 
			//      but when viewed from node2, it is considered a neighbor not part of the radical subgraph!
		}
	}
}
