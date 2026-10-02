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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import uchicago.src.sim.engine.BatchController;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.parameter.ParameterSetter;
import uchicago.src.sim.parameter.ParameterSetterFactory;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/* 
 * resources on junit
 * 1. http://www.laliluna.de/eclipse-junit-testing-tutorial.html
 * 2. http://supportweb.cs.bham.ac.uk/documentation/tutorials/docsystem/build/tutorials/junit/junit.html
 */


public class NewModelTest extends TestCase {
	static Hashtable <String, Hashtable> baselineData; 

	public static Test suite(){
		baselineData = new Hashtable <String, Hashtable>();
		setUpOnce();

		TestSuite suite = new TestSuite();

		suite.addTest(new NewModelTest("testMagnetsN"));
		suite.addTest(new NewModelTest("testMagnetsP"));
		suite.addTest(new NewModelTest("testMagnetsR"));
		suite.addTest(new NewModelTest("testAttritionRate"));
		suite.addTest(new NewModelTest("testAvgDegree"));
		suite.addTest(new NewModelTest("testAvgPressurability"));
		suite.addTest(new NewModelTest("testDiversity"));
		suite.addTest(new NewModelTest("testFixAttributeSalience"));
		suite.addTest(new NewModelTest("testFractionExposedToMagnets"));
		suite.addTest(new NewModelTest("testFractionInitPacifists"));
		suite.addTest(new NewModelTest("testFractionInitRadicals"));
		suite.addTest(new NewModelTest("testMagneticEncounterRate"));
		suite.addTest(new NewModelTest("testMaxLinkAgeBonus"));
		suite.addTest(new NewModelTest("testNumVarFixAttributes"));
		suite.addTest(new NewModelTest("testPopulation"));
		suite.addTest(new NewModelTest("testRadicalsAttritionIncrement"));
		suite.addTest(new NewModelTest("testRunLength"));
		suite.addTest(new NewModelTest("testTransitiveFriendship"));
		suite.addTest(new NewModelTest("testZealSalience"));

		return suite;
	}

	public NewModelTest(String name) {
		super(name);
		System.out.println("Executing NewModelTest:"+name);
	}

	private static NewModel runSim(Hashtable <String,Double> inputParams) {
		SimInit si     = new SimInit();
		NewModel model = new NewModel();
		model.setSingleParamScan(false);

		File tempBatchFile = null;
		ParameterSetter setter = null;
		try {
			tempBatchFile = File.createTempFile("abmTestBatch", ".tmp");
			writeTestBatchFile(tempBatchFile, inputParams);
			setter = ParameterSetterFactory.createParameterSetter(tempBatchFile.getAbsolutePath());
		} catch (IOException e) {
			System.out.println(e.toString());
			System.exit(1);
		}

		BatchController control = new BatchController(setter);
		model.setController(control);
		control.setModel(model);
		control.setExitOnExit(false);
		control.begin();

		//the previous begin call ends a run and resets the parameter values
		//this call is necessary to restore the parameters after the reset
		control.setModel(model);

		return model;
	}

