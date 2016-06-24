package com.creativemd.creativecore.gui.controls.gui;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import com.creativemd.creativecore.client.avatar.Avatar;
import com.creativemd.creativecore.common.utils.ColorUtils;

import net.minecraft.client.gui.FontRenderer;

public abstract class GuiAvatarButton extends GuiAvatarLabel{
	
	public GuiAvatarButton(String caption, int x, int y, Avatar avatar) {
		super(caption, x, y, ColorUtils.WHITE, avatar);
	}
	
	public GuiAvatarButton(String name, String caption, int x, int y, int width, int height, Avatar avatar) {
		super(name, caption, x, y, ColorUtils.WHITE, avatar);
		this.width = width+getContentOffset()*2;
		this.height = height+getContentOffset()*2;
	}
	
	public GuiAvatarButton(String caption, int x, int y, int width, int height, Avatar avatar) {
		this(caption, caption, x, y, width, height, avatar);
	}
	
	@Override
	public boolean hasBorder()
	{
		return true;
	}
	
	@Override
	public boolean hasBackground()
	{
		return true;
	}
	
	@Override
	public ArrayList<String> getTooltip()
	{
		ArrayList<String> tooltip = new ArrayList<String>();
		tooltip.add(caption);
		return tooltip;
	}
}