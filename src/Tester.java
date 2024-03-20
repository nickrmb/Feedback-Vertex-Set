import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.plaf.ColorUIResource;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.awt.Point;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * This class is a executable class that has a GUI for testing the
 * FeedbackVertexSet on own created graphs (read disclaimer below)
 */
public class Tester {

	/*
	 * !!!!!!!!!!!! DISCLAIMER !!!!!!!!!!!!
	 * 
	 * this is just a program to test the algorithms. This is NOT part of the actual
	 * (to be graded) submission, this just is supposed to show the correctness of
	 * the code. This program was not optimized in any way, it is just for test
	 * purposes.
	 */

	// Vertex size
	private static final int radius = 22;

	// Solution
	private static Set<Integer> solution = new HashSet<>();

	// drawing
	private static boolean drawBackground = true;

	public static void main(String[] args) {

		// Create a static frame

		JFrame frame = new JFrame();
		frame.setSize(1000, 1000);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

		frame.getContentPane().setLayout(null);

		frame.setResizable(false);

		// List of the coordinates of the vertices and their weight

		List<Point> points = new ArrayList<>();
		List<Float> weights = new ArrayList<>();
		List<List<Integer>> connections = new ArrayList<List<Integer>>();

		// Introduction lables

		JLabel label = new JLabel("");
		label.setBounds(100, 910, 800, 40);
		frame.getContentPane().add(label);

		JLabel intro1 = new JLabel("Create / edit vertex: LEFT CLICK");
		intro1.setBounds(100, 20, 800, 30);
		frame.getContentPane().add(intro1);
		JLabel intro2 = new JLabel(
				"Create edge between v1 and v2: CLICK and HOLD RIGHT CLICK on v1, STOP HOLDING on v2");
		intro2.setBounds(100, 40, 800, 30);
		frame.getContentPane().add(intro2);
		JLabel intro3 = new JLabel("Delete vertex: MIDDLE MOUSE BUTTON on vertex");
		intro3.setBounds(100, 60, 800, 30);
		frame.getContentPane().add(intro3);

		// Override draw method

		JPanel drawPanel = new JPanel() {
			private static final long serialVersionUID = 1L;

			@Override
			public void paintComponent(Graphics g2) {
				Graphics2D g = (Graphics2D) g2;
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(ColorUIResource.LIGHT_GRAY);
				if (drawBackground)
					g.fillRect(0, 0, getWidth(), getHeight());
				int i = 0;
				for (Point p : points) {
					for (Integer o : connections.get(i)) {
						if (o <= i)
							continue;
						Point other = points.get(o);
						g.setColor(ColorUIResource.BLACK);
						g.drawLine(p.x, p.y, other.x, other.y);
					}

					i++;
				}
				i = 0;
				for (Point p : points) {
					if (solution.contains(i)) {
						g.setColor(ColorUIResource.RED);
						g.fillOval(p.x - radius - 1, p.y - radius - 1, 2 * radius + 2, 2 * radius + 2);
						if (drawBackground) {
							g.setColor(ColorUIResource.LIGHT_GRAY);
						} else {
							g.setColor(ColorUIResource.WHITE);
						}
						g.fillOval(p.x - radius, p.y - radius, 2 * radius, 2 * radius);
					} else {
						if (drawBackground) {
							g.setColor(ColorUIResource.LIGHT_GRAY);
						} else {
							g.setColor(ColorUIResource.WHITE);
						}
						g.fillOval(p.x - radius, p.y - radius, 2 * radius, 2 * radius);
						g.setColor(ColorUIResource.BLACK);
						g.drawOval(p.x - radius, p.y - radius, 2 * radius, 2 * radius);
					}
					String id = String.valueOf(i);
					String weight = "w:" + weights.get(i);
					int idw = g.getFontMetrics().stringWidth(id);
					int weidghtw = g.getFontMetrics().stringWidth(weight);
					g.setColor(ColorUIResource.BLACK);
					g.drawString(id, p.x - idw / 2, p.y - 6);
					g.drawString(weight, p.x - weidghtw / 2, p.y + 6);
					i++;
				}
			}

		};
		drawPanel.setBounds(25, 100, 800, 800);
		drawPanel.setBackground(ColorUIResource.LIGHT_GRAY);
		drawPanel.setOpaque(true);

		frame.getContentPane().add(drawPanel);

		// Add mouse listener to panel

		drawPanel.addMouseListener(new MouseAdapter() {

			int i1 = -1;

			@Override
			public void mousePressed(MouseEvent e) {
				int button = e.getButton();
				int x = e.getX();
				int y = e.getY();

				// hitbox checking
				int index = overlapsWith(points, x, y);
				if (button == 1) {
					if (index != -1) {
						String weightText = JOptionPane.showInputDialog(frame, "Type new weight of vertex " + index);
						if (weightText == null)
							return;
						try {
							float weight = Float.parseFloat(weightText);
							weights.set(index, weight);
						} catch (Exception ex) {

						}
						solution = new HashSet<>();
						label.setText("");
						drawPanel.repaint();
						return;
					}
					points.add(new Point(x, y));
					weights.add(1.0f);
					connections.add(new ArrayList<Integer>());
					drawPanel.repaint();
					solution = new HashSet<>();
					label.setText("");
					return;
				}
				if (button == 2) {
					if (index != -1) {
						points.remove(index);
						weights.remove(index);
						connections.remove(index);
						for (int i = 0; i < connections.size(); i++) {
							List<Integer> conns = connections.get(i);
							conns.removeIf(n -> n == index);
							for (int j = 0; j < conns.size(); j++) {
								if (conns.get(j) > index) {
									conns.set(j, conns.get(j) - 1);
								}
							}
						}
						solution = new HashSet<>();
						label.setText("");
						drawPanel.repaint();
					}
					return;
				}
				if (button == 3) {
					i1 = -1;
					if (index != -1) {
						i1 = index;
					}
				}
				drawPanel.repaint();
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				int button = e.getButton();
				int x = e.getX();
				int y = e.getY();
				if (button == 3) {
					if (i1 == -1)
						return;
					int index = overlapsWith(points, x, y);
					if (index != -1) {
						int i2 = index;
						if (i1 != i2) {
							if (!connections.get(i1).contains(i2)) {
								connections.get(i1).add(i2);
								solution = new HashSet<>();
								label.setText("");
							}
							if (!connections.get(i2).contains(i1)) {
								connections.get(i2).add(i1);
								solution = new HashSet<>();
								label.setText("");
							}
						}
					}
					i1 = -1;
				}
				drawPanel.repaint();
			}
		});

		// Add buttons with their respective call

		JButton heuristic = new JButton("Heuristic");
		heuristic.setBounds(845, 350, 120, 30);
		frame.getContentPane().add(heuristic);

		heuristic.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Graph g = Graph.createGraph(weights, connections);
				long l1 = System.currentTimeMillis();
				Tuple<Set<Integer>, Float> sol = FeedbackVertexSet.heuristic(g);
				long l2 = System.currentTimeMillis();
				solution = sol.l;
				label.setText(
						"Heuristic: " + solution.toString() + " ; weight: " + sol.r + " ; time: " + (l2 - l1) + " ms");
				frame.repaint();
			}

		});

		JButton approximation = new JButton("Approximation");
		approximation.setBounds(845, 420, 120, 30);
		frame.getContentPane().add(approximation);

		approximation.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Graph g = Graph.createGraph(weights, connections);
				long l1 = System.currentTimeMillis();
				Tuple<Set<Integer>, Float> sol = FeedbackVertexSet.approximation(g);
				long l2 = System.currentTimeMillis();
				solution = sol.l;
				label.setText("Approximation: " + solution.toString() + " ; weight: " + sol.r + " ; time: " + (l2 - l1)
						+ " ms");
				frame.repaint();
			}

		});

		JButton exact = new JButton("Exact");
		exact.setBounds(845, 490, 120, 30);
		frame.getContentPane().add(exact);

		exact.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Graph g = Graph.createGraph(weights, connections);
				long l1 = System.currentTimeMillis();
				Tuple<Set<Integer>, Float> sol = FeedbackVertexSet.exact(g, -1.0f, 1);
				long l2 = System.currentTimeMillis();
				solution = sol.l;
				label.setText("Exact (1 processor, no bound): " + solution.toString() + " ; weight: " + sol.r
						+ " ; time: " + (l2 - l1) + " ms");
				frame.repaint();
			}

		});

		JButton save = new JButton("Save Graph");
		save.setBounds(845, 600, 120, 30);
		frame.getContentPane().add(save);

		save.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setDialogTitle("Choose file to save");

				int userSelection = fileChooser.showSaveDialog(frame);

				if (userSelection == JFileChooser.APPROVE_OPTION) {
					File file = fileChooser.getSelectedFile();

					if (file.exists()) {
						file.delete();
					}
					try {
						file.createNewFile();

						BufferedWriter writer = new BufferedWriter(new FileWriter(file));

						writer.write("" + weights.size());
						writer.newLine();

						for (float weight : weights) {
							writer.write("" + weight);
							writer.newLine();
						}
						int v = 0;
						for (List<Integer> conns : connections) {
							for (int w : conns) {
								writer.write(v + " " + w);
								writer.newLine();
							}
							v++;
						}

						writer.flush();
						writer.close();

					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

				}
			}

		});

		JButton save2 = new JButton("Save Image");
		save2.setBounds(845, 670, 120, 30);
		frame.getContentPane().add(save2);

		save2.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setDialogTitle("Choose file to save (.png)");

				int userSelection = fileChooser.showSaveDialog(frame);

				if (userSelection == JFileChooser.APPROVE_OPTION) {
					File file = fileChooser.getSelectedFile();

					if (file.exists()) {
						file.delete();
					}
					try {
						file.createNewFile();

						BufferedImage bImg = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);
						Graphics2D cg = bImg.createGraphics();
						drawBackground = false;
						drawPanel.paintAll(cg);
						drawBackground = true;

						if (ImageIO.write(bImg, "png", file)) {
							System.out.println("-- saved");
						}

					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

				}
			}

		});

		frame.repaint();
	}

	/**
	 * A simple method to test whether the position (x,y) overlaps with some point
	 * in a radius
	 * 
	 * @param points the points
	 * @param x
	 * @param y
	 * @return true if there is a point, false else
	 */
	public static int overlapsWith(List<Point> points, int x, int y) {
		int index = 0;
		for (Point p : points) {
			int px = p.x;
			int py = p.y;
			if (px - radius < x && px + radius > x && py - radius < y && py + radius > y) {
				return index;
			}
			index++;
		}
		return -1;
	}

}
