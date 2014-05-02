/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.backends.lwjgl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.Pool;

/** An implementation of the {@link Input} interface hooking a JavaFX ImageView for input.
 * 
 * @author Trixt0r */
final public class LwjglFXInput implements Input {
	static public float keyRepeatInitialTime = 0.4f;
	static public float keyRepeatTime = 0.1f;

	List<KeyEvent> keyEvents = new ArrayList<KeyEvent>();
	List<TouchEvent> touchEvents = new ArrayList<TouchEvent>();
	boolean mousePressed = false;
	int mouseX, mouseY;
	int deltaX, deltaY;
	int pressedKeys = 0;
	boolean justTouched = false;
	Set<Integer> pressedButtons = new HashSet<Integer>();
	InputProcessor processor;
	char lastKeyCharPressed;
	float keyRepeatTimer;
	long currentEventTimeStamp;
	ImageView target;
	int x,y, lastX, lastY;
	KeyCode lastKeyCode;
	MouseButton lastButton;
	boolean isPressed, hasFocus = false;

	Pool<KeyEvent> usedKeyEvents = new Pool<KeyEvent>(16, 1000) {
		protected KeyEvent newObject () {
			return new KeyEvent();
		}
	};

	Pool<TouchEvent> usedTouchEvents = new Pool<TouchEvent>(16, 1000) {
		protected TouchEvent newObject () {
			return new TouchEvent();
		}
	};

