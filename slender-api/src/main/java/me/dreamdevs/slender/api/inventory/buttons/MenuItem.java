package me.dreamdevs.slender.api.inventory.buttons;

import lombok.Getter;
import lombok.Setter;
import me.dreamdevs.slender.api.events.ItemClickEvent;
import me.dreamdevs.slender.api.utils.ColourUtil;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter @Setter
public class MenuItem {

	private String displayName;
	private ItemStack icon;
	private List<String> lore;

	public MenuItem(String displayName, ItemStack icon, String... lore) {
		this.displayName = displayName;
		this.icon = icon;
		this.lore = new ArrayList<>();
		this.lore.addAll(List.of(lore));
	}

	public ItemStack getFinalIcon() {
		return setNameAndLore(getIcon().clone(), getDisplayName(), getLore());
	}

	public ItemStack getFinalIcon(org.bukkit.entity.Player player) {
		return getFinalIcon();
	}

	public void onItemClick(ItemClickEvent event) {
		// Do nothing by default
	}

	public static ItemStack setNameAndLore(ItemStack itemStack, String displayName, List<String> lore) {
		ItemMeta meta = itemStack.getItemMeta();
		if (meta != null) {
			meta.displayName(ColourUtil.colorizeToComponent(displayName));
			if (lore != null) {
				meta.lore(lore.stream()
						.map(ColourUtil::colorizeToComponent)
						.collect(Collectors.toList()));
			}
			itemStack.setItemMeta(meta);
		}
		return itemStack;
	}
}