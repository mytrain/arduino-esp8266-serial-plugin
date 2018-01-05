package org.d2002.esp8266.arduino.serialmonitor;

import java.awt.Component;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ConnectException;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import com.jcraft.jsch.JSchException;

import cc.arduino.packages.BoardPort;
import jssc.SerialPortException;
import processing.app.Base;
import processing.app.Editor;
import processing.app.PreferencesData;
import processing.app.SerialException;
import processing.app.SerialMonitor;
import processing.app.forms.PasswordAuthorizationDialog;
import processing.app.tools.Tool;

public class ESP8266SM implements Tool {

	static final String SERIALMONITOR_FIELDNAME = "serialMonitor";
	static final String UPLOADING_FIELDNAME = "uploading";
	static final String TOOLSMENU_FIELDNAME = "toolsMenu";

	private Editor editor;

	@Override
	public String getMenuTitle() {
		return "ESP8266 Modify serial monitor";
	}

	@Override
	public void init(Editor editor) {
		this.editor = editor;
	}

	public static <T> T getPrivateField(Object owner, String fieldName) {
		try {
			return (T) getPrivateField(owner, fieldName, owner.getClass());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static Object getPrivateField(Object owner, String fieldName,
			Class clazz) {
		try {
			try {
				Field field = clazz.getDeclaredField(fieldName);
				field.setAccessible(true);
				return field.get(owner);
			} catch (NoSuchFieldException e) {
				if (clazz.getSuperclass() == null) {
					throw e;
				}
				return getPrivateField(owner, fieldName, clazz.getSuperclass());
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void setPrivateField(Object owner, String fieldName,
			Object value) {
		try {
			Field field = Editor.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(owner, value);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void run() {
		if (getPrivateField(editor, SERIALMONITOR_FIELDNAME) instanceof ESP8266SerialMonitor) {
			if (serialMonitor.isClosed()) {
				serialMonitor.dispose();
				setPrivateField(editor, SERIALMONITOR_FIELDNAME, null);
			} else {
				return;
			}
		} else {
			if (getPrivateField(editor, SERIALMONITOR_FIELDNAME) != null) {
				JOptionPane
						.showMessageDialog(
								editor,
								"Serial monitor cannot be modified if already started\nRestart the application and click on "
										+ getMenuTitle()
										+ " before opening the serial monitor ",
								getMenuTitle(), JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		// First of all disable normal monitoring
		for (Component menuItem : ((JMenu) getPrivateField(editor,
				TOOLSMENU_FIELDNAME)).getMenuComponents()) {
			if (menuItem instanceof JMenuItem) {
				if (((JMenuItem) menuItem).getText().equals("Serial Monitor")) {
					((JMenuItem) menuItem).setEnabled(false);
				}
			}

		}
		createMonitor();
	}

	private ESP8266SerialMonitor serialMonitor;

	protected void createMonitor() {
		BoardPort port = Base.getDiscoveryManager().find(
				PreferencesData.get("serial.port"));
		if (port == null) {
			editor.statusError(String.format("Board at %s is not available",
					PreferencesData.get("serial.port")));
			return;
		}
		serialMonitor = new ESP8266SerialMonitor(port);
		setPrivateField(editor, SERIALMONITOR_FIELDNAME, serialMonitor);
		serialMonitor.setIconImage(editor.getIconImage());

		if (getPrivateField(editor, UPLOADING_FIELDNAME)) {
			try {
				serialMonitor.suspend();
			} catch (Exception e) {
				editor.statusError(e);
			}
		}

		boolean success = false;
		do {
			if ((serialMonitor.requiresAuthorization())
					&& (!(PreferencesData.has(serialMonitor
							.getAuthorizationKey())))) {
				PasswordAuthorizationDialog dialog = new PasswordAuthorizationDialog(
						editor, "Type board password to access its console");
				dialog.setLocationRelativeTo(editor);
				dialog.setVisible(true);

				if (dialog.isCancelled()) {
					editor.statusNotice("Unable to open serial monitor");
					return;
				}

				PreferencesData.set(serialMonitor.getAuthorizationKey(),
						dialog.getPassword());
			}
			try {
				serialMonitor.open();
				serialMonitor.setVisible(true);
				success = true;
			} catch (ConnectException e) {
				editor.statusError("Unable to connect: is the sketch using the bridge?");
			} catch (JSchException e) {
				editor.statusError("Unable to connect: wrong password?");
			} catch (SerialException e) {
				String errorMessage = e.getMessage();
				if ((e.getCause() != null)
						&& (e.getCause() instanceof SerialPortException)) {
					errorMessage = errorMessage
							+ " ("
							+ ((SerialPortException) e.getCause())
									.getExceptionType() + ")";
				}
				editor.statusError(errorMessage);
				try {
					serialMonitor.close();
				} catch (Exception localException2) {
				}
			} catch (Exception e) {
				editor.statusError(e);
			} finally {
				if ((serialMonitor.requiresAuthorization()) && (!(success))) {
					PreferencesData.remove(serialMonitor.getAuthorizationKey());
				}
			}

			if (!(serialMonitor.requiresAuthorization()))
				return;
		} while (!(success));
	}

}
