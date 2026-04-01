package me.dreamdevs.slender.api.inventory;

import java.util.Arrays;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;
import me.dreamdevs.slender.api.SlenderApi;
import me.dreamdevs.slender.api.events.ItemClickEvent;
import me.dreamdevs.slender.api.inventory.buttons.MenuItem;
import me.dreamdevs.slender.api.inventory.handlers.ItemMenuListener;
import me.dreamdevs.slender.api.utils.ColourUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * A Menu controlled by ItemStacks in an Inventory.
 */
public class ItemMenu {

	private @Getter Component name;
	private @Getter Size size;
	private @Getter MenuItem[] items;
	private @Getter @Setter ItemMenu parent;

	private @Getter Inventory inventory;

	public ItemMenu(String name, Size size, ItemMenu parent) {
		this.name = ColourUtil.colorizeToComponent(name);
		this.size = size;
		this.items = new MenuItem[size.getSize()];
		this.parent = parent;
	}

	public ItemMenu(String name, Size size)
	{
		this(name, size, null);
	}

	/**
	 * Checks if the {@link MenuItem} has a parent.
	 *
	 * @return True if the {@link MenuItem} has a
	 *         parent, else false.
	 */
	public boolean hasParent()
	{
		return parent != null;
	}

	public final ItemMenu setItem(int position, MenuItem menuItem) {
		items[position] = menuItem;
		return this;
	}

	public final ItemMenu clearItem(int position) {
		items[position] = null;
		return this;
	}

	public final ItemMenu clearAllItems() {
		Arrays.fill(items, null);
		return this;
	}

	/**
	 * Fills all empty slots in the {@link MenuItem}
	 * with a certain {@link MenuItem}.
	 *
	 * @param menuItem
	 *            The {@link MenuItem}.
	 * @return The {@link MenuItem}.
	 */
	public ItemMenu fillEmptySlots(MenuItem menuItem) {
		for (int i = 0; i < items.length; i++) {
			if (items[i] == null) {
				items[i] = menuItem;
			}
		}
		return this;
	}

	public void open(Player player) {
		if (!ItemMenuListener.getInstance().isRegistered(SlenderApi.plugin)) {
			ItemMenuListener.getInstance().register(SlenderApi.plugin);
		}
		inventory = Bukkit.createInventory(new ItemMenuHolder(this,
						Bukkit.createInventory(null, size.getSize())),
				size.getSize(), name);
		apply(inventory, player);
		player.openInventory(inventory);
	}

	public void update(Player player) {
		if (player.getOpenInventory() != null) {
			inventory = player.getOpenInventory().getTopInventory();
			if (inventory.getHolder() instanceof ItemMenuHolder
					&& ((ItemMenuHolder) inventory.getHolder()).getMenu()
					.equals(this)) {
				apply(inventory, player);
				player.updateInventory();
			}
		}
	}

	private void apply(Inventory inventory, Player player) {
		for (int i = 0; i < items.length; i++) {
			if (items[i] != null) {
				inventory.setItem(i, items[i].getFinalIcon(player));
			}
		}
	}

	public MenuItem getItem(int slot) {
		return slot >= 0 && slot <= size.size ? items[slot] : null;
	}

	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
			int slot = event.getRawSlot();
			if (slot >= 0 && slot < size.getSize() && items[slot] != null) {
				Player player = (Player) event.getWhoClicked();
				ItemClickEvent itemClickEvent = new ItemClickEvent(player, event.getCurrentItem(), event.getClick());
				items[slot].onItemClick(itemClickEvent);
				if (itemClickEvent.willUpdate()) {
					update(player);
				} else {
					player.updateInventory();
					if (itemClickEvent.willClose() || itemClickEvent.willGoBack()) {
						final String playerName = player.getName();
						Bukkit.getScheduler().scheduleSyncDelayedTask(SlenderApi.plugin, () -> {
									Player p = Bukkit.getPlayerExact(playerName);
									if (p != null) {
										p.closeInventory();
									}
								}, 1);
					}
					if (itemClickEvent.willGoBack() && hasParent()) {
						final String playerName = player.getName();
						Bukkit.getScheduler().scheduleSyncDelayedTask(SlenderApi.plugin, () -> {
									Player p = Bukkit.getPlayerExact(playerName);
									if (p != null) {
										parent.open(p);
									}
								}, 3);
					}
				}
			}
		}
	}

	public MenuItem[] getActualItems() {
		return Arrays.stream(items).filter(Objects::nonNull).toArray(MenuItem[]::new);
	}

	/**
	 * Destroys the {@link MenuItem}.
	 */
	public void destroy() {
		name = null;
		size = null;
		items = null;
		parent = null;
	}

	/**
	 * Possible sizes of an {@link MenuItem}.
	 */
	public enum Size {
		ONE_LINE(9), TWO_LINE(18), THREE_LINE(27), FOUR_LINE(36), FIVE_LINE(45), SIX_LINE(54);

		private final int size;

		private Size(int size)
		{
			this.size = size;
		}

		public static Size fit(int slots) {
			if (slots < 10) {
				return ONE_LINE;
			} else if (slots < 19) {
				return TWO_LINE;
			} else if (slots < 28) {
				return THREE_LINE;
			} else if (slots < 37) {
				return FOUR_LINE;
			} else if (slots < 46) {
				return FIVE_LINE;
			} else {
				return SIX_LINE;
			}
		}

		public int getSize() {
			return size;
		}
	}
}