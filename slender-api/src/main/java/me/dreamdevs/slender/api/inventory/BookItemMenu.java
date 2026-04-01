package me.dreamdevs.slender.api.inventory;

import lombok.Getter;
import me.dreamdevs.slender.api.events.ItemClickEvent;
import me.dreamdevs.slender.api.inventory.buttons.MenuItem;
import me.dreamdevs.slender.api.inventory.buttons.NextMenuItem;
import me.dreamdevs.slender.api.inventory.buttons.ReturnMenuItem;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BookItemMenu {

	private final Map<Integer, BookPage> pages;
	private final List<NumerableMenuItem> realItems;
	private final @Getter MenuItem[] optionsItem;
	private final BookItemMenu.BookPage mainPage;

	public BookItemMenu(String title, List<MenuItem> icons, MenuItem[] optionsItem, boolean showPage, boolean fitSize, NextMenuItem nextButton, ReturnMenuItem backButton) {
		this.pages = new HashMap<>();
		this.realItems = new ArrayList<>();
		this.optionsItem = optionsItem;

		AtomicInteger page = new AtomicInteger(0);
		AtomicInteger counter = new AtomicInteger(0);

		this.mainPage = new BookItemMenu.BookPage(title + (showPage ? " | Page 1" : ""), fitSize ? ItemMenu.Size.fit(icons.size()) : ItemMenu.Size.SIX_LINE, 0);

		icons.forEach(menuItem -> {
			if (counter.getAndIncrement() == mainPage.getSize().getSize()-9) {
				counter.set(0);
				page.incrementAndGet();
			}

			this.realItems.add(new NumerableMenuItem(menuItem, page.get()));
		});

		for(int x = 0; x < page.get(); ++x) {
			this.pages.put(x, new BookItemMenu.BookPage(title + (showPage ? " | Page " + (x + 2) : ""), fitSize ? ItemMenu.Size.fit(icons.size()) : ItemMenu.Size.SIX_LINE, x));
		}

		this.pages.values().parallelStream().forEach(bookPage -> {
			int slot = bookPage.getPageNumber()-1;
			int pageNumber = bookPage.getPageNumber();
			if (slot >= 0) {
				ItemMenu returnMenu = this.pages.get(slot);
				if(returnMenu != null) {
					ReturnMenuItem returnMenuItem = backButton != null ? backButton : new ReturnMenuItem(returnMenu);
					returnMenuItem.setReturnMenu(returnMenu);
					bookPage.setItem(this.mainPage.getSize().getSize() - 2, returnMenuItem);
				}
			} else {
				ReturnMenuItem rs = backButton != null ? backButton : new ReturnMenuItem(this.mainPage);
				rs.setReturnMenu(this.mainPage);
				bookPage.setItem(this.mainPage.getSize().getSize()-2, rs);
			}

			pageNumber++;
			ItemMenu nextMenu = this.pages.get(pageNumber);
			if (nextMenu != null) {
				NextMenuItem set = nextButton != null ? nextButton : new NextMenuItem(nextMenu);
				set.setNextMenu(nextMenu);
				bookPage.setItem(this.mainPage.getSize().getSize()-1, set);
			}
		});

		counter.set(mainPage.getSize().getSize()-9);

		Stream.of(optionsItem).forEach(menuItem -> {
			int slot = counter.getAndIncrement();
			if (slot < mainPage.getSize().getSize()-2) {
				this.mainPage.setItem(slot, menuItem);
				this.pages.values().parallelStream().forEach(bookPage -> bookPage.setItem(slot, menuItem));
			}
		});

		if (page.get() > 0) {
			NextMenuItem set = nextButton != null ? nextButton : new NextMenuItem(this.pages.get(0));
			set.setNextMenu(this.pages.get(0));
			this.mainPage.setItem(this.mainPage.getSize().getSize()-1, set);
		}

		List<BookItemMenu.BookPage> realPagesNumber = new ArrayList<>();
		realPagesNumber.add(this.mainPage);

		this.pages.values().parallelStream().forEach(realPagesNumber::add);

		counter.set(0);

		realPagesNumber.forEach(bookPage -> {
			counter.set(0);
			getItemsFromPage(bookPage.getPageNumber()).forEach(numerableMenuItem -> {
				int slot = counter.getAndIncrement();
				if (slot < bookPage.getSize().getSize() - 9) { // leave last row for navigation
					bookPage.setItem(slot, numerableMenuItem);
				}
			});
		});
	}

	public void setOptionItems(MenuItem[] newOptionsItem) {
		Stream.of(newOptionsItem).forEach(menuItem -> {
			int counter = getMainPage().getSize().getSize();
			if (counter < 52) {
				this.mainPage.setItem(counter, menuItem);

				this.pages.values().forEach(bookPage -> bookPage.setItem(counter, menuItem));
			}
		});
	}

	public BookItemMenu(String title, List<MenuItem> icons, boolean showPage, boolean fitSize) {
		this(title, icons, new MenuItem[0], showPage, fitSize, null, null);
	}

	private List<BookItemMenu.NumerableMenuItem> getItemsFromPage(int page) {
		return realItems.stream().filter(numerableMenuItem -> numerableMenuItem.getPage() == page).collect(Collectors.toList());
	}

	public void open(Player p) {
		this.mainPage.open(p);
	}

	public BookItemMenu.BookPage getMainPage() {
		return this.mainPage;
	}

	private static class BookPage extends ItemMenu {
		private final int number;

		public BookPage(String name, Size size, int pageNumber) {
			super(name, size);
			this.number = pageNumber;
		}

		public int getPageNumber() {
			return this.number;
		}
	}

	private static class NumerableMenuItem extends MenuItem {
		private final int page;
		private final MenuItem original;

		public NumerableMenuItem(MenuItem item, int page) {
			super(item.getDisplayName(), item.getIcon(), item.getLore().toArray(String[]::new));
			this.page = page;
			this.original = item;
		}

		public int getPage() {
			return this.page;
		}

		@Override
		public void onItemClick(ItemClickEvent event) {
			if (this.original != null) {
				this.original.onItemClick(event);
			}
		}
	}
}