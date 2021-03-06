/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Masahiro Takatsuka*/
package geovista.common.ui;

/* ------------------ Import classes (packages) ------------------- */
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.plaf.metal.MetalSliderUI;

/*
 * ====================================================================
 * Implementation of class MultiSliderUI
 * ====================================================================
 */
/**
 * A Basic L&F implementation of SliderUI.
 * 
 * 
 * @author Masahiro Takatsuka (masa@psu.edu)
 * @see MetalSliderUI
 */

class MultiSliderUI extends MetalSliderUI {
	protected final static Logger logger = Logger.getLogger(MultiSliderUI.class
			.getName());
	transient private int currentIndex = 0;
	transient private boolean isDragging;
	transient private final int[] minmaxIndices = new int[2];

	private Rectangle[] thumbRects = null;
	private int thumbCount;

	/**
	 * ComponentUI Interface Implementation methods
	 */
	public static ComponentUI createUI(JComponent b) {
		return new MultiSliderUI();
	}

	/**
	 * Construct a new MultiSliderUI object.
	 */
	public MultiSliderUI() {
		super();
	}

	int getTrackBuffer() {
		return trackBuffer;
	}

	/**
	 * Sets the number of Thumbs.
	 */
	public void setThumbCount(int count) {
		thumbCount = count;
	}

	/**
	 * Returns the index number of the thumb currently operated.
	 */
	protected int getCurrentIndex() {
		return currentIndex;
	}

	@Override
	public void installUI(JComponent c) {
		thumbRects = new Rectangle[thumbCount];
		for (int i = 0; i < thumbCount; i++) {
			thumbRects[i] = new Rectangle();
		}
		currentIndex = 0;
		if (thumbCount > 0) {
			thumbRect = thumbRects[currentIndex];
		}
		super.installUI(c);
	}

	@Override
	public void uninstallUI(JComponent c) {
		super.uninstallUI(c);
		for (int i = 0; i < thumbCount; i++) {
			thumbRects[i] = null;
		}
		thumbRects = null;
	}

	@Override
	protected void installListeners(JSlider slider) {
		slider.addMouseListener(trackListener);
		slider.addMouseMotionListener(trackListener);
		slider.addFocusListener(focusListener);
		slider.addComponentListener(componentListener);
		slider.addPropertyChangeListener(propertyChangeListener);
		for (int i = 0; i < thumbCount; i++) {
			((MultiSlider) slider).getModelAt(i).addChangeListener(
					changeListener);
		}
	}

	@Override
	protected void uninstallListeners(JSlider slider) {
		slider.removeMouseListener(trackListener);
		slider.removeMouseMotionListener(trackListener);
		slider.removeFocusListener(focusListener);
		slider.removeComponentListener(componentListener);
		slider.removePropertyChangeListener(propertyChangeListener);
		for (int i = 0; i < thumbCount; i++) {
			BoundedRangeModel model = ((MultiSlider) slider).getModelAt(i);
			if (model != null) {
				model.removeChangeListener(changeListener);
			}
		}
	}

	@Override
	protected void calculateThumbSize() {
		Dimension size = getThumbSize();
		for (int i = 0; i < thumbCount; i++) {
			thumbRects[i].setSize(size.width, size.height);
		}
		thumbRect.setSize(size.width, size.height);
	}

