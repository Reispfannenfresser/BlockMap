package de.piegames.blockmap.renderer;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joml.Vector2ic;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import de.piegames.blockmap.MinecraftVersion;
import de.piegames.blockmap.color.BiomeColorMap.BiomeColor;
import de.piegames.blockmap.color.BlockColorMap;
import de.piegames.blockmap.color.BlockColorMap.BlockColor;
import de.piegames.blockmap.color.Color;
import de.piegames.blockmap.world.ChunkMetadata;
import de.piegames.blockmap.world.ChunkMetadata.ChunkMetadataFailed;
import de.piegames.blockmap.world.ChunkMetadata.ChunkMetadataRendered;
import de.piegames.nbt.CompoundMap;
import de.piegames.nbt.CompoundTag;
import de.piegames.nbt.ListTag;
import de.piegames.nbt.Tag;
import de.piegames.nbt.regionfile.Palette1_13;

/**
 * Use this class to transform a Minecraft region file into a top-down image view of it.
 * 
 * @author piegames
 */
class ChunkRenderer_1_14 extends ChunkRenderer {

	private static Log log = LogFactory.getLog(ChunkRenderer_1_14.class);

	public ChunkRenderer_1_14(RenderSettings settings) {
		super(MinecraftVersion.MC_1_14, settings);
	}

	@Override
	ChunkMetadata renderChunk(Vector2ic chunkPosRegion, Vector2ic chunkPosWorld, CompoundTag level, Color[] map, int[] height, String[] regionBiomes) {
		blockColors = settings.blockColors.get(version);

		try {
			/* Check chunk status */
			String generationStatus = level.getAsStringTag("Status").map(Tag::getValue).orElse("empty");
			if (ChunkMetadataRendered.STATUS_EMPTY.contains(generationStatus))
				return new ChunkMetadataRendered(chunkPosWorld, generationStatus);

			/* Load saved structures */
			Map<String, Vector3ic> structureCenters = new HashMap<>();

			level.getAsCompoundTag("Structures")
					.flatMap(t -> t.getAsCompoundTag("Starts"))
					.stream()
					.flatMap(tag -> tag.getValue().values().stream())
					.map(Tag::getAsCompoundTag)
					.forEach(structure -> {
						String id = structure.flatMap(t -> t.getStringValue("id")).orElse("INVALID");
						if (!id.equals("INVALID")) {
							int[] bb = structure.flatMap(t -> t.getIntArrayValue("BB")).get();
							Vector3i center = new Vector3i(bb[0], bb[1], bb[2]).add(bb[3], bb[4], bb[5]);
							// JOML has no Vector3i#div function, why?
							center.x /= 2;
							center.y /= 2;
							center.z /= 2;
							structureCenters.put(id, center);
						}
					});

			String[] biomes = level.getIntArrayValue("Biomes")
					/* For some curious reason, Minecraft sometimes saves the biomes as empty array. 'cause, why not? */
					.filter(b -> b.length > 0)
					.map(ints -> {
						var strings = new String[256];
						for (int i = 0; i < 256; i++) {
							strings[i] = Integer.toString(ints[i]);
						}
						return strings;
					})
					.orElse(new String[256]);

			/*
			 * The height of the lowest section that has already been loaded. Section are loaded lazily from top to bottom and this value gets decreased
			 * each time a new one has been loaded
			 */
			int lowestLoadedSection = 16;
			/* Null entries indicate a section full of air */
			BlockColor[][] loadedSections = new BlockColor[16][];

			/* Get the list of all sections and map them to their y coordinate using streams */
			Map<Byte, CompoundMap> sections = level.getAsListTag("Sections")
					.flatMap(ListTag::getAsCompoundTagList)
					.map(ListTag::getValue)
					.stream().flatMap(Collection::stream)
					.collect(Collectors.toMap(section -> section.getByteValue("Y").get(), Tag::getValue));

			/*
			 * Save the final color of this pixel. It starts with transparent and will be modified over time through overlay operations. The last color
			 * is saved with the amount of times it was present in a row. This way, overlaying the same color over and over again can be optimized into
			 * one operation with specialized alpha calculation.
			 */
			class ColorColumn {
				Color		color			= Color.TRANSPARENT;
				BlockColor	lastColor		= BlockColor.TRANSPARENT;
				String		biome;
				int			lastColorTimes	= 0;
				boolean		needStop		= false;

				ColorColumn(String biome) {
					this.biome = biome;
				}

				void putColor(BlockColor currentColor, int times) {
					// if (currentColor.equals(lastColor))
					if (currentColor == lastColor)
						lastColorTimes += times;
					else {
						Color color = lastColor.color;
						BiomeColor biomeColor = settings.biomeColors.getBiomeColor(biome);
						if (lastColor.isGrass)
							color = Color.multiplyRGB(color, biomeColor.grassColor);
						if (lastColor.isFoliage)
							color = Color.multiplyRGB(color, biomeColor.foliageColor);
						if (lastColor.isWater)
							color = Color.multiplyRGB(color, biomeColor.waterColor);
						this.color = Color.alphaUnder(this.color, color, lastColorTimes);
						lastColorTimes = times;
						lastColor = currentColor;
						if (currentColor.color.a > 0.9999)
							needStop = true;
					}
				}

				Color getFinal() {
					/*
					 * Due to the alpha optimizations, putColor will only update the color when that one changes. This means that color will never contain the
					 * latest results. Putting a different color (transparent here) will trigger it to apply the last remaining color. If the last color is
					 * already transparent, this will do nothing which doesn't matter since it wouldn't make any effect anyway.
					 */
					putColor(BlockColor.TRANSPARENT, 1);
					return color;
				}
			}

			boolean mayCull = (chunkPosWorld.x() << 4) < settings.minX
					|| (chunkPosWorld.x() << 4) + 16 > settings.maxX
					|| (chunkPosWorld.y() << 4) < settings.minZ
					|| (chunkPosWorld.y() << 4) + 16 > settings.maxZ;
			/* Traverse the chunk in YXZ order */
			for (byte z = 0; z < 16; z++)
				for (byte x = 0; x < 16; x++) {
					if (mayCull) {
						if (x + (chunkPosWorld.x() << 4) < settings.minX
								|| x + (chunkPosWorld.x() << 4) > settings.maxX
								|| z + (chunkPosWorld.y() << 4) < settings.minZ
								|| z + (chunkPosWorld.y() << 4) > settings.maxZ)
							continue;
					}

					/* xz index relative to the chunk */
					int xz = x | z << 4;
					regionBiomes[chunkPosRegion.x() << 4 | x | chunkPosRegion.y() << 13 | z << 9] = biomes[xz];

					/* Once the height calculation is completed (we found a non-translucent block), set this flag to stop searching. */
					boolean heightSet = false;
					/* If we discard all solid block until we hit a translucent one, we'll get a nice cave view effect */
					boolean discardTop = blockColors.isCaveView();
					ColorColumn color = new ColorColumn(biomes[xz & 0xFF]);
					height: for (byte s = 15; s >= 0; s--) {
						if ((s << 4) > settings.maxY)
							continue;
						if (s < lowestLoadedSection) {
							// log.debug("Loading section " + s);
							try {
								loadedSections[s] = renderSection(sections.get(s), blockColors);
							} catch (Exception e) {
								log.warn("Failed to render chunk (" + chunkPosRegion.x() + ", " + chunkPosRegion.y() + ") section " + s
										+ ". This is very likely because your chunk is corrupt. If possible, please verify it "
										+ "manually before sending a bug report.", e);
								return new ChunkMetadataFailed(chunkPosWorld, e);
							}
							lowestLoadedSection = s;
						}
						if (loadedSections[s] == null) {
							/* Sector is full of air */
							color.putColor(blockColors.getAirColor(), 16);
							discardTop = false;
							continue;
						}
						for (int y = 15; y >= 0; y--) {
							int h = s << 4 | y;
							if (h < settings.minY)
								break height;
							if (h > settings.maxY)
								continue;

							/* xzy index relative to the current section */
							int xzy = xz | y << 8;

							BlockColor colorData = loadedSections[s][xzy];
							if (discardTop && colorData.isTranslucent)
								discardTop = false;
							if (!discardTop && !colorData.isTranslucent && !heightSet) {
								height[chunkPosRegion.x() << 4 | x | chunkPosRegion.y() << 13 | z << 9] = h;
								heightSet = true;
							}

							if (!discardTop)
								color.putColor(colorData, 1);
							if (color.needStop)
								break height;
						}
					}
					map[chunkPosRegion.x() << 4 | x | chunkPosRegion.y() << 13 | z << 9] = color.getFinal();
				}
			return new ChunkMetadataRendered(chunkPosWorld, generationStatus, structureCenters);
		} catch (Exception e) {
			log.warn("Failed to render chunk (" + chunkPosRegion.x() + ", " + chunkPosRegion.y() + ")", e);
			return new ChunkMetadataFailed(chunkPosWorld, e);
		}
	}

