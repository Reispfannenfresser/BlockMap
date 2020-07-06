package de.piegames.blockmap.gui.decoration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.controlsfx.dialog.ExceptionDialog;
import org.joml.AABBd;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.joml.Vector3dc;
import org.joml.Vector3ic;

import com.codepoetics.protonpack.StreamUtils;

import de.piegames.blockmap.MinecraftVersion;
import de.piegames.blockmap.gui.DisplayViewport;
import de.piegames.blockmap.world.ChunkMetadata;
import de.piegames.blockmap.world.ChunkMetadata.ChunkMetadataCulled;
import de.piegames.blockmap.world.ChunkMetadata.ChunkMetadataFailed;
import de.piegames.blockmap.world.ChunkMetadata.ChunkMetadataRendered;
import de.piegames.blockmap.world.ChunkMetadata.ChunkMetadataVersion;
import de.piegames.blockmap.world.ChunkMetadata.ChunkMetadataVisitor;
import de.piegames.blockmap.world.LevelMetadata;
import de.saibotk.jmaw.ApiResponseException;
import de.saibotk.jmaw.MojangAPI;
import de.saibotk.jmaw.PlayerProfile;
import de.saibotk.jmaw.PlayerSkinTexture;
import de.saibotk.jmaw.PlayerTexturesProperty;
import de.saibotk.jmaw.TooManyRequestsException;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

public class Pin {

	public static class PinType {
		public static final PinType		MERGED_PIN					= new PinType("Merged pin", null, false, false, "/tmp.png");
		public static final PinType		ANY_PIN						= new PinType("Show pins", null, false, true, "textures/pins/pins.png");

		public static final PinType		CHUNK_PIN					= new PinType("Chunks", ANY_PIN, false, false, "textures/overlays/pin_chunks.png");
		public static final PinType		CHUNK_UNFINISHED			= new PinType("Unfinished chunk", CHUNK_PIN, false, false,
				"textures/overlays/pin_chunk_unfinished.png");
		public static final PinType		CHUNK_FAILED				= new PinType("Corrupt chunk", CHUNK_PIN, true, false,
				"textures/overlays/pin_chunk_corrupted.png");
		public static final PinType		CHUNK_OLD					= new PinType("Old chunk", CHUNK_PIN, true, false,
				"textures/overlays/pin_chunk_outdated.png");

		public static final PinType		PLAYER						= new PinType("Player", ANY_PIN, true, false, "textures/pins/player.png");
		public static final PinType		PLAYER_POSITION				= new PinType("Player position", PLAYER, true, false, "textures/pins/player.png");
		public static final PinType		PLAYER_SPAWN				= new PinType("Player spawnpoint", PLAYER, true, false, "textures/pins/spawn_player.png");

		public static final PinType		MAP							= new PinType("Map", ANY_PIN, true, false, "textures/pins/map.png");
		public static final PinType		MAP_POSITION				= new PinType("Map position", MAP, true, false, "textures/pins/map.png");
		public static final PinType		MAP_BANNER					= new PinType("Map banner", MAP, true, false, "textures/pins/banner.png");

		public static final PinType		VILLAGE						= new PinType("Village", ANY_PIN, true, false, "textures/structures/village.png");

		// Village structure
		public static final PinType		VILLAGE_HOME				= new PinType("Village home", VILLAGE, true, false, "textures/villages/home.png");
		public static final PinType		VILLAGE_MEETING				= new PinType("Meetingpoint", VILLAGE, true, false, "textures/villages/bell.png");

		// Village crafting
		public static final PinType		VILLAGE_LEATHERWORKER		= new PinType("Leatherworker", VILLAGE, true, false, "textures/villages/leatherworker.png");
		public static final PinType		VILLAGE_MASON				= new PinType("Mason", VILLAGE, true, false, "textures/villages/mason.png");
		public static final PinType		VILLAGE_FLETCHER			= new PinType("Fletcher", VILLAGE, true, false, "textures/villages/fletcher.png");

		// Ironworks
		public static final PinType		VILLAGE_TOOLSMITH			= new PinType("Toolsmith", VILLAGE, true, false, "textures/villages/toolsmith.png");
		public static final PinType		VILLAGE_WEAPONSMITH			= new PinType("Weaponsmith", VILLAGE, true, false, "textures/villages/weaponsmith.png");
		public static final PinType		VILLAGE_ARMORER				= new PinType("Armorer", VILLAGE, true, false, "textures/villages/armorer.png");

		// Food
		public static final PinType		VILLAGE_FARMER				= new PinType("Farmer", VILLAGE, true, false, "textures/villages/farmer.png");
		public static final PinType		VILLAGE_SHEPHERD			= new PinType("Shepherd", VILLAGE, true, false, "textures/villages/shepherd.png");
		public static final PinType		VILLAGE_BUTCHER				= new PinType("Butcher", VILLAGE, true, false, "textures/villages/butcher.png");
		public static final PinType		VILLAGE_FISHERMAN			= new PinType("Fisherman", VILLAGE, true, false, "textures/villages/fisherman.png");
		// TODO add texture
		public static final PinType VILLAGE_BEENEST = new PinType("Bee nest", VILLAGE, true, false, "textures/pins/spawn_map.png");
		public static final PinType VILLAGE_PORTAL = new PinType("Portal", VILLAGE, true, false, "textures/pins/spawn_map.png");

		// Intellectual
		public static final PinType		VILLAGE_CLERIC				= new PinType("Cleric", VILLAGE, true, false, "textures/villages/cleric.png");
		public static final PinType		VILLAGE_CARTOGRAPHER		= new PinType("Cartographer", VILLAGE, true, false, "textures/villages/cartographer.png");
		public static final PinType		VILLAGE_LIBRARIAN			= new PinType("Librarian", VILLAGE, true, false, "textures/villages/librarian.png");

		public static final PinType		WORLD_SPAWN					= new PinType("Spawnpoint", ANY_PIN, true, false, "textures/pins/spawn_map.png");