	private static void setUpOnce() {
		System.out.println("Running simulation to record stats with default params...");
		System.out.println("");
		Hashtable <String,Double> inputParams = new Hashtable <String,Double> ();
		inputParams.put("rngSeed", 666.0);
		inputParams.put("transitiveFriendship", 0.1);
		inputParams.put("fractionInitRadicals", 0.1);
		inputParams.put("fractionInitPacifists", 0.1);
		inputParams.put("fractionExposedToMagnets", 0.05);
		inputParams.put("numMagnetsN", 20.0);
		inputParams.put("population", new Double(2000));  	

		NewModel defaultModel = runSim(inputParams);

		Hashtable defSimData = new Hashtable();
		defSimData.put("model", defaultModel); 
		defSimData.put("params", inputParams);
		baselineData.put("default", defSimData);

		//baseline with zero zeal saliance
		{
			Hashtable <String,Double> inputParams2 = (Hashtable<String, Double>) inputParams.clone();
			inputParams2.put("zealSalience", new Double(0.0));

			NewModel zeroZealSalModel = runSim(inputParams2);

			Hashtable zeroZealSalSimData = new Hashtable();
			zeroZealSalSimData.put("model", zeroZealSalModel);
			zeroZealSalSimData.put("params", inputParams2);
			baselineData.put("zeroZealSal", zeroZealSalSimData);
		}

		//baseline with strong magnet exposure saliance
		{
			Hashtable <String,Double> inputParams3 = (Hashtable<String, Double>) inputParams.clone();
			inputParams3.put("fractionExposedToMagnets", new Double(0.3));

			NewModel highExposureModel = runSim(inputParams3);

			Hashtable highExposureSimData = new Hashtable();
			highExposureSimData.put("model", highExposureModel);
			highExposureSimData.put("params", inputParams3);
			baselineData.put("highExposure", highExposureSimData);
		}

		//baseline with high peer pressure
		{
			Hashtable <String,Double> inputParams4 = (Hashtable<String, Double>) inputParams.clone();
			inputParams4.put("avgPressurability", new Double(1.0));

			NewModel highPressureModel = runSim(inputParams4);

			Hashtable highPressureSimData = new Hashtable();
			highPressureSimData.put("model", highPressureModel);
			highPressureSimData.put("params", inputParams4);
			baselineData.put("highPressure", highPressureSimData);
		}

		//baseline with lots of pacifists
		{
			Hashtable <String,Double> inputParams5 = (Hashtable<String, Double>) inputParams.clone();
			inputParams5.put("fractionInitPacifists", new Double(0.6));

			NewModel pacifistModel = runSim(inputParams5);

			Hashtable pacifistSimData = new Hashtable();
			pacifistSimData.put("model", pacifistModel);
			pacifistSimData.put("params", inputParams5);
			baselineData.put("highPacifists", pacifistSimData);
		}

		//baseline with lots of radicals
		{
			Hashtable <String,Double> inputParams6 = (Hashtable<String, Double>) inputParams.clone();
			inputParams6.put("fractionInitRadicals", new Double(0.6));

			NewModel pacifistModel = runSim(inputParams6);

			Hashtable pacifistSimData = new Hashtable();
			pacifistSimData.put("model", pacifistModel);
			pacifistSimData.put("params", inputParams6);
			baselineData.put("highRadicals", pacifistSimData);
		}

		System.out.println("Running tests...");
		System.out.println("");
	}

	private static void writeTestBatchFile(File tempBatchFile, Hashtable <String,Double> inputParams) 
	throws IOException{
		BufferedWriter out = new BufferedWriter(new FileWriter(tempBatchFile));

		String lineSep = System.getProperty("line.separator");
		out.write("runs: 1" + lineSep);
		for (Enumeration <String> e = inputParams.keys() ; e.hasMoreElements() ;) {
			String paramName = e.nextElement();
			out.write(paramName + " {" + lineSep);
			double paramVal = inputParams.get(paramName).doubleValue();
			String paramValStr = "NotNumber";
			if(paramVal == Math.floor(paramVal)) {
				paramValStr = "" + Math.round(paramVal);
			} else {
				paramValStr = "" + paramVal;
			}
			out.write("   set: " + paramValStr + lineSep);
			out.write("}" + lineSep);
		}
		out.write(lineSep);

		out.close();	
	}

	private void assertLess(double val1, double val2) {
		System.out.print("Expected: ");
		System.out.print(val1);
		System.out.print(" < ");
		System.out.println(val2);
		if(val1<val2) {  	
			System.out.println("OK.");
		} else {
			System.out.println("Failed");
		}
		System.out.println();
		assertTrue(val1<val2);
	}

	private void assertMore(double val1, double val2) {
		System.out.print("Expected: ");
		System.out.print(val1);
		System.out.print(" > ");
		System.out.println(val2);
		if(val1>val2) {  	
			System.out.println("OK.");
		} else {
			System.out.println("Failed");
		}
		System.out.println();
		assertTrue(val1>val2);
	}

	private Hashtable <String,Double> copyInputParams(String baselineName) {
		return (Hashtable<String, Double>) ((Hashtable)baselineData.get(baselineName).get("params")).clone();
	}

	protected void setUp() {
	}

	protected void tearDown() {
	}