	/**
	 * Takes in the NBT data for a section and returns an int[] containing the color of each block in that section. The returned array thus has
	 * a length of 16³=4096 items and the blocks are mapped to them in XZY order.
	 */
	private BlockColor[] renderSection(CompoundMap section, BlockColorMap blockColors) {
		/* Sometimes sections only contain "Y" and "SkyLight", but no "Palette"?! */
		if (section == null || !section.containsKey("Palette"))
			return null;

		/* Parse palette */
		List<BlockColor> palette = section.get("Palette")
				.getAsListTag()
				.flatMap(ListTag::getAsCompoundTagList)
				.map(Tag::getValue)
				.stream().flatMap(Collection::stream)
				.map(map -> blockColors.getBlockColor(
						map.getStringValue("Name").get(),
						() -> parseBlockState(map.getAsCompoundTag("Properties").get(), version.getBlockStates())))
				.collect(Collectors.toList());

		long[] blocks = section.get("BlockStates").getAsLongArrayTag().get().getValue();

		BlockColor[] ret = new BlockColor[4096];

		Palette1_13 p = new Palette1_13(blocks);
		for (int i = 0; i < 4096; i += 64) {
			long[] output = p.next64();
			for (int j = 0; j < 64; j++) {
				int b = (int) output[j];
				if (b >= palette.size()) {
					log.warn("Block " + i + " " + b + " was out of bounds, is this world corrupt?");
					continue;
				}
				ret[i + j] = palette.get(b);
			}
		}

		return ret;
	}
}
