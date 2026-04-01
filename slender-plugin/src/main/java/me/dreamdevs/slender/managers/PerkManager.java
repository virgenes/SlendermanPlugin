package me.dreamdevs.slender.managers;

import lombok.Getter;
import me.dreamdevs.slender.api.game.Role;
import me.dreamdevs.slender.api.game.perks.Perk;
import me.dreamdevs.slender.api.game.perks.PerkInfo;
import me.dreamdevs.slender.api.utils.Util;
import me.dreamdevs.slender.game.perks.*;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class PerkManager {

	private @Getter final List<Perk> perks;

	public PerkManager() {
		this.perks = new LinkedList<>();
		// Survivor perks
		registerPerk(Runaway.class);
		registerPerk(BetterTogether.class);
		registerPerk(Archaeologist.class);
		registerPerk(Resilience.class);
		registerPerk(Tracking.class);
		registerPerk(Echo.class);
		registerPerk(Spirit.class);
		registerPerk(PrayerSpeed.class);
		// Slender perks
		registerPerk(KillerInstinct.class);
		registerPerk(EndlessAgony.class);
		registerPerk(DarkAbyss.class);
		registerPerk(FromTheDark.class);
		registerPerk(PagesBelongings.class);
	}

	public void registerPerk(Class<? extends Perk> perkClass) {
		try {
			PerkInfo info = perkClass.getAnnotation(PerkInfo.class);
			Perk perk = perkClass.getConstructor().newInstance();
			this.perks.add(perk);
			Util.sendPluginMessage("&aRegistered perk "+info.name());
		} catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public List<Perk> getPerksByRole(Role role) {
		return perks.stream().filter(perk -> perk.getClass().getAnnotation(PerkInfo.class).role().equals(role))
				.collect(Collectors.toList());
	}

	public Perk getPerk(String name) {
		return perks.stream().filter(perk -> perk.getClass().getAnnotation(PerkInfo.class).name().equalsIgnoreCase(name))
				.findFirst().orElse(null);
	}

	public Runaway getRunaway() {
		return perks.stream().filter(p -> p instanceof Runaway).map(p -> (Runaway) p).findFirst().orElse(null);
	}

	public BetterTogether getBetterTogether() {
		return perks.stream().filter(p -> p instanceof BetterTogether).map(p -> (BetterTogether) p).findFirst().orElse(null);
	}

	public Archaeologist getArchaeologist() {
		return perks.stream().filter(p -> p instanceof Archaeologist).map(p -> (Archaeologist) p).findFirst().orElse(null);
	}

	public EndlessAgony getEndlessAgony() {
		return perks.stream().filter(p -> p instanceof EndlessAgony).map(p -> (EndlessAgony) p).findFirst().orElse(null);
	}

	public DarkAbyss getDarkAbyss() {
		return perks.stream().filter(p -> p instanceof DarkAbyss).map(p -> (DarkAbyss) p).findFirst().orElse(null);
	}

	public FromTheDark getFromTheDark() {
		return perks.stream().filter(p -> p instanceof FromTheDark).map(p -> (FromTheDark) p).findFirst().orElse(null);
	}

	public PagesBelongings getPagesBelongings() {
		return perks.stream().filter(p -> p instanceof PagesBelongings).map(p -> (PagesBelongings) p).findFirst().orElse(null);
	}

}
