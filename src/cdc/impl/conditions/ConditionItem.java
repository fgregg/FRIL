/**
 * 
 */
package cdc.impl.conditions;

import cdc.components.AbstractDistance;
import cdc.datamodel.DataColumnDefinition;

public class ConditionItem {
	private DataColumnDefinition left;
	private DataColumnDefinition right;
	private AbstractDistance distanceFunction;
	private int weight;
	private double emptyMatchScore = 0;
	
	public ConditionItem(DataColumnDefinition left, DataColumnDefinition right, AbstractDistance distance, int weight) {
		this.left = left;
		this.right = right;
		this.distanceFunction = distance;
		this.weight = weight;
	}

	public DataColumnDefinition getLeft() {
		return left;
	}

	public DataColumnDefinition getRight() {
		return right;
	}

	public AbstractDistance getDistanceFunction() {
		return distanceFunction;
	}
	
	public int getWeight() {
		return weight;
	}
	
	public String toString() {
		return "Left column=" + left + "; right column=" + right + "; " + distanceFunction.toString();
	}

	public void setEmptyMatchScore(double score) {
		emptyMatchScore = score;
	}
	
	public double getEmptyMatchScore() {
		return emptyMatchScore;
	}
}