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

import java.awt.Color;

import uchicago.src.sim.gui.DrawableEdge;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.network.DefaultDrawableEdge;
import uchicago.src.sim.network.Node;

public class NewEdge extends DefaultDrawableEdge implements DrawableEdge{

	private int linkAge = 0;
	private double agreement = Double.POSITIVE_INFINITY;  //net agreement between the friends.  
	//Double.POSITIVE_INFINITY means unknown aka dirty

	public NewEdge(Node from, Node to, double s) {
		super(from, to, "");
		strength 	= s;
		linkAge   = 0;
		agreement = Double.POSITIVE_INFINITY;
	}
	public NewEdge(Node from, Node to, double s, double newAgreement) {
		super(from, to, "");
		strength 	= s;
		linkAge   = 0;
		agreement = newAgreement;
	}

	public void agreementSetDirty() {
		agreement = Double.POSITIVE_INFINITY;
	}
	public void draw(SimGraphics g, int fromX, int toX, int fromY, int toY) {
		g.drawDirectedLink(Color.BLACK, fromX, toX, fromY, toY);
	}
	public double getAgreement() {
		if(agreement == Double.POSITIVE_INFINITY) {
			agreement = ((NewNode)from).netAgreement((NewNode)to, this);
			NewEdge sisterEdge = ((NewEdge)((NewNode)from).getEdgesFrom(to).iterator().next());
			sisterEdge.agreement = agreement;
		}
		return agreement;
	}
	public double getStrength() {
		return strength;
	}
	public double getAge() {
		return linkAge;
	}

	public void setStrength(double newS) {
		strength = newS;

		strength = Math.min(1.0,  strength); 
		strength = Math.max(-1.0, strength); 

		agreement = Double.POSITIVE_INFINITY;
		//perhaps make the tie change color based on strength
		//the following code does not work
		//java.awt.Color c = new Color((new Float(1.0/2+strength/2)).floatValue(), 0.0f, (new Float(1.0/2-strength/2)).floatValue());
		//setColor(c);
	}
	public void step() {
		linkAge += 1;
		agreement = Double.POSITIVE_INFINITY;
	}
}
