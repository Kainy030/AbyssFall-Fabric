/*
 * Copyright (C) 2026 Kainy
 *
 * This file is part of AbyssFall.
 *
 * AbyssFall is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AbyssFall is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AbyssFall.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.abyssfall.item;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import com.abyssfall.AbyssFall;

/**
 * Registry holder for every item the mod adds.
 */
public final class AbyssFallItems {
	/**
	 * Translation key of the word standing in for the Final Death Omen's damage figure
	 * ("深渊" / "Abyss").
	 *
	 * <p>Public because the client recolours this exact segment every frame and has to be able to
	 * recognise it. Matching on the key rather than on the rendered text means the animation works
	 * in every language without a list of translations to keep in step.
	 */
	public static final String ABYSS_WORD_KEY = "item.abyssfall.final_death_omen.abyss";

	/**
	 * The word's colour when nothing is animating it — the midpoint of the range the client cycles
	 * through, so a still tooltip looks like a frozen frame of the animation rather than a
	 * different design.
	 */
	private static final int ABYSS_WORD_REST_COLOR = 0x4A4A4A;

	/**
	 * Translation key of the word standing in for the Sword of the Cosmos's damage figure
	 * ("无限" / "Infinity").
	 *
	 * <p>Public for the same reason as {@link #ABYSS_WORD_KEY}: the client finds the segment by key
	 * so the animation follows the translation rather than a list of literals. A key of its own,
	 * rather than sharing the Abyss word's, because the two words differ and because the client
	 * colours them differently — greys for one, hues for the other. The key is what tells them apart.
	 */
	public static final String INFINITY_WORD_KEY = "item.abyssfall.fake_infinity_sword.infinity";

	/**
	 * The Infinity word's colour when nothing is animating it.
	 *
	 * <p>Chosen the same way {@link #ABYSS_WORD_REST_COLOR} was — a still frame of the animation
	 * rather than a different design. This word cycles through hues, so its midpoint is not a grey
	 * but a colour: the value {@code Mth.hsvToRgb(0.5F, 0.8F, 1.0F)} actually returns at the
	 * client's saturation and value, which is the cyan halfway round the wheel. Read off the real
	 * call rather than guessed, so a still tooltip and a moving one agree.
	 */
	private static final int INFINITY_WORD_REST_COLOR = 0x32FFFF;

	/**
	 * Placeholder item. It has no behaviour yet and exists so the registry, the creative
	 * tab and the resource pipeline can be exercised end to end.
	 *
	 * <p>Uses {@link Rarity#EPIC}, the highest rarity vanilla provides.
	 */
	public static final Item ABYSS_FLOWER = register("abyss_flower", Item::new,
			new Item.Properties().rarity(Rarity.EPIC));

