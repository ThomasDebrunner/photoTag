package com.najiji.photoTag;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.util.StringConverter;

/**
 * Main viewcontroller for controlling the main view
 * @author najiji
 *
 */
public class MainController implements Initializable {

	private static final String DEFAULT_LOCATION = "/";
	
	@FXML private ImageView imageView;
	@FXML private TextField filenameTextField;
	@FXML private DatePicker datePicker;
	
	@FXML private Label currentFilenameLabel;
	@FXML private Label currentDateLabel;
	
	@FXML private Button nextButton;
	@FXML private Button previousButton;
	@FXML private Button saveButton;
	@FXML private Button openButton;
	
	// the current file
	private int currentIndex;
	private File[] files;
	
	
	
	/**
	 * Gets called as soon as the thing gets initialized
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		imageView.fitWidthProperty().bind(((VBox)imageView.getParent()).widthProperty());
		
		/*
		 * Add stringconverter to the date input
		 */
		datePicker.setConverter(new StringConverter<LocalDate>(){
			private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

			@Override
			public String toString(LocalDate date) {
				return dateTimeFormatter.format(date);
			}

			@Override
			public LocalDate fromString(String string) {
				if(string==null || string.trim().isEmpty())
		        {
		            return null;
		        }
		        return LocalDate.parse(string,dateTimeFormatter);
			}
			
		});
	}
	
	
	/**
	 * gets called as soon as the user clicks the open button
	 * @param event
	 */
	@FXML
	private void openButtonAction(ActionEvent event) {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setInitialDirectory(new File(DEFAULT_LOCATION));
		chooser.setTitle("Choose directory");
		File directory = chooser.showDialog(null);
		if(directory != null && directory.exists()) {
			files = directory.listFiles();
			currentIndex = 0;
			displayFile();
		}
	}
	
	/**
	 * gets called as soon as the user clicks the next button
	 * @param event
	 */
	@FXML
	public void nextButtonAction(ActionEvent event) {
		if(files == null || files.length == 0) return;
		
		String textFieldString = filenameTextField.getText();
		Pattern pattern = Pattern.compile("\\(([0-9]+)\\)$");
		Matcher matcher = pattern.matcher(textFieldString);
		
		if(matcher.find()) {
			int value = Integer.parseInt(matcher.group(1));
			value++;
			filenameTextField.setText(matcher.replaceFirst("("+Integer.toString(value)+")"));
		}
		
		currentIndex++;
		if(currentIndex >= files.length) {
			currentIndex = 0;
		}
		displayFile();
	}
	
	/**
	 * gets called as soon as the user clicks the previous button
	 * @param event
	 */
	@FXML
	public void previousButtonAction(ActionEvent event) {
		if(files == null || files.length == 0) return;
		
		String textFieldString = filenameTextField.getText();
		Pattern pattern = Pattern.compile("\\(([0-9]+)\\)$");
		Matcher matcher = pattern.matcher(textFieldString);
		
		if(matcher.find()) {
			int value = Integer.parseInt(matcher.group(1));
			value--;
			filenameTextField.setText(matcher.replaceFirst("("+Integer.toString(value)+")"));
		}
		
		
		currentIndex--;
		if(currentIndex < 0) {
			currentIndex = files.length -1;
		}
		displayFile();
	}
	
	/**
	 * Gets called as soon as the user clicks the save button
	 * @param event
	 */
	@FXML
	private void saveButtonAction(ActionEvent event) {
		if(files == null || files.length == 0) return;
				
		
		if(filenameTextField.getText() != null && !filenameTextField.getText().isEmpty()) {
			Path source = files[currentIndex].toPath();
			try {
				String newFilename = filenameTextField.getText()+".jpg";
				
				Files.move(source, source.resolveSibling(newFilename));
				files[currentIndex] = new File(files[currentIndex].getParent()+"/"+newFilename);
				displayFile(); 
			}
			catch(FileAlreadyExistsException e) {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setHeaderText("File already exists");
				alert.showAndWait();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if(datePicker.getValue() != null){
			
			Date newDate = Date.from(datePicker.getValue().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
			try {
				ExifManager.changeExifDate(files[currentIndex], newDate);
				displayFile();
				
			} catch (ImageReadException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ImageWriteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
		
	}
	
	
	/**
	 * Displays the i-th file
	 * @param i
	 */
	private void displayFile() {
		if(files == null || files.length == 0) return;
		if(currentIndex >= files.length) {
			currentIndex = 0;
		}
		
		if(currentIndex < 0) {
			currentIndex = files.length -1;
		}
		
		// set image to view
		try {
			imageView.setImage(new Image(new FileInputStream(files[currentIndex])));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		
		Date exifDate = null;
		try {
			exifDate = ExifManager.getExifDate(files[currentIndex]);
		} catch (ImageReadException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// set old labels
		currentFilenameLabel.setText(files[currentIndex].getName());
		currentDateLabel.setText(exifDate != null?new SimpleDateFormat("dd.MM.yyyy").format(exifDate) : "N/A");
		
	}
	
	
	
}
