package com.almejo.osom.input;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Joypad {

	public static final int BUTTON_A = 0;
	public static final int BUTTON_B = 1;
	public static final int BUTTON_SELECT = 2;
	public static final int BUTTON_START = 3;
	public static final int BUTTON_UP = 4;
	public static final int BUTTON_DOWN = 5;
	public static final int BUTTON_LEFT = 6;
	public static final int BUTTON_RIGHT = 7;

	// Keyboard mapping constants — raw AWT key code integer values (KeyEvent.VK_* equivalents)
	private static final int KEY_UP = 38;         // VK_UP
	private static final int KEY_DOWN = 40;       // VK_DOWN
	private static final int KEY_LEFT = 37;       // VK_LEFT
	private static final int KEY_RIGHT = 39;      // VK_RIGHT
	private static final int KEY_Z = 90;          // VK_Z (A button)
	private static final int KEY_X = 88;          // VK_X (B button)
	private static final int KEY_ENTER = 10;      // VK_ENTER (Start)
	private static final int KEY_BACK_SPACE = 8;  // VK_BACK_SPACE (Select)

	// Button groups ordered by bit position: bit0, bit1, bit2, bit3
	private static final int[] DIRECTION_BUTTONS = {BUTTON_RIGHT, BUTTON_LEFT, BUTTON_UP, BUTTON_DOWN};
	private static final int[] ACTION_BUTTONS = {BUTTON_A, BUTTON_B, BUTTON_SELECT, BUTTON_START};

	private final boolean[] buttonState = new boolean[8];
	private int selectBits = 0x30;

	public void keyPressed(int keyCode) {
		int button = mapKeyToButton(keyCode);
		if (button >= 0) {
			buttonState[button] = true;
			log.debug("Button pressed: button={}, keyCode={}", button, keyCode);
		}
	}

	public void keyReleased(int keyCode) {
		int button = mapKeyToButton(keyCode);
		if (button >= 0) {
			buttonState[button] = false;
			log.debug("Button released: button={}, keyCode={}", button, keyCode);
		}
	}

	public void write(int value) {
		selectBits = value & 0x30;
	}

	public int read() {
		int result = 0xC0 | selectBits;
		int buttonBits = 0x0F;

		if ((selectBits & 0x10) == 0) {
			buttonBits &= readButtonGroup(DIRECTION_BUTTONS);
		}
		if ((selectBits & 0x20) == 0) {
			buttonBits &= readButtonGroup(ACTION_BUTTONS);
		}

		return result | buttonBits;
	}

	public boolean isButtonPressed(int button) {
		if (button < 0 || button >= buttonState.length) {
			return false;
		}
		return buttonState[button];
	}

	private int readButtonGroup(int[] buttons) {
		int bits = 0x0F;
		for (int index = 0; index < buttons.length; index++) {
			if (buttonState[buttons[index]]) {
				bits &= ~(1 << index);
			}
		}
		return bits;
	}

	private int mapKeyToButton(int keyCode) {
		return switch (keyCode) {
			case KEY_UP -> BUTTON_UP;
			case KEY_DOWN -> BUTTON_DOWN;
			case KEY_LEFT -> BUTTON_LEFT;
			case KEY_RIGHT -> BUTTON_RIGHT;
			case KEY_Z -> BUTTON_A;
			case KEY_X -> BUTTON_B;
			case KEY_ENTER -> BUTTON_START;
			case KEY_BACK_SPACE -> BUTTON_SELECT;
			default -> -1;
		};
	}

}
