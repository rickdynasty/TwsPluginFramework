package com.example.plugindemo.activity.category;

import java.text.Collator;

import android.app.TwsActivity;
import android.os.Bundle;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.widget.SideBar;
import com.tencent.tws.assistant.widget.SideBar.OnTouchingLetterChangedListener;

public class SideBarActivity extends TwsActivity implements ListView.OnScrollListener {

	private ListView mListView;
	private SideBar mSideBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sidebar_widget);

		setTitle("测试共享控件SideBar");

		mListView = (ListView) findViewById(R.id.listview);
		mSideBar = (SideBar) findViewById(R.id.sidebar);
		mSideBar.setIsCSP(true);
		mSideBar.setOnTouchingLetterChangedListener(mLetterChangedListener);

		mListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mStrings));
		mSideBar.updateEntriesPropWithContentArray(mStrings);
		mSideBar.setHubbleNormalBgColor(0xFF60DBAA); // default is 0xFF000000
		mSideBar.setHubbleNonExistBgColor(0xFFc5c5c5); // default is 0xFFe5e5e5
		mSideBar.setHubbleNormalTextColor(0xFFFFFFFF); // default is 0xFFFFFFFF
		mSideBar.setHubbleNonExistTextColor(0xFFdddddd); // default is
															// 0xFFFFFFFF
		mSideBar.setNormalColor(0xFF101010); // default is 0xcc000000
		mSideBar.setNonExistColor(0xFFc5c5c5); // default is 0x4c000000
		mSideBar.setSelectedColor(0xff22b2b6); // default is 0xff22b2b6
		mSideBar.updateEntriesPropWithContentArray(mStrings);

		mListView.setOnScrollListener(this);

	}

	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		int index = getSectionForPosition(firstVisibleItem);
		mSideBar.updateCurrentIndex(index);
	}

	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}

	OnTouchingLetterChangedListener mLetterChangedListener = new OnTouchingLetterChangedListener() {
		@Override
		public void onTouchingLetterChanged(int letterIndex) {
		}

		@Override
		public void onTouchingLetterChanged(String touchIndexString) {
			int index = getPositionForSection(touchIndexString);
			mListView.setSelection(index);
		}

		@Override
		public void onTouchUp() {

		}
	};

	public int getPositionForSection(String s) {
		for (int i = 0; i < mStrings.length; i++) {
			char firstLetter = mStrings[i].charAt(0);
			if (compare(firstLetter + "", s) == 0) {
				return i;
			}
		}
		return -1;
	}

	public int getSectionForPosition(int position) {
		char firstLetter = mStrings[position].charAt(0);
		String[] DEFALUT_ENTRIES = (String[]) mSideBar.getSideBarEntries();
		for (int i = 0; i < DEFALUT_ENTRIES.length; i++) {
			if (compare(firstLetter + "", DEFALUT_ENTRIES[i]) == 0) {
				return i;
			}
		}
		return 0; // Don't recognize the letter - falls under zero'th section
	}

	protected int compare(String word, String letter) {
		final String firstLetter;
		if (word.length() == 0) {
			firstLetter = " ";
		} else {
			firstLetter = word.substring(0, 1);
		}
		Collator collator = java.text.Collator.getInstance();
		collator.setStrength(java.text.Collator.PRIMARY);
		return collator.compare(firstLetter, letter);
	}

	public static final String[] mStrings = { "Abbaye de Belloc", "Abbaye du Mont des Cats", "Abertam", "Abondance",
			"Ackawi", "Acorn", "Adelost", "Affidelice au Chablis", "Afuega'l Pitu", "Airag", "Airedale",
			"Caciocavallo", "Caciotta", "Caerphilly", "Cairnsmore", "Calenzana", "Cambazola", "Camembert de Normandie",
			"Canadian Cheddar", "Canestrato", "Cantal", "Caprice des Dieux", "Capricorn Goat", "Capriole Banon",
			"Carre de l'Est", "Casciotta di Urbino", "Cashel Blue", "Castellano", "Castelleno", "Castelmagno",
			"Castelo Branco", "Castigliano", "Cathelain", "Celtic Promise", "Cendre d'Olivet", "Cerney", "Chabichou",
			"Chabichou du Poitou", "Chabis de Gatine", "Double Gloucester", "Double Worcester", "Dreux a la Feuille",
			"Dry Jack", "Duddleswell", "Dunbarra", "Dunlop", "Dunsyre Blue", "Duroblando", "Durrus",
			"Dutch Mimolette (Commissiekaas)", "Edam", "Edelpilz", "Gammelost", "Gaperon a l'Ail", "Garrotxa",
			"Gastanberra", "Geitost", "Gippsland Blue", "Gjetost", "Gloucester", "Golden Cross", "Gorgonzola",
			"Gornyaltajski", "Gospel Green", "Gouda", "Goutu", "Gowrie", "Grabetto", "Graddost",
			"Grafton Village Cheddar", "Grana", "Grana Padano", "Grand Vatel", "Grataron d' Areches", "Gratte-Paille",
			"Graviera", "Greuilh", "Greve", "Gris de Lille", "Gruyere", "Gubbeen", "Guerbigny", "Halloumi",
			"Halloumy (Australian)", "Haloumi-Style Cheese", "Harbourne Blue", "Havarti", "Heidi Gruyere",
			"Hereford Hop", "Herrgardsost", "Herriot Farmhouse", "Herve", "Hipi Iti", "Hubbardston Blue Cow",
			"Hushallsost", "Iberico", "Idaho Goatster", "Idiazabal", "Il Boschetto al Tartufo", "Ile d'Yeu",
			"Isle of Mull", "Jarlsberg", "Jermi Tortes", "Jibneh Arabieh", "Jindi Brie", "Jubilee Blue", "Juustoleipa",
			"Kadchgall", "Kaseri", "Kashta", "Kefalotyri", "Kenafa", "Kernhem", "Kervella Affine", "Kikorangi",
			"King Island Cape Wickham Brie", "King River Gold", "Klosterkaese", "Knockalara", "Kugelkase",
			"L'Aveyronnais", "L'Ecir de l'Aubrac", "La Taupiniere", "La Vache Qui Rit", "Laguiole", "Lairobell",
			"Lajta", "Lanark Blue", "Lancashire", "Langres", "Lappi", "Laruns", "Lavistown", "Le Brin", "Le Fium Orbo",
			"Le Lacandou", "Le Roule", "Leafield", "Lebbene", "Leerdammer", "Leicester", "Leyden", "Limburger",
			"Lincolnshire Poacher", "Lingot Saint Bousquet d'Orb", "Liptauer", "Little Rydings", "Livarot",
			"Llanboidy", "Llanglofan Farmhouse", "Loch Arthur Farmhouse", "Loddiswell Avondale", "Longhorn",
			"Lou Palou", "Lou Pevre", "Lyonnais", "Maasdam", "Macconais", "Mahoe Aged Gouda", "Mahon", "Malvern",
			"Mamirolle", "Manchego", "Manouri", "Manur", "Marble Cheddar", "Marbled Cheeses", "Maredsous", "Margotin",
			"Maribo", "Maroilles", "Mascares", "Mascarpone", "Mascarpone (Australian)", "Mascarpone Torta", "Matocq",
			"Maytag Blue", "Meira", "Menallack Farmhouse", "Menonita", "Meredith Blue", "Mesost",
			"Metton (Cancoillotte)", "Meyer Vintage Gouda", "Mihalic Peynir", "Milleens", "Mimolette", "Mine-Gabhar",
			"Mini Baby Bells", "Mixte", "Molbo", "Monastery Cheeses", "Mondseer", "Mont D'or Lyonnais", "Montasio",
			"Monterey Jack", "Monterey Jack Dry", "Morbier", "Morbier Cru de Montagne", "Mothais a la Feuille",
			"Mozzarella", "Mozzarella (Australian)", "Mozzarella di Bufala", "Mozzarella Fresh, in water",
			"Mozzarella Rolls", "Munster", "Murol", "Mycella", "Myzithra", "Naboulsi", "Nantais", "Neufchatel",
			"Neufchatel (Australian)", "Niolo", "Nokkelost", "Northumberland", "Oaxaca", "Olde York", "Olivet au Foin",
			"Olivet Bleu", "Olivet Cendre", "Orkney Extra Mature Cheddar", "Orla", "Oschtjepka", "Ossau Fermier",
			"Ossau-Iraty", "Oszczypek", "Oxford Blue", "P'tit Berrichon", "Palet de Babligny", "Paneer", "Panela",
			"Pannerone", "Pant ys Gawn", "Parmesan (Parmigiano)", "Parmigiano Reggiano", "Pas de l'Escalette",
			"Passendale", "Pasteurized Processed", "Pate de Fromage", "Patefine Fort", "Pave d'Affinois",
			"Pave d'Auge", "Pave de Chirac", "Pave du Berry", "Pecorino", "Pecorino in Walnut Leaves",
			"Pecorino Romano", "Peekskill Pyramid", "Pelardon des Cevennes", "Pelardon des Corbieres", "Penamellera",
			"Penbryn", "Pencarreg", "Perail de Brebis", "Petit Morin", "Petit Pardou", "Petit-Suisse",
			"Picodon de Chevre", "Picos de Europa", "Piora", "Pithtviers au Foin", "Plateau de Herve",
			"Plymouth Cheese", "Podhalanski", "Poivre d'Ane", "Polkolbin", "Pont l'Eveque", "Port Nicholson",
			"Port-Salut", "Postel", "Pouligny-Saint-Pierre", "Zamorano", "Zanetti Grana Padano",
			"Zanetti Parmigiano Reggiano", "# Reggiano", "★ Zaneo" };
}
