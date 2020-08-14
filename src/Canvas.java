import com.interactivemesh.jfx.importer.ImportException;
import com.interactivemesh.jfx.importer.ModelImporter;
import com.interactivemesh.jfx.importer.col.ColModelImporter;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.PerspectiveCamera;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ColorPicker;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class Canvas extends Application {
    private static final int WIDTH = 1400;
    private static final int HEIGHT = 800;
    private javafx.scene.canvas.Canvas drawingCanvas;
    private GraphicsContext gc;
    private ColorPicker colour = new ColorPicker();
    private static int trashCount = 0;
    private Group parentGroup = new Group();
    private Group menuObjects = new Group();
    private Group trash = new Group();

    private EventHandler<MouseEvent> drag = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {
            Node obj = (Node) event.getSource();
            obj.setTranslateX(event.getSceneX());
            obj.setTranslateY(event.getSceneY());
        }
    };

    private EventHandler<MouseEvent> createTrash = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {
            Node source = (Node) event.getSource();
            Node duplicate = importModel(source.getId());

            duplicate.getTransforms().setAll(source.getTransforms());

            duplicate.setId(source.getId());
            source.setId(duplicate.getId() + trashCount);

            duplicate.setTranslateX(source.getTranslateX());
            duplicate.setTranslateY(source.getTranslateY());

            source.removeEventHandler(MouseDragEvent.MOUSE_PRESSED, createTrash);
            source.addEventHandler(MouseDragEvent.MOUSE_DRAGGED, drag);
            duplicate.addEventHandler(MouseDragEvent.MOUSE_PRESSED, createTrash);

            menuObjects.getChildren().remove(source);
            menuObjects.getChildren().add(duplicate);
            trash.getChildren().add(source);
            trashCount++;

        }
    };

    private EventHandler<MouseEvent> startDraw = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {
            gc.beginPath();
        }
    };
    private EventHandler<MouseEvent> draw = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) { // TODO: Adjust for multitouch
            if(colour == null) {
                gc.setStroke(Color.BLACK);
            } // TODO: Add for colour input
            gc.setLineJoin(StrokeLineJoin.ROUND);
            gc.setLineWidth(3);
            gc.lineTo(event.getSceneX(), event.getSceneY());
            gc.stroke();
        }
    };

    private EventHandler<MouseEvent> endDraw = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {
            gc.closePath();
        }
    };

    private EventHandler<TouchEvent> changeColour = new EventHandler<TouchEvent>() {
        @Override
        public void handle(TouchEvent event) { // TODO: Adjust for multitouch
            colour = new ColorPicker();
            colour.relocate(event.getTouchPoint().getSceneX(), event.getTouchPoint().getSceneY() + 10);
            colour.toFront();
            colour.setVisible(true);
            parentGroup.getChildren().add(colour);
        }
    };

    private EventHandler<GestureEvent> rotate = new EventHandler<GestureEvent>() {
        @Override
        public void handle(GestureEvent event) {

        }
    };

    @Override
    public void start(Stage primaryStage) {
        // Set up canvas
        drawingCanvas = new javafx.scene.canvas.Canvas(WIDTH, HEIGHT);
        gc = drawingCanvas.getGraphicsContext2D();

        // Set up objects by importing models and adding them to the group
        readObjectDetails();

        parentGroup.getChildren().add(drawingCanvas);
        parentGroup.getChildren().add(menuObjects);
        parentGroup.getChildren().add(trash);

        // define handlers for static models (menu objects) and moveable models (trash objects)
        for(Node mo: menuObjects.getChildren()) {
            mo.addEventHandler(MouseDragEvent.MOUSE_PRESSED, createTrash);
        }
        drawingCanvas.addEventHandler(MouseDragEvent.MOUSE_PRESSED, startDraw);
        drawingCanvas.addEventHandler(MouseDragEvent.MOUSE_DRAGGED, draw);
        drawingCanvas.addEventHandler(MouseDragEvent.MOUSE_RELEASED, endDraw);
        drawingCanvas.addEventHandler(TouchEvent.TOUCH_MOVED, changeColour);

        Camera camera = new PerspectiveCamera();
        Scene scene = new Scene(parentGroup, WIDTH, HEIGHT);

        scene.setFill(Color.WHITE);
        scene.setCamera(camera);

        primaryStage.setTitle("Canvas");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public List<Object> readObjectDetails() {
        ArrayList<Object> objects = new ArrayList<>();
        try {
            File objFile = new File("Objects/objectList");
            Scanner fileReader = new Scanner(objFile);
            String line;
            while(fileReader.hasNextLine()) {
                line = fileReader.nextLine();
                String[] spiltStr = line.split(",");
                Node mesh = importModel(spiltStr[0]);
                applyTransformations(mesh, spiltStr[1], Double.parseDouble(spiltStr[2]), Double.parseDouble(spiltStr[3]), Double.parseDouble(spiltStr[4]), Double.parseDouble(spiltStr[5]));
                mesh.setId(spiltStr[0]);
                menuObjects.getChildren().add(mesh);
            }
            fileReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("Object list could not be found");
        }
        return objects;
    }

    private Node applyTransformations(Node mesh, String rotationAxis, double angle, double scale, double x, double y) {
        switch (rotationAxis) {
            case "X":
                mesh.getTransforms().add(new Rotate(angle, Rotate.X_AXIS));
                break;
            case "Y":
                mesh.getTransforms().add(new Rotate(angle, Rotate.Y_AXIS));
                break;
            case "Z":
                mesh.getTransforms().add(new Rotate(angle, Rotate.Z_AXIS));
                break;
        }
        mesh.getTransforms().add(new Scale(scale, scale, scale));
        mesh.setTranslateX(x);
        mesh.setTranslateY(y);
        return mesh;
    }

    private Node importModel(String fileName) {
        Node[] importModel = null;
        try {
            ModelImporter model = new ColModelImporter();
            model.read("Objects/" + fileName + ".dae");

            importModel = (Node[]) model.getImport();
            model.close();

        } catch (ImportException e) {
            System.out.println("File cannot be found");
        }
        return importModel[0];
    }

    public static void main(String[] args) {
        launch(args);
    }
}
