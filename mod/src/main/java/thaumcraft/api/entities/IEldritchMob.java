package thaumcraft.api.entities;

/**
 * Marks creatures of the Outer Lands (eldritch guardians, wardens, constructs, crabs, inhabited zombies, mind spiders,
 * giant taintacles). They are immune to the curse focus' withering field, share a team and regenerate a void shield.
 *
 * <p>Creatures ported by other code that can't implement this interface can be listed in the entity type tag
 * {@code thaumcraft:eldritch} instead; use {@code EldritchHelper.isEldritch} to test for both.
 */
public interface IEldritchMob {
}
