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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.nio.ByteBuffer;

import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;


public class LwjglFXGraphics extends LwjglGraphics {
	ImageView target;
	LwjglToJavaFX toFX;

	LwjglFXGraphics (LwjglApplicationConfiguration config, ImageView target) {
		super(config);
		this.target = target;
	}
	
	LwjglFXGraphics (ImageView target) {
		this(new LwjglApplicationConfiguration(), target);
	}
	
	@Override
	public int getHeight () {
		return (int) target.getLayoutBounds().getHeight();
	}
	
	@Override
	public int getWidth () {
		return (int) target.getLayoutBounds().getWidth();
	}

	@Override
	void setupDisplay () throws LWJGLException {
		if (canvas != null) {
			Display.setParent(canvas);
		} else {
			if (!setDisplayMode(config.width, config.height, config.fullscreen))
				throw new GdxRuntimeException("Couldn't set display mode " + config.width + "x" + config.height + ", fullscreen: "
					+ config.fullscreen);

			if (config.iconPaths.size > 0) {
				ByteBuffer[] icons = new ByteBuffer[config.iconPaths.size];
				for (int i = 0, n = config.iconPaths.size; i < n; i++) {
					Pixmap pixmap = new Pixmap(Gdx.files.getFileHandle(config.iconPaths.get(i), config.iconFileTypes.get(i)));
					if (pixmap.getFormat() != Format.RGBA8888) {
						Pixmap rgba = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), Format.RGBA8888);
						rgba.drawPixmap(pixmap, 0, 0);
						pixmap = rgba;
					}
					icons[i] = ByteBuffer.allocateDirect(pixmap.getPixels().limit());
					icons[i].put(pixmap.getPixels()).flip();
					pixmap.dispose();
				}
			}
		}
		Display.setInitialBackground(config.initialBackgroundColor.r, config.initialBackgroundColor.g,
			config.initialBackgroundColor.b);

		if (config.x != -1 && config.y != -1) Display.setLocation(config.x, config.y);
		createDisplayPixelFormat();
		config.x = Display.getX();
		config.y = Display.getY();
		initiateGLInstances();
	}

	private void createDisplayPixelFormat () {
		bufferFormat = new BufferFormat(config.r, config.g, config.b, config.a, config.depth, config.stencil, config.samples, false);
		this.toFX = new LwjglToJavaFX(target);
	}
	
	@Override
	public void setTitle(String title){
		Platform.runLater(() -> ((Stage)target.getScene().getWindow()).setTitle(title));
	}
}
