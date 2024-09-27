package said.ahmad.javafx.tracker.datatype;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class FavoriteView {
	private String title;
	private List<SplitViewState> splitStates;

	/** Empty constructor */
	public FavoriteView() {
		title = "";
		splitStates = new ArrayList<>();
	}

	public FavoriteView(String title, List<SplitViewState> splitStates) {
		this.title = title;
		this.splitStates = splitStates;
	}

	public FavoriteView(String title, SplitViewState... splitStates) {
		this.title = title;
		this.splitStates = new ArrayList<SplitViewState>(Arrays.asList(splitStates));
	}

	/**
	 * Do not use these split states in your view as it has Omitted fields and
	 * changing its internal fields is restricted
	 *
	 * @return the splitStates
	 * @see SplitViewState#SplitViewState(SplitViewState)
	 * @see #getSplitStatessInitializedCopy()
	 */
	public List<SplitViewState> getSplitStates() {
		return splitStates;
	}

	/**
	 *
	 * @return A copy of states after initializing omitted fields for each state
	 */
	public List<SplitViewState> getSplitStatessInitializedCopy() {
		return splitStates.stream().map(ss -> new SplitViewState(ss)).collect(Collectors.toList());
	}

	@Override
	public String toString() {
		return "FavoriteView [title=" + title + ", splitStates=" + splitStates + "]";
	}

}
