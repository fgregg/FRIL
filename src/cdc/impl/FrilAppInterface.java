package cdc.impl;

import cdc.configuration.ConfiguredSystem;

public interface FrilAppInterface {

	public String getMinusDirectory();
	public void appendLinkageSummary(String text);
	public ConfiguredSystem getJoin();
	
}
