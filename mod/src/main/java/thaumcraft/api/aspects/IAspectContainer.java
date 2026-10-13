package thaumcraft.api.aspects;

/** Something that stores essentia: jars, alembics, buffers... */
public interface IAspectContainer {
   AspectList getAspects();

   void setAspects(AspectList aspects);

   boolean doesContainerAccept(Aspect tag);

   /** Returns what could not be added. */
   int addToContainer(Aspect tag, int amount);

   boolean takeFromContainer(Aspect tag, int amount);

   boolean doesContainerContainAmount(Aspect tag, int amount);

   int containerContains(Aspect tag);
}