		public static final PinType		STRUCTURE					= new PinType("Structures", ANY_PIN, false, true, "textures/pins/structures.png");
		public static final PinType		STRUCTURE_TREASURE			= new PinType("Treasure", STRUCTURE, true, false,
				"textures/structures/buried_treasure.png");
		public static final PinType		STRUCTURE_PYRAMID			= new PinType("Pyramid", STRUCTURE, true, false, "textures/structures/desert_pyramid.png");
		public static final PinType		STRUCTURE_END_CITY			= new PinType("End city", STRUCTURE, true, false, "textures/structures/end_city.png");
		public static final PinType		STRUCTURE_FORTRESS			= new PinType("Nether Fortress", STRUCTURE, true, false,
				"textures/structures/fortress.png");
		public static final PinType		STRUCTURE_IGLOO				= new PinType("Igloo", STRUCTURE, true, false, "textures/structures/igloo.png");
		public static final PinType		STRUCTURE_JUNGLE_TEMPLE		= new PinType("Jungle temple", STRUCTURE, true, false,
				"textures/structures/jungle_pyramid.png");
		public static final PinType		STRUCTURE_MANSION			= new PinType("Mansion", STRUCTURE, true, false, "textures/structures/mansion.png");
		public static final PinType		STRUCTURE_MINESHAFT			= new PinType("Mineshaft", STRUCTURE, false, false, "textures/structures/mineshaft.png");
		public static final PinType		STRUCTURE_OCEAN_MONUMENT	= new PinType("Ocean monument", STRUCTURE, true, false, "textures/structures/monument.png");
		public static final PinType		STRUCTURE_OCEAN_RUIN		= new PinType("Ocean ruin", STRUCTURE, false, false, "textures/structures/ocean_ruin.png");
		public static final PinType		STRUCTURE_SHIPWRECK			= new PinType("Shipwreck", STRUCTURE, false, false, "textures/structures/shipwreck.png");
		public static final PinType		STRUCTURE_STRONGHOLD		= new PinType("Stronghold", STRUCTURE, true, false, "textures/structures/stronghold.png");
		public static final PinType		STRUCTURE_WITCH_HUT			= new PinType("Witch hut", STRUCTURE, true, false, "textures/structures/swamp_hut.png");
		public static final PinType		STRUCTURE_OUTPOST			= new PinType("Pillager outpost", STRUCTURE, true, false,
				"textures/structures/outpost.png");
		public static final PinType STRUCTURE_RUINED_PORTAL = new PinType("Ruined portal", STRUCTURE, true, false, "textures/structures/outpost.png");
		public static final PinType STRUCTURE_NETHER_FOSSIL = new PinType("Nether fossil", STRUCTURE, true, false, "textures/structures/outpost.png");
		public static final PinType STRUCTURE_BASTION_REMNANT = new PinType("Bastion remnant", STRUCTURE, true, false, "textures/structures/outpost.png");

		protected final List<PinType>	children					= new ArrayList<>();
		private final String			name;
		public final boolean			selectedByDefault, expandedByDefault;
		public final Image				image;

		PinType(String name, PinType parent, boolean selectedByDefault, boolean expandedByDefault, String path) {
			this(name, parent, selectedByDefault, expandedByDefault, new Image(Optional.ofNullable(Pin.class.getResource(path)).map(Object::toString).orElse(
					"/tmp.png"),
					128, 128, true, false));
		}

		PinType(String name, PinType parent, boolean selectedByDefault, boolean expandedByDefault, Image image) {
			this.name = name;
			this.selectedByDefault = selectedByDefault;
			this.expandedByDefault = expandedByDefault;
			this.image = image;
			if (parent != null)
				parent.children.add(this);
		}

		public List<PinType> getChildren() {
			return Collections.unmodifiableList(children);
		}

		@Override
		public String toString() {
			return name;
		}

		/* Code to map structure names to their respective chunk pin */
		static final Map<String, PinType>	STRUCTURE_TYPES;
		static final Map<String, PinType>	VILLAGE_MAPPING;

		static {
			Map<String, PinType> structureTypes = new HashMap<>();
			/* Minecraft 1.13 */
			structureTypes.put("Buried_Treasure", STRUCTURE_TREASURE);
			structureTypes.put("Desert_Pyramid", STRUCTURE_PYRAMID);
			structureTypes.put("Igloo", STRUCTURE_IGLOO);
			structureTypes.put("Jungle_Pyramid", STRUCTURE_JUNGLE_TEMPLE);
			structureTypes.put("Mansion", STRUCTURE_MANSION);
			structureTypes.put("Mineshaft", STRUCTURE_MINESHAFT);
			structureTypes.put("Monument", STRUCTURE_OCEAN_MONUMENT);
			structureTypes.put("Ocean_Ruin", STRUCTURE_OCEAN_RUIN);
			structureTypes.put("Shipwreck", STRUCTURE_SHIPWRECK);
			structureTypes.put("Stronghold", STRUCTURE_STRONGHOLD);
			structureTypes.put("Swamp_Hut", STRUCTURE_WITCH_HUT);
			structureTypes.put("EndCity", STRUCTURE_END_CITY);
			structureTypes.put("Fortress", STRUCTURE_FORTRESS);
			structureTypes.put("Village", VILLAGE);
			/* Minecraft 1.14+ */
			structureTypes.put("minecraft:buried_treasure", STRUCTURE_TREASURE);
			structureTypes.put("minecraft:desert_pyramid", STRUCTURE_PYRAMID);
			structureTypes.put("minecraft:igloo", STRUCTURE_IGLOO);
			structureTypes.put("minecraft:jungle_pyramid", STRUCTURE_JUNGLE_TEMPLE);
			structureTypes.put("minecraft:mansion", STRUCTURE_MANSION);
			structureTypes.put("minecraft:mineshaft", STRUCTURE_MINESHAFT);
			structureTypes.put("minecraft:monument", STRUCTURE_OCEAN_MONUMENT);
			structureTypes.put("minecraft:ocean_ruin", STRUCTURE_OCEAN_RUIN);
			structureTypes.put("minecraft:shipwreck", STRUCTURE_SHIPWRECK);
			structureTypes.put("minecraft:stronghold", STRUCTURE_STRONGHOLD);
			structureTypes.put("minecraft:swamp_hut", STRUCTURE_WITCH_HUT);
			structureTypes.put("minecraft:end_city", STRUCTURE_END_CITY);
			structureTypes.put("minecraft:fortress", STRUCTURE_FORTRESS);
			structureTypes.put("minecraft:pillager_outpost", STRUCTURE_OUTPOST);
			structureTypes.put("minecraft:village", VILLAGE);
			structureTypes.put("minecraft:ruined_portal", STRUCTURE_RUINED_PORTAL);
			structureTypes.put("minecraft:nether_fossil", STRUCTURE_NETHER_FOSSIL);
			structureTypes.put("minecraft:bastion_remnant", STRUCTURE_BASTION_REMNANT);
			STRUCTURE_TYPES = Collections.unmodifiableMap(structureTypes);

			Map<String, PinType> villageMapping = new HashMap<>();

			villageMapping.put("minecraft:home", VILLAGE_HOME);
			villageMapping.put("minecraft:meeting", VILLAGE_MEETING);

			villageMapping.put("minecraft:leatherworker", VILLAGE_LEATHERWORKER);
			villageMapping.put("minecraft:mason", VILLAGE_MASON);
			villageMapping.put("minecraft:fletcher", VILLAGE_FLETCHER);

			villageMapping.put("minecraft:toolsmith", VILLAGE_TOOLSMITH);
			villageMapping.put("minecraft:weaponsmith", VILLAGE_WEAPONSMITH);
			villageMapping.put("minecraft:armorer", VILLAGE_ARMORER);

			villageMapping.put("minecraft:farmer", VILLAGE_FARMER);
			villageMapping.put("minecraft:shepherd", VILLAGE_SHEPHERD);
			villageMapping.put("minecraft:butcher", VILLAGE_BUTCHER);
			villageMapping.put("minecraft:fisherman", VILLAGE_FISHERMAN);

			villageMapping.put("minecraft:bee_nest", VILLAGE_BEENEST);
			villageMapping.put("minecraft:nether_portal", VILLAGE_PORTAL);

			villageMapping.put("minecraft:cleric", VILLAGE_CLERIC);
			villageMapping.put("minecraft:cartographer", VILLAGE_CARTOGRAPHER);
			villageMapping.put("minecraft:librarian", VILLAGE_LIBRARIAN);

			VILLAGE_MAPPING = Collections.unmodifiableMap(villageMapping);
		}
	}

