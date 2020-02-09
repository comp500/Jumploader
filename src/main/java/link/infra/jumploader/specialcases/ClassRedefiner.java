package link.infra.jumploader.specialcases;

public interface ClassRedefiner extends SpecialCase {
	boolean shouldRedefineClass(String name);
}