	public void testAttritionRate() throws Exception {
		System.out.println("Test: When the attrition rate is high\n" +
		"     the avg energy should go UP a lot");
		System.out.println();

		NewModel baselineModel = (NewModel) baselineData.get("default").get("model");

		//input conditions
		Hashtable <String,Double> inputParams = copyInputParams("default");
		inputParams.put("attritionRate",   new Double(baselineModel.getAttritionRate()+0.5));

		//execution
		NewModel model = runSim(inputParams);

		assertMore(model.getAvgStat("energy"), 0.3*baselineModel.getAvgStat("energy"));
		System.out.println();
		System.out.println();
	}

	public void testAvgDegree() throws Exception {
		System.out.println("Test: Increasing avgDegree\n" +
		"     the avg isolation should go DOWN somewhat");
		System.out.println();

		NewModel baselineModel = (NewModel) baselineData.get("default").get("model");

		//input conditions
		Hashtable <String,Double> inputParams = copyInputParams("default");
		inputParams.put("avgDegree",   new Double(baselineModel.getAvgDegree()+5));

		//execution
		NewModel model = runSim(inputParams);

		assertLess(model.getAvgStat("R-homophilyPin"), baselineModel.getAvgStat("R-homophilyPin"));
		System.out.println();
		System.out.println();
	}

	public void testAvgPressurability() throws Exception {
		System.out.println("Test: When the avgPressurability is high\n" +
				"     the avg energy should go DOWN a lot\n" +
		"     the avg density should go DOWN a bit");  //radicals are endanged minority
		System.out.println();

		NewModel baselineModel = (NewModel) baselineData.get("default").get("model");

		//input conditions
		Hashtable <String,Double> inputParams = copyInputParams("default");
		inputParams.put("avgPressurability",   new Double(1.0));

		//execution
		NewModel model = runSim(inputParams);

		assertLess(model.getAvgStat("energy"), 1.5*baselineModel.getAvgStat("energy"));
		assertLess(model.getAvgStat("R-fraction"), 1.1*baselineModel.getAvgStat("R-fraction"));
		System.out.println();
		System.out.println();
	}

	public void testDiversity() throws Exception {
		System.out.println("Test: When the diversity is low\n" +
		"     the avg energy should go down a lot");
		System.out.println();

		NewModel baselineModel = (NewModel) baselineData.get("default").get("model");

		//input conditions
		Hashtable <String,Double> inputParams = copyInputParams("default");
		inputParams.put("diversity",   new Double(0.01));

		//execution
		NewModel model = runSim(inputParams);

		assertLess(model.getAvgStat("energy"), 2*baselineModel.getAvgStat("energy"));
		System.out.println();
		System.out.println();
	}

	public void testFixAttributeSalience() throws Exception {
		System.out.println("Test: When fixed attributes' salience is very high\n" +
		"     the energy should go DOWN (more negative)");
		//there is an effect on energy, but it's hard to predict: go up because of hardness of maintaining ties
		//or go down because saliance is a factor in the energy function

		System.out.println();

		NewModel baselineModel
		= (NewModel) baselineData.get("default").get("model");

		//input conditions
		Hashtable <String,Double> inputParams = copyInputParams("default");
		inputParams.put("fixedAttribSalience",   new Double(baselineModel.getFixedAttribSalience()*10));

		//execution
		NewModel model = runSim(inputParams);


		assertLess(model.getAvgStat("energy"), 1.1*baselineModel.getAvgStat("energy"));
		System.out.println();
		System.out.println();
	}

	public void testFractionExposedToMagnets() throws Exception {
		System.out.println("Test: When fractionExposedToMagnets is raised\n" +
		"     the avg energy should go down");
		System.out.println();

		NewModel baselineModel = (NewModel) baselineData.get("default").get("model");

		//input conditions
		Hashtable <String,Double> inputParams = copyInputParams("default");
		inputParams.put("fractionExposedToMagnets",   new Double(1.0));

		//execution
		NewModel model = runSim(inputParams);

		assertLess(model.getAvgStat("energy"), 1.1*baselineModel.getAvgStat("energy"));
		System.out.println();
		System.out.println();
	}