	/**
	 * Cognition Lens — switches the San readout between the icon row and the percentage bar.
	 *
	 * <p>Registered here rather than in {@code AbyssFallDevInventory} even though it was built from
	 * the San Counter, because it is player-facing content: it reveals nothing the design wants
	 * hidden, only changing how an already-visible reading is drawn. Stacks to one, since a second
	 * copy would do nothing a first cannot.
	 *
	 * <p>Uses {@link Rarity#EPIC}, the highest rarity vanilla provides.
	 */
	public static final Item SAN_LENS = register("san_lens", SanLensItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

	/**
	 * Gold Lens — a mirror with nothing looking back out of it.
	 *
	 * <p>No behaviour at all, and a plain {@link Item} rather than a subclass, because there is
	 * nothing yet for a subclass to do. It shares the Cognition Lens's frame and glass and differs
	 * only in having no eye, which is the whole of what it currently says: the same object, before
	 * or after whatever it is that looks through the other one.
	 *
	 * <p>Stacks to one, like the Cognition Lens. Not for that item's reason — a second copy of this
	 * one would be no less useful than the first, since neither does anything — but because the two
	 * are the same object at different moments and a pile of framed mirrors is not the register the
	 * item is written in.
	 */
	public static final Item GOLD_LENS = register("gold_lens", Item::new,
			new Item.Properties().stacksTo(1));

	/**
	 * Final Death Omen — the endgame blade.
	 *
	 * <p>Built from {@code sword(NETHERITE, ...)} for everything a sword ought to have: durability,
	 * the sweep and cobweb mining rules, the sword item tag behaviours, netherite's repair
	 * material. Two of the resulting components are then deliberately replaced.
	 *
	 * <p>The attack damage attribute is {@link Float#MAX_VALUE}, and its tooltip line is replaced
	 * with a fixed phrase rather than the number. Vanilla would print the value in full — a
	 * thirty-nine digit wall of green that says nothing except that a float was overflowed, and
	 * that dominates the item so thoroughly there is nothing else left to look at. The figure is
	 * kept because it is the largest damage the game has a way of meaning: vanilla uses this same
	 * constant in {@code LivingEntity#kill} and clamps any infinity down to it inside
	 * {@code hurtServer}. What is shown instead is what the number is trying to say.
	 *
	 * <p>No attack speed modifier at all. Every other weapon needs one because a cooldown decides
	 * how much of its damage lands; this one deals whatever the target has left, so a wind-up
	 * would only govern how often the blade may be told to do the one thing it does. Leaving the
	 * attribute out means it stays at the player's base value — the same rate as an empty hand,
	 * with no recovery to sit through.
	 *
	 * <p>Enchantability is set to zero, which removes the component
	 * {@code sword(...)} adds and leaves the blade unenchantable at a table. Every enchantment
	 * worth putting on a sword modifies part of the damage pipeline this weapon steps around, so
	 * they would be promises the item cannot keep. See {@link FinalDeathOmen} for what does happen
	 * when it connects.
	 *
	 * <p>Its rarity is {@link AbyssFallRarity#ABYSSAL}, so its name drifts through greys — a wave
	 * running along it one character at a time — in tooltips and in the held-item popup alike. The
	 * {@code Rarity.EPIC} beneath that is the fallback for anywhere neither of those reaches. See
	 * {@link AbyssFallRarity} for why the two coexist rather than one replacing the other.
	 */
	public static final Item FINAL_DEATH_OMEN = AbyssFallRarity.assign(
			register("final_death_omen", Item::new,
					new Item.Properties()
							.sword(ToolMaterial.NETHERITE, 3.0F, -2.4F)
							.fireResistant()
							.rarity(Rarity.EPIC)
							.attributes(deathOmenAttributes())
							.component(DataComponents.ENCHANTABLE, null)),
			AbyssFallRarity.ABYSSAL);

	/**
	 * Sword of the Cosmos — a sword that is only the sky it is made of.
	 *
	 * <p>The first item to exist purely as a consumer of the shader system. It contributes nothing
	 * to a fight: its attack damage modifier is zero and it declares no attack speed at all. What it
	 * has is the starfield, stated for it in {@code AbyssFallShader.json} exactly as the Final Death
	 * Omen's is. The registry name says {@code fake} because that is what it is — the appearance of a
	 * legendary weapon with none of the weapon behind it.
	 *
	 * <p>The zero-amount damage modifier is deliberate and is not the same as omitting the
	 * attribute. Vanilla shows any modifier carrying {@code BASE_ATTACK_DAMAGE_ID} as the total
	 * including the player's own base value ({@code ItemAttributeModifiers$Display$Default}, which
	 * takes that branch without looking at the amount), so there is a line to override. Leaving the
	 * attribute out entirely would remove the line, and the Infinity wording with it.
	 *
	 * <p>The attribute set is replaced wholesale rather than tuned through {@code sword(...)}'s
	 * baselines, for the same reason the Final Death Omen replaces its own: the baseline is not the
	 * final figure. {@code sword(material, damage, speed)} adds the material's
	 * {@code attackDamageBonus} to the damage baseline — four, for netherite — so asking for a
	 * modifier of zero through that argument would mean writing {@code -4.0F} and quietly depending
	 * on a constant belonging to a different class. Stating the modifier outright says what the item
	 * has.
	 *
	 * <p>⚠️ The two numbers still passed to {@code sword(...)} are therefore dead as far as the
	 * attributes go — {@link #cosmosSwordAttributes()} replaces whatever they produced. They are left
	 * as zeroes rather than removed because the call is still wanted for everything else it does:
	 * durability, repair material, enchantability, the sweep and the cobweb rules. There is no
	 * overload that supplies those without the two baselines.
	 *
	 * <p>Everything else is netherite's, unchanged — and, unlike the Final Death Omen, its
	 * enchantability. That blade cannot be enchanted because it steps around the pipeline every
	 * worthwhile enchantment modifies; this one goes through the pipeline like any sword, so the
	 * enchantments mean what they say.
	 *
	 * <p>Its rarity is {@link AbyssFallRarity#INFINITY}, so its name is drawn in vanilla's {@code §c}
	 * red in tooltips and in the held-item popup alike. The {@code Rarity.EPIC} beneath that is the
	 * fallback for anywhere neither of those reaches. See {@link AbyssFallRarity} for why the two
	 * coexist rather than one replacing the other.
	 */
	public static final Item FAKE_INFINITY_SWORD = AbyssFallRarity.assign(
			register("fake_infinity_sword", Item::new,
					new Item.Properties()
							.sword(ToolMaterial.NETHERITE, 0.0F, 0.0F)
							.fireResistant()
							.rarity(Rarity.EPIC)
							.attributes(cosmosSwordAttributes())),
			AbyssFallRarity.INFINITY);

	private AbyssFallItems() {
	}

	/**
	 * The blade's attribute set: an attack damage of {@link Float#MAX_VALUE}, shown as a phrase,
	 * and nothing else.
	 *
	 * <p>Written out rather than adjusted afterwards because {@code ItemAttributeModifiers} is
	 * immutable and {@code sword(...)} has already installed a complete set; replacing it wholesale
	 * is the only way to change what it contains.
	 *
	 * <p>The damage line uses {@code Display.override}, which is vanilla's own provision for an
	 * attribute whose number is not the point — no mixin and no tooltip event needed for the
	 * substitution itself, and the replacement travels with the component, so a stack copied into
	 * another inventory or sent to a client carries its own wording. Only the display is
	 * overridden: what the attribute actually contributes is untouched.
	 *
	 * <p>What is built here is the still version, coloured as vanilla would colour it. The word
	 * naming the Abyss is recoloured every frame on the client
	 * ({@code AbyssFallTooltips}); this component is what a server, a screenshot or any other
	 * reader without that animation sees, so it has to be legible on its own rather than a
	 * placeholder.
	 *
	 * <p>Reuses vanilla's {@code BASE_ATTACK_DAMAGE_ID} so the entry occupies the same slot in the
	 * layout a weapon's own damage always does, and so anything reading the modifier by id — the
	 * anvil, a comparison screen, another mod — finds it where it expects to.
	 */
	private static ItemAttributeModifiers deathOmenAttributes() {
		return ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE,
						new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, Float.MAX_VALUE,
								AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND,
						ItemAttributeModifiers.Display.override(
								damageLine(ABYSS_WORD_KEY, ABYSS_WORD_REST_COLOR)))
				.build();
	}

