package link.infra.jumploader.specialcases;

import java.util.Collections;
import java.util.List;

public interface ClassBlacklist extends SpecialCase {
	boolean shouldBlacklistClass(String name);

	List<ClassBlacklist> BLACKLISTS = Collections.singletonList(new MixinHideModLauncherBlacklist());
}