	public void testFractionInitPacifists() throws Exception {
		System.out.println("Test: When the fractionInitPacifists is high\n" +
		"     the avg isolation should go up a bit");
		System.out.println();

		NewModel baselineModel = (NewModel) baselineData.get("default").get("model");

		//input conditions
		Hashtable <String,Double> inputParams = copyInputParams("default");
		inputParams.put("fractionInitPacifists",   new Double(baselineModel.getFractionInitPacifists() + 0.5));

		//execution
		NewModel model = runSim(inputParams);

		//assertLess(model.getAvgStat("energy"), 1.05*baselineModel.getAvgStat("energy"));
		assertMore(model.getAvgStat("R-homophilyPin"), 1.05*baselineModel.getAvgStat("R-homophilyPin"));
		System.out.println();
		System.out.println();
	}

	public void testFractionInitRadicals() throws Exception {
		System.out.println("Test: When the fractionInitRadicals is high\n" +
				"     the avg density should go up a bit\n" +
				"     the avg isolation should go up a bit\n" +
		"     the avg energy should go down a bit");
		System.out.println();

		NewModel baselineModel = (NewModel) baselineData.get("default").get("model");

		//input conditions
		Hashtable <String,Double> inputParams = copyInputParams("default");
		inputParams.put("fractionInitRadicals",   new Double(baselineModel.getFractionInitRadicals() + 0.5));

		//execution
		NewModel model = runSim(inputParams);

		//assertLess(model.getAvgStat("energy"), 1.05*baselineModel.getAvgStat("energy"));
		assertMore(model.getAvgStat("R-fraction"), 1.5*baselineModel.getAvgStat("R-fraction"));
		assertMore(model.getAvgStat("R-homophilyPin"), 1.5*baselineModel.getAvgStat("R-homophilyPin"));
		System.out.println();
		System.out.println();
	}

	public void testMagneticEncounterRate() throws Exception {
		System.out.println("Test: When magneticEncounterRate is raised\n" +
		"     the avg energy should go down");
		System.out.println();

		NewModel baselineModel = (NewModel) baselineData.get("highExposure").get("model");

		//input conditions
		Hashtable <String,Double> inputParams = copyInputParams("highExposure");
		inputParams.put("magneticEncounterRate",   
				new Double(baselineModel.getMagneticEncounterRate() + 0.5));

		//execution
		NewModel model = runSim(inputParams);

		assertLess(model.getAvgStat("energy"), 1.1*baselineModel.getAvgStat("energy"));
		System.out.println();
		System.out.println();
	}

	public void testMagnetsN() throws Exception {
		System.out.println("Test: When there are lots of magnets\n" +
				"     the avg energy should go down\n" + 
		"     the avg isolation should go down");
		System.out.println();

		NewModel baselineModel
		= (NewModel) baselineData.get("highExposure").get("model");

		//input conditions
		Hashtable <String,Double> inputParams = copyInputParams("highExposure");
		inputParams.put("numMagnetsN",   new Double(baselineModel.getNumMagnetsN() + 100));

		//execution
		NewModel model = runSim(inputParams);

		assertLess(model.getAvgStat("energy"), 1.1*baselineModel.getAvgStat("energy"));
		assertMore(model.getAvgStat("avgTieStrength"), 1.05*baselineModel.getAvgStat("avgTieStrength"));
		assertMore(model.getAvgStat("R-homophilyPin"), 1.1*baselineModel.getAvgStat("R-homophilyPin"));
		System.out.println();
		System.out.println();
	}

	public void testMagnetsP() throws Exception {
		System.out.println("Test: When there are lots of pacifist magnets\n" +
				"     the avg energy should go up a lot\n" +
		"     the avg tie strength should go up a bit");
		System.out.println();
		//validation
		NewModel baselineModel
		= (NewModel) baselineData.get("highPacifists").get("model");

		//input conditions
		Hashtable <String,Double> inputParams = copyInputParams("highPacifists");
		inputParams.put("numMagnetsP",   new Double(baselineModel.getNumMagnetsP() + 100));

		//execution
		NewModel model = runSim(inputParams);

		assertLess(model.getAvgStat("energy"), 1.1*baselineModel.getAvgStat("energy"));

		assertMore(model.getAvgStat("avgTieStrength"), 1.05*baselineModel.getAvgStat("avgTieStrength"));
		System.out.println();
		System.out.println();
	}



