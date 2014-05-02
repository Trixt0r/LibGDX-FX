import java.net.URL;

import com.badlogic.gdx.backends.lwjgl.LwjglFXApplication;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;


public class LwjglJavaFX extends Application{
	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage stage) {
		stage.setTitle("JavaFX Window");

		stage.setMinWidth(640);
		stage.setMinHeight(480);

		final Screen screen = Screen.getPrimary();
		final Rectangle2D screenBounds = screen.getVisualBounds();

		if ( screenBounds.getWidth() < stage.getWidth() || screenBounds.getHeight() < stage.getHeight() ) {
			stage.setX(screenBounds.getMinX());
			stage.setY(screenBounds.getMinY());

			stage.setWidth(screenBounds.getWidth());
			stage.setHeight(screenBounds.getHeight());
		}

        BorderPane pane = new BorderPane();
		final URL fxmlURL = getClass().getClassLoader().getResource("mapeditor.fxml");
		final FXMLLoader fxmlLoader = new FXMLLoader(fxmlURL);
		fxmlLoader.setRoot(pane);

		try {
			pane = (BorderPane)fxmlLoader.load();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
			return;
		}

		final Scene scene = new Scene(pane);

		stage.setScene(scene);
		stage.show();
		ImageView glArea = (ImageView) pane.lookup("#glTarget");
		glArea.fitWidthProperty().bind(((AnchorPane)glArea.getParent()).widthProperty());
		glArea.fitHeightProperty().bind(((AnchorPane)glArea.getParent()).heightProperty());
		
		//LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
		//cfg.vSyncEnabled = true;
		new LwjglFXApplication(new TestApp(), glArea);
	}
}