	public LwjglFXInput (ImageView target) {
		this.target = target;
		this.target.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> lastButton = e.getButton());
		this.target.getScene().addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, e ->{
			if(!hasFocus) return;
			lastKeyCode = e.getCode();
			int keyCode = getGdxKeyCode(lastKeyCode);
			char keyChar = e.getText().charAt(0);
			long timeStamp = System.nanoTime();
			KeyEvent event = usedKeyEvents.obtain();
			event.keyCode = keyCode;
			event.keyChar = 0;
			event.type = KeyEvent.KEY_DOWN;
			event.timeStamp = timeStamp;
			keyEvents.add(event);

			event = usedKeyEvents.obtain();
			event.keyCode = 0;
			event.keyChar = keyChar;
			event.type = KeyEvent.KEY_TYPED;
			event.timeStamp = timeStamp;
			keyEvents.add(event);

			pressedKeys++;
			lastKeyCharPressed = keyChar;
			keyRepeatTimer = keyRepeatInitialTime;
		});
		this.target.getScene().addEventHandler(javafx.scene.input.KeyEvent.KEY_RELEASED, e ->{
			if(!hasFocus) return;
			lastKeyCode = null;
			int keyCode = getGdxKeyCode(e.getCode());
			KeyEvent event = usedKeyEvents.obtain();
			event.keyCode = keyCode;
			event.keyChar = 0;
			event.type = KeyEvent.KEY_UP;
			event.timeStamp = System.nanoTime();
			keyEvents.add(event);
			pressedKeys--;
			lastKeyCharPressed = 0;
		});
		
		
		this.target.addEventHandler(MouseEvent.ANY, e -> {
			TouchEvent event = usedTouchEvents.obtain();
			event.x = (int) e.getX();
			event.y = (int) e.getY();
			event.button = toGdxButton(e.getButton());
			event.pointer = 0;
			event.timeStamp = System.nanoTime();
			deltaX = 0; deltaY = 0;
			if (e.getEventType() == MouseEvent.MOUSE_DRAGGED) 
				event.type = TouchEvent.TOUCH_DRAGGED;
			 else if(e.getEventType() == MouseEvent.MOUSE_MOVED) 
				event.type = TouchEvent.TOUCH_MOVED;
			else if (e.getEventType() == MouseEvent.MOUSE_PRESSED) {
				event.type = TouchEvent.TOUCH_DOWN;
				pressedButtons.add(event.button);
				justTouched = true;
			} else if (e.getEventType() == MouseEvent.MOUSE_RELEASED) {
				event.type = TouchEvent.TOUCH_UP;
				pressedButtons.remove(event.button);
			} else return;
			touchEvents.add(event);
			lastX = mouseX;
			lastY = mouseY;
			mouseX = event.x;
			mouseY = event.y;
		});
		
		
		this.target.addEventHandler(ScrollEvent.SCROLL, e -> {
			TouchEvent event = usedTouchEvents.obtain();
			event.x = (int) e.getX();
			event.y = (int) e.getY();
			event.timeStamp = System.nanoTime();
			event.type = TouchEvent.TOUCH_SCROLLED;
			event.scrollAmount = (int)-Math.signum(e.getDeltaY());
			touchEvents.add(event);
		});
	}

	public float getAccelerometerX () {
		return 0;
	}

	public float getAccelerometerY () {
		return 0;
	}

	public float getAccelerometerZ () {
		return 0;
	}

	public void getTextInput (final TextInputListener listener, final String title, final String text) {
		throw new GdxRuntimeException("Not supported");
	}

	public void getPlaceholderTextInput (final TextInputListener listener, final String title, final String placeholder) {
		throw new GdxRuntimeException("Not supported");
	}

	public int getX () {
		return mouseX;
	}

	public int getY () {
		return mouseY;
	}

	public boolean isAccelerometerAvailable () {
		return false;
	}

	public boolean isKeyPressed (int key) {		
		if (key == Input.Keys.ANY_KEY)
			return pressedKeys > 0;
		else
			return getGdxKeyCode(lastKeyCode) == key;
	}

	public boolean isTouched () {
		return target.isPressed();
	}

	public int getX (int pointer) {
		if (pointer > 0)
			return 0;
		else
			return getX();
	}

	public int getY (int pointer) {
		if (pointer > 0)
			return 0;
		else
			return getY();
	}

	public boolean isTouched (int pointer) {
		if (pointer > 0)
			return false;
		else
			return isTouched();
	}

	public boolean supportsMultitouch () {
		return false;
	}

	@Override
	public void setOnscreenKeyboardVisible (boolean visible) {

	}

	@Override
	public void setCatchBackKey (boolean catchBack) {

	}
	void processEvents () {
		isPressed = target.isPressed();
		if(isPressed && !hasFocus){
			hasFocus = true;
			Platform.runLater(() -> {
				target.requestFocus();
			});
		}
		if(!isPressed && hasFocus && target.getScene().getRoot().isPressed()) hasFocus = false;
		synchronized (this) {
			if (processor != null) {
				InputProcessor processor = this.processor;
				int len = keyEvents.size();
				for (int i = 0; i < len; i++) {
					KeyEvent e = keyEvents.get(i);
					currentEventTimeStamp = e.timeStamp;
					switch (e.type) {
					case KeyEvent.KEY_DOWN:
						processor.keyDown(e.keyCode);
						break;
					case KeyEvent.KEY_UP:
						processor.keyUp(e.keyCode);
						break;
					case KeyEvent.KEY_TYPED:
						processor.keyTyped(e.keyChar);
					}
					usedKeyEvents.free(e);
				}

				len = touchEvents.size();
				for (int i = 0; i < len; i++) {
					TouchEvent e = touchEvents.get(i);
					currentEventTimeStamp = e.timeStamp;
					switch (e.type) {
					case TouchEvent.TOUCH_DOWN:
						processor.touchDown(e.x, e.y, e.pointer, e.button);
						break;
					case TouchEvent.TOUCH_UP:
						processor.touchUp(e.x, e.y, e.pointer, e.button);
						break;
					case TouchEvent.TOUCH_DRAGGED:
						processor.touchDragged(e.x, e.y, e.pointer);
						break;
					case TouchEvent.TOUCH_MOVED:
						processor.mouseMoved(e.x, e.y);
						break;
					case TouchEvent.TOUCH_SCROLLED:
						processor.scrolled(e.scrollAmount);
					}
					usedTouchEvents.free(e);
				}
			} else {
				int len = touchEvents.size();
				for (int i = 0; i < len; i++) {
					usedTouchEvents.free(touchEvents.get(i));
				}

				len = keyEvents.size();
				for (int i = 0; i < len; i++) {
					usedKeyEvents.free(keyEvents.get(i));
				}
			}

			keyEvents.clear();
			touchEvents.clear();
			deltaX = mouseX - lastX;
			deltaY = mouseY - lastY;
			lastX = mouseX;
			lastY = mouseY;
			
		}
	}

	public static int getGdxKeyCode (KeyCode code) {
		if(code == null) return Keys.UNKNOWN;
		switch (code) {
		case LEFT_PARENTHESIS:
			return Input.Keys.LEFT_BRACKET;
		case RIGHT_PARENTHESIS:
			return Input.Keys.RIGHT_BRACKET;
		case DEAD_GRAVE:
			return Input.Keys.GRAVE;
		case MULTIPLY:
			return Input.Keys.STAR;
		case NUM_LOCK:
			return Input.Keys.NUM;
		case DECIMAL:
			return Input.Keys.PERIOD;
		case DIVIDE:
			return Input.Keys.SLASH;
		case META:
			return Input.Keys.SYM;
		case AT:
			return Input.Keys.AT;
		case EQUALS:
			return Input.Keys.EQUALS;
		case DIGIT0:
			return Input.Keys.NUM_0;
		case DIGIT1:
			return Input.Keys.NUM_1;
		case DIGIT2:
			return Input.Keys.NUM_2;
		case DIGIT3:
			return Input.Keys.NUM_3;
		case DIGIT4:
			return Input.Keys.NUM_4;
		case DIGIT5:
			return Input.Keys.NUM_5;
		case DIGIT6:
			return Input.Keys.NUM_6;
		case DIGIT7:
			return Input.Keys.NUM_7;
		case DIGIT8:
			return Input.Keys.NUM_8;
		case DIGIT9:
			return Input.Keys.NUM_9;
		case A:
			return Input.Keys.A;
		case B:
			return Input.Keys.B;
		case C:
			return Input.Keys.C;
		case D:
			return Input.Keys.D;
		case E:
			return Input.Keys.E;
		case F:
			return Input.Keys.F;
		case G:
			return Input.Keys.G;
		case H:
			return Input.Keys.H;
		case I:
			return Input.Keys.I;
		case J:
			return Input.Keys.J;
		case K:
			return Input.Keys.K;
		case L:
			return Input.Keys.L;
		case M:
			return Input.Keys.M;
		case N:
			return Input.Keys.N;
		case O:
			return Input.Keys.O;
		case P:
			return Input.Keys.P;
		case Q:
			return Input.Keys.Q;
		case R:
			return Input.Keys.R;
		case S:
			return Input.Keys.S;
		case T:
			return Input.Keys.T;
		case U:
			return Input.Keys.U;
		case V:
			return Input.Keys.V;
		case W:
			return Input.Keys.W;
		case X:
			return Input.Keys.X;
		case Y:
			return Input.Keys.Y;
		case Z:
			return Input.Keys.Z;
		case ALT:
			return Input.Keys.ALT_LEFT;
		case BACK_SLASH:
			return Input.Keys.BACKSLASH;
		case COMMA:
			return Input.Keys.COMMA;
		case LEFT:
			return Input.Keys.DPAD_LEFT;
		case RIGHT:
			return Input.Keys.DPAD_RIGHT;
		case UP:
			return Input.Keys.DPAD_UP;
		case DOWN:
			return Input.Keys.DPAD_DOWN;
		case ENTER:
			return Input.Keys.ENTER;
		case HOME:
			return Input.Keys.HOME;
		case MINUS:
			return Input.Keys.MINUS;
		case PERIOD:
			return Input.Keys.PERIOD;
		case ADD:
			return Input.Keys.PLUS;
		case SEMICOLON:
			return Input.Keys.SEMICOLON;
		case SHIFT:
			return Input.Keys.SHIFT_LEFT;
		case SLASH:
			return Input.Keys.SLASH;
		case SPACE:
			return Input.Keys.SPACE;
		case TAB:
			return Input.Keys.TAB;
		case CONTROL:
			return Input.Keys.CONTROL_LEFT;
		case PAGE_DOWN:
			return Input.Keys.PAGE_DOWN;
		case PAGE_UP:
			return Input.Keys.PAGE_UP;
		case ESCAPE:
			return Input.Keys.ESCAPE;
		case END:
			return Input.Keys.END;
		case INSERT:
			return Input.Keys.INSERT;
		case DELETE:
			return Input.Keys.DEL;
		case SUBTRACT:
			return Input.Keys.MINUS;
		case QUOTE:
			return Input.Keys.APOSTROPHE;
		case F1:
			return Input.Keys.F1;
		case F2:
			return Input.Keys.F2;
		case F3:
			return Input.Keys.F3;
		case F4:
			return Input.Keys.F4;
		case F5:
			return Input.Keys.F5;
		case F6:
			return Input.Keys.F6;
		case F7:
			return Input.Keys.F7;
		case F8:
			return Input.Keys.F8;
		case F9:
			return Input.Keys.F9;
		case F10:
			return Input.Keys.F10;
		case F11:
			return Input.Keys.F11;
		case F12:
			return Input.Keys.F12;
		case COLON:
			return Input.Keys.COLON;
		case NUMPAD0:
			return Input.Keys.NUMPAD_0;
		case NUMPAD1:
			return Input.Keys.NUMPAD_1;
		case NUMPAD2:
			return Input.Keys.NUMPAD_2;
		case NUMPAD3:
			return Input.Keys.NUMPAD_3;
		case NUMPAD4:
			return Input.Keys.NUMPAD_4;
		case NUMPAD5:
			return Input.Keys.NUMPAD_5;
		case NUMPAD6:
			return Input.Keys.NUMPAD_6;
		case NUMPAD7:
			return Input.Keys.NUMPAD_7;
		case NUMPAD8:
			return Input.Keys.NUMPAD_8;
		case NUMPAD9:
			return Input.Keys.NUMPAD_9;
		default:
			return Input.Keys.UNKNOWN;
		}
	}

	public static KeyCode getFXKeyCode (int gdxKeyCode) {
		switch (gdxKeyCode) {
		case Input.Keys.APOSTROPHE:
			return KeyCode.QUOTE;
		case Input.Keys.LEFT_BRACKET:
			return KeyCode.LEFT_PARENTHESIS;
		case Input.Keys.RIGHT_BRACKET:
			return KeyCode.RIGHT_PARENTHESIS;
		case Input.Keys.GRAVE:
			return KeyCode.DEAD_GRAVE;
		case Input.Keys.STAR:
			return KeyCode.MULTIPLY;
		case Input.Keys.NUM:
			return KeyCode.NUM_LOCK;
		case Input.Keys.AT:
			return KeyCode.AT;
		case Input.Keys.EQUALS:
			return KeyCode.EQUALS;
		case Input.Keys.SYM:
			return KeyCode.META;
		case Input.Keys.NUM_0:
			return KeyCode.DIGIT0;
		case Input.Keys.NUM_1:
			return KeyCode.DIGIT1;
		case Input.Keys.NUM_2:
			return KeyCode.DIGIT2;
		case Input.Keys.NUM_3:
			return KeyCode.DIGIT3;
		case Input.Keys.NUM_4:
			return KeyCode.DIGIT4;
		case Input.Keys.NUM_5:
			return KeyCode.DIGIT5;
		case Input.Keys.NUM_6:
			return KeyCode.DIGIT6;
		case Input.Keys.NUM_7:
			return KeyCode.DIGIT7;
		case Input.Keys.NUM_8:
			return KeyCode.DIGIT8;
		case Input.Keys.NUM_9:
			return KeyCode.DIGIT9;
		case Input.Keys.A:
			return KeyCode.A;
		case Input.Keys.B:
			return KeyCode.B;
		case Input.Keys.C:
			return KeyCode.C;
		case Input.Keys.D:
			return KeyCode.D;
		case Input.Keys.E:
			return KeyCode.E;
		case Input.Keys.F:
			return KeyCode.F;
		case Input.Keys.G:
			return KeyCode.G;
		case Input.Keys.H:
			return KeyCode.H;
		case Input.Keys.I:
			return KeyCode.I;
		case Input.Keys.J:
			return KeyCode.J;
		case Input.Keys.K:
			return KeyCode.K;
		case Input.Keys.L:
			return KeyCode.L;
		case Input.Keys.M:
			return KeyCode.M;
		case Input.Keys.N:
			return KeyCode.N;
		case Input.Keys.O:
			return KeyCode.O;
		case Input.Keys.P:
			return KeyCode.P;
		case Input.Keys.Q:
			return KeyCode.Q;
		case Input.Keys.R:
			return KeyCode.R;
		case Input.Keys.S:
			return KeyCode.S;
		case Input.Keys.T:
			return KeyCode.T;
		case Input.Keys.U:
			return KeyCode.U;
		case Input.Keys.V:
			return KeyCode.V;
		case Input.Keys.W:
			return KeyCode.W;
		case Input.Keys.X:
			return KeyCode.X;
		case Input.Keys.Y:
			return KeyCode.Y;
		case Input.Keys.Z:
			return KeyCode.Z;
		case Input.Keys.ALT_LEFT:
			return KeyCode.ALT;
		case Input.Keys.ALT_RIGHT:
			return KeyCode.ALT;
		case Input.Keys.BACKSLASH:
			return KeyCode.BACK_SLASH;
		case Input.Keys.COMMA:
			return KeyCode.COMMA;
		case Input.Keys.FORWARD_DEL:
			return KeyCode.DELETE;
		case Input.Keys.DPAD_LEFT:
			return KeyCode.LEFT;
		case Input.Keys.DPAD_RIGHT:
			return KeyCode.RIGHT;
		case Input.Keys.DPAD_UP:
			return KeyCode.UP;
		case Input.Keys.DPAD_DOWN:
			return KeyCode.DOWN;
		case Input.Keys.ENTER:
			return KeyCode.ENTER;
		case Input.Keys.HOME:
			return KeyCode.HOME;
		case Input.Keys.END:
			return KeyCode.END;
		case Input.Keys.PAGE_DOWN:
			return KeyCode.PAGE_DOWN;
		case Input.Keys.PAGE_UP:
			return KeyCode.PAGE_UP;
		case Input.Keys.INSERT:
			return KeyCode.INSERT;
		case Input.Keys.MINUS:
			return KeyCode.MINUS;
		case Input.Keys.PERIOD:
			return KeyCode.PERIOD;
		case Input.Keys.PLUS:
			return KeyCode.ADD;
		case Input.Keys.SEMICOLON:
			return KeyCode.SEMICOLON;
		case Input.Keys.SHIFT_LEFT:
			return KeyCode.SHIFT;
		case Input.Keys.SHIFT_RIGHT:
			return KeyCode.SHIFT;
		case Input.Keys.SLASH:
			return KeyCode.SLASH;
		case Input.Keys.SPACE:
			return KeyCode.SPACE;
		case Input.Keys.TAB:
			return KeyCode.TAB;
		case Input.Keys.DEL:
			return KeyCode.DELETE;
		case Input.Keys.CONTROL_LEFT:
			return KeyCode.CONTROL;
		case Input.Keys.CONTROL_RIGHT:
			return KeyCode.CONTROL;
		case Input.Keys.ESCAPE:
			return KeyCode.ESCAPE;
		case Input.Keys.F1:
			return KeyCode.F1;
		case Input.Keys.F2:
			return KeyCode.F2;
		case Input.Keys.F3:
			return KeyCode.F3;
		case Input.Keys.F4:
			return KeyCode.F4;
		case Input.Keys.F5:
			return KeyCode.F5;
		case Input.Keys.F6:
			return KeyCode.F6;
		case Input.Keys.F7:
			return KeyCode.F7;
		case Input.Keys.F8:
			return KeyCode.F8;
		case Input.Keys.F9:
			return KeyCode.F9;
		case Input.Keys.F10:
			return KeyCode.F10;
		case Input.Keys.F11:
			return KeyCode.F11;
		case Input.Keys.F12:
			return KeyCode.F12;
		case Input.Keys.COLON:
			return KeyCode.COLON;
		case Input.Keys.NUMPAD_0:
			return KeyCode.NUMPAD0;
		case Input.Keys.NUMPAD_1:
			return KeyCode.NUMPAD1;
		case Input.Keys.NUMPAD_2:
			return KeyCode.NUMPAD2;
		case Input.Keys.NUMPAD_3:
			return KeyCode.NUMPAD3;
		case Input.Keys.NUMPAD_4:
			return KeyCode.NUMPAD4;
		case Input.Keys.NUMPAD_5:
			return KeyCode.NUMPAD5;
		case Input.Keys.NUMPAD_6:
			return KeyCode.NUMPAD6;
		case Input.Keys.NUMPAD_7:
			return KeyCode.NUMPAD7;
		case Input.Keys.NUMPAD_8:
			return KeyCode.NUMPAD8;
		case Input.Keys.NUMPAD_9:
			return KeyCode.NUMPAD9;
		default:
			return KeyCode.ACCEPT;
		}
	}


	public static int toGdxButton (MouseButton button) {
		if (button == MouseButton.PRIMARY) return Buttons.LEFT;
		if (button == MouseButton.SECONDARY) return Buttons.RIGHT;
		if (button == MouseButton.MIDDLE) return Buttons.MIDDLE;
		return Buttons.LEFT;
	}

	@Override
	public void setInputProcessor (InputProcessor processor) {
		this.processor = processor;
	}

	@Override
	public InputProcessor getInputProcessor () {
		return this.processor;
	}

	@Override
	public void vibrate (int milliseconds) {
	}

	@Override
	public boolean justTouched () {
		return justTouched;
	}

	public static MouseButton toLwjglButton (int button) {
		switch (button) {
		case Buttons.LEFT:
			return MouseButton.PRIMARY;
		case Buttons.RIGHT:
			return MouseButton.SECONDARY;
		case Buttons.MIDDLE:
			return MouseButton.MIDDLE;
		}
		return MouseButton.NONE;
	}

	@Override
	public boolean isButtonPressed (int button) {
		return target.isPressed() && lastButton == toLwjglButton(button);
	}

	@Override
	public void vibrate (long[] pattern, int repeat) {
	}

	@Override
	public void cancelVibrate () {
	}

	@Override
	public float getAzimuth () {
		return 0;
	}

	@Override
	public float getPitch () {
		return 0;
	}

	@Override
	public float getRoll () {
		return 0;
	}

	@Override
	public boolean isPeripheralAvailable (Peripheral peripheral) {
		if (peripheral == Peripheral.HardwareKeyboard) return true;
		return false;
	}

	@Override
	public int getRotation () {
		return 0;
	}

	@Override
	public Orientation getNativeOrientation () {
		return Orientation.Landscape;
	}

	@Override
	public void setCursorCatched (boolean catched) {
		//Mouse.setGrabbed(catched);
	}

	@Override
	public boolean isCursorCatched () {
		return false;
	}

	@Override
	public int getDeltaX () {
		return deltaX;
	}

	@Override
	public int getDeltaX (int pointer) {
		if (pointer == 0)
			return deltaX;
		else
			return 0;
	}

	@Override
	public int getDeltaY () {
		return -deltaY;
	}

	@Override
	public int getDeltaY (int pointer) {
		if (pointer == 0)
			return -deltaY;
		else
			return 0;
	}

	@Override
	public void setCursorPosition (int x, int y) {
		//TODO
	}

  @Override
  public void setCursorImage(Pixmap pixmap, int xHotspot, int yHotspot) {
	  throw new GdxRuntimeException("Not supported yet!");
  }

  @Override
	public void setCatchMenuKey (boolean catchMenu) {
	}

	@Override
	public long getCurrentEventTime () {
		return currentEventTimeStamp;
	}

	@Override
	public void getRotationMatrix (float[] matrix) {
		// TODO Auto-generated method stub

	}

	class KeyEvent {
		static final int KEY_DOWN = 0;
		static final int KEY_UP = 1;
		static final int KEY_TYPED = 2;

		long timeStamp;
		int type;
		int keyCode;
		char keyChar;
	}

	class TouchEvent {
		static final int TOUCH_DOWN = 0;
		static final int TOUCH_UP = 1;
		static final int TOUCH_DRAGGED = 2;
		static final int TOUCH_SCROLLED = 3;
		static final int TOUCH_MOVED = 4;

		long timeStamp;
		int type;
		int x;
		int y;
		int scrollAmount;
		int button;
		int pointer;
	}
}
