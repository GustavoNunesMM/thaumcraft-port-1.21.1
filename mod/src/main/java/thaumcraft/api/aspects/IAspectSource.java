package thaumcraft.api.aspects;

/** A container machines may draw essentia from (or push it into) wirelessly, like jars near an infusion altar. */
public interface IAspectSource extends IAspectContainer {
   boolean isBlocked();
}