	private static Log				log		= LogFactory.getLog(Pin.class);

	/* Pin specific */

	public final PinType			type;
	protected final Vector2dc		position;
	protected final DisplayViewport	viewport;

	/* GUI specific */

	private Node					topGui, bottomGui;
	protected Button				button;
	protected PopOver				info;

	/* Animation specific */

	private List<KeyValue>			animShow, animHide;
	private Timeline				timeline;
	private boolean					added	= false, visible = false;
	double							minHeight, maxHeight;

	public Pin(Vector2dc position, PinType type, DisplayViewport viewport) {
		this.type = Objects.requireNonNull(type);
		this.viewport = viewport;
		this.position = Objects.requireNonNull(position);
	}

	public final Node getTopGui() {
		if (topGui != null)
			return topGui;
		else
			return topGui = initTopGui();
	}

	public final Node getBottomGui() {
		if (bottomGui != null)
			return bottomGui;
		else
			return bottomGui = initBottomGui();
	}

	protected final PopOver getInfo() {
		if (info != null)
			return info;
		else
			return info = initInfo();
	}

	protected Node initTopGui() {
		ImageView img = new ImageView(type.image);
		img.setSmooth(false);
		button = new Button(null, img);
		button.getStyleClass().add("pin");
		img.setPreserveRatio(true);
		button.setTooltip(new Tooltip(type.toString()));
		img.fitHeightProperty().bind(Bindings.createDoubleBinding(() -> button.getFont().getSize() * 2, button.fontProperty()));

		button.setOnAction(mouseEvent -> getInfo().show(button));
		return wrapGui(button, position, viewport);
	}

	protected Node initBottomGui() {
		return null;
	}

	protected PopOver initInfo() {
		PopOver info = new PopOver();
		info.setArrowLocation(ArrowLocation.BOTTOM_CENTER);
		info.setAutoHide(true);
		info.setHeaderAlwaysVisible(true);
		/* Workaround: If the PopOver it too thin, it will be placed a bit off. Bug report: https://github.com/controlsfx/controlsfx/issues/1095 */
		Label content = new Label();
		content.setPrefWidth(130);
		info.setContentNode(content);
		info.setTitle(type.name);
		return info;
	}

	private final List<KeyValue> getAnimShow() {
		if (animShow == null)
			return animShow = animationKeys(true);
		else
			return animShow;
	}

	private final List<KeyValue> getAnimHide() {
		if (animHide == null)
			return animHide = animationKeys(false);
		else
			return animHide;
	}

	private final List<KeyValue> animationKeys(boolean visible) {
		return Collections.unmodifiableList(Arrays.asList(
				new KeyValue(getTopGui().opacityProperty(), visible ? 1.0 : 0.0, Interpolator.EASE_BOTH),
				new KeyValue(getTopGui().visibleProperty(), visible, visible ? DISCRETE_INSTANT : Interpolator.DISCRETE)));
	}

	final void updateAnimation(double zoom, double height, boolean added, Pane parent) {
		boolean visible = added && height >= this.minHeight && height < maxHeight;
		if (visible != this.visible || added != this.added) {
			this.visible = visible;

			if (!this.added && added) {
				if (getBottomGui() != null)
					parent.getChildren().add(getBottomGui());
				if (getTopGui() != null)
					parent.getChildren().add(getTopGui());
				this.added = added;
			}

			if (timeline != null)
				timeline.pause();
			timeline = new Timeline(
					new KeyFrame(
							Duration.millis(500),
							null,
							e -> {
								if (this.added && !added) {
									if (getTopGui() != null)
										parent.getChildren().remove(getTopGui());
									if (getBottomGui() != null)
										parent.getChildren().remove(getBottomGui());
									this.added = added;
								}
							},
							visible ? getAnimShow() : getAnimHide()));
			timeline.playFromStart();
		}
		/* Use bottomGui instead of getBottomGui() here since we do not want to trigger an initialization here */
		if (bottomGui != null)
			bottomGui.setVisible(zoom > -3);
	}

	private static class ChunkPin extends Pin {
		public final List<Vector2ic>	chunkPositions;
		public final Image				image;

		public ChunkPin(PinType type, Vector2dc centerPos, List<Vector2ic> chunkPositions, Image image, DisplayViewport viewport) {
			super(centerPos, type, viewport);
			this.chunkPositions = Objects.requireNonNull(chunkPositions);
			this.image = image;
		}

		@Override
		protected Node initBottomGui() {
			Polygon shape = new Polygon();
			shape.getPoints().setAll(chunkPositions.stream().flatMap(v -> Stream.of(v.x(), v.y())).map(d -> (d + 1) * 16.0).collect(Collectors.toList()));
			shape.setFill(new ImagePattern(image, 0, 0, 16, 16, false));
			getTopGui().hoverProperty().addListener(e -> {
				if (getTopGui().isHover())
					shape.setOpacity(0.6);
				else
					shape.setOpacity(0.2);
			});
			shape.setOpacity(0.2);
			shape.setMouseTransparent(true);
			shape.setCache(true);
			shape.setCacheHint(CacheHint.SCALE);
			shape.setViewOrder(1);
			return shape;
		}
	}

	private static class UnfinishedChunkPin extends ChunkPin {

		protected List<String> chunkGeneration;

		public UnfinishedChunkPin(Vector2dc centerPos, List<Vector2ic> chunkPositions, List<String> chunkGeneration, DisplayViewport viewport) {
			super(PinType.CHUNK_UNFINISHED, centerPos, chunkPositions, new Image(Pin.class.getResource("textures/overlays/chunk_unfinished.png").toString(), 64,
					64, true, false), viewport);
			this.chunkGeneration = Objects.requireNonNull(chunkGeneration);
		}

