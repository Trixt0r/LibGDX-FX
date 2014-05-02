import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;

public class TestApp implements ApplicationListener {
	
	private OrthographicCamera camera;
	private ShapeRenderer renderer;
	float deg = 0, x = 0, y = 0;

	@Override
	public void create() {
      float w = Gdx.graphics.getWidth();
      float h = Gdx.graphics.getHeight();

      camera = new OrthographicCamera(w, h);
      renderer = new ShapeRenderer();
	}
	
	@Override
	public void resize(int width, int height) {
		if(camera != null) camera.setToOrtho(false, width, height);
		x = camera.position.x;
		y = camera.position.y;
	}
	@Override
	public void render() {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		
		camera.update();
		renderer.setProjectionMatrix(camera.combined);
		renderer.setColor(Color.RED);
		renderer.begin(ShapeType.Line);
		renderer.line(x, y, x+100f*MathUtils.cos(-deg/10f), y+100f*MathUtils.sin(-deg/10f));
		renderer.end();
		deg++;
		
		Gdx.graphics.setTitle("Fps: "+ Gdx.graphics.getFramesPerSecond());
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void dispose() {
		renderer.dispose();
	}
}
