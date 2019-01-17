package com.creativemd.creativecore.common.gui.controls.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.creativemd.creativecore.common.gui.GuiControl;
import com.creativemd.creativecore.common.gui.GuiRenderHelper;
import com.creativemd.creativecore.common.gui.client.style.ColoredDisplayStyle;
import com.creativemd.creativecore.common.gui.client.style.DisplayStyle;
import com.creativemd.creativecore.common.gui.client.style.Style;
import com.creativemd.creativecore.common.gui.container.GuiParent;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlEvent;
import com.creativemd.creativecore.common.utils.math.SmoothValue;
import com.creativemd.creativecore.common.utils.mc.ColorUtils;
import com.creativemd.creativecore.common.utils.type.Pair;
import com.creativemd.creativecore.common.utils.type.PairList;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class GuiTimeline extends GuiParent {
	
	protected double ticksPerPixel;
	protected double basePixelWidth;
	protected static final double maxZoom = 10;
	protected SmoothValue zoom = new SmoothValue(100);
	protected SmoothValue scrollX = new SmoothValue(100);
	protected SmoothValue scrollY = new SmoothValue(100);
	protected int duration;
	
	public DisplayStyle timelineBackground = new ColoredDisplayStyle(255, 255, 255);
	
	public List<TimelineChannel> channels;
	public int sidebarWidth = 50;
	private int channelHeight = 10;
	private int timelineHeight = 11;
	protected KeyControl dragged = null;
	
	public GuiTimeline(String name, int x, int y, int width, int height, int duration, List<TimelineChannel> channels) {
		super(name, x, y, width, height);
		marginWidth = 0;
		this.channels = channels;
		int i = 0;
		for (TimelineChannel channel : channels) {
			channel.index = i;
			for (KeyControl control : channel.controls) {
				adjustKeyPositionY(control);
				addControl(control);
			}
			i++;
		}
		setDuration(duration);
	}
	
	public GuiTimeline setSidebarWidth(int width) {
		this.sidebarWidth = width;
		return this;
	}
	
	public void adjustKeyPositionY(KeyControl control) {
		control.posY = timelineHeight + control.height / 2 + channelHeight * control.channel.index;
	}
	
	public void adjustKeyPositionX(KeyControl control) {
		control.posX = (int) (control.tick * getTickWidth()) - control.width / 2;
	}
	
	public void adjustKeysPositionX() {
		double tickWidth = getTickWidth();
		for (TimelineChannel channel : channels) {
			for (KeyControl control : channel.controls) {
				control.posX = (int) (control.tick * tickWidth) - control.width / 2;
			}
		}
	}
	
	@Override
	protected double getOffsetX() {
		return sidebarWidth - scrollX.current();
	}
	
	@Override
	public void mouseMove(int x, int y, int button) {
		if (dragged != null) {
			if (!movedSelected)
				movedSelected = Math.abs(movedStart - x) > 5;
			
			if (movedSelected) {
				int tick = Math.max(0, getTickAt(x));
				if (dragged.channel.isSpaceFor(dragged, tick)) {
					dragged.tick = tick;
					adjustKeyPositionX(dragged);
				}
			}
		}
		
		super.mouseMove(x, y, button);
	}
	
	private KeyControl selected = null;
	private boolean movedSelected = false;
	private int movedStart = 0;
	
	@Override
	public boolean mousePressed(int x, int y, int button) {
		selected = null;
		boolean result = super.mousePressed(x, y, button);
		for (GuiControl control : controls) {
			if (control instanceof KeyControl && selected != control && ((KeyControl) control).selected) {
				((KeyControl) control).selected = false;
				if (selected == null)
					raiseEvent(new KeyDeselectedEvent((KeyControl) control));
			}
		}
		
		if (!result && selected == null && button == 1) {
			int channel = getChannelAt(y);
			if (channel != -1) {
				int tick = getTickAt(x);
				KeyControl control = channels.get(channel).addKey(tick, channels.get(channel).getValueAt(tick));
				adjustKeyPositionX(control);
				adjustKeyPositionY(control);
				addControl(control);
			}
		}
		
		return result;
	}
	
	@Override
	protected void clickControl(GuiControl control, int x, int y, int button) {
		if (control instanceof KeyControl) {
			
			if (button == 1) {
				((KeyControl) control).removeKey();
				selected = null;
				return;
			}
			
			dragged = (KeyControl) control;
			dragged.selected = true;
			selected = dragged;
			movedSelected = false;
			movedStart = x;
			raiseEvent(new KeySelectedEvent(selected));
		}
		super.clickControl(control, x, y, button);
	}
	
	@Override
	public void mouseReleased(int x, int y, int button) {
		super.mouseReleased(x, y, button);
		if (dragged != null) {
			dragged.channel.movedKey(dragged);
			dragged = null;
		}
	}
	
	public GuiTimeline setDuration(int duration) {
		int useableWidth = width - sidebarWidth - 2 - getContentOffset() * 2;
		ticksPerPixel = (double) duration / useableWidth;
		basePixelWidth = 1D / ticksPerPixel;
		zoom.setStart(0);
		this.duration = duration;
		scrollX.setStart(0);
		scrollY.setStart(0);
		adjustKeysPositionX();
		return this;
	}
	
	public int getDuration() {
		return duration;
	}
	
	@Override
	public boolean mouseScrolled(int x, int y, int scrolled) {
		if (GuiScreen.isShiftKeyDown())
			scrollX.set(MathHelper.clamp(scrollX.aimed() - scrolled * 10, 0, maxScrollX));
		else if (GuiScreen.isCtrlKeyDown())
			scrollY.set(MathHelper.clamp(scrollY.aimed() + scrolled, 0, maxScrollY));
		else {
			int focusedTick = Math.max(0, getTickAtAimed(x));
			zoom.set(MathHelper.clamp(zoom.aimed() + scrolled * basePixelWidth * 2 * Math.max(basePixelWidth * 2, zoom.aimed()) / maxZoom, 0, maxZoom));
			int currentTick = Math.max(0, getTickAtAimed(x));
			double aimedTickWidth = getTickWidthAimed();
			
			double sizeX = aimedTickWidth * duration;
			double maxScrollX = Math.max(0, sizeX - (width - getContentOffset() * 2));
			scrollX.set(MathHelper.clamp(scrollX.aimed() + (focusedTick - currentTick) * aimedTickWidth, 0, maxScrollX));
		}
		return true;
	}
	
	public int getChannelAt(int y) {
		int channel = (y - timelineHeight) / channelHeight;
		if (channel < 0 || channel >= channels.size())
			return -1;
		return channel;
	}
	
	public int getTickAt(int x) {
		if (x <= sidebarWidth)
			return -1;
		return MathHelper.clamp((int) ((x - sidebarWidth + getTickWidth() / 2 + scrollX.current()) / getTickWidth()), 0, (int) duration);
	}
	
	public int getTickAtAimed(int x) {
		if (x <= sidebarWidth)
			return -1;
		return MathHelper.clamp((int) ((x - sidebarWidth + getTickWidth() / 2 + scrollX.aimed()) / getTickWidthAimed()), 0, (int) duration);
	}
	
	private double lastZoom = 0;
	protected double sizeX;
	protected double maxScrollX;
	protected double sizeY;
	protected double maxScrollY;
	
	protected double getTickWidth() {
		return basePixelWidth + zoom.current();
	}
	
	protected double getTickWidthAimed() {
		return basePixelWidth + zoom.aimed();
	}
	
	@Override
	protected void renderContent(GuiRenderHelper helper, Style style, int width, int height) {
		zoom.tick();
		scrollX.tick();
		scrollY.tick();
		
		int usuableWidth = width - sidebarWidth;
		
		double tickWidth = getTickWidth();
		if (lastZoom != zoom.current()) {
			sizeX = tickWidth * duration;
			maxScrollX = Math.max(0, sizeX - usuableWidth);
			lastZoom = zoom.current();
			adjustKeysPositionX();
		}
		
		// Render sidebar
		GlStateManager.pushMatrix();
		timelineBackground.renderStyle(helper, sidebarWidth, height);
		GlStateManager.translate(0, timelineHeight, 0);
		for (int i = 0; i < channels.size(); i++) {
			
			font.drawString(channels.get(i).name, 1, 1, ColorUtils.BLACK);
			GlStateManager.translate(0, channelHeight, 0);
		}
		GlStateManager.popMatrix();
		
		// Render timeline
		GlStateManager.translate(sidebarWidth, 0, 0);
		
		GlStateManager.pushMatrix();
		timelineBackground.renderStyle(helper, width, timelineHeight);
		GlStateManager.translate(0, timelineHeight, 0);
		getStyle().getBorder(this).renderStyle(helper, width, 1);
		
		int ticks = (int) (usuableWidth / tickWidth);
		int area = 5;
		int halfArea = 5;
		int smallestStep = 0;
		while (Math.pow(area, smallestStep) * tickWidth < 3) {
			smallestStep++;
		}
		smallestStep = (int) Math.pow(area, smallestStep);
		
		double stepWidth = tickWidth * smallestStep;
		int stepOffset = (int) (scrollX.current() / stepWidth);
		int stamps = ticks / smallestStep;
		int begin = Math.max(0, stepOffset);
		int end = stepOffset + stamps + 1;
		
		GlStateManager.pushMatrix();
		GlStateManager.translate(-scrollX.current() + begin * stepWidth, -2, 0);
		for (int i = begin; i < end; i++) {
			if (i % halfArea == 0) {
				getStyle().getBorder(this).renderStyle(helper, 1, 2);
				String text = "" + (i * smallestStep);
				font.drawString(text, 0 - font.getStringWidth(text) / 2, -8, ColorUtils.BLACK);
			} else
				getStyle().getBorder(this).renderStyle(helper, 1, 1);
			
			GlStateManager.translate(stepWidth, 0, 0);
		}
		GlStateManager.popMatrix();
		
		GlStateManager.popMatrix();
		
		// Render channels
		GlStateManager.pushMatrix();
		GlStateManager.translate(0, timelineHeight, 0);
		for (int i = 0; i < channels.size(); i++) {
			GlStateManager.translate(0, channelHeight, 0);
			getStyle().getBorder(this).renderStyle(helper, usuableWidth, 1);
		}
		GlStateManager.popMatrix();
		
		// Render scrollbar
		if (maxScrollX > 0) {
			GlStateManager.pushMatrix();
			GlStateManager.translate((scrollX.current() / sizeX) * usuableWidth, height - 2, 0);
			getStyle().getBorder(this).renderStyle(helper, (int) Math.max(1, (1 - (maxScrollX / sizeX)) * usuableWidth), 1);
			GlStateManager.popMatrix();
		}
	}
	
	@Override
	public boolean hasMouseOverEffect() {
		return false;
	}
	
	@Override
	public List<String> getTooltip() {
		Vec3d mouse = getMousePos();
		double x = mouse.x + getOffsetX();
		List<String> lines = new ArrayList<>();
		if (x > sidebarWidth && x < width - getContentOffset() * 2) {
			int channelId = getChannelAt((int) mouse.y);
			if (channelId >= 0) {
				TimelineChannel channel = channels.get(channelId);
				int tick = getTickAt((int) x);
				lines.add("" + tick + ". " + channel.name + ": " + (Math.round(channel.getValueAt(tick) * 100) / 100));
			}
		}
		return lines;
	}
	
	public static class TimelineChannel {
		
		public int index;
		public String name;
		public List<KeyControl> controls = new ArrayList<>();
		
		public TimelineChannel(String name) {
			this.name = name;
		}
		
		public TimelineChannel addKeys(PairList<Integer, Double> keys) {
			if (keys == null || keys.isEmpty())
				return this;
			for (Pair<Integer, Double> pair : keys) {
				addKey(pair.key, pair.value);
			}
			return this;
		}
		
		public KeyControl addKey(int tick, double value) {
			KeyControl control = new KeyControl(this, controls.size(), tick, value);
			for (int i = 0; i < controls.size(); i++) {
				KeyControl other = controls.get(i);
				
				if (other.tick == tick)
					return other;
				
				if (other.tick > tick) {
					controls.add(i, control);
					return control;
				}
			}
			controls.add(control);
			return control;
		}
		
		public void removeKey(KeyControl control) {
			controls.remove(control);
		}
		
		public void movedKey(KeyControl control) {
			Collections.sort(controls);
		}
		
		public boolean isSpaceFor(KeyControl control, int tick) {
			for (int i = 0; i < controls.size(); i++) {
				int otherTick = controls.get(i).tick;
				if (otherTick == tick)
					return false;
				if (otherTick > tick)
					return true;
			}
			return true;
		}
		
		public double getValueAt(int tick) {
			if (controls.isEmpty())
				return 0;
			
			int higher = controls.size();
			for (int i = 0; i < controls.size(); i++) {
				int otherTick = controls.get(i).tick;
				if (otherTick == tick)
					return controls.get(i).value;
				if (otherTick > tick) {
					higher = i;
					break;
				}
			}
			
			if (higher == 0 || higher == controls.size())
				return controls.get(higher == 0 ? 0 : controls.size() - 1).value;
			
			KeyControl before = controls.get(higher - 1);
			KeyControl after = controls.get(higher);
			double percentage = (double) (tick - before.tick) / (after.tick - before.tick);
			return Math.round(((after.value - before.value) * percentage + before.value) * 100) / 100;
		}
		
		public PairList<Integer, Double> getPairs() {
			if (controls.isEmpty())
				return null;
			PairList<Integer, Double> list = new PairList<>();
			for (KeyControl control : controls) {
				list.add(control.tick, control.value);
			}
			return list;
		}
	}
	
	public static class KeyControl extends GuiControl implements Comparable<KeyControl> {
		
		public TimelineChannel channel;
		public int tick;
		public boolean selected = false;
		public double value;
		
		public KeyControl(TimelineChannel channel, int index, int tick, double value) {
			super("" + index + ".", 0, 0, 0, 0);
			this.channel = channel;
			this.rotation = 45;
			this.tick = tick;
			this.value = value;
		}
		
		@Override
		public DisplayStyle getBorderDisplay(DisplayStyle display) {
			if (selected)
				return new ColoredDisplayStyle(40, 40, 140);
			if (isMouseOver())
				return new ColoredDisplayStyle(20, 20, 20);
			return super.getBorderDisplay(display);
		}
		
		@Override
		protected void renderContent(GuiRenderHelper helper, Style style, int width, int height) {
			
		}
		
		@Override
		public boolean mousePressed(int x, int y, int button) {
			return true;
		}
		
		@Override
		public List<String> getTooltip() {
			List<String> tooltip = new ArrayList<>();
			tooltip.add("" + value);
			return tooltip;
		}
		
		public void removeKey() {
			channel.removeKey(this);
			getParent().removeControl(this);
		}
		
		@Override
		public int compareTo(KeyControl o) {
			return Integer.compare(this.tick, o.tick);
		}
	}
	
	public static class KeySelectedEvent extends GuiControlEvent {
		
		public KeySelectedEvent(KeyControl source) {
			super(source);
		}
		
		@Override
		public boolean isCancelable() {
			return false;
		}
		
	}
	
	public static class KeyDeselectedEvent extends GuiControlEvent {
		
		public KeyDeselectedEvent(KeyControl source) {
			super(source);
		}
		
		@Override
		public boolean isCancelable() {
			return false;
		}
		
	}
}
