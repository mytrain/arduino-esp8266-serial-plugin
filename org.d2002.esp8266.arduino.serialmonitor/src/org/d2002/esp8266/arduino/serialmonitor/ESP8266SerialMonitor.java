package org.d2002.esp8266.arduino.serialmonitor;

import java.awt.event.ActionListener;

import cc.arduino.packages.BoardPort;

import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.swing.JButton;
import javax.swing.JCheckBox;

import processing.app.Serial;
import processing.app.SerialMonitor;

public class ESP8266SerialMonitor extends SerialMonitor {

	private final static String  VERSION = "1.0.1";
	private Serial serialCopy;
	protected JCheckBox resetBox;
	protected JCheckBox dtrBox;
	protected JButton btnClear;

	
	
	public ESP8266SerialMonitor(BoardPort port) {
		super(port);

		this.btnClear = new JButton("Clear");
		btnClear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setText("");			
			}
		});
		this.resetBox = new JCheckBox("Reset", true);
		this.resetBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				appendText("<<<<<< ESP8266 FORCING RTS to "+resetBox.isSelected()+" >>>>>\n");
				serialCopy.setRTS(resetBox.isSelected());
			}
		});
		this.dtrBox = new JCheckBox("Programmation", false);
		this.dtrBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				appendText("<<<<<< ESP8266 FORCING DTR to "+dtrBox.isSelected()+" >>>>>\n");
				serialCopy.setDTR(dtrBox.isSelected());
			}
		});

		this.autoscrollBox.getParent().add(this.btnClear);
		this.autoscrollBox.getParent().add(this.resetBox);
		this.autoscrollBox.getParent().add(this.dtrBox);
		appendText("<<<<<< ESP8266 SERIAL MONITOR "+VERSION+" >>>>>\n");

	}

	@Override
	public void open() throws Exception {
		appendText("<<<<<< ESP8266 SERIAL CONNECTION OPENING >>>>>\n");
		super.open();
		serialCopy = ESP8266SM.getPrivateField(this, "serial");
		appendText("<<<<<< ESP8266 FORCING RTS to "+resetBox.isSelected()+" >>>>>\n");
		serialCopy.setRTS(resetBox.isSelected());
		appendText("<<<<<< ESP8266 FORCING DTR to "+dtrBox.isSelected()+" >>>>>\n");
		serialCopy.setDTR(dtrBox.isSelected());
	}

	@Override
	public void close() throws Exception {
		appendText("<<<<<< ESP8266 SERIAL CONNECTION CLOSING >>>>>\n");
		super.close();
		serialCopy = null;
	}

	@Override
	public void enableWindow(boolean enable) {
		if (enable) {
			appendText("<<<<<< ESP8266 ENTERING PROGRAMMING MODE >>>>>\n");
		}else{
			appendText("<<<<<< ESP8266 EXITING PROGRAMMING MODE >>>>>\n");
		}
		super.enableWindow(enable);
		resetBox.setEnabled(enable);
		dtrBox.setEnabled(enable);
		btnClear.setEnabled(enable);
	}

	private void appendText(String text) {
		callMethod("append", text);
	}

	private void setText(String text) {
		callMethod("setText", text);
	}
	
	private void callMethod(String method,String text) {
		StringBuffer logs = new StringBuffer();
		try {
			Class clazz = this.getClass();
			while (clazz!=null) {
				Field field=null;
				try {
					field = clazz.getDeclaredField("textArea");
					logs.append("textArea found on "+clazz.getName()+"\n");
					field.setAccessible(true);
					Object textArea = field.get(this);
					Class clazz2=textArea.getClass();
					while (clazz2!=null) {
						Method met=null;
						try {
							met=clazz2.getMethod(method, String.class);
							logs.append("method "+method+" found on "+clazz2.getName()+"\n");
							met.invoke(textArea, text);
							return;
						} catch (Exception e) {
							logs.append("no method "+method+" on "+clazz2.getName()+"\n");
						}
						clazz2 = clazz2.getSuperclass();
						
					}
				} catch (Exception e) {
					logs.append("no textArea on "+clazz.getName()+"\n");
				}
				clazz = clazz.getSuperclass();
			}
			throw new Exception("Unable to invoke textArea."+method+"\n"+logs.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
