package cdc.components;

public class CompoundCondition implements Condition {
	
	public static final int AND = 1;
	public static final int OR = 2;
	
	private Condition[] children;
	private int[] operators;
	
	public CompoundCondition(Condition[] children, int[] operators) {
		this.children = children;
		this.operators = operators;
	}
	
	public Condition[] getChildren() {
		return children;
	}
	
	public int[] getOperators() {
		return operators;
	}
	
}
