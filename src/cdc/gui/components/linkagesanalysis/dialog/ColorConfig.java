package cdc.gui.components.linkagesanalysis.dialog;

import java.awt.Color;

public class ColorConfig {
	
	private static final Color ODD_COLOR = new Color(240, 255, 240);
	private static final Color EVEN_COLOR = new Color(255, 255, 240);
	private static final Color DIFF_COLOR = new Color(255, 220, 220);
	private static final Color EDITOR_COLOR = new Color(187, 207, 255);
	private static final Color MOUSE_OVER_COLOR = new Color(199, 207, 255);
	
	private Color oddRowColor;
	private Color evenRowColor;
	private Color diffColor;
	private Color editorColor;
	private Color mouseOverColor;
	
	public ColorConfig(Color oddColor, Color evenColor, Color diffColor2, Color editorColor, Color mouse_over_color2) {
		this.oddRowColor = oddColor;
		this.evenRowColor = evenColor;
		this.diffColor = diffColor2;
		this.editorColor = editorColor;
		this.mouseOverColor = mouse_over_color2;
	}

	public static ColorConfig getDefault() {
		return new ColorConfig(ODD_COLOR, EVEN_COLOR, DIFF_COLOR, EDITOR_COLOR, MOUSE_OVER_COLOR);	
	}
	
	public Color getOddRowColor() {
		return oddRowColor;
	}
	
	public void setOddRowColor(Color oddRowColor) {
		this.oddRowColor = oddRowColor;
	}
	
	public Color getEvenRowColor() {
		return evenRowColor;
	}
	
	public void setEvenRowColor(Color evenRowColor) {
		this.evenRowColor = evenRowColor;
	}
	
	public Color getDiffColor() {
		return diffColor;
	}
	
	public void setDiffColor(Color diffColor) {
		this.diffColor = diffColor;
	}
	
	public Color getEditorColor() {
		return editorColor;
	}
	
	public void setEditorColor(Color editorColor) {
		this.editorColor = editorColor;
	}
	
	public Color getMouseOverColor() {
		return mouseOverColor;
	}
	
	public void setMouseOverColor(Color mouseOverColor) {
		this.mouseOverColor = mouseOverColor;
	}
}