	/**
	 * The Sword of the Cosmos's attribute set: one entry, an attack damage modifier of zero.
	 *
	 * <p>Stated in full rather than derived from {@code sword(...)}'s baselines, because the baseline
	 * and the resulting modifier are not the same number: the material's {@code attackDamageBonus} is
	 * added to it. Writing the modifier here means the figure in this method is the figure the tooltip
	 * is computed from, with nothing in between.
	 *
	 * <p>Zero is a real value here, not a missing one. Vanilla's default display adds the player's own
	 * base attack damage to any modifier bearing {@code BASE_ATTACK_DAMAGE_ID}, so a zero modifier
	 * prints the player's bare figure. The display is overridden anyway, but the modifier has to exist
	 * for there to be a line to override.
	 *
	 * <p>🔴 <strong>No {@code ATTACK_SPEED} modifier at all — the entry is absent, not set to zero.</strong>
	 * {@code attributes(...)} replaces the whole set, so leaving it out means the player keeps their base
	 * swing speed and the tooltip has no speed line to show. This is what the Final Death Omen does, for a
	 * related reason: a speed figure is only meaningful when a cooldown decides how much damage lands, and
	 * this sword deals none. Setting {@code 0.0} instead would print {@code 4 攻击速度} — a claim about a
	 * weapon that does not fight.
	 *
	 * <p>The damage entry's display is overridden with the same kind of line the Final Death Omen
	 * uses, naming Infinity instead of the Abyss. The number is still zero and still real — the
	 * override changes what is printed, not what the attribute contributes.
	 */
	private static ItemAttributeModifiers cosmosSwordAttributes() {
		return ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE,
						new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 0.0,
								AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND,
						ItemAttributeModifiers.Display.override(
								damageLine(INFINITY_WORD_KEY, INFINITY_WORD_REST_COLOR)))
				.build();
	}

	/**
	 * A damage line reading {@code +<word> 攻击伤害} / {@code +<word> Attack Damage}.
	 *
	 * <p>Assembled from four pieces because they are not all the same kind of text. The leading
	 * space, the {@code +} and the attribute's name are vanilla's own furniture and are coloured
	 * the way vanilla colours them — {@code ATTACK_DAMAGE} is a {@code POSITIVE} attribute and this
	 * is an increase, so {@link Attribute#getStyle} yields blue. Only the word standing in for the
	 * number is ours, and only that word is styled differently.
	 *
	 * <p>Shaped after vanilla's {@code attribute.modifier.plus} line so it reads as the same kind
	 * of statement rather than as a note bolted on. The attribute name is looked up from the
	 * attribute itself, so it stays translated and stays correct if the name ever changes.
	 *
	 * <p>The {@code +} is a plain ASCII {@code U+002B}, deliberately, not the fullwidth {@code ＋}
	 * an IME would produce. It renders narrow and cross-like in Minecraft's default font, which is
	 * simply what that glyph looks like at this size.
	 *
	 * <p>Parameterised rather than duplicated once a second weapon wanted the same line with a
	 * different word: everything except those two values is what makes the line read as vanilla's,
	 * and two copies of it would be two places to keep that right.
	 *
	 * @param wordKey    translation key of the word replacing the number. The client recognises the
	 *                   segment by this key, so it is also what selects which animation the word gets
	 * @param restColor  the word's colour with nothing animating it
	 */
	private static MutableComponent damageLine(String wordKey, int restColor) {
		Style vanilla = Style.EMPTY.withColor(
				Attributes.ATTACK_DAMAGE.value().getStyle(true));

		return CommonComponents.space()
				.append(Component.literal("+").withStyle(vanilla))
				.append(Component.translatable(wordKey)
						.withStyle(Style.EMPTY.withColor(restColor)))
				.append(CommonComponents.SPACE)
				.append(Component.translatable(Attributes.ATTACK_DAMAGE.value().getDescriptionId())
						.withStyle(vanilla));
	}

	private static <T extends Item> T register(String name, Function<Item.Properties, T> itemFactory, Item.Properties properties) {
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, AbyssFall.id(name));

		T item = itemFactory.apply(properties.setId(itemKey));

		Registry.register(BuiltInRegistries.ITEM, itemKey, item);

		return item;
	}

	public static void initialize() {
		// Registration happens purely through the static initialiser above; this call exists
		// so the class is loaded from the mod initializer.
	}
}
