package link.infra.jumploader.specialcases;

public interface ClassBlacklist extends SpecialCase {
	boolean shouldBlacklistClass(String name);
}