	public void testMagnetsR() throws Exception {
		System.out.println("Test: When there are lots of radical magnets\n" +
				"      isolation should go up a lot\n" +
				"      the avg energy should go down a lot\n" +
		"      the avg medianCellSize should go down a bit");
		System.out.println();
		//validation
		NewModel baselineModel
		= (NewModel) baselineData.get("highRadicals").get("model");

		//input conditions
		Hashtable <String,Double> inputParams = copyInputParams("highExposure");
		inputParams.put("numMagnetsR",   new Double(baselineModel.getNumMagnetsR() + 100));

		//execution
		NewModel model = runSim(inputParams);

		assertLess(model.getAvgStat("energy"), 1.05*baselineModel.getAvgStat("energy"));
		assertMore(model.getAvgStat("R-homophilyPin"), 1.1*baselineModel.getAvgStat("R-homophilyPin"));
		assertMore(model.getAvgStat("R-medianCellSize"), 1.1*baselineModel.getAvgStat("R-medianCellSize"));
		assertMore(model.getAvgStat("R-avgCellSize"), 1.1*baselineModel.getAvgStat("R-avgCellSize")); //unconfirmed
		//only if there are enought radicals:
		//assertMore(model.getAvgStat("avgTieStrength"), 1.1*baselineModel.getAvgStat("avgTieStrength"));
		System.out.println();
		System.out.println();
	}

	public void testMaxLinkAgeBonus() throws Exception {
		/* too hard to predict
  	System.out.println("Test: When the maxLinkAgeBonus is high (hard to form new ties)\n" +
  			                "     the avg density should go not be effected much\n" +
  			                "     the avg isolation should go DOWN a bit\n" +
  			                "     the avg medianCellSize should go DOWN a bit");
  	System.out.println();

  	NewModel baselineModel = (NewModel) baselineData.get("default").get("model");

  	//input conditions
  	Hashtable <String,Double> inputParams = copyInputParams("default");
  	inputParams.put("maxLinkAgeBonus",   new Double(baselineModel.getMaxLinkAgeBonus() + 5));

  	//execution
  	NewModel model = runSim(inputParams);

  	assertLess(Math.abs(model.getAvgStat("R-fraction") - baselineModel.getAvgStat("R-fraction")), 0.1);
  	assertLess(model.getAvgStat("R-homophilyPin"), 0.95*baselineModel.getAvgStat("R-homophilyPin"));  
  	assertLess(model.getAvgStat("R-medianCellSize"), baselineModel.getAvgStat("R-medianCellSize"));
  	//the effect on energy is hard to predict, but in practice it raises it b/c
  	//	good ties are so much harder to build (even though it increases theoretic max weight of a tie)
  	//  similarly, avg tie strength is hard to predict (it's strongly correlated with energy)
  	//assertLess(model.getAvgStat("energy"), 0.9*baselineModel.getAvgStat("energy"));
  	//assertMore(model.getAvgStat("avgTieStrength"), 1.1*baselineModel.getAvgStat("avgTieStrength"));
  	System.out.println();
  	System.out.println();
		 *
		 */
	}

	public void testNumVarFixAttributes() throws Exception {
		System.out.println("Test: When num variable attributes is slightly decreased and\n" +
				"      num fixed attributes is equally increased\n" +
		"      the avg energy should go up");

		System.out.println();

		NewModel baselineModel	= (NewModel) baselineData.get("highPressure").get("model");

		//input conditions
		Hashtable <String,Double> inputParams = copyInputParams("highPressure");
		inputParams.put("numVarAttributes",   new Double(baselineModel.getNumVarAttributes()-4));
		inputParams.put("numFixedAttributes", new Double(baselineModel.getNumFixedAttributes()+2));
		//note: default fixed var attrib salience is 2

		//execution
		NewModel model = runSim(inputParams);

		assertMore(model.getAvgStat("energy"), baselineModel.getAvgStat("energy"));
		//assertLess(model.getAvgStat("R-homophilyPin"), baselineModel.getAvgStat("R-homophilyPin"));
		System.out.println();
		System.out.println();
	}

	public void testPopulation() throws Exception {
		System.out.println("Test: When population is doubled\n" +
		"     the avg energy should double as well");
		System.out.println();

		NewModel baselineModel = (NewModel) baselineData.get("default").get("model");

		//input conditions
		Hashtable <String,Double> inputParams = copyInputParams("default");
		inputParams.put("population",   new Double(baselineModel.getPopulation() * 2));

		//execution
		NewModel model = runSim(inputParams);

		assertLess(model.getAvgStat("energy"), 1.9*baselineModel.getAvgStat("energy"));
		System.out.println();
		System.out.println();
	}

