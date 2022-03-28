package de.piegames.blockmap.generate;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joml.Vector2d;
import org.joml.Vector2i;

import de.piegames.blockmap.color.BlockColorMap;
import de.piegames.blockmap.gui.decoration.Pin.PinType;
import de.piegames.blockmap.gui.standalone.GuiMain;
import de.piegames.blockmap.renderer.RegionRenderer;
import de.piegames.blockmap.renderer.RegionShader;
import de.piegames.blockmap.renderer.RenderSettings;
import de.piegames.nbt.regionfile.RegionFile;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;

public class Screenshots {

	private static Log log = LogFactory.getLog(Screenshots.class);

	public static void generateDemoRenders() throws IOException {
		RenderSettings settings = new RenderSettings();
		settings.loadDefaultColors();
		RegionRenderer renderer = new RegionRenderer(settings);
		{ /* Color maps */
			log.info("Generating color map screenshots");
			settings.maxY = 50;
			BufferedImage img1 = generateScreenshot(renderer, settings, new Vector2i(5, 2), BlockColorMap.InternalColorMap.CAVES);
			settings.maxY = 255;
			BufferedImage img2 = generateScreenshot(renderer, settings, new Vector2i(6, 2), BlockColorMap.InternalColorMap.NO_FOLIAGE);
			BufferedImage img3 = generateScreenshot(renderer, settings, new Vector2i(5, 3), BlockColorMap.InternalColorMap.DEFAULT);
			BufferedImage img4 = generateScreenshot(renderer, settings, new Vector2i(6, 3), BlockColorMap.InternalColorMap.OCEAN_GROUND);
			BufferedImage img = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = img.createGraphics();
			/* Background */
			g.setColor(new Color(0.2f, 0.2f, 0.6f, 1.0f));
			g.fillRect(0, 0, 1024, 1024);
			/* Insert the images */
			g.drawImage(img1, 0, 0, null);
			g.drawImage(img2, 512, 0, null);
			g.drawImage(img3, 0, 512, null);
			g.drawImage(img4, 512, 512, null);
			/* Add text on top */
			g.setColor(Color.WHITE);
			g.setFont(g.getFont().deriveFont(0, 32.0f));
			g.drawString("Caves", 0 + 32, 512 - 32);
			g.drawString("No foliage", 1024 - 32 - g.getFontMetrics().stringWidth("No foliage"), 512 - 32);
			g.drawString("Default", 0 + 32, 1024 - 32);
			g.drawString("Ocean ground", 1024 - 32 - g.getFontMetrics().stringWidth("Default"), 1024 - 32);
			g.dispose();
			try (OutputStream out = Files.newOutputStream(Generator.OUTPUT_SCREENSHOTS.resolve("screenshot-1.png"))) {
				ImageIO.write(img, "png", out);
			}
		}
		{ /* Shaders */
			log.info("Generating shader screenshots");
			BufferedImage img1 = generateScreenshot(renderer, settings, new Vector2i(5, 2), BlockColorMap.InternalColorMap.DEFAULT);
			settings.regionShader = RegionShader.DefaultShader.RELIEF.getShader();
			settings.regionShader = RegionShader.DefaultShader.FLAT.getShader();
			BufferedImage img2 = generateScreenshot(renderer, settings, new Vector2i(6, 2), BlockColorMap.InternalColorMap.DEFAULT);
			settings.regionShader = RegionShader.DefaultShader.BIOMES.getShader();
			BufferedImage img3 = generateScreenshot(renderer, settings, new Vector2i(5, 3), BlockColorMap.InternalColorMap.DEFAULT);
			settings.regionShader = RegionShader.DefaultShader.HEIGHTMAP.getShader();
			BufferedImage img4 = generateScreenshot(renderer, settings, new Vector2i(6, 3), BlockColorMap.InternalColorMap.OCEAN_GROUND);
			BufferedImage img = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);

			Graphics2D g = img.createGraphics();
			/* Background */
			g.setColor(new Color(0.2f, 0.2f, 0.6f, 1.0f));
			g.fillRect(0, 0, 1024, 1024);
			/* Insert the images */
			g.drawImage(img1, 0, 0, null);
			g.drawImage(img2, 512, 0, null);
			g.drawImage(img3, 0, 512, null);
			g.drawImage(img4, 512, 512, null);
			/* Add text on top */
			g.setColor(Color.WHITE);
			g.setFont(g.getFont().deriveFont(0, 32.0f));
			g.drawString("Relief", 0 + 32, 512 - 32);
			g.drawString("Flat", 1024 - 32 - g.getFontMetrics().stringWidth("Flat"), 512 - 32);
			g.drawString("Biomes", 0 + 32, 1024 - 32);
			g.drawString("Heightmap", 1024 - 32 - g.getFontMetrics().stringWidth("Biomes"), 1024 - 32);
			g.dispose();
			try (OutputStream out = Files.newOutputStream(Generator.OUTPUT_SCREENSHOTS.resolve("screenshot-2.png"))) {
				ImageIO.write(img, "png", out);
			}
		}
	}

	public static void generateScreenshots() throws Exception {
		Thread th = new Thread(() -> GuiMain.main(Generator.OUTPUT_INTERNAL_CACHE.resolve("BlockMapWorld").toString()));
		th.start();
		while (GuiMain.instance == null)
			Thread.yield();

		{ /* screenshot */
			log.info("Rendering world to generate screenshot");
			processSteps(Arrays.asList(
					() -> {
						GuiMain.instance.stage.setWidth(1280);
						GuiMain.instance.stage.setHeight(720);
						GuiMain.instance.stage.hide();
						GuiMain.instance.stage.show();
						GuiMain.instance.controller.loadLocal(Generator.OUTPUT_INTERNAL_CACHE.resolve("BlockMapWorld"));
						GuiMain.instance.controller.pinBox.requestFocus();
						/* Some random but fancy looking coordinates */
						GuiMain.instance.controller.renderer.viewport.translationProperty.set(new Vector2d(1024 + 512, 512));
						log.debug("Initialized GUI");
						return true;
					},
					() -> !GuiMain.instance.controller.renderer.getStatus().get().equals("No regions loaded"),
					() -> GuiMain.instance.controller.renderer.getStatus().get().equals("Done"),
					() -> {
						GuiMain.instance.controller.renderer.repaint();
						Thread.sleep(2000);
						GuiMain.instance.controller.renderer.repaint();
						return true;
					},
					() -> {
						log.info("Taking screenshot 1");
						WritableImage img = GuiMain.instance.stage.getScene().snapshot(null);
						try (OutputStream out = Files.newOutputStream(Generator.OUTPUT_SCREENSHOTS.resolve("screenshot-3.png"))) {
							ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", out);
						} catch (IOException e) {
							log.error(e);
						}
						return true;
					},
					() -> {
						/* Show all pins visible by default plus all structures */
						GuiMain.instance.controller.pinView.getCheckModel().check(GuiMain.instance.controller.checkedPins.get(PinType.STRUCTURE_MINESHAFT));
						GuiMain.instance.controller.pinView.getCheckModel().check(GuiMain.instance.controller.checkedPins.get(PinType.STRUCTURE_OCEAN_RUIN));
						GuiMain.instance.controller.pinView.getCheckModel().check(GuiMain.instance.controller.checkedPins.get(PinType.STRUCTURE_SHIPWRECK));
						//GuiMain.instance.controller.renderer.viewport.translationProperty.set(new Vector2d(1024, -1200));
						/* Some random but fancy looking coordinates */
						GuiMain.instance.controller.renderer.viewport.translationProperty.set(new Vector2d(1024 + 512, 512));
						GuiMain.instance.controller.renderer.viewport.zoomProperty.set(-1);
						GuiMain.instance.controller.renderer.repaint();
						return true;
					},
					() -> {
						Thread.sleep(5000);
						GuiMain.instance.controller.renderer.repaint();
						return true;
					},
					() -> {
						Thread.sleep(5000);
						GuiMain.instance.controller.renderer.repaint();
						return true;
					},
					() -> {
						Thread.sleep(5000);
						GuiMain.instance.controller.renderer.repaint();
						return true;
					},
					() -> {
						log.info("Taking screenshot 2");
						WritableImage img = GuiMain.instance.stage.getScene().snapshot(null);
						try (OutputStream out = Files.newOutputStream(Generator.OUTPUT_SCREENSHOTS.resolve("screenshot-4.png"))) {
							ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", out);
						} catch (IOException e) {
							log.error(e);
						}
						GuiMain.instance.controller.pinView.getCheckModel().clearCheck(GuiMain.instance.controller.checkedPins.get(
								PinType.STRUCTURE_MINESHAFT));
						GuiMain.instance.controller.pinView.getCheckModel().clearCheck(GuiMain.instance.controller.checkedPins.get(
								PinType.STRUCTURE_OCEAN_RUIN));
						GuiMain.instance.controller.pinView.getCheckModel().clearCheck(GuiMain.instance.controller.checkedPins.get(
								PinType.STRUCTURE_SHIPWRECK));
						return true;
					}));
		}

		{/* zoom gif */
			log.info("Rendering animated zoom gif");
			List<Callable<Boolean>> tasks = new ArrayList<>();
			List<BufferedImage> screenshots = new ArrayList<>();
			tasks.add(() -> {
				GuiMain.instance.controller.renderer.viewport.zoomProperty.set(0);
				GuiMain.instance.controller.renderer.viewport.translationProperty.set(new Vector2d(-512, 512));
				GuiMain.instance.controller.renderer.viewport.mousePosProperty.set(new Vector2d(640, 360));
				GuiMain.instance.controller.renderer.viewport.zoomProperty.set(7);
				GuiMain.instance.controller.renderer.repaint();
				return true;
			});
			tasks.add(() -> {
				Thread.sleep(5000);
				GuiMain.instance.controller.renderer.repaint();
				return true;
			});
			tasks.add(() -> {
				GuiMain.instance.controller.renderer.viewport.mousePosProperty.set(new Vector2d(640, 360));
				GuiMain.instance.controller.renderer.viewport.zoomProperty.set(-7);
				GuiMain.instance.controller.renderer.repaint();
				return true;
			});
			tasks.add(() -> {
				Thread.sleep(5000);
				GuiMain.instance.controller.renderer.repaint();
				return true;
			});
			for (double i = 5.6; i >= -5; i -= 0.5) {
				final double zoom = i;
				tasks.add(() -> {
					GuiMain.instance.controller.renderer.viewport.mousePosProperty.set(new Vector2d(640, 360));
					GuiMain.instance.controller.renderer.viewport.zoomProperty.set(zoom);
					GuiMain.instance.controller.renderer.repaint();
					Thread.sleep(750);
					GuiMain.instance.controller.renderer.repaint();
					Thread.sleep(750);
					GuiMain.instance.controller.renderer.repaint();
					return true;
				});
				tasks.add(() -> {
					log.info("Taking gif screenshot with zoom " + zoom);
					WritableImage img = GuiMain.instance.stage.getScene().snapshot(null);
					screenshots.add(SwingFXUtils.fromFXImage(img, null));
					return true;
				});
			}
			tasks.add(() -> {
				/* Unload the world and thus force caching */
				GuiMain.instance.controller.unload();
				return true;
			});
			processSteps(tasks);

			log.debug("Writing final gif to disk");
			GifSequenceWriter writer = new GifSequenceWriter(
					new FileImageOutputStream(Generator.OUTPUT_SCREENSHOTS.resolve("screenshot-0.gif").toFile()),
					BufferedImage.TYPE_INT_ARGB, 350, true);
			try {
				for (BufferedImage i : screenshots)
					writer.writeToSequence(i);
				writer.close();
			} catch (IOException e) {
				log.error("Could not write gif", e);
			}
		}

		log.debug("Closing GUI");
		processSteps(Arrays.asList(() -> {
			GuiMain.instance.controller.exit();
			return true;
		}));
		log.debug("Waiting for closed GUI");
		try {
			th.join();
		} catch (InterruptedException e) {
			log.error(e);
		}
	}

	private static BufferedImage generateScreenshot(RegionRenderer renderer, RenderSettings settings, Vector2i toRender, BlockColorMap.InternalColorMap colors)
			throws IOException {
		String fileName = "r." + toRender.x + "." + toRender.y + ".mca";
		/* Uncomment if you checked in the respective region files as resources*/
		// RegionFile file = new RegionFile(Paths.get(URI.create(Generator.class.getResource("/BlockMapWorld/region/" + fileName).toString())));
		/* Load them from the automatically generated world */
		RegionFile file = new RegionFile(Generator.OUTPUT_INTERNAL_CACHE.resolve("BlockMapWorld/region/" + fileName));
		settings.blockColors = colors.getColorMap();
		return renderer.render(toRender, file).getImage();
	}

	private static void processSteps(List<Callable<Boolean>> steps) throws InterruptedException, ExecutionException {
		for (Callable<Boolean> action : steps) {
			FutureTask<Boolean> task2 = null;
			do {
				FutureTask<Boolean> task = new FutureTask<>(action);
				task2 = task;
				Platform.runLater(() -> task.run());
				Thread.yield();
			} while (!task2.get());
		}
	}
}
