package said.ahmad.javafx.tracker.app.pref;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import javafx.application.Platform;
import said.ahmad.javafx.tracker.app.DialogHelper;
import said.ahmad.javafx.tracker.controller.WelcomeController;
import said.ahmad.javafx.tracker.datatype.FavoriteView;
import said.ahmad.javafx.tracker.datatype.FavoriteView_OldV5_2;
import said.ahmad.javafx.tracker.datatype.SplitViewState;
import said.ahmad.javafx.tracker.datatype.UserContextMenu;
import said.ahmad.javafx.tracker.system.file.PathLayer;
import said.ahmad.javafx.tracker.system.file.PathLayerXMLConverter;
import said.ahmad.javafx.tracker.system.file.ftp.FTPPathLayer;
import said.ahmad.javafx.tracker.system.file.local.FilePathLayer;
import said.ahmad.javafx.util.CallBackToDo;

/**
 * ------------- Setting -----------------<br>
 * when adding a new setting To do:<br>
 * 0- Setting declaration in Setting + getters and setters <br>
 * 1- same declaration here with no modifiers (no static) <br>
 * 2- add Setting.getVar() in {@link SettingSaver#pullFromSetting} <br>
 * 3- add Setting.setVar() in {@link SettingSaver#pushToSetting} <br>
 * (if the setting have no null default value add: null check for part 3 <br>
 * i.e. check null status for no primitive types)<br>
 * Note primitive type takes default value as in java, int->0, boolean->false...<br>
 * ---- functional notes <br>
 * 4- if setting can be changed (usually the case) and UI don't access setting class directly like {@link PathLayer#getDateFormat()} <br>
 * add affectation in method {@link WelcomeController initializeSettingXmlRelated()},
 * that's called after committing changes to Setting UI or at initialization<br>
 */
