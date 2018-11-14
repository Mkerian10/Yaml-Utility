import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class YamlManager extends JFrame{
	
	private final static int ASSIGNMENT_COL = 0;
	private final static int EXTENSION_COL = 1;
	
	private JTextField filePath;
	private Vector<Vector<String>> data;
	private Vector<String> columns;
	private DefaultTableModel tableModel;
	
	private List<YamlFormat> extensions = new ArrayList<>();
	private List<YamlFormat> assignments = new ArrayList<>();
	
	private String settingsFile = null;
	
	private void init(){
		JPanel mainFrame = new JPanel();
		mainFrame.setLayout(new BoxLayout(mainFrame, BoxLayout.Y_AXIS));
		
		JPanel configPanel = new JPanel();
		configPanel.setLayout(new GridBagLayout());
		
		JTable table = new JTable();
		table.setGridColor(new Color(10, 10, 10, 40));
		table.setRowSelectionAllowed(false);
		
		data = new Vector<>();
		
		columns = new Vector<>();
		columns.add("Assignments");
		columns.add("Extensions");
		tableModel = new DefaultTableModel(){
			@Override
			public boolean isCellEditable(int row, int column){
				return false;
			}
		};
		tableModel.setDataVector(data, columns);
		
		table.setModel(tableModel);
		
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setPreferredSize(new Dimension(650, 300));
		configPanel.add(scrollPane);
		
		JPanel pathPanel = new JPanel();
		configPanel.setLayout(new GridBagLayout());
		pathPanel.setPreferredSize(new Dimension(300, 40));
		filePath = new JTextField("", 15);
		filePath.setEditable(false);
		filePath.getDocument().addDocumentListener(new DocumentListener(){
			@Override
			public void insertUpdate(DocumentEvent e){
				deleteSets();
				populateLists();
				for(YamlFormat y : assignments){
					addRow(y.getTableFormattedString(), ASSIGNMENT_COL);
				}
				for(YamlFormat y : extensions){
					addRow(y.getTableFormattedString(), EXTENSION_COL);
				}
			}
			
			@Override
			public void removeUpdate(DocumentEvent e){
			}
			
			@Override
			public void changedUpdate(DocumentEvent e){
			}
		});
		
		JButton getFile = new JButton("File");
		getFile.addActionListener(e -> fileChooserJPane());
		
		JButton addAssignment = new JButton("Add Assign.");
		addAssignment.addActionListener(e -> {
			try{
				YamlFormat y = fetchNewFormat();
				if(y != null){
					assignments.add(y);
					addRow(y.getTableFormattedString(), ASSIGNMENT_COL);
				}
			}catch(ParseException e1){
				e1.printStackTrace();
			}
			
		});
		
		JButton addExtension = new JButton("Add Extension");
		addExtension.addActionListener(e -> {
			try{
				YamlFormat y = fetchNewFormat();
				if(y != null){
					extensions.add(y);
					addRow(y.getTableFormattedString(), EXTENSION_COL);
					System.out.println(y.getFileFormattedLine());
				}
			}catch(ParseException e1){
				e1.printStackTrace();
			}
		});
		
		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(e -> saveData());
		
		
		pathPanel.add(filePath);
		pathPanel.add(getFile);
		pathPanel.add(addAssignment);
		pathPanel.add(addExtension);
		pathPanel.add(saveButton);
		mainFrame.add(pathPanel);
		mainFrame.add(configPanel);
		this.add(mainFrame);
		
		String settings = getSavedSettings();
		if(settings != null && settings.length() > 4){
			settingsFile = settings;
			filePath.setText(settings);
		}
		
		addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e){
				saveSettings();
				super.windowClosing(e);
			}
		});
	}
	
	private List<String> getFileContents(){
		File f = new File(filePath.getText());
		if(!f.isFile()){
			return new ArrayList<>();
		}
		
		List<String> list = new ArrayList<>();
		try{
			Scanner s = new Scanner(f);
			while(s.hasNextLine()){
				list.add(s.nextLine());
			}
			return list;
		}catch(FileNotFoundException e){
			return new ArrayList<>();
		}
	}
	
	private List<String> getAssignments(List<String> input){
		List<String> list = new ArrayList<>();
		int startIndex = 0;
		int endIndex = 0;
		int i = 0;
		for(String s : input){
			if(s.equalsIgnoreCase("assignments:")){
				startIndex = i + 1;
			}else if(s.equalsIgnoreCase("extensions:")){
				endIndex = i;
			}
			i++;
		}
		if(endIndex < startIndex){
			endIndex = input.size();
		}
		
		for(int j = startIndex; j < endIndex; j++){
			list.add(input.get(j));
		}
		return list;
	}
	
	private void populateLists(){
		List<String> lines = getFileContents();
		List<String> extensions = getExtensions(lines);
		List<String> assignments = getAssignments(lines);
		
		for(String s : extensions){
			try{
				this.extensions.add(new YamlFormat(s));
			}catch(NullPointerException ignored){
			
			}
		}
		
		for(String s : assignments){
			try{
				this.assignments.add(new YamlFormat(s));
			}catch(NullPointerException ignored){
			
			}
		}
	}
	
	private List<String> getExtensions(List<String> input){
		List<String> list = new ArrayList<>();
		int startIndex = 0;
		int endIndex = 0;
		int i = 0;
		for(String s : input){
			if(s.equalsIgnoreCase("extensions:")){
				startIndex = i + 1;
			}else if(s.equalsIgnoreCase("assignments:")){
				endIndex = i;
			}
			i++;
		}
		if(endIndex < startIndex){
			endIndex = input.size();
		}
		
		for(int j = startIndex; j < endIndex; j++){
			list.add(input.get(j));
		}
		return list;
	}
	
	private void fileChooserJPane(){
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fileChooser.setFileFilter(new FileFilter(){
			@Override
			public boolean accept(File f){
				return f.isDirectory() || f.getName().toLowerCase().endsWith(".yaml");
			}
			
			@Override
			public String getDescription(){
				return null;
			}
		});
		int input = fileChooser.showOpenDialog(null);
		if(input == JFileChooser.APPROVE_OPTION){
			if(fileChooser.getSelectedFile().isDirectory()){
				filePath.setText(fileChooser.getSelectedFile().getAbsolutePath() + "/sdn.yaml");
			}else{
				filePath.setText(fileChooser.getSelectedFile().getAbsolutePath());
			}
		}
	}
	
	@SuppressWarnings("all")
	private void addRow(String data, int column){
		Vector<Vector<String>> v = (Vector<Vector<String>>) tableModel.getDataVector();
		int nextRow = 0;
		for(int i = 0; i <= v.size(); i++){
			if(v.size() <= i){
				Vector<String> row = new Vector<>();
				row.add(null);
				row.add(null);
				v.add(row);
				nextRow = i;
				break;
			}
			
			if(v.get(i).get(column) == null){
				nextRow = i;
				break;
			}
		}
		
		if(v.size() == 0){
			Vector<String> row = new Vector<>();
			row.add(null);
			row.add(null);
			v.add(row);
		}
		
		v.get(nextRow).remove(column);
		v.get(nextRow).add(column, data);
		tableModel.fireTableRowsInserted(0, v.size());
	}
	
	private YamlFormat fetchNewFormat() throws ParseException{
		JPanel main = new JPanel();
		main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
		
		JPanel datePanel = new JPanel(new GridBagLayout());
		JLabel dateLabel = new JLabel("Start Date:");
		datePanel.add(dateLabel);
		Calendar.getInstance().clear();
		Date d = Calendar.getInstance().getTime();
		String formattedTime = format.format(d);
		MaskFormatter mf = new MaskFormatter("####-##-##");
		JFormattedTextField dateField = new JFormattedTextField(mf);
		dateField.setText(formattedTime);
		datePanel.add(dateField);
		main.add(datePanel);
		
		JPanel daysPanel = new JPanel(new GridBagLayout());
		JLabel daysLabel = new JLabel("Allotted Days:");
		JTextField daysField = new JTextField("", 5);
		((AbstractDocument) daysField.getDocument()).setDocumentFilter(new IntegerFilter());
		daysPanel.add(daysLabel);
		daysPanel.add(daysField);
		main.add(daysPanel);
		
		JPanel receiverPanel = new JPanel(new GridBagLayout());
		JLabel receiverLabel = new JLabel("Receiver ID:");
		JTextField receiverField = new JTextField("", 7);
		((AbstractDocument) receiverField.getDocument()).setDocumentFilter(new IntegerFilter());
		receiverPanel.add(receiverLabel);
		receiverPanel.add(receiverField);
		main.add(receiverPanel);
		
		JPanel ownerPanel = new JPanel(new GridBagLayout());
		JLabel ownerLabel = new JLabel("Script Writer ID:");
		JTextField ownerField = new JTextField("", 7);
		((AbstractDocument) ownerField.getDocument()).setDocumentFilter(new IntegerFilter());
		ownerPanel.add(ownerLabel);
		ownerPanel.add(ownerField);
		main.add(ownerPanel);
		
		JPanel scriptPanel = new JPanel(new GridBagLayout());
		JLabel scriptLabel = new JLabel("Script's Class Name:");
		JTextField scriptField = new JTextField("", 12);
		scriptPanel.add(scriptLabel);
		scriptPanel.add(scriptField);
		main.add(scriptPanel);
		
		
		int i = JOptionPane.showConfirmDialog(this, main, "Add Assignment/Extension", JOptionPane.OK_CANCEL_OPTION);
		if(i == JOptionPane.OK_OPTION){
			return new YamlFormat(dateField.getText(), Integer.parseInt(daysField.getText()), Integer.parseInt(receiverField.getText()), Integer.parseInt(ownerField.getText()), scriptField.getText());
		}
		
		return null;
	}
	
	private String getSavedSettings(){
		File hiddenFile = new File(".yaml_manager_config");
		if(!hiddenFile.isFile()){
			return null;
		}
		try{
			Scanner s = new Scanner(hiddenFile);
			return s.nextLine();
		}catch(FileNotFoundException e){
			e.printStackTrace();
			return null;
		}
	}
	
	private void saveSettings(){
		String filePath = this.filePath.getText();
		if(filePath == null || settingsFile == null){
			return;
		}
		if(settingsFile.equalsIgnoreCase(filePath)){
			return;
		}
		File localHidden = new File(".yaml_manager_config");
		if(filePath.length() < 4){
			return;
		}
		try{
			FileWriter fw = new FileWriter(localHidden);
			fw.write(filePath);
			fw.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	private void saveData(){
		StringBuilder sb = new StringBuilder();
		if(assignments.size() > 0){
			sb.append("assignments:").append("\n");
		}
		for(YamlFormat y : assignments){
			sb.append(y.getFileFormattedLine()).append("\n");
		}
		if(extensions.size() > 0){
			sb.append("extensions:").append("\n");
		}
		for(YamlFormat y : extensions){
			sb.append(y.getFileFormattedLine()).append("\n");
		}
		sb.delete(sb.length() - 1, sb.length());
		
		File outputFile = new File(filePath.getText());
		try{
			FileWriter fw = new FileWriter(outputFile);
			fw.write(sb.toString());
			fw.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	private void deleteSets(){
		assignments.clear();
		extensions.clear();
		data.clear();
		tableModel.setDataVector(data, columns);
	}
	
	public static void main(String[] args){
		YamlManager manager = new YamlManager();
		EventQueue.invokeLater(() -> {
			manager.init();
			manager.pack();
			manager.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			manager.setLocationRelativeTo(null);
			manager.setVisible(true);
		});
	}
	
	private final static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
	
	private class YamlFormat{
		
		public YamlFormat(String formattedDate, int days, int receiverId, int ownerId, String scriptName){
			this.formattedDate = formattedDate;
			this.days = days;
			this.receiverId = receiverId;
			this.ownerId = ownerId;
			this.scriptName = scriptName;
		}
		
		public YamlFormat(String line){
			if(!line.contains("[") || !line.contains("]") || line.length() < 10){
				throw new NullPointerException();
			}
			int firstIndex = line.indexOf("[");
			int secondIndex = line.indexOf("]");
			String s = line.substring(firstIndex + 1, secondIndex);
			String[] strings = s.split(",");
			formattedDate = strings[0].trim();
			days = Integer.parseInt(strings[1].trim());
			receiverId = Integer.parseInt(strings[2].trim());
			String ownerAndScript = strings[3];
			ownerAndScript = ownerAndScript.replaceAll("[\"]", "");
			String[] split = ownerAndScript.split("/");
			String owner = split[0];
			String scriptName = split[1];
			ownerId = Integer.parseInt(owner.trim());
			this.scriptName = scriptName.trim();
		}
		
		private final String formattedDate;
		
		private final int days;
		
		private final int receiverId;
		
		private final int ownerId;
		
		private final String scriptName;
		
		private String dateDifferenceString(){
			Date initialDate;
			try{
				initialDate = format.parse(formattedDate);
			}catch(ParseException e){
				e.printStackTrace();
				return "";
			}
			
			Calendar cal = Calendar.getInstance();
			cal.setTime(initialDate);
			cal.add(Calendar.DATE, days);
			String finalDate = format.format(cal.getTime());
			return formattedDate + "->" + finalDate;
		}
		
		private String getTableFormattedString(){
			return "[" + dateDifferenceString() + "] User: " + receiverId;
		}
		
		private String getFileFormattedLine(){
			return String.format("- [%s, %d, %d, \"%d/%s\"]", formattedDate, days, receiverId, ownerId, scriptName);
		}
	}
	
	private class IntegerFilter extends DocumentFilter{
		
		@Override
		public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException{
			string = string.replaceAll("\\D+", "");
			super.insertString(fb, offset, string, attr);
		}
		
		@Override
		public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException{
			text = text.replaceAll("\\D+", "");
			super.replace(fb, offset, length, text, attrs);
		}
	}
}
