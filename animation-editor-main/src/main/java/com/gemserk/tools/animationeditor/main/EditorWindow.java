package com.gemserk.tools.animationeditor.main;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.gemserk.commons.reflection.Injector;
import com.gemserk.commons.reflection.InjectorImpl;
import com.gemserk.resources.ResourceManager;
import com.gemserk.resources.ResourceManagerImpl;
import com.gemserk.tools.animationeditor.core.Skeleton;
import com.gemserk.tools.animationeditor.core.SkeletonAnimation;
import com.gemserk.tools.animationeditor.core.SkeletonAnimationKeyFrame;
import com.gemserk.tools.animationeditor.core.Skin;
import com.gemserk.tools.animationeditor.core.tree.SkeletonEditorImpl;
import com.gemserk.tools.animationeditor.main.list.AnimationKeyFrameListModel;
import com.gemserk.tools.animationeditor.main.tree.EditorInterceptorImpl;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class EditorWindow {

	protected static final Logger logger = LoggerFactory.getLogger(EditorWindow.class);

	private JFrame mainFrame;
	private JList keyFramesList;
	private EditorInterceptorImpl editor;

	private ResourceManager<String> resourceManager;
	private ResourceBundle messages;

	private Project currentProject;

	private EditorLibgdxApplicationListener editorApplication;

	public JFrame getMainFrame() {
		return mainFrame;
	}

	/**
	 * Create the application.
	 */
	public EditorWindow() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {

		messages = ResourceBundle.getBundle("messages");
		resourceManager = new ResourceManagerImpl<String>();

		Injector injector = new InjectorImpl();

		injector.bind("resourceManager", resourceManager);
		injector.bind("messages", messages);

		editorApplication = injector.getInstance(EditorLibgdxApplicationListener.class);

		final Canvas canvasEditor = new Canvas() {

			private LwjglApplication lwjglApplication;

			public final void addNotify() {
				super.addNotify();
				lwjglApplication = new LwjglApplication(editorApplication, false, this);
			}

			public final void removeNotify() {
				lwjglApplication.stop();
				super.removeNotify();
			}

			{
				addMouseListener(new MouseAdapter() {
					@Override
					public void mouseEntered(MouseEvent e) {
						requestFocus();
					}

					@Override
					public void mouseExited(MouseEvent e) {
						getParent().requestFocus();
					};
				});
			}
		};

		mainFrame = new JFrame();
		mainFrame.setPreferredSize(new Dimension(1024, 768));
		mainFrame.setTitle("Gemserk's Animation Editor");
		mainFrame.setMinimumSize(new Dimension(800, 600));
		BorderLayout borderLayout = (BorderLayout) mainFrame.getContentPane().getLayout();
		borderLayout.setHgap(1);
		mainFrame.setBounds(100, 100, 800, 600);

		mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		mainFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				exit();
			}
		});

		JMenuBar menuBar = new JMenuBar();
		mainFrame.setJMenuBar(menuBar);

		JMenu mnFile = new JMenu(messages.getString(Messages.MenuFileTitle));
		menuBar.add(mnFile);

		JMenuItem mntmNewProject = new JMenuItem(messages.getString(Messages.MenuFileNew));
		mntmNewProject.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				newProject();
			}

		});
		mnFile.add(mntmNewProject);

		JMenuItem mntmOpen = new JMenuItem("Open");
		mntmOpen.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				open(editorApplication);
			}

		});
		mnFile.add(mntmOpen);

		JMenuItem mntmSave = new JMenuItem("Save");
		mntmSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (currentProject != null)
					save();
				else
					saveAs(editorApplication);
			}
		});
		mnFile.add(mntmSave);

		JMenuItem mntmSaveAs = new JMenuItem("Save as...");
		mntmSaveAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveAs(editorApplication);
			}
		});
		mnFile.add(mntmSaveAs);

		mnFile.addSeparator();

		JMenuItem mntmImport = new JMenuItem("Import");
		mntmImport.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();

				if (currentProject != null)
					chooser = new JFileChooser(FilenameUtils.getFullPath(currentProject.getProjectFile()));

				FileNameExtensionFilter filter = new FileNameExtensionFilter("Images only", "png", "jpg", "gif");

				chooser.setFileFilter(filter);
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				chooser.setMultiSelectionEnabled(false);

				int returnVal = chooser.showOpenDialog(mainFrame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File selectedFile = chooser.getSelectedFile();

					editorApplication.setCurrentSkin(selectedFile);
					// File[] selectedFiles = chooser.getSelectedFiles();
					// for (int i = 0; i < selectedFiles.length; i++) {
					// System.out.println("file " + i + " : " + selectedFiles[i].getName());
					// }
				}
			}
		});
		mnFile.add(mntmImport);

		mnFile.addSeparator();

		JMenuItem mntmExport = new JMenuItem("Export animation...");
		mntmExport.addActionListener(new ActionListener() {

			private String lastExportDir;

			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();

				String startDir = null;
				if (lastExportDir != null)
					startDir = lastExportDir;
				else if (currentProject != null)
					startDir = currentProject.getProjectFile();

				if (startDir != null)
					chooser = new JFileChooser(FilenameUtils.getFullPath(startDir));

				FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG files", "png");

				chooser.setFileFilter(filter);
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				chooser.setMultiSelectionEnabled(false);

				int returnVal = chooser.showOpenDialog(mainFrame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File selectedFile = chooser.getSelectedFile();
					lastExportDir = selectedFile.getParent() + File.separatorChar;
					String absolutePath = selectedFile.getAbsolutePath();

					String baseName = FilenameUtils.getBaseName(absolutePath);
					String fullPath = FilenameUtils.getFullPath(absolutePath);

					editorApplication.exportAnimation(fullPath + baseName + "_");
				}
			}
		});
		mnFile.add(mntmExport);

		mnFile.addSeparator();

		JMenuItem mntmExit = new JMenuItem(messages.getString(Messages.MenuFileExit));
		mntmExit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				exit();
			}
		});
		mnFile.add(mntmExit);

		JPanel panelTools = new JPanel();
		panelTools.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		mainFrame.getContentPane().add(panelTools, BorderLayout.WEST);
		panelTools.setMinimumSize(new Dimension(300, 400));
		panelTools.setPreferredSize(new Dimension(200, 400));
		panelTools.setLayout(new BorderLayout(0, 0));

		JSplitPane splitPane = new JSplitPane();
		splitPane.setPreferredSize(new Dimension(200, 29));
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		panelTools.add(splitPane);

		JPanel panel_1 = new JPanel();
		panel_1.setMinimumSize(new Dimension(100, 100));
		splitPane.setRightComponent(panel_1);
		panel_1.setLayout(new BorderLayout(0, 0));

		JLabel lblNewLabel = new JLabel("Preview");
		panel_1.add(lblNewLabel, BorderLayout.NORTH);
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel.setVerticalAlignment(SwingConstants.TOP);

		Canvas canvas_2 = new Canvas();
		panel_1.add(canvas_2);
		canvas_2.setBackground(Color.LIGHT_GRAY);

		JPanel panel_4 = new JPanel();
		panel_4.setPreferredSize(new Dimension(100, 400));
		panel_4.setMinimumSize(new Dimension(100, 200));
		splitPane.setLeftComponent(panel_4);
		panel_4.setLayout(new BorderLayout(0, 0));

		JLabel lblTitle = new JLabel("Select Image");
		panel_4.add(lblTitle, BorderLayout.NORTH);
		lblTitle.setVerticalAlignment(SwingConstants.TOP);
		lblTitle.setToolTipText("Select image");
		lblTitle.setHorizontalAlignment(SwingConstants.CENTER);

		JScrollPane scrollPane_2 = new JScrollPane();
		panel_4.add(scrollPane_2, BorderLayout.CENTER);

		JPanel panelImages = new JPanel();
		panelImages.setBackground(Color.LIGHT_GRAY);
		scrollPane_2.setViewportView(panelImages);

		JPanel panel = new JPanel();
		panel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		mainFrame.getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(new BorderLayout(0, 0));

		JPanel panelEditor = new JPanel();
		panel.add(panelEditor, BorderLayout.CENTER);
		panelEditor.setMaximumSize(new Dimension(800, 600));
		panelEditor.setPreferredSize(new Dimension(800, 600));
		panelEditor.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panelEditor.setLayout(new BorderLayout(0, 0));

		JLabel lblNewLabel_1 = new JLabel("Editor");
		lblNewLabel_1.setVerticalAlignment(SwingConstants.TOP);
		lblNewLabel_1.setHorizontalAlignment(SwingConstants.CENTER);
		panelEditor.add(lblNewLabel_1, BorderLayout.NORTH);

		panelEditor.add(canvasEditor, BorderLayout.CENTER);
		canvasEditor.setBackground(Color.BLACK);

		JPanel panelTimeline = new JPanel();
		panel.add(panelTimeline, BorderLayout.SOUTH);
		panelTimeline.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		final JToggleButton toggleButtonPlay = new JToggleButton("");
		toggleButtonPlay.setSelectedIcon(new ImageIcon(TestWindow.class.getResource("/data/buttonpause.png")));
		toggleButtonPlay.setIcon(new ImageIcon(TestWindow.class.getResource("/data/buttonplay.png")));
		toggleButtonPlay.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (toggleButtonPlay.isSelected())
					editor.playAnimation();
				else
					editor.stopAnimation();
			}
		});
		panelTimeline.add(toggleButtonPlay);

		JPanel panelStructure = new JPanel();
		panelStructure.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panelStructure.setPreferredSize(new Dimension(200, 10));
		panelStructure.setMinimumSize(new Dimension(150, 10));
		mainFrame.getContentPane().add(panelStructure, BorderLayout.EAST);
		panelStructure.setLayout(new BorderLayout(0, 0));

		JSplitPane splitPane_1 = new JSplitPane();
		splitPane_1.setOrientation(JSplitPane.VERTICAL_SPLIT);
		panelStructure.add(splitPane_1, BorderLayout.CENTER);

		JPanel panel_2 = new JPanel();
		panel_2.setMinimumSize(new Dimension(10, 300));
		splitPane_1.setLeftComponent(panel_2);
		panel_2.setLayout(new BorderLayout(0, 0));

		JLabel labelPanelStructureTitle = new JLabel("Structure");
		panel_2.add(labelPanelStructureTitle, BorderLayout.NORTH);

		final JTree tree = new JTree();
		tree.setEditable(true);
		tree.setRootVisible(false);
		tree.setExpandsSelectedPaths(true);

		tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("root")));
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		panel_2.add(tree);
		tree.setBackground(Color.LIGHT_GRAY);

		keyFramesList = new JList();
		keyFramesList.setModel(new AnimationKeyFrameListModel());
		keyFramesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		keyFramesList.setBackground(Color.LIGHT_GRAY);

		editor = new EditorInterceptorImpl( //
				new SkeletonEditorImpl(), tree, keyFramesList);

		editorApplication.setTreeEditor(editor);
		editorApplication.setAnimationEditor(editor);

		JPanel panel_3 = new JPanel();
		splitPane_1.setRightComponent(panel_3);
		panel_3.setLayout(new BorderLayout(0, 0));

		JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		panel_3.add(scrollPane_1, BorderLayout.CENTER);

		JPanel panel_6 = new JPanel();
		scrollPane_1.setViewportView(panel_6);
		panel_6.setLayout(new BorderLayout(0, 0));

		panel_6.add(keyFramesList, BorderLayout.CENTER);

		JLabel lblNewLabel_2 = new JLabel("KeyFrames");
		scrollPane_1.setColumnHeaderView(lblNewLabel_2);

		JPanel panel_7 = new JPanel();
		panel_3.add(panel_7, BorderLayout.SOUTH);

		JButton buttonAddKeyFrame = new JButton("");
		buttonAddKeyFrame.setIcon(new ImageIcon(TestWindow.class.getResource("/data/buttonadd.png")));
		buttonAddKeyFrame.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SkeletonAnimationKeyFrame keyFrame = editor.addKeyFrame();
				editor.selectKeyFrame(keyFrame);
			}
		});
		panel_7.add(buttonAddKeyFrame);

		JButton buttonRemoveKeyFrame = new JButton("");
		buttonRemoveKeyFrame.setIcon(new ImageIcon(TestWindow.class.getResource("/data/buttonremove.png")));
		buttonRemoveKeyFrame.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				editor.removeKeyFrame();
			}
		});
		panel_7.add(buttonRemoveKeyFrame);

		JButton buttonStoreKeyFrame = new JButton("");
		buttonStoreKeyFrame.setIcon(new ImageIcon(TestWindow.class.getResource("/data/buttonsave.png")));
		buttonStoreKeyFrame.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				editor.updateKeyFrame();
			}
		});
		panel_7.add(buttonStoreKeyFrame);
	}

	private void save() {
		// currentProject.texturePaths.clear();
		// // convert to relativ paths!
		// currentProject.texturePaths.putAll(editorApplication.texturePaths);

		currentProject.setTextureAbsolutePaths(editorApplication.texturePaths);

		ProjectUtils.saveSkeleton(currentProject, editor.getSkeleton());
		ProjectUtils.saveSkin(currentProject, editorApplication.skin);
		ProjectUtils.saveAnimations(currentProject, Arrays.asList(editor.getCurrentAnimation()));
		ProjectUtils.saveProject(currentProject);
	}

	private void saveAs(EditorLibgdxApplicationListener editorApplication) {
		JFileChooser chooser = new JFileChooser();

		FileNameExtensionFilter filter = new FileNameExtensionFilter("Project files only", //
				Project.PROJECT_EXTENSION);

		chooser.setFileFilter(filter);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setMultiSelectionEnabled(false);

		int returnVal = chooser.showSaveDialog(mainFrame);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File selectedFile = chooser.getSelectedFile();

			// String projectFileNameWithoutExtension = FileUtils.getFileNameWithoutExtension(selectedFile.getAbsolutePath());

			Project project = new Project(selectedFile.getAbsolutePath());

			currentProject = project;
			save();

			// project.texturePaths.clear();
			// // convert to relativ paths!
			// project.texturePaths.putAll(editorApplication.texturePaths);
			//
			// ProjectUtils.saveSkeleton(project, editor.getSkeleton());
			// ProjectUtils.saveSkin(project, editorApplication.skin);
			// ProjectUtils.saveAnimations(project, Arrays.asList(editor.getCurrentAnimation()));
			// ProjectUtils.saveProject(project);
			//
			// currentProject = project;
		}
	}

	private void open(final EditorLibgdxApplicationListener editorApplication) {
		JFileChooser chooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Project files only", Project.PROJECT_EXTENSION);

		chooser.setFileFilter(filter);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setMultiSelectionEnabled(false);

		int returnVal = chooser.showOpenDialog(mainFrame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File selectedFile = chooser.getSelectedFile();

			try {
				Gson gson = new GsonBuilder() //
						.setPrettyPrinting() //
						.create();

				logger.info("Loading project from " + selectedFile);
				Project project = gson.fromJson(new FileReader(selectedFile), Project.class);

				project.setProjectFile(selectedFile.getAbsolutePath());

				// resourceManager.unloadAll();

				Skeleton skeleton = ProjectUtils.loadSkeleton(project);
				Skin skin = ProjectUtils.loadSkin(project);
				List<SkeletonAnimation> animations = ProjectUtils.loadAnimations(project);

				// editor.setSkeleton(skeleton);
				// editorApplication.skin = skin;

				currentProject = project;

				// convert to absolute paths again!
				Map<String, String> absoluteTexturePaths = project.getAbsoluteTexturePaths();
				editorApplication.updateResources(skeleton, absoluteTexturePaths, skin);
				// updateResourceMAnager(resourceManager, map of images, skin)

				if (animations.size() > 0) {
					SkeletonAnimation skeletonAnimation = animations.get(0);
					editor.setCurrentAnimation(skeletonAnimation);
				}

			} catch (JsonSyntaxException e1) {
				logger.error("Failed when loading project from file " + selectedFile, e1);
			} catch (JsonIOException e1) {
				logger.error("Failed when loading project from file " + selectedFile, e1);
			} catch (FileNotFoundException e1) {
				logger.error("Failed when loading project from file " + selectedFile, e1);
			}

		}
	}

	private void exit() {
		// if project has modifications and was not saved ->
		int showConfirmDialog = JOptionPane.showConfirmDialog(mainFrame, //
				messages.getString(Messages.DialogExitMessage), //
				messages.getString(Messages.DialogExitTitle), //
				JOptionPane.YES_NO_OPTION);
		if (showConfirmDialog == JOptionPane.NO_OPTION)
			return;
		// else exit without asking!!!
		Gdx.app.exit();
	}

	private void newProject() {

		// should ask if want to save only if project was modified...

		int showConfirmDialog = JOptionPane.showConfirmDialog(mainFrame, //
				messages.getString(Messages.DialogNewMessage), //
				messages.getString(Messages.DialogNewTitle), //
				JOptionPane.YES_NO_CANCEL_OPTION);
		
		if (showConfirmDialog == JOptionPane.CANCEL_OPTION) 
			return;
		
		if (showConfirmDialog == JOptionPane.YES_OPTION) {
			if (currentProject != null)
				save();
		}
		
		currentProject = null;
		
		Gdx.app.postRunnable(new Runnable() {
			@Override
			public void run() {
				editorApplication.newProject();
			}
		});

	}

}