@SuppressWarnings("deprecation")
class SettingSaver {
	private static File mSettingFileXML;
	static {
		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			mSettingFileXML = new File(System.getenv("APPDATA") + "\\Tracker Explorer\\TrackerExplorerSetting.xml");
		} else {
			mSettingFileXML = new File(System.getProperty("user.home") + "/Tracker Explorer/TrackerExplorerSetting.xml");
		}
	}
	// only write if successfully read
	private static boolean successfullyRead = true;
	private static SettingSaver toBeSaved = new SettingSaver();

	// just to save version that was used to generate XML
	private String version = Setting.getVersion();


	// favorite stuff
	private boolean restoreLastOpenedFavorite = true;
	private ArrayList<String> lastOpenedFavoriteTitle = new ArrayList<>();

	/** @since v5.2 */
	private ArrayList<FavoriteView> favoritesViews = new ArrayList<>();
	@XStreamOmitField
	private ArrayList<FavoriteView_OldV5_2> favoritesLocations;

	// misc stuff
	private FavoriteView lastOpenedView;

	private boolean notifyFilesChanges;
	private boolean showWindowOnTopWhenNotify;
	private String dateFormatPattern;
	private HashMap<String, ArrayList<String>> extensionGroups;
	private List<UserContextMenu> userContextMenus;

	public static void pushToSetting() {
		// favorite stuff
		Setting.setRestoreLastOpenedFavorite(toBeSaved.restoreLastOpenedFavorite);

		// just to keep backward compatibility
		if (toBeSaved.favoritesLocations != null) {
			// converting old favorite view to new favorite view
			Setting.getFavoritesViews().addAll(toBeSaved.favoritesLocations.stream().map(fav -> {
				return new FavoriteView(fav.getTitle(), fav.getLocations().stream()
						.map(loc -> new SplitViewState(loc).setAutoExpandRight(true)).collect(Collectors.toList()));
			}).collect(Collectors.toList()));
		}
		// replacement
		if (toBeSaved.favoritesViews != null) {
			Setting.getFavoritesViews().addAll(toBeSaved.favoritesViews);
		}

		Setting.setLastOpenedFavoriteTitle(toBeSaved.lastOpenedFavoriteTitle);

		// misc stuff
		Setting.setLastOpenedView(toBeSaved.lastOpenedView);

		Setting.setNotifyFilesChanges(toBeSaved.notifyFilesChanges);
		Setting.setShowWindowOnTopWhenNotify(toBeSaved.showWindowOnTopWhenNotify);
		if (toBeSaved.dateFormatPattern != null)
			Setting.setDateFormatPattern(toBeSaved.dateFormatPattern);
		if(toBeSaved.extensionGroups != null)
			Setting.setExtensionGroups(toBeSaved.extensionGroups);
		if(toBeSaved.userContextMenus != null)
			Setting.setUserContextMenus(toBeSaved.userContextMenus);
	}

	public static void pullFromSetting() {
		toBeSaved.version = Setting.getVersion();
		// favorite stuff
		toBeSaved.restoreLastOpenedFavorite = Setting.isRestoreLastOpenedFavorite();
		toBeSaved.favoritesLocations = null;
		toBeSaved.favoritesViews = new ArrayList<>(Setting.getFavoritesViews().getList().values());
		toBeSaved.lastOpenedFavoriteTitle = Setting.getLastOpenedFavoriteTitle();

		// misc Stuff
		toBeSaved.lastOpenedView = Setting.getLastOpenedView();

		toBeSaved.notifyFilesChanges = Setting.isNotifyFilesChanges();
		toBeSaved.showWindowOnTopWhenNotify = Setting.isShowWindowOnTopWhenNotify();
		toBeSaved.dateFormatPattern = Setting.getDateFormatPattern();
		toBeSaved.extensionGroups = Setting.getExtensionGroups();
		toBeSaved.userContextMenus = Setting.getUserContextMenus();
	}

	private static XStream getXStream() {
		XStream xstream = new XStream();

		xstream.alias("Tracker.Explorer.preferences", SettingSaver.class);

		xstream.alias("PathLayer", PathLayer.class);
		xstream.alias("LocalFile", FilePathLayer.class);
		xstream.alias("FTPFile", FTPPathLayer.class);

		// just to keep backward compatibility
		xstream.alias("FavoriteView", FavoriteView_OldV5_2.class);
		xstream.addImplicitCollection(FavoriteView_OldV5_2.class, "locations");

		// replacement
		xstream.alias("FavoriteViewStates", FavoriteView.class);
		xstream.addImplicitCollection(FavoriteView.class, "splitStates");

		xstream.alias("SplitViewState", SplitViewState.class);
		xstream.processAnnotations(SplitViewState.class);

		xstream.alias("UserContextMenu", UserContextMenu.class);
		xstream.processAnnotations(UserContextMenu.class);

		xstream.allowTypesByWildcard(new String[] { "said.ahmad.javafx.**" });
		xstream.ignoreUnknownElements();

		xstream.registerConverter(new PathLayerXMLConverter());
		return xstream;
	}

	public static void saveSetting() {
		// Saving data to file
		if (!successfullyRead) {
			return;
		}
		pullFromSetting();
		try {
			XStream xstream = getXStream();
			OutputStream outputStream = new FileOutputStream(mSettingFileXML);
			xstream.toXML(toBeSaved, outputStream);

		} catch (Exception e) { // catches ANY exception
			e.printStackTrace();
		}
	}

	/**
	 *
	 * @param onFinishLoadingPlatformRun list of action to do on finish<br>
	 *                                   Can be null
	 */
	public static void loadSetting(List<CallBackToDo> onFinishLoadingPlatformRun) {
		// reading data from file
		if (!mSettingFileXML.exists()) {
			callOnFinishLoadingPlatformRun(onFinishLoadingPlatformRun);
			return;
		}
		XStream xstream = getXStream();
		try {
			toBeSaved = (SettingSaver) xstream.fromXML(mSettingFileXML);
			pushToSetting();
		} catch (XStreamException e) {
			successfullyRead = false;
			Platform.runLater(() -> {
				successfullyRead = DialogHelper.showExpandableConfirmationDialog("Tracker Explorer",
						"Something went Wrong loading XML Setting...",
						"\nDo you want To Overwrite Setting Next Time?" + "\nWarning you will lose your setting!",
						"File located at\n\t" + mSettingFileXML + "\n" + ExceptionUtils.getStackTrace(e));
			});
			e.printStackTrace();
		}
		callOnFinishLoadingPlatformRun(onFinishLoadingPlatformRun);
	}

	private static void callOnFinishLoadingPlatformRun(List<CallBackToDo> onFinishLoadingPlatformRun) {
		if (onFinishLoadingPlatformRun != null) {
			Platform.runLater(() -> {
				onFinishLoadingPlatformRun.forEach(action -> {
					try {
						// call registered action independently
						action.call();
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
				onFinishLoadingPlatformRun.clear();
			});
		}
	}

	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}

}
