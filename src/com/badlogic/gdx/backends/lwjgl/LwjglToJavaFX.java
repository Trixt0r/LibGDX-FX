package com.badlogic.gdx.backends.lwjgl;
import static org.lwjgl.opengl.AMDDebugOutput.glDebugMessageCallbackAMD;
import static org.lwjgl.opengl.ARBDebugOutput.glDebugMessageCallbackARB;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL30.GL_MAX_SAMPLES;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.AMDDebugOutputCallback;
import org.lwjgl.opengl.ARBDebugOutputCallback;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.Drawable;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.stream.RenderStream;
import org.lwjgl.util.stream.StreamHandler;
import org.lwjgl.util.stream.StreamUtil;
import org.lwjgl.util.stream.StreamUtil.RenderStreamFactory;


public class LwjglToJavaFX {

	static Drawable drawable;

	private final ConcurrentLinkedQueue<Runnable> pendingRunnables;

	private final Pbuffer pbuffer;
	private final int maxSamples;
	
	private RenderStreamFactory renderStreamFactory;
	private RenderStream renderStream;

	private ImageView  targetView;
	private WritableImage renderImage;

	private int transfersToBuffer = 3;
	private int samples = 1;

	private final AtomicLong snapshotRequest;

	LwjglToJavaFX(final ImageView target) {
		targetView = target;
		target.setScaleY(-1);
		this.pendingRunnables = new ConcurrentLinkedQueue<Runnable>();

		if ( (Pbuffer.getCapabilities() & Pbuffer.PBUFFER_SUPPORTED) == 0 )
			throw new UnsupportedOperationException("Support for pbuffers is required.");

		try {
			pbuffer = new Pbuffer(1, 1, new PixelFormat(), null, null, new ContextAttribs().withDebug(true));
			pbuffer.makeCurrent();
		} catch (LWJGLException e) {
			throw new RuntimeException(e);
		}

		drawable = pbuffer;

		final ContextCapabilities caps = GLContext.getCapabilities();

		if ( caps.OpenGL30 || (caps.GL_EXT_framebuffer_multisample && caps.GL_EXT_framebuffer_blit) )
			maxSamples = glGetInteger(GL_MAX_SAMPLES);
		else
			maxSamples = 1;

		if ( caps.GL_ARB_debug_output )
			glDebugMessageCallbackARB(new ARBDebugOutputCallback());
		else if ( caps.GL_AMD_debug_output )
			glDebugMessageCallbackAMD(new AMDDebugOutputCallback());

		this.renderStreamFactory = StreamUtil.getRenderStreamImplementation();
		this.renderStream = renderStreamFactory.create(getReadHandler(), 1, transfersToBuffer);

		this.snapshotRequest = new AtomicLong();
	}

	public int getMaxSamples() {
		return maxSamples;
	}

	public RenderStreamFactory getRenderStreamFactory() {
		return renderStreamFactory;
	}

	public void setRenderStreamFactory(final RenderStreamFactory renderStreamFactory) {
		pendingRunnables.offer(new Runnable() {
			public void run() {
				if ( renderStream != null )
					renderStream.destroy();

				LwjglToJavaFX.this.renderStreamFactory = renderStreamFactory;

				renderStream = renderStreamFactory.create(renderStream.getHandler(), samples, transfersToBuffer);
			}
		});
	}

	void dispose() {
		renderStream.destroy();
		pbuffer.destroy();
	}

	public void updateSnapshot() {
		snapshotRequest.incrementAndGet();
	}

	public int getTransfersToBuffer() {
		return transfersToBuffer;
	}

	public void setTransfersToBuffer(final int transfersToBuffer) {
		if ( this.transfersToBuffer == transfersToBuffer )
			return;

		this.transfersToBuffer = transfersToBuffer;
		resetStreams();
	}

	public void setSamples(final int samples) {
		if ( this.samples == samples )
			return;

		this.samples = samples;
		resetStreams();
	}

	private void resetStreams() {
		pendingRunnables.offer(new Runnable() {
			public void run() {
				renderStream.destroy();
				renderStream = renderStreamFactory.create(renderStream.getHandler(), samples, transfersToBuffer);
				updateSnapshot();
			}
		});
	}

	private void drainPendingActionsQueue() {
		Runnable runnable;
		while ( (runnable = pendingRunnables.poll()) != null )
			runnable.run();
	}
	
	void begin(){
		drainPendingActionsQueue();
		renderStream.bind();
	}
	
	void end(){
		renderStream.swapBuffers();
	}
	
	private StreamHandler getReadHandler() {
		return new StreamHandler() {

			public int getWidth() {
				return (int)targetView.getFitWidth();
			}

			public int getHeight() {
				return (int)targetView.getFitHeight();
			}

			public void process(final int width, final int height, final ByteBuffer data, final int stride, final Semaphore signal) {
				// This method runs in the background rendering thread
				// TODO: Run setPixels on the PlatformImage in this thread, run pixelsDirty on JFX application thread with runLater.
				Platform.runLater(new Runnable() {
					public void run() {
						try {
							// If we're quitting, discard update
							if ( !targetView.isVisible() )
								return;
							// Detect resize and recreate the image
							if ( renderImage == null || (int)renderImage.getWidth() != width || (int)renderImage.getHeight() != height ) {
								renderImage = new WritableImage(width, height);
								targetView.setImage(renderImage);
							}

							// Upload the image to JavaFX
							renderImage.getPixelWriter().setPixels(0, 0, width, height, javafx.scene.image.PixelFormat.getByteBgraPreInstance(), data, stride);
						} finally {
							// Notify the render thread that we're done processing
							signal.release();
						}
					}
				});
			}
		};
	}
}