	public void testRadicalsAttritionIncrement() throws Exception {
		System.out.println("Test: When the radical attrition rate is high\n" +
				//		                "     the avg energy should go UP a bit (since they are only a small fraction of pop)\n"+
				"     the avg isolation should go DOWN a LOT\n"+
		"     the avg R-medianCellSize should go DOWN a LOT");
		System.out.println();

		NewModel baselineModel = (NewModel) baselineData.get("default").get("model");

		//input conditions
		Hashtable <String,Double> inputParams = copyInputParams("default");
		inputParams.put("radicalsAttritionIncrement",   new Double(baselineModel.getRadicalsAttritionIncrement()+0.5));

		//execution
		NewModel model = runSim(inputParams);

		//assertMore(model.getAvgStat("energy"), 0.95*baselineModel.getAvgStat("energy"));
		assertLess(model.getAvgStat("R-homophilyPin"), 0.5*baselineModel.getAvgStat("R-homophilyPin"));
		assertLess(model.getAvgStat("R-medianCellSize"), 2*baselineModel.getAvgStat("R-medianCellSize"));
		assertLess(model.getAvgStat("R-avgCellSize"), 2*baselineModel.getAvgStat("R-avgCellSize"));
		System.out.println();
		System.out.println();
	}

	public void testRunLength() throws Exception {
		System.out.println("Test: Setting the run length to a small value should dramatically raise the energy\n" +
				"     the avg isolation should go DOWN considerably\n" +
		"     the avg medianCellSize should go DOWN considerably");
		System.out.println();
		//input conditions
		Hashtable <String,Double> inputParams = copyInputParams("default");
		inputParams.put("runLength", new Double(3));

		//execution
		NewModel model = runSim(inputParams);

		NewModel baselineModel = (NewModel) baselineData.get("default").get("model");

		assertMore(model.getAvgStat("energy"), 0.2*baselineModel.getAvgStat("energy"));
		assertLess(model.getAvgStat("R-homophilyPin"), 0.5*baselineModel.getAvgStat("R-homophilyPin"));
		//assertLess(model.getAvgStat("R-medianCellSize"), 0.8*baselineModel.getAvgStat("R-medianCellSize"));
		System.out.println();
		System.out.println();
	}
	public void testTransitiveFriendship() throws Exception {
		System.out.println("Test: When the transitiveFriendship is high\n" +
				"     the avg clustering should go up a bit\n" +
		"     the avg energy should go down a bit");
		System.out.println();

		NewModel baselineModel = (NewModel) baselineData.get("default").get("model");

		//input conditions
		Hashtable <String,Double> inputParams = copyInputParams("default");
		inputParams.put("transitiveFriendship",   new Double(baselineModel.getTransitiveFriendship() + 1.0));

		//execution
		NewModel model = runSim(inputParams);

		assertLess(model.getAvgStat("energy"), 1.05*baselineModel.getAvgStat("energy"));
		assertMore(model.getAvgStat("R-clusteringSoffer"), 1.1*baselineModel.getAvgStat("R-clusteringSoffer"));
		System.out.println();
		System.out.println();
	}

	public void testZealSalience() throws Exception {
		System.out.println("Test: When zeal salience is very high\n" +
				"     avg isolation should increase\n" +
		"     avg energy should go more negative too");
		System.out.println();

		NewModel baselineModel
		= (NewModel) baselineData.get("default").get("model");

		//input conditions
		Hashtable <String,Double> inputParams = copyInputParams("default");
		inputParams.put("zealSalience",   new Double(baselineModel.getZealSalience()*10));

		//execution
		NewModel model = runSim(inputParams);

		assertMore(model.getAvgStat("R-homophilyPin"), 0.5*baselineModel.getAvgStat("R-homophilyPin"));
		//assertMore(model.getAvgStat("R-medianCellSize"), 2*baselineModel.getAvgStat("R-medianCellSize"));
		assertLess(model.getAvgStat("energy"), 1.1*baselineModel.getAvgStat("energy"));
		System.out.println();
		System.out.println();
	}
}