		@Override
		protected PopOver initInfo() {
			PopOver info = super.initInfo();

			GridPane popContent = new GridPane();
			popContent.getStyleClass().add("grid");
			info.setContentNode(popContent);

			StreamUtils.zipWithIndex(chunkGeneration.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).entrySet().stream())
					.forEach(index -> {
						popContent.add(new Label(index.getValue().getKey() + ":"), 0, (int) index.getIndex());
						popContent.add(new Label(index.getValue().getValue() + " chunks"), 1, (int) index.getIndex());
					});
			return info;
		}
	}

	private static class OldChunkPin extends ChunkPin {

		protected Collection<ChunkMetadataVersion> chunks;

		public OldChunkPin(Vector2dc centerPos, List<Vector2ic> chunkPositions, Collection<ChunkMetadataVersion> chunks, DisplayViewport viewport) {
			super(PinType.CHUNK_OLD, centerPos, chunkPositions, new Image(Pin.class.getResource("textures/overlays/chunk_outdated.png").toString(), 64, 64,
					true, false), viewport);
			this.chunks = chunks;
		}

		@Override
		protected PopOver initInfo() {
			PopOver info = super.initInfo();

			GridPane popContent = new GridPane();
			popContent.getStyleClass().add("grid");
			info.setContentNode(popContent);

			popContent.add(new Label("Found version(s):"), 0, 0);
			popContent.add(new Label(
					chunks.stream().map(c -> c.version).distinct().sorted().map(String::valueOf).collect(Collectors.joining(", ", "{", "}"))), 1, 0);
			popContent.add(new Label("Unsupported for reason(s):"), 0, 1);
			popContent.add(new Label(
					chunks.stream().map(c -> c.message).distinct().sorted().map(String::valueOf).collect(Collectors.joining(", ", "{", "}"))), 1, 1);
			popContent.add(new Separator(), 0, 2, 2, 1);
			popContent.add(new Label("Supported versions:"), 0, 3);
			popContent.add(new Label("Name:"), 1, 3);
			int row = 4;
			for (MinecraftVersion supported : MinecraftVersion.values()) {
				if (supported.maxVersion == Integer.MAX_VALUE)
					popContent.add(new Label(supported.minVersion + "+"), 0, row);
				else
					popContent.add(new Label(supported.minVersion + "-" + supported.maxVersion), 0, row);

				popContent.add(new Label(supported.versionName), 1, row++);
			}
			return info;
		}
	}

	private static class FailedChunkPin extends ChunkPin {

		protected Collection<ChunkMetadataFailed> chunks;

		public FailedChunkPin(Vector2dc centerPos, List<Vector2ic> chunkPositions, Collection<ChunkMetadataFailed> chunks, DisplayViewport viewport) {
			super(PinType.CHUNK_FAILED, centerPos, chunkPositions, new Image(Pin.class.getResource("textures/overlays/chunk_corrupted.png").toString(), 64, 64,
					true, false), viewport);
			this.chunks = Objects.requireNonNull(chunks);
		}

		@Override
		protected PopOver initInfo() {
			PopOver info = super.initInfo();

			GridPane popContent = new GridPane();
			popContent.getStyleClass().add("grid");
			info.setContentNode(popContent);

			StreamUtils.zipWithIndex(chunks.stream()
					.map(e -> e.error).collect(Collectors.groupingBy(Exception::toString)).entrySet().stream())
					.forEach(index -> {
						Exception e = index.getValue().getValue().get(0);
						popContent.add(new Label(e.getClass().getSimpleName()), 0, (int) index.getIndex());
						popContent.add(new Label("×" + index.getValue().getValue().size()), 1, (int) index.getIndex());
						Button button = new Button("Show trace…");
						button.setOnAction(__ -> {
							ExceptionDialog d = new ExceptionDialog(e);
							d.setHeaderText(e.toString());
							d.showAndWait();
						});
						popContent.add(button, 2, (int) index.getIndex());
					});
			return info;
		}
	}

	private static class MapPin extends Pin {

		static final Color[] COLOR_IDS;

		static {
			/* Source: https://minecraft-de.gamepedia.com/Kartendaten#Liste_der_Farbwerte */
			Color[] rawColors = new Color[] {
					Color.TRANSPARENT,
					Color.rgb(125, 176, 55), /* grass */
					Color.rgb(244, 230, 161), /* sand */
					Color.rgb(197, 197, 197), /* cobweb */
					Color.rgb(252, 0, 0), /* lava */
					Color.rgb(158, 158, 252), /* ice */
					Color.rgb(165, 165, 165), /* iron */
					Color.rgb(0, 123, 0), /* foliage */
					Color.rgb(252, 252, 252), /* quartz */
					Color.rgb(162, 166, 182), /* clay */
					Color.rgb(149, 108, 76), /* dirt */
					Color.rgb(111, 111, 111), /* stone */
					Color.rgb(63, 63, 252), /* water */
					Color.rgb(141, 118, 71), /* oak */
					Color.rgb(252, 249, 242), /* white wool */
					Color.rgb(213, 125, 50), /* orange wool */
					Color.rgb(176, 75, 213), /* magenta wool */
					Color.rgb(101, 151, 213), /* light blue wool */
					Color.rgb(226, 226, 50), /* yellow wool */
					Color.rgb(125, 202, 25), /* light green wool */
					Color.rgb(239, 125, 163), /* pink wool */
					Color.rgb(75, 75, 75), /* grey wool */
					Color.rgb(151, 151, 151), /* light grey wool */
					Color.rgb(75, 125, 151), /* turquoise wool */
					Color.rgb(125, 62, 176), /* purple wool */
					Color.rgb(50, 75, 176), /* blue wool */
					Color.rgb(101, 75, 50), /* brown wool */
					Color.rgb(101, 125, 50), /* green wool */
					Color.rgb(151, 50, 50), /* red wool */
					Color.rgb(25, 25, 25), /* black wool */
					Color.rgb(247, 235, 76), /* gold */
					Color.rgb(91, 216, 210), /* diamond */
					Color.rgb(73, 129, 252), /* lapis lazuli */
					Color.rgb(0, 214, 57), /* emerald */
					Color.rgb(127, 85, 48), /* spruce */
					Color.rgb(111, 2, 0), /* nether */
					Color.rgb(209, 177, 161), /* white hardened clay */
					Color.rgb(159, 82, 36), /* orange hardened clay */
					Color.rgb(149, 87, 108), /* magenta hardened clay */
					Color.rgb(112, 108, 138), /* light blue hardened clay */
					Color.rgb(186, 133, 36), /* yellow hardened clay */
					Color.rgb(103, 117, 53), /* light green hardened clay */
					Color.rgb(160, 77, 78), /* pink hardened clay */
					Color.rgb(57, 41, 35), /* grey hardened clay */
					Color.rgb(135, 107, 98), /* light grey hardened clay */
					Color.rgb(87, 92, 92), /* turquoise hardened clay */
					Color.rgb(122, 73, 88), /* purple hardened clay */
					Color.rgb(76, 62, 92), /* blue hardened clay */
					Color.rgb(76, 50, 35), /* brown hardened clay */
					Color.rgb(76, 82, 42), /* green hardened clay */
					Color.rgb(142, 60, 46), /* red hardened clay */
					Color.rgb(37, 22, 16),/* black hardened clay */
			};
			COLOR_IDS = new Color[rawColors.length * 4];
			for (int i = 0; i < rawColors.length; i++) {
				COLOR_IDS[4 * i + 0] = rawColors[i].deriveColor(0, 1, 180.0 / 255.0, 1);
				COLOR_IDS[4 * i + 1] = rawColors[i].deriveColor(0, 1, 220.0 / 255.0, 1);
				COLOR_IDS[4 * i + 2] = rawColors[i];
				COLOR_IDS[4 * i + 3] = rawColors[i].deriveColor(0, 1, 135.0 / 255.0, 1);
			}
		}

		protected List<de.piegames.blockmap.world.LevelMetadata.MapPin> maps;

		public MapPin(Vector2d position, List<de.piegames.blockmap.world.LevelMetadata.MapPin> maps, DisplayViewport viewport) {
			super(position, PinType.MAP_POSITION, viewport);
			this.maps = Objects.requireNonNull(maps);
		}

		@Override
		protected PopOver initInfo() {
			PopOver info = super.initInfo();
			GridPane content = new GridPane();
			content.getStyleClass().add("grid");

			int rowCount = 0;

			if (maps.size() > 1) {
				content.add(new Label("Map count:"), 0, rowCount);
				content.add(new Label(Integer.toString(maps.size())), 1, rowCount++);
			}

			for (de.piegames.blockmap.world.LevelMetadata.MapPin map : maps) {
				BorderPane mapPane = new BorderPane();
				mapPane.setLeft(new Label("Scale:"));
				mapPane.setRight(new Label("1:" + (1 << map.getScale())));

				if (map.getColors().isPresent()) {
					byte[] data = map.getColors().get();
					WritableImage image = new WritableImage(128, 128);
					for (int x = 0; x < 128; x++)
						for (int y = 0; y < 128; y++)
							image.getPixelWriter().setColor(x, y, COLOR_IDS[0xFF & data[y << 7 | x]]);
					mapPane.setBottom(new ImageView(image));
				}
				content.add(mapPane, 0, rowCount++, 1, 2);
			}
			info.setContentNode(content);
			return info;
		}

		@Override
		protected Node initBottomGui() {
			StackPane stack = new StackPane();
			stack.getChildren().setAll(maps.stream().map(map -> map.getScale()).distinct().map(scale -> {
				int size = 128 * (1 << scale);
				Rectangle rect = new Rectangle(size, size, new Color(0.9f, 0.15f, 0.15f, 0.02f));
				rect.setStroke(new Color(0.9f, 0.15f, 0.15f, 0.4f));
				rect.setMouseTransparent(true);
				rect.setPickOnBounds(false);
				getTopGui().hoverProperty().addListener(e -> {
					if (getTopGui().isHover())
						rect.setFill(new Color(0.9f, 0.15f, 0.15f, 0.2f));
					else
						rect.setFill(new Color(0.9f, 0.15f, 0.15f, 0.02f));
				});
				return rect;
			}).collect(Collectors.toList()));
			Translate t = new Translate();
			t.xProperty().bind(stack.widthProperty().multiply(-0.5));
			t.yProperty().bind(stack.heightProperty().multiply(-0.5));
			stack.getTransforms().addAll(t, new Translate(position.x(), position.y()));
			stack.setPickOnBounds(false);
			stack.setMouseTransparent(true);
			stack.setViewOrder(1);
			return stack;
		}
	}

	private static class PlayerPin extends Pin implements Runnable {

		/*
		 * Cache the API object because even if the API itself is stateless, there is still some initialization (HTTP client, GSON type adapters)
		 * done at the beginning.
		 */
		private static MojangAPI			api;

		protected LevelMetadata.PlayerPin		player;
		protected StringProperty			playerName	= new SimpleStringProperty("loading…");
		protected ScheduledExecutorService	backgroundThread;

		public PlayerPin(LevelMetadata.PlayerPin player, ScheduledExecutorService backgroundThread, DisplayViewport viewport) {
			super(new Vector2d(player.getPosition().x(), player.getPosition().z()), PinType.PLAYER_POSITION, viewport);
			this.player = player;
			this.backgroundThread = Objects.requireNonNull(backgroundThread);
		}

		@Override
		protected Node initTopGui() {
			Node node = super.initTopGui();
			backgroundThread.execute(this);
			return node;
		}

		@Override
		protected PopOver initInfo() {
			PopOver info = super.initInfo();
			GridPane content = new GridPane();
			content.getStyleClass().add("grid");

			content.add(new Label("Name:"), 0, 2);
			Label playerName = new Label("loading...");
			playerName.textProperty().bind(this.playerName);
			content.add(playerName, 1, 2);

			player.getSpawnpoint().ifPresent(spawn -> {
				content.add(new Label("Spawnpoint: "), 0, 3);
				Button jumpButton = new Button(spawn.toString());
				jumpButton.setTooltip(new Tooltip("Click to go there"));
				content.add(jumpButton, 1, 3);
				jumpButton.setOnAction(e -> {
					Vector2d spawnpoint = new Vector2d(spawn.x(), spawn.z());
					AABBd frustum = viewport.frustumProperty.get();
					viewport.translationProperty.set(spawnpoint.negate().add((frustum.maxX - frustum.minX) / 2, (frustum.maxY - frustum.minY) / 2));
					info.hide();
				});
			});

			info.setContentNode(content);
			return info;
		}

		private ImageView getSkin(String url) {
			log.debug("Loading player skin from: " + url);
			Image image = new Image(url);
			PixelReader reader = image.getPixelReader();
			image = new WritableImage(reader, 8, 8, 8, 8);

			ImageView graphic = new ImageView(image);
			graphic.setSmooth(false);
			graphic.setPreserveRatio(true);
			graphic.fitHeightProperty().bind(Bindings.createDoubleBinding(() -> button.getFont().getSize() * 2, button.fontProperty()));
			return graphic;
		}

		@Override
		public void run() {
			/* This does not need to be thread safe */
			if (api == null)
				api = new MojangAPI();
			Optional<PlayerProfile> playerInfo = player.getUUID().flatMap(uuid -> {
				try {
					return api.getPlayerProfile(uuid);
				} catch (TooManyRequestsException e) {
					log.warn("Too many requests, trying again later…");
					backgroundThread.schedule(this, 61, TimeUnit.SECONDS);
					return Optional.empty();
				} catch (ApiResponseException e) {
					log.warn("Could not load player profile for uuid <" + uuid + ">", e);
					return Optional.empty();
				}
			});
			Platform.runLater(() -> playerName.set(playerInfo.map(PlayerProfile::getUsername).orElse("(failed loading)")));
			playerInfo.flatMap(PlayerProfile::getTexturesProperty)
					.flatMap(PlayerTexturesProperty::getSkin)
					.map(PlayerSkinTexture::getUrl)
					.map(this::getSkin)
					.ifPresent(image -> Platform.runLater(() -> button.setGraphic(image)));
		}
	}

	private static class PlayerSpawnpointPin extends Pin {
		protected LevelMetadata.PlayerPin player;

		public PlayerSpawnpointPin(LevelMetadata.PlayerPin player, DisplayViewport viewport) {
			super(new Vector2d(player.getSpawnpoint().get().x(), player.getSpawnpoint().get().z()), PinType.PLAYER_SPAWN, viewport);
			this.player = Objects.requireNonNull(player);
		}

		@Override
		protected PopOver initInfo() {
			PopOver info = super.initInfo();
			GridPane content = new GridPane();
			content.getStyleClass().add("grid");

			content.add(new Label("Player position:"), 0, 2);

			Vector3dc position = player.getPosition();
			Button jumpButton = new Button(position.toString());
			jumpButton.setTooltip(new Tooltip("Click to go there"));
			content.add(jumpButton, 1, 2);
			jumpButton.setOnAction(e -> {
				Vector2d spawnpoint = new Vector2d(position.x(), position.z());
				AABBd frustum = viewport.frustumProperty.get();
				viewport.translationProperty.set(spawnpoint.negate().add((frustum.maxX - frustum.minX) / 2, (frustum.maxY - frustum.minY) / 2));
				info.hide();
			});

			info.setContentNode(content);
			return info;
		}
	}

	private static class VillageObjectPin extends Pin {
		protected LevelMetadata.VillageObjectPin villageObjectPin;

		public VillageObjectPin(LevelMetadata.VillageObjectPin villageObjectPin, DisplayViewport viewport) {
			super(new Vector2d(villageObjectPin.getPosition().x(), villageObjectPin.getPosition().z()), PinType.VILLAGE_MAPPING.get(villageObjectPin.getType()),
					viewport);
			this.villageObjectPin = Objects.requireNonNull(villageObjectPin);
		}

		@Override
		protected PopOver initInfo() {
			PopOver info = super.initInfo();
			GridPane content = new GridPane();
			content.getStyleClass().add("grid");

			content.add(new Label("Position: "), 0, 2, 1, 2);
			content.add(new Label(villageObjectPin.getPosition().toString()), 1, 2, 2, 2);

			content.add(new Label("Free tickets: "), 0, 4);
			content.add(new Label(String.valueOf(villageObjectPin.getFreeTickets())), 1, 4);

			content.add(new Label(type.name), 0, 5);

			info.setContentNode(content);
			return info;
		}
	}

	private static final class WorldSpawnPin extends Pin {
		protected Vector3ic spawn;

		public WorldSpawnPin(Vector3ic spawn, DisplayViewport viewport) {
			super(new Vector2d(spawn.x(), spawn.z()), PinType.WORLD_SPAWN, viewport);
			this.spawn = spawn;
		}

		@Override
		protected PopOver initInfo() {
			PopOver info = super.initInfo();
			GridPane content = new GridPane();
			content.getStyleClass().add("grid");

			content.add(new Label("Position:"), 0, 2);
			content.add(new Label(spawn.toString()), 1, 2);

			info.setContentNode(content);
			return info;
		}
	}

	static final class MergedPin extends Pin {

		final int					subCount;
		final Map<PinType, Long>	pinCount;

		public MergedPin(Pin subLeft, Pin subRight, int subCount, Vector2dc position, Map<PinType, Long> pinCount, DisplayViewport viewport) {
			super(position, PinType.MERGED_PIN, viewport);
			this.subCount = subCount;
			this.pinCount = Collections.unmodifiableMap(pinCount);
		}

		@Override
		protected Node initTopGui() {
			/* If there are to many different pins, merge some */
			Map<PinType, Long> pinCount = new HashMap<>(this.pinCount);

			/*
			 * Merge the village pins not based on their count, but based on the current zoom factor.
			 */
			if (minHeight > 50) {
				// if (minHeight > 50 ||
				// (pinCount.size() > 4 && (PinType.VILLAGE_MAPPING.values().stream().filter(x -> pinCount.getOrDefault(x, 0L) > 0).count() > 1))) {
				/* Merge village pins to one */
				List<PinType> villageObjects = PinType.VILLAGE.children.stream().filter(this.pinCount::containsKey).collect(Collectors.toList());
				if (!villageObjects.isEmpty()) {
					pinCount.keySet().removeAll(villageObjects);
					pinCount.put(PinType.VILLAGE, villageObjects.stream().mapToLong(this.pinCount::get).sum());
				}
			}

			if (pinCount.size() > 4) {
				/* Merge all structures */
				List<PinType> structures = PinType.STRUCTURE.children.stream().filter(this.pinCount::containsKey).collect(Collectors.toList());
				if (!structures.isEmpty()) {
					pinCount.keySet().removeAll(structures);
					pinCount.put(PinType.STRUCTURE, structures.stream().mapToLong(this.pinCount::get).sum());
				}

			}

			if (pinCount.size() > 4 && pinCount.getOrDefault(PinType.MAP_POSITION, 0L) > 0 && pinCount.getOrDefault(PinType.MAP_BANNER, 0L) > 0) {
				/* Merge map with banners */
				pinCount.put(PinType.MAP_POSITION, pinCount.get(PinType.MAP_POSITION) + pinCount.get(PinType.MAP_BANNER));
				pinCount.remove(PinType.MAP_BANNER);
			}
			if (pinCount.size() > 4 && pinCount.getOrDefault(PinType.PLAYER_POSITION, 0L) > 0 && pinCount.getOrDefault(PinType.PLAYER_SPAWN, 0L) > 0) {
				/* Just remove them, showing more players than there are may be confusing */
				pinCount.remove(PinType.PLAYER_SPAWN);
			}

			int columns = (int) Math.floor(Math.sqrt(pinCount.size()));
			GridPane box = new GridPane();
			box.getStyleClass().add("mergedpin-box");

			/* Image for the pin's button */
			StreamUtils.zipWithIndex(pinCount.entrySet().stream()).forEach(e -> {
				ImageView img = new ImageView(e.getValue().getKey().image);
				img.setSmooth(false);
				img.setPreserveRatio(true);
				Label label = new Label(String.format("%dx", e.getValue().getValue()), img);
				img.fitHeightProperty().bind(Bindings.createDoubleBinding(() -> label.getFont().getSize() * 1.5, label.fontProperty()));

				box.add(label, (int) e.getIndex() % columns, (int) e.getIndex() / columns);
			});

			button = new Button(null, box);
			button.getStyleClass().add("pin");
			button.setOnAction(mouseEvent -> getInfo().show(button));

			DoubleBinding scale = Bindings.createDoubleBinding(
					() -> 1.0 * Math.min(1 / viewport.scaleProperty.get(), 4) * Math.pow(pinCount.size(), -0.3),
					viewport.scaleProperty);

			return wrapGui(button, position, scale, viewport);
		}

		@Override
		protected PopOver initInfo() {
			PopOver info = super.initInfo();
			GridPane popContent = new GridPane();
			popContent.getStyleClass().add("grid");

			/* Image+Text for the popover */
			StreamUtils.zipWithIndex(pinCount.entrySet().stream()).forEach(e -> {
				ImageView img = new ImageView(e.getValue().getKey().image);
				img.setSmooth(false);
				img.setPreserveRatio(true);
				Label label1 = new Label(e.getValue().getKey().toString(), img);
				img.fitHeightProperty().bind(Bindings.createDoubleBinding(() -> label1.getFont().getSize() * 1.3, label1.fontProperty()));
				popContent.add(label1, 0, (int) e.getIndex());
				Label label2 = new Label(String.format("%dx", e.getValue().getValue()));
				popContent.add(label2, 1, (int) e.getIndex());
			});

			info.setContentNode(popContent);
			return info;
		}
	}

	/** Convert a {@link LevelMetadata} object containing information retrieved directly from the world to (static) pins. */
	public static Set<Pin> convertStatic(LevelMetadata pin, ScheduledExecutorService backgroundThread, DisplayViewport viewport) {
		Set<Pin> pins = new HashSet<>();
		for (LevelMetadata.PlayerPin player : pin.getPlayers().orElse(Collections.emptyList())) {
			pins.add(new PlayerPin(player, backgroundThread, viewport));
			if (player.getSpawnpoint().isPresent())
				pins.add(new PlayerSpawnpointPin(player, viewport));
		}
		for (LevelMetadata.VillageObjectPin villageObject : pin.getVillageObjects().orElse(Collections.emptyList())) {
			if (PinType.VILLAGE_MAPPING.containsKey(villageObject.getType())) {
				pins.add(new VillageObjectPin(villageObject, viewport));
			} else {
				log.warn("Invalid type for village pin: '" + villageObject.getType() + "' at position " + villageObject.getPosition()
						+ ". Allowed values: " + PinType.VILLAGE_MAPPING.keySet());
			}
		}

		/* Cluster maps at identical position to merge their pins. */
		pins.addAll(pin.getMaps().map(List::stream).orElse(Stream.empty())
				.collect(Collectors.groupingBy(map -> map.getPosition()))
				.entrySet()
				.stream()
				.map(e -> new MapPin(new Vector2d(e.getKey().x(), e.getKey().y()), e.getValue(), viewport))
				.collect(Collectors.toList()));
		/* All banner pins of the maps */
		pins.addAll(pin.getMaps().map(List::stream).orElse(Stream.empty())
				.flatMap(map -> map.getBanners().map(List::stream).orElse(Stream.empty()))
				.map(banner -> new Pin(new Vector2d(banner.getPosition().x(), banner.getPosition().y()), PinType.MAP_BANNER, viewport))
				.collect(Collectors.toList()));

		pin.getWorldSpawn().map(spawn -> new WorldSpawnPin(spawn.getSpawnpoint(), viewport)).ifPresent(pins::add);

		return pins;
	}

	/** Convert the {@link ChunkMetadata} that was generated while rendering into (dynamic) pins. */
	public static List<Pin> convertDynamic(Map<Vector2ic, ChunkMetadata> metadataMap, DisplayViewport viewport) {
		List<Pin> pins = new ArrayList<>();
		Set<Vector2ic> unfinishedChunks = new HashSet<>();
		List<String> chunkGeneration = new ArrayList<>();
		Map<Vector2ic, ChunkMetadataVersion> oldChunks = new HashMap<>();
		Map<Vector2ic, ChunkMetadataFailed> failedChunks = new HashMap<>();
		for (ChunkMetadata metadata : metadataMap.values()) {
			metadata.visit(new ChunkMetadataVisitor<Void>() {

				@Override
				public Void rendered(ChunkMetadataRendered metadata) {
					if (!ChunkMetadataRendered.STATUS_FINISHED.contains(metadata.generationStatus)) {
						unfinishedChunks.add(metadata.position);
						chunkGeneration.add(metadata.generationStatus);
					}
					return null;
				}

				@Override
				public Void failed(ChunkMetadataFailed metadata) {
					failedChunks.put(metadata.position, metadata);
					return null;
				}

				@Override
				public Void culled(ChunkMetadataCulled metadata) {
					return null;
				}

				@Override
				public Void version(ChunkMetadataVersion metadata) {
					oldChunks.put(metadata.position, metadata);
					return null;
				}
			});
		}

		for (Set<Vector2ic> chunks : splitChunks(unfinishedChunks)) {
			Vector2dc center = chunks.stream().map(v -> new Vector2d(v.x() * 16.0 + 8, v.y() * 16.0 + 8)).collect(Vector2d::new, Vector2d::add,
					Vector2d::add).mul(1.0 / chunks.size());
			pins.add(new UnfinishedChunkPin(center, outlineSet(chunks), chunkGeneration, viewport));
		}
		for (Map<Vector2ic, ChunkMetadataFailed> chunks : splitChunks(failedChunks)) {
			Vector2dc center = chunks.keySet().stream().map(v -> new Vector2d(v.x() * 16.0 + 8, v.y() * 16.0 + 8)).collect(Vector2d::new, Vector2d::add,
					Vector2d::add).mul(1.0 / chunks.size());
			pins.add(new FailedChunkPin(center, outlineSet(chunks.keySet()), chunks.values(), viewport));
		}
		for (Map<Vector2ic, ChunkMetadataVersion> chunks : splitChunks(oldChunks)) {
			Vector2dc center = chunks.keySet().stream().map(v -> new Vector2d(v.x() * 16.0 + 8, v.y() * 16.0 + 8)).collect(Vector2d::new, Vector2d::add,
					Vector2d::add).mul(1.0 / chunks.size());
			pins.add(new OldChunkPin(center, outlineSet(chunks.keySet()), chunks.values(), viewport));
		}

		/*
		 * Retrieve all structures from all chunks as <name, position> tuples. Map the name to the appropriate PinType through lookup table. Create
		 * a pin from it and add it to the list.
		 */
		pins.addAll(metadataMap.values().stream()
				.filter(m -> m instanceof ChunkMetadataRendered)
				.map(m -> (ChunkMetadataRendered) m)
				.flatMap(m -> m.structures.entrySet().stream())
				.filter(e -> {
					if (PinType.STRUCTURE_TYPES.containsKey(e.getKey()))
						return true;
					else {
						log.warn("Could not parse structure id " + e.getKey());
						return false;
					}
				})
				.map(e -> new Pin(new Vector2d(e.getValue().x(), e.getValue().z()), PinType.STRUCTURE_TYPES.get(e.getKey()), viewport))
				.collect(Collectors.toList()));
		return pins;
	}

	/** {@link #wrapGui(Node, Vector2dc, DoubleBinding, DisplayViewport)} with a default value for the scaling. */
	static StackPane wrapGui(Node node, Vector2dc position, DisplayViewport viewport) {
		return wrapGui(node, position, Bindings.createDoubleBinding(
				() -> 2 * Math.min(1 / viewport.scaleProperty.get(), 1),
				viewport.scaleProperty), viewport);
	}

	/**
	 * Wrap a given Node (e.g. a Pin button) in a {@link StackPane} to be placed on to the map. Applies a scaling function (used to scales it
	 * relative to the current zoom factor) and translations (to keep it at a given world space position and centered).
	 */
	static StackPane wrapGui(Node node, Vector2dc position, DoubleBinding scale, DisplayViewport viewport) {
		StackPane stack = new StackPane(node);
		Translate center = new Translate();
		center.xProperty().bind(stack.widthProperty().multiply(-0.5));
		center.yProperty().bind(stack.heightProperty().multiply(-0.5));
		if (position != null)
			stack.getTransforms().add(new Translate(position.x(), position.y()));
		if (scale != null) {
			Scale scaleTransform = new Scale();
			scaleTransform.xProperty().bind(scale);
			scaleTransform.yProperty().bind(scale);
			stack.getTransforms().add(scaleTransform);
		}
		stack.getTransforms().add(center);
		stack.setVisible(false);
		stack.setPickOnBounds(false);
		stack.setOpacity(0.0);
		return stack;
	}

	/**
	 * Takes in a set of chunk positions and identifies all connected subsets.
	 * 
	 * @param chunks
	 *            A set of chunk positions. This will be emptied during the calculation.
	 */
	private static List<Set<Vector2ic>> splitChunks(Set<Vector2ic> chunks) {
		List<Set<Vector2ic>> islands = new ArrayList<>();
		while (!chunks.isEmpty()) {
			Set<Vector2ic> done = new HashSet<>();
			Queue<Vector2ic> todo = new LinkedList<>();
			todo.add(chunks.iterator().next());
			chunks.remove(todo.element());
			while (!todo.isEmpty()) {
				Vector2ic current = todo.remove();
				for (Vector2i neighbor : new Vector2i[] { new Vector2i(-1, 0), new Vector2i(1, 0), new Vector2i(0, -1), new Vector2i(0, 1) }) {
					neighbor.add(current);
					if (chunks.remove(neighbor))
						todo.add(neighbor);
				}
				done.add(current);
			}
			islands.add(done);
		}
		return islands;
	}

	/**
	 * Takes in a map of chunk positions and identifies all connected subsets.
	 * 
	 * @param chunks
	 *            A map of chunk positions. This will be emptied during the calculation.
	 */
	private static <T> List<Map<Vector2ic, T>> splitChunks(Map<Vector2ic, T> chunks) {
		List<Map<Vector2ic, T>> islands = new ArrayList<>();
		while (!chunks.isEmpty()) {
			Map<Vector2ic, T> done = new HashMap<>();
			Queue<Vector2ic> todo = new LinkedList<>();
			Queue<T> todoV = new LinkedList<>();
			todo.add(chunks.entrySet().iterator().next().getKey());
			todoV.add(chunks.entrySet().iterator().next().getValue());
			chunks.remove(todo.element());
			while (!todo.isEmpty()) {
				Vector2ic current = todo.remove();
				T val = todoV.remove();
				for (Vector2i neighbor : new Vector2i[] { new Vector2i(-1, 0), new Vector2i(1, 0), new Vector2i(0, -1), new Vector2i(0, 1) }) {
					neighbor.add(current);
					if (chunks.containsKey(neighbor)) {
						todoV.add(chunks.remove(neighbor));
						todo.add(neighbor);
					}
				}
				done.put(current, val);
			}
			islands.add(done);
		}
		return islands;
	}

	/**
	 * This method takes in a set of four-connected chunk coordinates (as {@link #splitChunks(Set)} would output) and calculates its outline.
	 * The outline is a set of vertices that when taken together as a polygon result in a shape exactly covering the input set. The input may
	 * have concavities, but no enclaves.<br />
	 * 
	 * The algorithm starts by picking out a random point on the set. It then looks at the 2×2 coordinates next to it (where the current
	 * position is at the bottom-right corner of that sample) and depending on which of these pixels are part of the input set the position will
	 * be moved. This way, the algorithm will walk the outline of the set clock-wise and output a vertex at each direction change.
	 * <ul>
	 * <li>If all four pixels are set (which is mostly the case at the beginning and only at the beginning), move to the left.</li>
	 * <li>If the left half is outside and the right half is inside of the set, we are at the left side of the set and move to the top.</li>
	 * <li>Similarly, move to the right/bottom/left if the top/right/bottom half of the current sample is set.</li>
	 * <li>Similar rules apply when the sample covers three or only one points of the input.</li>
	 * <li>If two pixels are set, but they are placed diagonally to each other, <i>turn</i> right.</li>
	 * <li>If the current direction is not equal to the last one, output a vertex.</li>
	 * <li>If the current position is equal to the first discovered vertex of the polygon, terminate.</li>
	 * </ul>
	 * <br/>
	 * The sample is stored in the four lowest bits of an integer. Each step's calculation is done in a switch-case over all 16 possible
	 * samples. Then, the position is updated based on the current move direction and the sample is updated based on the new position. The
	 * sample is updated using bit magic, so (except for the start) only two queries are made to the input set per move.
	 */
	private static List<Vector2ic> outlineSet(Set<Vector2ic> chunks) {
		if (chunks.isEmpty())
			return Collections.emptyList();
		Vector2i pos = new Vector2i(chunks.iterator().next());
		/* O O <- bit 4, 3 */
		/* O X <- bit 2, 1 | X: Current position */
		int sample = 0;
		/* top-left */
		if (chunks.contains(new Vector2i(0, 0).add(pos)))
			sample |= 0b1000;
		/* top-right */
		if (chunks.contains(new Vector2i(1, 0).add(pos)))
			sample |= 0b0100;
		/* bottom-left */
		if (chunks.contains(new Vector2i(0, 1).add(pos)))
			sample |= 0b0010;
		/* bottom-right */
		if (chunks.contains(new Vector2i(1, 1).add(pos)))
			sample |= 0b0001;
		int direction = 0;

		ArrayList<Vector2ic> outline = new ArrayList<>();
		while (outline.isEmpty() || !pos.equals(outline.get(0))) {

			/* To which pixel to move next? */
			switch (sample) {
			/* Move right */
			case 0b0001:
			case 0b1011:
				outline.add(new Vector2i(pos));
			case 0b0011:
			case 0b1111:
				direction = 0;
				break;
			/* Move down */
			case 0b0010:
			case 0b1110:
				outline.add(new Vector2i(pos));
			case 0b1010:
				direction = 1;
				break;
			/* Move left */
			case 0b1000:
			case 0b1101:
				outline.add(new Vector2i(pos));
			case 0b1100:
				direction = 2;
				break;
			/* Move up */
			case 0b0100:
			case 0b0111:
				outline.add(new Vector2i(pos));
			case 0b0101:
				direction = 3;
				break;
			/* Ambiguous: Turn 90° CW from previous */
			case 0b1001:
			case 0b0110:
				direction = (direction + 1) & 3;
				break;

			default:
				throw new InternalError("Invalid state while finding the outline. Either the input is not valid or something is seriously wrong.");
			}

			/* Move to the next pixel and sample at the new position */
			switch (direction) {
			/* Move right */
			case 0:
				pos.add(1, 0);
				sample = (sample << 1) & 0b1010;

				/* top-right */
				if (chunks.contains(new Vector2i(1, 0).add(pos)))
					sample |= 0b0100;
				/* bottom-right */
				if (chunks.contains(new Vector2i(1, 1).add(pos)))
					sample |= 0b0001;
				break;

			/* Move down */
			case 1:
				pos.add(0, 1);
				sample = (sample << 2) & 0b1100;

				/* bottom-left */
				if (chunks.contains(new Vector2i(0, 1).add(pos)))
					sample |= 0b0010;
				/* bottom-right */
				if (chunks.contains(new Vector2i(1, 1).add(pos)))
					sample |= 0b0001;
				break;

			/* Move left */
			case 2:
				pos.add(-1, 0);
				sample = (sample >>> 1) & 0b0101;

				/* top-left */
				if (chunks.contains(new Vector2i(0, 0).add(pos)))
					sample |= 0b1000;
				/* bottom-left */
				if (chunks.contains(new Vector2i(0, 1).add(pos)))
					sample |= 0b0010;
				break;

			/* Move up */
			case 3:
				pos.add(0, -1);
				sample = (sample >>> 2) & 0b0011;

				/* top-left */
				if (chunks.contains(new Vector2i(0, 0).add(pos)))
					sample |= 0b1000;
				/* top-right */
				if (chunks.contains(new Vector2i(1, 0).add(pos)))
					sample |= 0b0100;
				break;
			}
		}

		return outline;
	}

	private static final double			EPSILON				= 1e-12;
	/**
	 * Modification of the {@link Interpolator#DISCRETE} interpolation. Instead of jumping to 1 at the end of the animation, this one does so at
	 * the beginning.
	 */
	private static final Interpolator	DISCRETE_INSTANT	= new Interpolator() {
																@Override
																protected double curve(double t) {
																	return (t < EPSILON) ? 0.0 : 1.0;
																}

																@Override
																public String toString() {
																	return "Interpolator.DISCRETE_INSTANT";
																}
															};
}