	@Override
	protected void calculateThumbLocation() {
		MultiSlider slider = (MultiSlider) this.slider;
		int majorTickSpacing = slider.getMajorTickSpacing();
		int minorTickSpacing = slider.getMinorTickSpacing();
		int tickSpacing = 0;

		if (minorTickSpacing > 0) {
			tickSpacing = minorTickSpacing;
		} else if (majorTickSpacing > 0) {
			tickSpacing = majorTickSpacing;
		}
		for (int i = 0; i < thumbCount; i++) {
			if (slider.getSnapToTicks()) {
				int sliderValue = slider.getValueAt(i);
				int snappedValue = sliderValue;
				if (tickSpacing != 0) {
					// If it's not on a tick, change the value
					if ((sliderValue - slider.getMinimum()) % tickSpacing != 0) {
						float temp = (float) (sliderValue - slider.getMinimum())
								/ (float) tickSpacing;
						int whichTick = Math.round(temp);
						snappedValue = slider.getMinimum()
								+ (whichTick * tickSpacing);
					}

					if (snappedValue != sliderValue) {
						slider.setValueAt(i, snappedValue);
					}
				}
			}

			if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
				int valuePosition = xPositionForValue(slider.getValueAt(i));
				thumbRects[i].x = valuePosition - (thumbRects[i].width / 2);
				thumbRects[i].y = trackRect.y;
			} else {
				int valuePosition = yPositionForValue(slider.getValueAt(i));
				thumbRects[i].x = trackRect.x;
				thumbRects[i].y = valuePosition - (thumbRects[i].height / 2);
			}
		}
	}

	@Override
	public void paint(Graphics g, JComponent c) {
		recalculateIfInsetsChanged();
		recalculateIfOrientationChanged();
		Rectangle clip = g.getClipBounds();

		if (slider.getPaintTrack() && clip.intersects(trackRect)) {
			paintTrack(g);
		}
		if (slider.getPaintTicks() && clip.intersects(tickRect)) {
			paintTicks(g);
		}
		if (slider.getPaintLabels() && clip.intersects(labelRect)) {
			paintLabels(g);
		}
		if (slider.hasFocus() && clip.intersects(focusRect)) {
			paintFocus(g);
		}

		// first paint unfocused thumbs.
		for (int i = 0; i < thumbCount; i++) {
			if (i != currentIndex) {
				if (clip.intersects(thumbRects[i])) {
					thumbRect = thumbRects[i];
					paintThumb(g);
				}
			}
		}
		// then paint currently focused thumb.
		if (clip.intersects(thumbRects[currentIndex])) {
			thumbRect = thumbRects[currentIndex];
			paintThumb(g);
		}
	}

	@Override
	public void paintThumb(Graphics g) {
		super.paintThumb(g);
	}

	@Override
	public void paintTrack(Graphics g) {
		super.paintTrack(g);
	}

	@Override
	public void scrollByBlock(int direction) {
		synchronized (slider) {
			int oldValue = ((MultiSlider) slider).getValueAt(currentIndex);
			int blockIncrement = slider.getMaximum() / 10;
			int delta = blockIncrement
					* ((direction > 0) ? POSITIVE_SCROLL : NEGATIVE_SCROLL);
			((MultiSlider) slider).setValueAt(currentIndex, oldValue + delta);
		}
	}

	@Override
	public void scrollByUnit(int direction) {
		synchronized (slider) {
			int oldValue = ((MultiSlider) slider).getValueAt(currentIndex);
			int delta = 1 * ((direction > 0) ? POSITIVE_SCROLL
					: NEGATIVE_SCROLL);
			((MultiSlider) slider).setValueAt(currentIndex, oldValue + delta);
		}
	}

	@Override
	protected TrackListener createTrackListener(JSlider slider) {
		return new MultiTrackListener();
	}

	/**
	 * Track Listener Class tracks mouse movements.
	 */
	class MultiTrackListener extends BasicSliderUI.TrackListener {
		int _trackTop;
		int _trackBottom;
		int _trackLeft;
		int _trackRight;
		transient private final int[] firstXY = new int[2];

		/**
		 * If the mouse is pressed above the "thumb" component then reduce the
		 * scrollbars value by one page ("page up"), otherwise increase it by
		 * one page. If there is no thumb then page up if the mouse is in the
		 * upper half of the track.
		 */
		@Override
		public void mousePressed(MouseEvent e) {
			int[] neighbours = new int[2];
			boolean bounded = ((MultiSlider) slider).isBounded();
			if (!slider.isEnabled()) {
				return;
			}

			currentMouseX = e.getX();
			currentMouseY = e.getY();
			firstXY[0] = currentMouseX;
			firstXY[1] = currentMouseY;

			slider.requestFocus();
			// Clicked in the Thumb area?
			minmaxIndices[0] = -1;
			minmaxIndices[1] = -1;
			for (int i = 0; i < thumbCount; i++) {
				if (thumbRects[i].contains(currentMouseX, currentMouseY)) {
					if (minmaxIndices[0] == -1) {
						minmaxIndices[0] = i;
						currentIndex = i;
					}
					if (minmaxIndices[1] < i) {
						minmaxIndices[1] = i;
					}
					switch (slider.getOrientation()) {
					case SwingConstants.VERTICAL:
						offset = currentMouseY - thumbRects[i].y;
						break;
					case SwingConstants.HORIZONTAL:
						offset = currentMouseX - thumbRects[i].x;
						break;
					}
					isDragging = true;
					thumbRect = thumbRects[i];
					if (bounded) {
						neighbours[0] = ((i - 1) < 0) ? -1 : (i - 1);
						neighbours[1] = ((i + 1) >= thumbCount) ? -1 : (i + 1);
					} else {
						currentIndex = i;
						((MultiSlider) slider).setValueIsAdjustingAt(i, true);
						neighbours[0] = -1;
						neighbours[1] = -1;
					}
					setThumbBounds(neighbours);
				}
			}
			if (minmaxIndices[0] > -1) {
				return;
			}

			currentIndex = findClosest(currentMouseX, currentMouseY,
					neighbours, -1);
			thumbRect = thumbRects[currentIndex];
			isDragging = false;
			((MultiSlider) slider).setValueIsAdjustingAt(currentIndex, true);

			Dimension sbSize = slider.getSize();
			int direction = POSITIVE_SCROLL;

			switch (slider.getOrientation()) {
			case SwingConstants.VERTICAL:
				if (thumbRect.isEmpty()) {
					int scrollbarCenter = sbSize.height / 2;
					if (!drawInverted()) {
						direction = (currentMouseY < scrollbarCenter)
								? POSITIVE_SCROLL : NEGATIVE_SCROLL;
					} else {
						direction = (currentMouseY < scrollbarCenter)
								? NEGATIVE_SCROLL : POSITIVE_SCROLL;
					}
				} else {
					int thumbY = thumbRect.y;
					if (!drawInverted()) {
						direction = (currentMouseY < thumbY) ? POSITIVE_SCROLL
								: NEGATIVE_SCROLL;
					} else {
						direction = (currentMouseY < thumbY) ? NEGATIVE_SCROLL
								: POSITIVE_SCROLL;
					}
				}
				break;
			case SwingConstants.HORIZONTAL:
				if (thumbRect.isEmpty()) {
					int scrollbarCenter = sbSize.width / 2;
					if (!drawInverted()) {
						direction = (currentMouseX < scrollbarCenter)
								? NEGATIVE_SCROLL : POSITIVE_SCROLL;
					} else {
						direction = (currentMouseX < scrollbarCenter)
								? POSITIVE_SCROLL : NEGATIVE_SCROLL;
					}
				} else {
					int thumbX = thumbRect.x;
					if (!drawInverted()) {
						direction = (currentMouseX < thumbX) ? NEGATIVE_SCROLL
								: POSITIVE_SCROLL;
					} else {
						direction = (currentMouseX < thumbX) ? POSITIVE_SCROLL
								: NEGATIVE_SCROLL;
					}
				}
				break;
			}
			scrollDueToClickInTrack(direction);
			Rectangle r = thumbRect;
			if (!r.contains(currentMouseX, currentMouseY)) {
				if (shouldScroll(direction)) {
					scrollTimer.stop();
					scrollListener.setDirection(direction);
					scrollTimer.start();
				}
			}
		}

		/**
		 * Sets a track bound for th thumb currently operated.
		 */
		private void setThumbBounds(int[] neighbours) {
			int halfThumbWidth = thumbRect.width / 2;
			int halfThumbHeight = thumbRect.height / 2;

			switch (slider.getOrientation()) {
			case SwingConstants.VERTICAL:
				_trackTop = (neighbours[1] == -1) ? trackRect.y
						: thumbRects[neighbours[1]].y + halfThumbHeight;
				_trackBottom = (neighbours[0] == -1) ? trackRect.y
						+ (trackRect.height - 1) : thumbRects[neighbours[0]].y
						+ halfThumbHeight;
				break;
			case SwingConstants.HORIZONTAL:
				_trackLeft = (neighbours[0] == -1) ? trackRect.x
						: thumbRects[neighbours[0]].x + halfThumbWidth;
				_trackRight = (neighbours[1] == -1) ? trackRect.x
						+ (trackRect.width - 1) : thumbRects[neighbours[1]].x
						+ halfThumbWidth;
				break;
			}
		}

		/*
		 * this is a very lazy way to find the closest. One might want to
		 * implement a much faster algorithm.
		 */
		private int findClosest(int x, int y, int[] neighbours, int excluded) {
			int orientation = slider.getOrientation();
			int rightmin = Integer.MAX_VALUE; // for dxw, dy
			int leftmin = -Integer.MAX_VALUE; // for dx, dyh
			int dx = 0;
			int dxw = 0;
			int dy = 0;
			int dyh = 0;
			neighbours[0] = -1; // left
			neighbours[1] = -1; // right
			for (int i = 0; i < thumbCount; i++) {
				if (i == excluded) {
					continue;
				}
				switch (orientation) {
				case SwingConstants.VERTICAL:
					dy = thumbRects[i].y - y;
					dyh = (thumbRects[i].y + thumbRects[i].height) - y;
					if (dyh <= 0) {
						if (dyh > leftmin) { // has to be > and not >=
							leftmin = dyh;
							neighbours[0] = i;
						}
					}
					if (dy >= 0) {
						if (dy <= rightmin) {
							rightmin = dy;
							neighbours[1] = i;
						}
					}
					break;
				case SwingConstants.HORIZONTAL:
					dx = thumbRects[i].x - x;
					dxw = (thumbRects[i].x + thumbRects[i].width) - x;
					if (dxw <= 0) {
						if (dxw >= leftmin) {
							leftmin = dxw;
							neighbours[0] = i;
						}
					}
					if (dx >= 0) {
						if (dx < rightmin) { // has to be < and not <=
							rightmin = dx;
							neighbours[1] = i;
						}
					}
					break;
				}
			}
			int closest = (Math.abs(leftmin) <= Math.abs(rightmin))
					? neighbours[0] : neighbours[1];
			return (closest == -1) ? 0 : closest;
		}

		/**
		 * Set the models value to the position of the top/left of the thumb
		 * relative to the origin of the track.
		 */
		@Override
		public void mouseDragged(MouseEvent e) {
			((MultiSlider) slider)
					.setValueBeforeStateChange(((MultiSlider) slider)
							.getValueAt(currentIndex));
			int thumbMiddle = 0;
			boolean bounded = ((MultiSlider) slider).isBounded();

			if (!slider.isEnabled()) {
				return;
			}

			currentMouseX = e.getX();
			currentMouseY = e.getY();

			if (!isDragging) {
				return;
			}

			switch (slider.getOrientation()) {
			case SwingConstants.VERTICAL:
				int halfThumbHeight = thumbRect.height / 2;
				int thumbTop = e.getY() - offset;
				if (bounded) {
					int[] neighbours = new int[2];
					int idx = -1;

					if (e.getY() - firstXY[1] > 0) {
						idx = minmaxIndices[0];
					} else {
						idx = minmaxIndices[1];
					}
					minmaxIndices[0] = minmaxIndices[1] = idx;
					if (logger.isLoggable(Level.FINEST)) {
						logger.finest("idx = " + idx);
					}
					if (idx == -1) {
						break;
					}
					if (logger.isLoggable(Level.FINEST)) {
						logger.finest("thumbTop = " + thumbTop);
					}
					neighbours[0] = ((idx - 1) < 0) ? -1 : (idx - 1);
					neighbours[1] = ((idx + 1) >= thumbCount) ? -1 : (idx + 1);
					thumbRect = thumbRects[idx];
					currentIndex = idx;
					((MultiSlider) slider).setValueIsAdjustingAt(idx, true);
					setThumbBounds(neighbours);
				}

				thumbTop = Math.max(thumbTop, _trackTop - halfThumbHeight);
				thumbTop = Math.min(thumbTop, _trackBottom - halfThumbHeight);

				setThumbLocation(thumbRect.x, thumbTop);

				thumbMiddle = thumbTop + halfThumbHeight;
				((MultiSlider) slider).setValueAt(currentIndex,
						valueForYPosition(thumbMiddle));
				break;
			case SwingConstants.HORIZONTAL:
				int halfThumbWidth = thumbRect.width / 2;
				int thumbLeft = e.getX() - offset;
				if (bounded) {
					int[] neighbours = new int[2];
					int idx = -1;
					if (e.getX() - firstXY[0] <= 0) {
						idx = minmaxIndices[0];
					} else {
						idx = minmaxIndices[1];
					}
					minmaxIndices[0] = minmaxIndices[1] = idx;
					if (logger.isLoggable(Level.FINEST)) {
						logger.finest("idx = " + idx);
					}
					if (idx == -1) {
						break;
					}
					if (logger.isLoggable(Level.FINEST)) {
						logger.finest("thumbLeft = " + thumbLeft);
					}
					neighbours[0] = ((idx - 1) < 0) ? -1 : (idx - 1);
					neighbours[1] = ((idx + 1) >= thumbCount) ? -1 : (idx + 1);
					thumbRect = thumbRects[idx];
					currentIndex = idx;
					((MultiSlider) slider).setValueIsAdjustingAt(idx, true);
					setThumbBounds(neighbours);
				}

				thumbLeft = Math.max(thumbLeft, _trackLeft - halfThumbWidth);
				thumbLeft = Math.min(thumbLeft, _trackRight - halfThumbWidth);

				setThumbLocation(thumbLeft, thumbRect.y);

				thumbMiddle = thumbLeft + halfThumbWidth;

				((MultiSlider) slider).setValueAt(currentIndex,
						valueForXPosition(thumbMiddle));
				break;
			default:
				return;
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if (!slider.isEnabled()) {
				return;
			}

			offset = 0;
			scrollTimer.stop();

			if (slider.getSnapToTicks()) {
				isDragging = false;
				((MultiSlider) slider).setValueIsAdjustingAt(currentIndex,
						false);
			} else {
				((MultiSlider) slider).setValueIsAdjustingAt(currentIndex,
						false);
				isDragging = false;
			}

			slider.repaint();
		}
	}

	/**
	 * A static version of the above.
	 */
	static class SharedActionScroller extends AbstractAction {
		int _dir;
		boolean _block;

		public SharedActionScroller(int dir, boolean block) {
			_dir = dir;
			_block = block;
		}

		public void actionPerformed(ActionEvent e) {
			JSlider slider = (JSlider) e.getSource();
			MultiSliderUI ui = (MultiSliderUI) slider.getUI();
			if (_dir == NEGATIVE_SCROLL || _dir == POSITIVE_SCROLL) {
				int realDir = _dir;
				if (slider.getInverted()) {
					realDir = _dir == NEGATIVE_SCROLL ? POSITIVE_SCROLL
							: NEGATIVE_SCROLL;
				}
				if (_block) {
					ui.scrollByBlock(realDir);
				} else {
					ui.scrollByUnit(realDir);
				}
			} else {
				if (slider.getInverted()) {
					if (_dir == MIN_SCROLL) {
						((MultiSlider) slider).setValueAt(ui.currentIndex,
								slider.getMaximum());
					} else if (_dir == MAX_SCROLL) {
						((MultiSlider) slider).setValueAt(ui.currentIndex,
								slider.getMinimum());
					}
				} else {
					if (_dir == MIN_SCROLL) {
						((MultiSlider) slider).setValueAt(ui.currentIndex,
								slider.getMinimum());
					} else if (_dir == MAX_SCROLL) {
						((MultiSlider) slider).setValueAt(ui.currentIndex,
								slider.getMaximum());
					}
				}
			}
		}
	}
}
