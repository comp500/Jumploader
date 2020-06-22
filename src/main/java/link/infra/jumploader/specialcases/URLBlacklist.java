package link.infra.jumploader.specialcases;

import java.net.URL;

public interface URLBlacklist extends SpecialCase {
	boolean shouldBlacklistUrl(URL url);
}
