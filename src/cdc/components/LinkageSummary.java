package cdc.components;

public class LinkageSummary {
	
	private int cntReadSrcA;
	private int cntReadSrcB;
	
	private int cntLinked;
	
	public LinkageSummary(int cntReadSrcA, int cntReadSrcB, int linked) {
		this.cntReadSrcA = cntReadSrcA;
		this.cntReadSrcB = cntReadSrcB;
		this.cntLinked = linked;
	}

	public int getCntReadSrcA() {
		return cntReadSrcA;
	}

	public int getCntReadSrcB() {
		return cntReadSrcB;
	}

	public int getCntLinked() {
		return cntLinked;
	}
	
	
	
	
}
