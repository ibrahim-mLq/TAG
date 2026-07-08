package games.sushigo;

import core.actions.*;
import games.sushigo.actions.ChooseCard;
import games.sushigo.cards.SGCard;
import org.junit.*;
import java.util.*;
import java.util.stream.IntStream;
import static java.util.stream.Collectors.*;
import static org.junit.Assert.*;

public class testCardReveals {
    SGGameState state;
    SGForwardModel fm = new SGForwardModel();

    @Before public void setup() {
        SGParameters p = new SGParameters(); p.setRandomSeed(1234);
        state = new SGGameState(p, 4); fm.setup(state);
    }

    @Test public void testTwoDumplings() {
        IntStream.range(0, 3).forEach(i -> state.getPlayerHands().get(0).draw());
        IntStream.range(0, 3).forEach(i -> state.getPlayerHands().get(0).add(new SGCard(SGCard.SGCardType.Dumpling)));
        for (int i = 0; i < 8; i++) {
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            fm.next(state, (i % 4 != 0) ? actions.stream().map(a -> (ChooseCard) a)
                    .filter(a -> state.getPlayerHands().get(0).get(a.cardIdx).type == SGCard.SGCardType.Dumpling)
                    .findFirst().orElseThrow() : actions.get(0));
        }
        assertEquals(2, state.getPlayedCards().get(0).stream().filter(c -> c.type == SGCard.SGCardType.Dumpling).count());
        assertEquals(3, state.getGameScore(0), 0.001);
    }

    @Test public void testChopsticksAllowSecondPick() { runChopsticksTest(2); }

    @Test public void testTwoPlayersChopsticks() { runChopsticksTest(2, 3); }

    private void runChopsticksTest(int... pIds) {
        Set<Integer> players = IntStream.of(pIds).boxed().collect(toSet());
        // Setup: replace one card in each target player's hand with Chopsticks
        players.forEach(p -> { state.getPlayerHands().get(p).draw(); state.getPlayerHands().get(p).add(new SGCard(SGCard.SGCardType.Chopsticks)); });

        choose(Set.of()); // Everyone plays card 0; target players now have Chopsticks in front of them
        choose(players);  // Target players use their played Chopsticks to take two cards this turn

        for (int p : pIds) {
            assertTrue("Action should be in progress for extra pick", state.isActionInProgress());
            fm.next(state, fm.computeAvailableActions(state).get(0));
        }
        assertFalse("Action should be finished", state.isActionInProgress());
        IntStream.range(0, 4).forEach(i -> assertEquals(2, state.getPlayedCards().get(i).getSize()));
        assertEquals(List.of(0, 1, 2, 3), state.getCurrentSimultaneousPlayers());
    }

    private void choose(Set<Integer> chopsticksPlayers) {
        fm.next(state, new SimultaneousAction(IntStream.range(0, 4).boxed().collect(toMap(i -> i, i -> new ChooseCard(i, 0, chopsticksPlayers.contains(i))))));
    }
}
